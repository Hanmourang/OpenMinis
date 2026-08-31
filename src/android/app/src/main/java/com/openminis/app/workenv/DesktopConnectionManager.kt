package com.openminis.app.workenv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class DesktopConnectionManager(
    context: Context,
    private val store: WorkEnvironmentStore,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val nsd = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val clientId: String = storeClientId()
    private val clientName: String = Build.MODEL ?: "Android"

    private val _connectingId = MutableStateFlow<String?>(null)
    val connectingId: StateFlow<String?> = _connectingId.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var pollJob: Job? = null

    fun start() {
        startDiscovery()
        if (pollJob == null) {
            pollJob = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    refreshPaired()
                    delay(8_000)
                }
            }
        }
    }

    fun stop() {
        discoveryListener?.let { listener ->
            runCatching { nsd?.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun connect(id: String) {
        val device = store.devices.value.firstOrNull { it.id == id } ?: return
        _connectingId.value = id
        try {
            val existing = device.token
            if (!existing.isNullOrEmpty() && statusOK(device, existing)) {
                store.selectDevice(id)
                return
            }
            val paired = pair(device)
            val resolvedId = paired.optString("deviceId").ifEmpty { id }
            store.applyPairing(
                id = resolvedId,
                token = paired.getString("token"),
                name = paired.optString("name", device.name),
                os = paired.optString("os", device.os),
            )
        } catch (e: Exception) {
            store.setError(e.message)
        } finally {
            _connectingId.value = null
        }
    }

    suspend fun addManual(hostPort: String) {
        val parsed = WorkEnvironmentStore.parseHostPort(hostPort)
        if (parsed == null) {
            store.setError("Invalid host:port")
            return
        }
        try {
            val hello = hello(parsed.first, parsed.second)
            val id = hello.optString("id").ifEmpty { "${parsed.first}:${parsed.second}" }
            store.upsertDiscovered(
                DesktopDevice(
                    id = id,
                    name = hello.optString("name").ifEmpty { parsed.first },
                    os = hello.optString("os").ifEmpty { "desktop" },
                    host = parsed.first,
                    port = parsed.second,
                    online = true,
                    paired = false,
                )
            )
            connect(id)
        } catch (e: Exception) {
            store.setError(e.message)
        }
    }

    suspend fun execute(command: String, timeoutSec: Int): Pair<String, Int> {
        val device = store.selectedDevice() ?: throw IllegalStateException("no desktop")
        val token = device.token ?: throw IllegalStateException("unpaired")
        val body = JSONObject().put("command", command).put("timeout", timeoutSec)
        val obj = request("POST", "${device.endpoint}/v1/exec", body, token, (timeoutSec + 5) * 1000)
        val stdout = obj.optString("stdout")
        val stderr = obj.optString("stderr")
        val code = obj.optInt("exitCode", -1)
        val output = if (stderr.isEmpty()) stdout else if (stdout.isEmpty()) stderr else "$stdout\n$stderr"
        return output to code
    }

    private fun startDiscovery() {
        val manager = nsd ?: return
        if (discoveryListener != null) return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String?) {}
            override fun onDiscoveryStopped(serviceType: String?) {}
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host?.hostAddress ?: return
                        val port = if (resolved.port > 0) resolved.port else WorkEnvironmentDefaults.DefaultPort
                        scope.launch(Dispatchers.IO) {
                            val hello = runCatching { hello(host, port) }.getOrNull()
                            val id = hello?.optString("id").orEmpty().ifEmpty { "$host:$port" }
                            store.upsertDiscovered(
                                DesktopDevice(
                                    id = id,
                                    name = hello?.optString("name").orEmpty().ifEmpty { resolved.serviceName },
                                    os = hello?.optString("os").orEmpty().ifEmpty { "desktop" },
                                    host = host,
                                    port = port,
                                    online = true,
                                    paired = false,
                                )
                            )
                        }
                    }
                })
            }
        }
        discoveryListener = listener
        runCatching {
            manager.discoverServices(WorkEnvironmentDefaults.ServiceType, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    private suspend fun refreshPaired() {
        for (device in store.devices.value) {
            val hello = runCatching { hello(device.host, device.port) }.getOrNull()
            if (hello != null) {
                store.upsertDiscovered(
                    device.copy(
                        online = true,
                        name = hello.optString("name").ifEmpty { device.name },
                        os = hello.optString("os").ifEmpty { device.os },
                        lastSeen = System.currentTimeMillis(),
                    )
                )
            } else {
                store.upsertDiscovered(device.copy(online = false))
            }
        }
    }

    private suspend fun hello(host: String, port: Int): JSONObject =
        request("GET", "http://$host:$port/v1/hello", null, null, 4000)

    private suspend fun pair(device: DesktopDevice): JSONObject {
        val body = JSONObject().put("clientId", clientId).put("clientName", clientName)
        return request("POST", "${device.endpoint}/v1/pair", body, null, 8000)
    }

    private suspend fun statusOK(device: DesktopDevice, token: String): Boolean =
        runCatching { request("GET", "${device.endpoint}/v1/status", null, token, 4000) }.isSuccess

    private suspend fun request(
        method: String,
        url: String,
        body: JSONObject?,
        token: String?,
        timeoutMs: Int,
    ): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray()) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.let { BufferedReader(InputStreamReader(it)).readText() } ?: "{}"
        conn.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
        JSONObject(text)
    }

    private fun storeClientId(): String {
        val prefs = appContext.getSharedPreferences("work_environment", Context.MODE_PRIVATE)
        val existing = prefs.getString("client_id", null)
        if (existing != null) return existing
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString("client_id", fresh).apply()
        return fresh
    }
}
