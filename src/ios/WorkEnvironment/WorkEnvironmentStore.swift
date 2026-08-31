import Combine
import Foundation

/// Persisted work-environment selection plus the live desktop device list.
@MainActor
final class WorkEnvironmentStore: ObservableObject {
    static let shared = WorkEnvironmentStore()

    static let defaultDownloadURL = "https://openminis.app/desktop"
    private static let kindKey = "workEnvironment.kind"
    private static let selectedKey = "workEnvironment.selectedDeviceId"
    private static let devicesKey = "workEnvironment.pairedDevices"

    @Published private(set) var kind: WorkEnvironmentKind = .cloud
    @Published private(set) var selectedDeviceId: String?
    @Published private(set) var devices: [DesktopDevice] = []
    @Published var showPicker = false
    @Published var showGuide = false
    @Published var lastError: String?

    let downloadURL = WorkEnvironmentStore.defaultDownloadURL

    var selectedDevice: DesktopDevice? {
        guard let selectedDeviceId else { return nil }
        return devices.first { $0.id == selectedDeviceId }
    }

    var statusLabel: String {
        if kind == .desktop, let device = selectedDevice {
            return device.online
                ? "\(WorkEnvironmentL10n.desktopShort) · \(device.name)"
                : "\(WorkEnvironmentL10n.desktopShort) · \(WorkEnvironmentL10n.offline)"
        }
        return WorkEnvironmentL10n.cloudShort
    }

    var shouldRouteToDesktop: Bool {
        kind == .desktop && (selectedDevice?.online == true) && !(selectedDevice?.token ?? "").isEmpty
    }

    var snapshot: WorkEnvironmentSnapshot {
        WorkEnvironmentSnapshot(
            kind: kind,
            selectedDeviceId: selectedDeviceId,
            devices: devices,
            downloadURL: downloadURL
        )
    }

    private init() {
        if let raw = UserDefaults.standard.string(forKey: Self.kindKey),
           let kind = WorkEnvironmentKind(rawValue: raw) {
            self.kind = kind
        }
        selectedDeviceId = UserDefaults.standard.string(forKey: Self.selectedKey)
        if let data = UserDefaults.standard.data(forKey: Self.devicesKey),
           let saved = try? JSONDecoder().decode([DesktopDevice].self, from: data) {
            devices = saved.map {
                var copy = $0
                copy.online = false
                return copy
            }
        }
    }

    func selectCloud() {
        kind = .cloud
        persist()
    }

    func selectDevice(id: String) {
        guard let device = devices.first(where: { $0.id == id }), device.paired else { return }
        kind = .desktop
        selectedDeviceId = id
        persist()
    }

    func upsertDiscovered(_ incoming: DesktopDevice) {
        if let index = devices.firstIndex(where: { $0.id == incoming.id || hostPortMatch($0, incoming) }) {
            var merged = devices[index]
            merged.name = incoming.name
            merged.os = incoming.os
            merged.host = incoming.host
            merged.port = incoming.port
            merged.online = incoming.online
            merged.lastSeen = incoming.lastSeen ?? Date()
            if incoming.paired { merged.paired = true }
            if let token = incoming.token, !token.isEmpty { merged.token = token }
            devices[index] = merged
        } else {
            devices.append(incoming)
        }
        persistDevices()
    }

    func markOffline(except ids: Set<String>) {
        var changed = false
        for i in devices.indices where !ids.contains(devices[i].id) && devices[i].online {
            devices[i].online = false
            changed = true
        }
        if changed { persistDevices() }
    }

    func applyPairing(id: String, token: String, name: String, os: String) {
        if let index = devices.firstIndex(where: { $0.id == id }) {
            devices[index].token = token
            devices[index].paired = true
            devices[index].online = true
            devices[index].name = name
            devices[index].os = os
        }
        kind = .desktop
        selectedDeviceId = id
        persist()
    }

    func forget(id: String) {
        devices.removeAll { $0.id == id }
        if selectedDeviceId == id {
            selectedDeviceId = nil
            kind = .cloud
        }
        persist()
    }

    private func hostPortMatch(_ a: DesktopDevice, _ b: DesktopDevice) -> Bool {
        a.host == b.host && a.port == b.port
    }

    private func persist() {
        UserDefaults.standard.set(kind.rawValue, forKey: Self.kindKey)
        UserDefaults.standard.set(selectedDeviceId, forKey: Self.selectedKey)
        persistDevices()
    }

    private func persistDevices() {
        if let data = try? JSONEncoder().encode(devices) {
            UserDefaults.standard.set(data, forKey: Self.devicesKey)
        }
        objectWillChange.send()
    }
}
