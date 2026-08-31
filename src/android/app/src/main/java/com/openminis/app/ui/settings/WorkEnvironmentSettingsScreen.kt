package com.openminis.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.openminis.app.R
import com.openminis.app.ui.workenv.WorkEnvironmentSheetHost
import com.openminis.app.ui.workenv.WorkEnvironmentToggle
import com.openminis.app.workenv.WorkEnvironmentHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkEnvironmentSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = WorkEnvironmentHolder.store(context)
    val connection = WorkEnvironmentHolder.connection(context)
    val kind by store.kind.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_workenv)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            WorkEnvironmentToggle(store)
            TextButton(onClick = { store.setShowPicker(true) }) {
                Text(stringResource(R.string.workenv_picker_title))
            }
            TextButton(onClick = { store.setShowGuide(true) }) {
                Text(stringResource(R.string.workenv_guide_link))
            }
            Text(
                text = store.statusLabel(
                    cloud = stringResource(R.string.workenv_cloud_short),
                    desktop = stringResource(R.string.workenv_desktop_short),
                    offline = stringResource(R.string.workenv_offline),
                ),
            )
            kind
        }
    }
    WorkEnvironmentSheetHost(store, connection)
}
