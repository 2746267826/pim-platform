package com.pim.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pim.app.ui.components.PimSection

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        PimSection("API 地址") {
            OutlinedTextField(
                value = state.apiAddress,
                onValueChange = viewModel::updateApiAddress,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("API 地址") },
                placeholder = { Text("https://pim.example.com/api/v1/") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            Text("支持公网 IP 或域名。")
            if (state.apiWarnings.contains("real-device-localhost")) {
                Text(
                    text = "真机上的 127.0.0.1 指向手机本机，通常无法连接你的服务器。",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            state.apiError?.let { reason ->
                Text("地址问题：$reason", color = MaterialTheme.colorScheme.error)
            }
            state.apiStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(onClick = viewModel::saveApiAddress) {
                Text("保存")
            }
            OutlinedButton(onClick = viewModel::testConnection) {
                Text("测试连接")
            }
        }
        PimSection("账号") {
            Text(if (state.isLoggedIn) "当前状态：已登录" else "当前状态：未登录")
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("用户名") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
            )
            Button(
                onClick = { viewModel.login(username, password) },
                enabled = !state.isBusy
            ) {
                Text(if (state.isBusy) "登录中" else "登录")
            }
            OutlinedButton(onClick = viewModel::logout) {
                Text("退出登录")
            }
            state.loginStatus?.let { Text(it) }
        }
        PimSection("持续采集") {
            Text("默认关闭，需要手动开启。缺少 API、登录或权限时保持关闭。")
            Switch(
                checked = state.continuousCollectionEnabled,
                onCheckedChange = viewModel::setContinuousCollectionEnabled
            )
            Text(if (state.continuousCollectionEnabled) "当前已开启" else "当前未开启")
            state.collectionStatus?.let { Text(it) }
        }
        PimSection("省电档") {
            Text("常规间隔：3 分钟")
            Text("日程低频：15 分钟")
            Text("移动观察：1 分钟")
            Text("恢复阈值：100m")
            Text("上传精度：< 50m")
            Text("高度等待：15 秒，仍缺失则上传空高度并标记质量。")
        }
        PimSection("权限") {
            Text("通知、前台定位、后台定位、活动识别和使用情况访问会在状态中心逐项展示。")
        }
    }
}
