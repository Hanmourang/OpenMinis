package com.openminis.app.workenv

enum class WorkEnvironmentKind { CLOUD, DESKTOP }

data class DesktopDevice(
    val id: String,
    val name: String,
    val os: String,
    val host: String,
    val port: Int,
    val online: Boolean,
    val paired: Boolean,
    val token: String? = null,
    val lastSeen: Long? = null,
) {
    val endpoint: String get() = "http://$host:$port"
}

data class WorkEnvironmentSnapshot(
    val kind: WorkEnvironmentKind,
    val selectedDeviceId: String?,
    val devices: List<DesktopDevice>,
    val downloadURL: String,
)

object WorkEnvironmentPalette {
    const val Teal = 0xFF1FA97A
    const val Offline = 0xFFD4654A
    const val Ink = 0xFF1A1A1A
    const val Secondary = 0xFF8E8E93
    const val Card = 0xFFF4F4F6
    const val Line = 0xFFEFEFF2
}

object WorkEnvironmentDefaults {
    const val DownloadURL = "https://openminis.app/desktop"
    const val DefaultPort = 18765
    const val ServiceType = "_minis-desktop._tcp"
}
