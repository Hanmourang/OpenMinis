import SwiftUI

/// Accio-style segmented Cloud / Desktop control.
struct WorkEnvironmentToggle: View {
    @ObservedObject var store: WorkEnvironmentStore

    var body: some View {
        HStack(spacing: 0) {
            segment(
                icon: "cloud",
                title: WorkEnvironmentL10n.cloudShort,
                selected: store.kind == .cloud
            ) {
                store.selectCloud()
            }
            segment(
                icon: "desktopcomputer",
                title: WorkEnvironmentL10n.desktopShort,
                selected: store.kind == .desktop
            ) {
                store.showPicker = true
            }
        }
        .padding(3)
        .background(Color(white: 0.94), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func segment(icon: String, title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 13, weight: .medium))
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
            }
            .foregroundStyle(Color(hex: selected ? WorkEnvironmentPalette.ink : WorkEnvironmentPalette.secondary))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background {
                if selected {
                    RoundedRectangle(cornerRadius: 11, style: .continuous)
                        .fill(Color.white)
                        .shadow(color: Color.black.opacity(0.08), radius: 3, y: 1)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

/// Compact status chip used in the chat header, matching Accio's gray "云端" label.
struct WorkEnvironmentBadge: View {
    @ObservedObject var store: WorkEnvironmentStore

    var body: some View {
        Button {
            store.showPicker = true
        } label: {
            HStack(spacing: 4) {
                Image(systemName: store.kind == .desktop ? "desktopcomputer" : "cloud")
                    .font(.system(size: 10, weight: .medium))
                Text(store.statusLabel)
                    .font(.system(size: 11, weight: .medium))
                    .lineLimit(1)
            }
            .foregroundStyle(Color(hex: WorkEnvironmentPalette.secondary))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(store.statusLabel)
    }
}

/// Hosts the picker + guide sheets at the app root.
struct WorkEnvironmentSheetHost: ViewModifier {
    @ObservedObject var store: WorkEnvironmentStore
    @ObservedObject var connection: DesktopConnectionManager

    func body(content: Content) -> some View {
        content
            .sheet(isPresented: $store.showPicker) {
                WorkEnvironmentPickerSheet(store: store, connection: connection)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.hidden)
                    .sheet(isPresented: $store.showGuide) {
                        DesktopConnectGuideSheet(downloadURL: store.downloadURL) {
                            store.showGuide = false
                        }
                        .presentationDetents([.medium, .large])
                        .presentationDragIndicator(.hidden)
                    }
            }
            .onAppear { connection.start() }
    }
}

struct WorkEnvironmentSettingsView: View {
    @ObservedObject private var store = WorkEnvironmentStore.shared

    var body: some View {
        List {
            Section {
                WorkEnvironmentToggle(store: store)
                    .listRowInsets(EdgeInsets(top: 10, leading: 16, bottom: 10, trailing: 16))
                    .listRowBackground(Color.clear)
                Button {
                    store.showPicker = true
                } label: {
                    Label(WorkEnvironmentL10n.pickerTitle, systemImage: "slider.horizontal.3")
                }
                Button {
                    store.showGuide = true
                } label: {
                    Label(WorkEnvironmentL10n.guideLink, systemImage: "questionmark.circle")
                }
            } footer: {
                Text(store.statusLabel)
            }
        }
        .navigationTitle(WorkEnvironmentL10n.settingsTitle)
        .navigationBarTitleDisplayMode(.inline)
        .workEnvironmentSheets()
    }
}

extension View {
    func workEnvironmentSheets() -> some View {
        modifier(WorkEnvironmentSheetHost(store: .shared, connection: .shared))
    }
}
