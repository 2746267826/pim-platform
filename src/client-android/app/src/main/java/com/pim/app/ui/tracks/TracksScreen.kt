package com.pim.app.ui.tracks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.ui.shell.PimWebViewScreen
import com.pim.app.ui.today.TodayViewModel

@Composable
fun TracksScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshVersion by viewModel.refreshVersion.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        when (state.embedSupported) {
            false -> {
                EmbedUnsupportedBanner(onOpenSettings = onOpenSettings)
            }
            else -> {
                PimWebViewScreen(
                    route = "/embed/android/tracks",
                    serverUrl = viewModel.serverUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    bridge = viewModel.bridge,
                    reloadKey = refreshVersion
                )
            }
        }
    }
}

@Composable
private fun EmbedUnsupportedBanner(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "服务器版本不支持嵌入页面",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "请升级服务器或切换到支持嵌入页面的服务器",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null)
            Text("打开设置")
        }
    }
}
