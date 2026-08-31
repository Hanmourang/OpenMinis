import SwiftUI

struct WorkEnvironmentPickerSheet: View {
    @ObservedObject var store: WorkEnvironmentStore
    @ObservedObject var connection: DesktopConnectionManager
    @Environment(\.dismiss) private var dismiss
    @State private var manualHost = ""

    var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(Color(hex: 0xD8D8DC))
                .frame(width: 36, height: 4)
                .padding(.top, 10)

            ZStack {
                Text(WorkEnvironmentL10n.pickerTitle)
                    .font(.system(size: 17, weight: .bold))
                HStack {
                    Spacer()
                    Button { dismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color(hex: 0x3A3A3C))
                            .frame(width: 32, height: 32)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 6)

            ScrollView {
                VStack(spacing: 0) {
                    cloudRow
                    Divider().overlay(Color(hex: WorkEnvironmentPalette.line))
                    desktopHeader
                    deviceList
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 28)
            }
        }
        .background(Color.white)
        .onAppear { connection.start() }
    }

    private var cloudRow: some View {
        Button {
            store.selectCloud()
            dismiss()
        } label: {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "cloud")
                    .font(.system(size: 18, weight: .regular))
                    .foregroundStyle(Color(hex: 0x3A3A3C))
                    .frame(width: 28, height: 28)
                VStack(alignment: .leading, spacing: 4) {
                    Text(WorkEnvironmentL10n.cloudTitle)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Color(hex: WorkEnvironmentPalette.ink))
                    Text(WorkEnvironmentL10n.cloudDesc)
                        .font(.system(size: 13))
                        .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                radio(on: store.kind == .cloud)
            }
            .padding(.vertical, 16)
        }
        .buttonStyle(.plain)
    }

    private var desktopHeader: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "desktopcomputer")
                .font(.system(size: 18, weight: .regular))
                .foregroundStyle(Color(hex: 0x3A3A3C))
                .frame(width: 28, height: 28)
            VStack(alignment: .leading, spacing: 4) {
                Text(WorkEnvironmentL10n.desktopTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color(hex: WorkEnvironmentPalette.ink))
                (
                    Text(WorkEnvironmentL10n.desktopDesc + " ")
                        .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
                    +
                    Text(WorkEnvironmentL10n.guideLink)
                        .foregroundStyle(Color(hex: WorkEnvironmentPalette.teal))
                )
                .font(.system(size: 13))
                .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 16)
        .contentShape(Rectangle())
        .onTapGesture { store.showGuide = true }
    }

    private var deviceList: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 8) {
                TextField(WorkEnvironmentL10n.hostPlaceholder, text: $manualHost)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .font(.system(size: 13))
                    .padding(.horizontal, 12)
                    .frame(height: 36)
                    .background(RoundedRectangle(cornerRadius: 10).stroke(Color(hex: 0xE5E5EA)))
                Button(WorkEnvironmentL10n.add) {
                    let host = manualHost
                    Task { await connection.addManual(hostPort: host) }
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color(hex: WorkEnvironmentPalette.ink))
                .padding(.horizontal, 14)
                .frame(height: 32)
                .overlay(Capsule().stroke(Color(hex: WorkEnvironmentPalette.ink)))
            }

            if store.devices.isEmpty {
                Text(WorkEnvironmentL10n.emptyDevices)
                    .font(.system(size: 13))
                    .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
                    .padding(.bottom, 8)
            } else {
                ForEach(store.devices) { device in
                    deviceRow(device)
                }
            }

            if let error = store.lastError, !error.isEmpty {
                Text(error)
                    .font(.system(size: 12))
                    .foregroundStyle(Color(hex: WorkEnvironmentPalette.offline))
            }
        }
        .padding(.leading, 40)
        .padding(.bottom, 8)
    }

    private func deviceRow(_ device: DesktopDevice) -> some View {
        let selected = store.kind == .desktop && store.selectedDeviceId == device.id
        return HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(device.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color(hex: WorkEnvironmentPalette.ink))
                HStack(spacing: 0) {
                    Text("\(device.os) · ")
                        .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
                    Text(device.online ? WorkEnvironmentL10n.online : WorkEnvironmentL10n.offline)
                        .foregroundStyle(Color(hex: device.online ? WorkEnvironmentPalette.teal : WorkEnvironmentPalette.offline))
                    Text(" · \(device.paired ? WorkEnvironmentL10n.paired : WorkEnvironmentL10n.unpaired)")
                        .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
                    if selected {
                        Text(" · \(WorkEnvironmentL10n.current)")
                            .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
                    }
                }
                .font(.system(size: 12))
            }
            Spacer()
            if device.online && !selected {
                Button(connection.connectingId == device.id ? WorkEnvironmentL10n.connecting : WorkEnvironmentL10n.connect) {
                    Task { await connection.connect(id: device.id) }
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Color(hex: WorkEnvironmentPalette.ink))
                .padding(.horizontal, 14)
                .frame(height: 32)
                .overlay(Capsule().stroke(Color(hex: WorkEnvironmentPalette.ink)))
                .disabled(connection.connectingId != nil)
            } else {
                radio(on: selected)
                    .onTapGesture {
                        if device.online && device.paired {
                            store.selectDevice(id: device.id)
                            dismiss()
                        }
                    }
            }
        }
        .padding(.vertical, 10)
    }

    private func radio(on: Bool) -> some View {
        ZStack {
            Circle()
                .stroke(Color(hex: on ? WorkEnvironmentPalette.ink : 0xC7C7CC), lineWidth: 1.5)
            if on {
                Circle()
                    .fill(Color(hex: WorkEnvironmentPalette.ink))
                    .padding(5)
            }
        }
        .frame(width: 22, height: 22)
    }
}

struct DesktopConnectGuideSheet: View {
    let downloadURL: String
    var onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Capsule()
                .fill(Color(hex: 0xD8D8DC))
                .frame(width: 36, height: 4)
                .padding(.top, 10)

            ZStack {
                Text(WorkEnvironmentL10n.guideTitle)
                    .font(.system(size: 17, weight: .bold))
                HStack {
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Color(hex: 0x3A3A3C))
                            .frame(width: 32, height: 32)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 12)

            VStack(spacing: 12) {
                step(icon: "desktopcomputer") {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(WorkEnvironmentL10n.downloadHint)
                        HStack(spacing: 6) {
                            Text(downloadURL)
                                .foregroundStyle(Color(hex: WorkEnvironmentPalette.teal))
                            Button {
                                UIPasteboard.general.string = downloadURL
                            } label: {
                                Image(systemName: "square.on.square")
                                    .font(.system(size: 13))
                                    .foregroundStyle(Color(hex: WorkEnvironmentPalette.teal))
                            }
                            .buttonStyle(.plain)
                        }
                        .font(.system(size: 13))
                    }
                }
                step(icon: "person.crop.circle.badge.checkmark") {
                    Text(WorkEnvironmentL10n.step2)
                }
                step(icon: "gearshape") {
                    Text(WorkEnvironmentL10n.step3)
                }
            }
            .padding(.horizontal, 20)

            Spacer(minLength: 16)

            Button(action: onClose) {
                Text(WorkEnvironmentL10n.gotIt)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color(hex: WorkEnvironmentPalette.ink), in: Capsule())
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 18)
        }
        .background(Color.white)
    }

    private func step(icon: String, @ViewBuilder content: () -> some View) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundStyle(Color(hex: 0x6E6E73))
                .frame(width: 24, height: 24)
                .padding(.top, 2)
            content()
                .font(.system(size: 14))
                .foregroundStyle(Color(hex: 0x2C2C2E))
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .padding(14)
        .background(Color(hex: WorkEnvironmentPalette.card), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

extension Color {
    init(hex: Int) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
