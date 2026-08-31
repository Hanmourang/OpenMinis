import Foundation

enum WorkEnvironmentKind: String, Codable, Sendable {
    case cloud
    case desktop
}

struct DesktopDevice: Identifiable, Codable, Equatable, Sendable {
    var id: String
    var name: String
    var os: String
    var host: String
    var port: Int
    var online: Bool
    var paired: Bool
    var token: String?
    var lastSeen: Date?

    var endpoint: String { "http://\(host):\(port)" }
}

struct WorkEnvironmentSnapshot: Equatable, Sendable {
    var kind: WorkEnvironmentKind
    var selectedDeviceId: String?
    var devices: [DesktopDevice]
    var downloadURL: String
}

enum WorkEnvironmentL10n {
    static var isChinese: Bool {
        Locale.current.language.languageCode?.identifier.hasPrefix("zh") == true
    }

    static var pickerTitle: String { isChinese ? "选择工作环境" : "Choose work environment" }
    static var cloudTitle: String { isChinese ? "使用云端工作" : "Work in the cloud" }
    static var cloudDesc: String { isChinese ? "在本机 Linux 沙箱中运行任务" : "Run tasks in the on-device Linux sandbox" }
    static var desktopTitle: String { isChinese ? "使用本地电脑工作" : "Work on a local computer" }
    static var desktopDesc: String {
        isChinese
            ? "通过桌面端应用，让 Minis 查看并控制你的本地电脑。"
            : "Let Minis view and control your desktop through the companion app."
    }
    static var guideLink: String { isChinese ? "查看连接指引" : "View connection guide" }
    static var guideTitle: String { isChinese ? "连接到桌面端" : "Connect to desktop" }
    static var settingsTitle: String { isChinese ? "工作环境" : "Work environment" }
    static var settingsSubtitle: String {
        isChinese ? "在云端沙箱与桌面端电脑之间切换" : "Switch between the cloud sandbox and a desktop companion"
    }
    static var cloudShort: String { isChinese ? "云端" : "Cloud" }
    static var desktopShort: String { isChinese ? "桌面端" : "Desktop" }
    static var connect: String { isChinese ? "连接" : "Connect" }
    static var online: String { isChinese ? "在线" : "online" }
    static var offline: String { isChinese ? "离线" : "offline" }
    static var unpaired: String { isChinese ? "未配对" : "unpaired" }
    static var paired: String { isChinese ? "已配对" : "paired" }
    static var current: String { isChinese ? "当前" : "current" }
    static var gotIt: String { isChinese ? "我知道了" : "Got it" }
    static var add: String { isChinese ? "添加" : "Add" }
    static var emptyDevices: String {
        isChinese
            ? "未发现桌面端。请启动 minis-desktop，或手动填写 host:port。"
            : "No desktop companion found. Start minis-desktop, or add host:port manually."
    }
    static var connecting: String { isChinese ? "正在连接…" : "Connecting…" }
    static var hostPlaceholder: String { isChinese ? "host:port，例如 192.168.1.8:18765" : "host:port, e.g. 192.168.1.8:18765" }
    static var downloadHint: String {
        isChinese
            ? "Minis 会借助你的桌面电脑完成复杂任务。下载并运行桌面端："
            : "Minis uses your desktop computer for heavier work. Download and run the companion:"
    }
    static var step2: String {
        isChinese
            ? "在同一局域网打开桌面端，保持可被发现。"
            : "Open the companion on the same network and keep it discoverable."
    }
    static var step3: String {
        isChinese
            ? "在桌面端设置中保持「允许此电脑被发现和控制」开启，并允许连接授权。"
            : "Keep “Allow this computer to be discovered and controlled” on, and approve pairing."
    }
}

enum WorkEnvironmentPalette {
    static let teal = 0x1FA97A
    static let offline = 0xD4654A
    static let ink = 0x1A1A1A
    static let secondary = 0x8E8E93
    static let card = 0xF4F4F6
    static let line = 0xEFEFF2
    static let mint = 0xD7F4E6
}
