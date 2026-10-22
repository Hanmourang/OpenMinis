import Foundation
import Network
import UIKit

/// Browses `_minis-desktop._tcp`, pairs over HTTP, and forwards shell commands.
@MainActor
final class DesktopConnectionManager: ObservableObject {
    static let shared = DesktopConnectionManager()
    static let serviceType = "_minis-desktop._tcp"
    static let defaultPort = 18765

    @Published private(set) var browsing = false
    @Published var connectingId: String?

    private let store: WorkEnvironmentStore
    private var browser: NWBrowser?
    private var pollTask: Task<Void, Never>?
    private let clientId: String
    private let clientName: String

    init(store: WorkEnvironmentStore = .shared) {
        self.store = store
        if let existing = UserDefaults.standard.string(forKey: "workEnvironment.clientId") {
            clientId = existing
        } else {
            let fresh = UUID().uuidString
            UserDefaults.standard.set(fresh, forKey: "workEnvironment.clientId")
            clientId = fresh
        }
        clientName = UIDevice.current.name
    }

    func start() {
        guard !browsing else { return }
        browsing = true
        startBrowser()
        pollTask = Task { [weak self] in
            while let self, !Task.isCancelled {
                await self.refreshPaired()
                try? await Task.sleep(nanoseconds: 8_000_000_000)
            }
        }
    }

    func stop() {
        browsing = false
        browser?.cancel()
        browser = nil
        pollTask?.cancel()
        pollTask = nil
    }

    func connect(id: String) async {
        guard let device = store.devices.first(where: { $0.id == id }) else { return }
        connectingId = id
        defer { connectingId = nil }
        do {
            if let token = device.token, !token.isEmpty,
               try await statusOK(device: device, token: token) {
                store.selectDevice(id: id)
                return
            }
            let paired = try await pair(device: device)
            store.applyPairing(id: paired.deviceId.isEmpty ? id : paired.deviceId,
                               token: paired.token,
                               name: paired.name,
                               os: paired.os)
        } catch {
            store.lastError = error.localizedDescription
        }
    }

    func addManual(hostPort: String) async {
        let parsed = Self.parseHostPort(hostPort)
        guard let parsed else {
            store.lastError = "Invalid host:port"
            return
        }
        do {
            let hello = try await hello(host: parsed.host, port: parsed.port)
            var device = DesktopDevice(
                id: hello.id.isEmpty ? "\(parsed.host):\(parsed.port)" : hello.id,
                name: hello.name.isEmpty ? parsed.host : hello.name,
                os: hello.os.isEmpty ? "desktop" : hello.os,
                host: parsed.host,
                port: parsed.port,
                online: true,
                paired: false,
                token: nil,
                lastSeen: Date()
            )
            store.upsertDiscovered(device)
            await connect(id: device.id)
        } catch {
            store.lastError = error.localizedDescription
        }
    }

    func execute(command: String, timeout: TimeInterval) async throws -> (output: String, exitCode: Int) {
        guard let device = store.selectedDevice, let token = device.token else {
            throw URLError(.userAuthenticationRequired)
        }
        var request = URLRequest(url: URL(string: "\(device.endpoint)/v1/exec")!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = timeout + 5
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "command": command,
            "timeout": Int(timeout),
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        let obj = try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:]
        let stdout = obj["stdout"] as? String ?? ""
        let stderr = obj["stderr"] as? String ?? ""
        let code = obj["exitCode"] as? Int ?? -1
        let output = stderr.isEmpty ? stdout : (stdout.isEmpty ? stderr : stdout + "\n" + stderr)
        return (output, code)
    }

    static func parseHostPort(_ raw: String) -> (host: String, port: Int)? {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }
        if let at = trimmed.lastIndex(of: ":"), at != trimmed.startIndex {
            let host = String(trimmed[..<at])
            let port = Int(trimmed[trimmed.index(after: at)...])
            if !host.isEmpty, let port, port > 0, port < 65536 {
                return (host, port)
            }
        }
        if trimmed.contains(".") || trimmed.contains(":") {
            return (trimmed, defaultPort)
        }
        return nil
    }

    // MARK: - Private

    private func startBrowser() {
        let params = NWParameters()
        params.includePeerToPeer = true
        let browser = NWBrowser(for: .bonjour(type: Self.serviceType, domain: nil), using: params)
        browser.stateUpdateHandler = { [weak self] state in
            if case .failed = state {
                Task { @MainActor in self?.restartBrowserSoon() }
            }
        }
        browser.browseResultsChangedHandler = { [weak self] results, _ in
            Task { @MainActor in
                self?.applyBrowseResults(results)
            }
        }
        browser.start(queue: .main)
        self.browser = browser
    }

    private func restartBrowserSoon() {
        browser?.cancel()
        browser = nil
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) { [weak self] in
            guard let self, self.browsing else { return }
            self.startBrowser()
        }
    }

    private func applyBrowseResults(_ results: Set<NWBrowser.Result>) {
        var seen = Set<String>()
        for result in results {
            guard case let .service(name, type, domain, interface) = result.endpoint else { continue }
            _ = type
            _ = domain
            _ = interface
            let endpoint = result.endpoint
            resolve(endpoint, advertisedName: name) { [weak self] device in
                guard let self, let device else { return }
                Task { @MainActor in
                    self.store.upsertDiscovered(device)
                    seen.insert(device.id)
                }
            }
        }
        // Offline marking happens in the poll loop so we don't flap on a single browse tick.
    }

    private func resolve(_ endpoint: NWEndpoint, advertisedName: String, completion: @escaping (DesktopDevice?) -> Void) {
        let conn = NWConnection(to: endpoint, using: .tcp)
        conn.stateUpdateHandler = { state in
            if case .ready = state {
                let host: String
                let port: Int
                if let inner = conn.currentPath?.remoteEndpoint, case let .hostPort(h, p) = inner {
                    host = "\(h)"
                    port = Int(p.rawValue)
                } else {
                    conn.cancel()
                    completion(nil)
                    return
                }
                conn.cancel()
                Task {
                    let hello = try? await self.hello(host: host, port: port)
                    let id = hello?.id.isEmpty == false ? hello!.id : "\(host):\(port)"
                    completion(DesktopDevice(
                        id: id,
                        name: hello?.name.isEmpty == false ? hello!.name : advertisedName,
                        os: hello?.os.isEmpty == false ? hello!.os : "desktop",
                        host: host,
                        port: port,
                        online: true,
                        paired: false,
                        token: nil,
                        lastSeen: Date()
                    ))
                }
            } else if case .failed = state {
                conn.cancel()
                completion(nil)
            }
        }
        conn.start(queue: .global())
    }

    private func refreshPaired() async {
        var online = Set<String>()
        for device in store.devices {
            do {
                let hello = try await hello(host: device.host, port: device.port)
                var copy = device
                copy.online = true
                copy.name = hello.name.isEmpty ? device.name : hello.name
                copy.os = hello.os.isEmpty ? device.os : hello.os
                if !hello.id.isEmpty { copy.id = hello.id }
                copy.lastSeen = Date()
                store.upsertDiscovered(copy)
                online.insert(copy.id)
            } catch {
                var copy = device
                copy.online = false
                store.upsertDiscovered(copy)
            }
        }
        store.markOffline(except: online.union(Set(store.devices.filter(\.online).map(\.id))))
    }

    private struct Hello: Decodable {
        var id: String
        var name: String
        var os: String
        var version: String?
        var allowDiscover: Bool?
        var paired: Bool?
    }

    private struct PairResponse: Decodable {
        var token: String
        var deviceId: String
        var name: String
        var os: String
    }

    private func hello(host: String, port: Int) async throws -> Hello {
        let url = URL(string: "http://\(host):\(port)/v1/hello")!
        var request = URLRequest(url: url)
        request.timeoutInterval = 4
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return try JSONDecoder().decode(Hello.self, from: data)
    }

    private func pair(device: DesktopDevice) async throws -> PairResponse {
        var request = URLRequest(url: URL(string: "\(device.endpoint)/v1/pair")!)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 8
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "clientId": clientId,
            "clientName": clientName,
        ])
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.cannotConnectToHost)
        }
        return try JSONDecoder().decode(PairResponse.self, from: data)
    }

    private func statusOK(device: DesktopDevice, token: String) async throws -> Bool {
        var request = URLRequest(url: URL(string: "\(device.endpoint)/v1/status")!)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 4
        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }
}
