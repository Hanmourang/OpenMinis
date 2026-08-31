package com.openminis.app.ui.workenv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openminis.app.R
import com.openminis.app.workenv.WorkEnvironmentKind
import com.openminis.app.workenv.WorkEnvironmentPalette
import com.openminis.app.workenv.WorkEnvironmentStore

@Composable
fun WorkEnvironmentToggle(
    store: WorkEnvironmentStore,
    modifier: Modifier = Modifier,
) {
    val kind by store.kind.collectAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF0F0F2))
            .padding(3.dp),
    ) {
        ToggleSegment(
            icon = Icons.Outlined.Cloud,
            title = stringResource(R.string.workenv_cloud_short),
            selected = kind == WorkEnvironmentKind.CLOUD,
            modifier = Modifier.weight(1f),
            onClick = { store.selectCloud() },
        )
        ToggleSegment(
            icon = Icons.Outlined.Computer,
            title = stringResource(R.string.workenv_desktop_short),
            selected = kind == WorkEnvironmentKind.DESKTOP,
            modifier = Modifier.weight(1f),
            onClick = { store.setShowPicker(true) },
        )
    }
}

@Composable
private fun ToggleSegment(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .then(
                if (selected) Modifier.shadow(2.dp, shape, clip = false).clip(shape).background(Color.White)
                else Modifier.clip(shape)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(if (selected) WorkEnvironmentPalette.Ink else WorkEnvironmentPalette.Secondary),
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(if (selected) WorkEnvironmentPalette.Ink else WorkEnvironmentPalette.Secondary),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
fun WorkEnvironmentBadge(
    store: WorkEnvironmentStore,
    modifier: Modifier = Modifier,
) {
    val kind by store.kind.collectAsState()
    val selectedId by store.selectedDeviceId.collectAsState()
    val devices by store.devices.collectAsState()
    val label = store.statusLabel(
        cloud = stringResource(R.string.workenv_cloud_short),
        desktop = stringResource(R.string.workenv_desktop_short),
        offline = stringResource(R.string.workenv_offline),
    )
    Row(
        modifier = modifier.clickable { store.setShowPicker(true) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            if (kind == WorkEnvironmentKind.DESKTOP) Icons.Outlined.Computer else Icons.Outlined.Cloud,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = Color(WorkEnvironmentPalette.Secondary),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(WorkEnvironmentPalette.Secondary),
            maxLines = 1,
        )
        // Keep collected state live so the label refreshes when devices change.
        Box { }
        selectedId
        devices
    }
}
