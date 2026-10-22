package com.openminis.app.ui.workenv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.workenv.DesktopConnectionManager
import com.openminis.app.workenv.DesktopDevice
import com.openminis.app.workenv.WorkEnvironmentKind
import com.openminis.app.workenv.WorkEnvironmentPalette
import com.openminis.app.workenv.WorkEnvironmentStore
import kotlinx.coroutines.launch

private val Ink = Color(WorkEnvironmentPalette.Ink)
private val Teal = Color(WorkEnvironmentPalette.Teal)
private val Offline = Color(WorkEnvironmentPalette.Offline)
private val Secondary = Color(WorkEnvironmentPalette.Secondary)
private val Card = Color(WorkEnvironmentPalette.Card)
private val Line = Color(WorkEnvironmentPalette.Line)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkEnvironmentSheetHost(
    store: WorkEnvironmentStore,
    connection: DesktopConnectionManager,
) {
    val showPicker by store.showPicker.collectAsState()
    val showGuide by store.showGuide.collectAsState()
    LaunchedEffect(Unit) { connection.start() }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { store.setShowPicker(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFD8D8DC)),
                )
            },
        ) {
            WorkEnvironmentPickerContent(store, connection)
        }
    }
    if (showGuide) {
        ModalBottomSheet(
            onDismissRequest = { store.setShowGuide(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
        ) {
            DesktopConnectGuideContent(
                downloadURL = store.downloadURL,
                onClose = { store.setShowGuide(false) },
            )
        }
    }
}

@Composable
private fun WorkEnvironmentPickerContent(
    store: WorkEnvironmentStore,
    connection: DesktopConnectionManager,
) {
    val kind by store.kind.collectAsState()
    val selectedId by store.selectedDeviceId.collectAsState()
    val devices by store.devices.collectAsState()
    val connectingId by connection.connectingId.collectAsState()
    val error by store.lastError.collectAsState()
    val scope = rememberCoroutineScope()
    var manualHost by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.workenv_picker_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(
                onClick = { store.setShowPicker(false) },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = Color(0xFF3A3A3C))
            }
        }

        OptionRow(
            icon = Icons.Outlined.Cloud,
            title = stringResource(R.string.workenv_cloud_title),
            desc = stringResource(R.string.workenv_cloud_desc),
            selected = kind == WorkEnvironmentKind.CLOUD,
            onClick = {
                store.selectCloud()
                store.setShowPicker(false)
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        OptionRow(
            icon = Icons.Outlined.Computer,
            title = stringResource(R.string.workenv_desktop_title),
            desc = null,
            selected = false,
            onClick = { store.setShowGuide(true) },
            trailing = false,
            extra = {
                Text(
                    text = stringResource(R.string.workenv_desktop_desc) + " ",
                    fontSize = 13.sp,
                    color = Secondary,
                    lineHeight = 18.sp,
                )
                Text(
                    text = stringResource(R.string.workenv_guide_link),
                    fontSize = 13.sp,
                    color = Teal,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { store.setShowGuide(true) },
                )
            },
        )

        Column(Modifier.padding(start = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualHost,
                    onValueChange = { manualHost = it },
                    placeholder = { Text(stringResource(R.string.workenv_host_placeholder), fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal,
                        unfocusedBorderColor = Color(0xFFE5E5EA),
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
                Spacer(Modifier.width(8.dp))
                AccioPillButton(stringResource(R.string.workenv_add)) {
                    val host = manualHost
                    scope.launch { connection.addManual(host) }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.workenv_empty),
                    fontSize = 13.sp,
                    color = Secondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                devices.forEach { device ->
                    DeviceRow(
                        device = device,
                        selected = kind == WorkEnvironmentKind.DESKTOP && selectedId == device.id,
                        connecting = connectingId == device.id,
                        onConnect = { scope.launch { connection.connect(device.id) } },
                        onSelect = {
                            if (device.online && device.paired) {
                                store.selectDevice(device.id)
                                store.setShowPicker(false)
                            }
                        },
                    )
                }
            }
            if (!error.isNullOrEmpty()) {
                Text(error!!, fontSize = 12.sp, color = Offline)
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    desc: String?,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: Boolean = true,
    extra: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF3A3A3C), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (desc != null) {
                Text(desc, fontSize = 13.sp, color = Secondary, modifier = Modifier.padding(top = 4.dp))
            }
            extra?.invoke()
        }
        if (trailing) Radio(selected)
    }
}

@Composable
private fun DeviceRow(
    device: DesktopDevice,
    selected: Boolean,
    connecting: Boolean,
    onConnect: () -> Unit,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Row {
                Text("${device.os} · ", fontSize = 12.sp, color = Secondary)
                Text(
                    text = stringResource(if (device.online) R.string.workenv_online else R.string.workenv_offline),
                    fontSize = 12.sp,
                    color = if (device.online) Teal else Offline,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = " · " + stringResource(if (device.paired) R.string.workenv_paired else R.string.workenv_unpaired),
                    fontSize = 12.sp,
                    color = Secondary,
                )
                if (selected) {
                    Text(" · " + stringResource(R.string.workenv_current), fontSize = 12.sp, color = Secondary)
                }
            }
        }
        if (device.online && !selected) {
            AccioPillButton(
                if (connecting) stringResource(R.string.workenv_connecting) else stringResource(R.string.workenv_connect),
                enabled = !connecting,
                onClick = onConnect,
            )
        } else {
            Box(Modifier.clickable(onClick = onSelect)) { Radio(selected) }
        }
    }
}

@Composable
private fun Radio(on: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.5.dp, if (on) Ink else Color(0xFFC7C7CC), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Box(Modifier.size(12.dp).clip(CircleShape).background(Ink))
    }
}

@Composable
private fun AccioPillButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink.copy(alpha = if (enabled) 1f else 0.45f),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, Ink, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

@Composable
private fun DesktopConnectGuideContent(downloadURL: String, onClose: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.workenv_guide_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))
        GuideStep(Icons.Outlined.Computer) {
            Text(stringResource(R.string.workenv_step1), fontSize = 14.sp, color = Color(0xFF2C2C2E), lineHeight = 20.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(downloadURL, fontSize = 13.sp, color = Teal)
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("url", downloadURL))
                        },
                )
            }
        }
        GuideStep(Icons.Outlined.Person) {
            Text(stringResource(R.string.workenv_step2), fontSize = 14.sp, color = Color(0xFF2C2C2E), lineHeight = 20.sp)
        }
        GuideStep(Icons.Outlined.Settings) {
            Text(stringResource(R.string.workenv_step3), fontSize = 14.sp, color = Color(0xFF2C2C2E), lineHeight = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Ink)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.workenv_got_it), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuideStep(icon: ImageVector, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6E6E73), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { content() }
    }
}
