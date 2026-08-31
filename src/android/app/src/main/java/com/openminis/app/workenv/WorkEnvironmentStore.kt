package com.openminis.app.workenv

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class WorkEnvironmentStore private constructor(private val prefs: SharedPreferences) {
    private val _kind = MutableStateFlow(readKind())
    val kind: StateFlow<WorkEnvironmentKind> = _kind.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow(prefs.getString(KEY_SELECTED, null))
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _devices = MutableStateFlow(readDevices())
    val devices: StateFlow<List<DesktopDevice>> = _devices.asStateFlow()

    private val _showPicker = MutableStateFlow(false)
    val showPicker: StateFlow<Boolean> = _showPicker.asStateFlow()

    private val _showGuide = MutableStateFlow(false)
    val showGuide: StateFlow<Boolean> = _showGuide.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val downloadURL: String = WorkEnvironmentDefaults.DownloadURL

    fun snapshot(): WorkEnvironmentSnapshot = WorkEnvironmentSnapshot(
        kind = _kind.value,
        selectedDeviceId = _selectedDeviceId.value,
        devices = _devices.value,
        downloadURL = downloadURL,
    )

    fun selectedDevice(): DesktopDevice? {
        val id = _selectedDeviceId.value ?: return null
        return _devices.value.firstOrNull { it.id == id }
    }

    fun statusLabel(cloud: String, desktop: String, offline: String): String {
        val device = selectedDevice()
        return if (_kind.value == WorkEnvironmentKind.DESKTOP && device != null) {
            if (device.online) "$desktop · ${device.name}" else "$desktop · $offline"
        } else {
            cloud
        }
    }

    fun shouldRouteToDesktop(): Boolean {
        val device = selectedDevice()
        return _kind.value == WorkEnvironmentKind.DESKTOP &&
            device?.online == true &&
            !device.token.isNullOrEmpty()
    }

    fun selectCloud() {
        _kind.value = WorkEnvironmentKind.CLOUD
        persist()
    }

    fun selectDevice(id: String) {
        val device = _devices.value.firstOrNull { it.id == id } ?: return
        if (!device.paired) return
        _kind.value = WorkEnvironmentKind.DESKTOP
        _selectedDeviceId.value = id
        persist()
    }

    fun setShowPicker(show: Boolean) { _showPicker.value = show }
    fun setShowGuide(show: Boolean) { _showGuide.value = show }
    fun setError(message: String?) { _lastError.value = message }

    fun upsertDiscovered(incoming: DesktopDevice) {
        _devices.update { current ->
            val index = current.indexOfFirst { it.id == incoming.id || hostPortMatch(it, incoming) }
            if (index >= 0) {
                val merged = current[index].copy(
                    name = incoming.name,
                    os = incoming.os,
                    host = incoming.host,
                    port = incoming.port,
                    online = incoming.online,
                    lastSeen = incoming.lastSeen ?: System.currentTimeMillis(),
                    paired = incoming.paired || current[index].paired,
                    token = incoming.token?.takeIf { it.isNotEmpty() } ?: current[index].token,
                    id = incoming.id.ifEmpty { current[index].id },
                )
                current.toMutableList().also { it[index] = merged }
            } else {
                current + incoming
            }
        }
        persistDevices()
    }

    fun applyPairing(id: String, token: String, name: String, os: String) {
        _devices.update { current ->
            current.map {
                if (it.id == id) it.copy(token = token, paired = true, online = true, name = name, os = os)
                else it
            }
        }
        _kind.value = WorkEnvironmentKind.DESKTOP
        _selectedDeviceId.value = id
        persist()
    }

    companion object {
        private const val PREFS = "work_environment"
        private const val KEY_KIND = "kind"
        private const val KEY_SELECTED = "selected"
        private const val KEY_DEVICES = "devices"

        @Volatile private var instance: WorkEnvironmentStore? = null

        fun get(context: Context): WorkEnvironmentStore {
            return instance ?: synchronized(this) {
                instance ?: WorkEnvironmentStore(
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }

        internal fun parseHostPort(raw: String, defaultPort: Int = WorkEnvironmentDefaults.DefaultPort): Pair<String, Int>? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            val idx = trimmed.lastIndexOf(':')
            if (idx > 0) {
                val host = trimmed.substring(0, idx)
                val port = trimmed.substring(idx + 1).toIntOrNull()
                if (host.isNotEmpty() && port != null && port in 1..65535) return host to port
            }
            if (trimmed.contains('.') || trimmed.contains(':')) return trimmed to defaultPort
            return null
        }
    }

    private fun readKind(): WorkEnvironmentKind =
        if (prefs.getString(KEY_KIND, "cloud") == "desktop") WorkEnvironmentKind.DESKTOP
        else WorkEnvironmentKind.CLOUD

    private fun readDevices(): List<DesktopDevice> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        DesktopDevice(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            os = o.optString("os"),
                            host = o.optString("host"),
                            port = o.optInt("port", WorkEnvironmentDefaults.DefaultPort),
                            online = false,
                            paired = o.optBoolean("paired"),
                            token = o.optString("token").ifEmpty { null },
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY_KIND, if (_kind.value == WorkEnvironmentKind.DESKTOP) "desktop" else "cloud")
            .putString(KEY_SELECTED, _selectedDeviceId.value)
            .apply()
        persistDevices()
    }

    private fun persistDevices() {
        val arr = JSONArray()
        for (d in _devices.value) {
            arr.put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("os", d.os)
                put("host", d.host)
                put("port", d.port)
                put("paired", d.paired)
                put("token", d.token ?: "")
            })
        }
        prefs.edit().putString(KEY_DEVICES, arr.toString()).apply()
    }

    private fun hostPortMatch(a: DesktopDevice, b: DesktopDevice) =
        a.host == b.host && a.port == b.port
}
