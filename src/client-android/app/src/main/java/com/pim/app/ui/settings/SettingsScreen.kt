package com.pim.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import com.pim.app.settings.TrackingPresetCatalog
import com.pim.app.status.StatusPermissionNavigator
import com.pim.app.ui.components.PimSection
import com.pim.app.ui.permissions.permissionSettingRows
import com.pim.app.ui.status.repeatConnectionProbePolling
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatConnectionProbePolling {
            viewModel.refreshConnectionForVisibleScreen()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("恢复默认设置") },
            text = { Text("确定恢复采集、网络和日志的默认设置吗？服务器地址和登录状态将保留。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetOperationalDefaults()
                    showResetDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.titleLarge)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.saveApiAddress() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("保存")
                }
                OutlinedButton(onClick = viewModel::testConnection, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("测试连接")
                }
            }
        }

        PimSection("账号") {
            if (state.isLoggedIn) {
                Text("当前状态：已登录", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("退出登录")
                }
            } else {
                Text("当前状态：未登录")
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy && username.isNotBlank() && password.isNotBlank()
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.isBusy) "登录中" else "登录")
                }
            }
            state.loginStatus?.let { Text(it) }
        }

        PimSection("持续采集") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("持续采集")
                Switch(
                    checked = state.continuousCollectionEnabled,
                    onCheckedChange = viewModel::setContinuousCollectionEnabled
                )
            }
            Text(
                text = if (state.continuousCollectionEnabled) "当前：已开启" else "当前：已关闭",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.collectionStatus?.let { Text(it) }
        }

        PimSection("采集预设") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackingPresetCatalog.presets.forEach { preset ->
                    FilterChip(
                        selected = state.trackingProfile == preset.id,
                        onClick = { viewModel.applyTrackingPreset(preset.id) },
                        label = { Text(preset.displayName) }
                    )
                }
            }
        }

        PimSection("高级参数") {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { advancedExpanded = !advancedExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("高级参数", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (advancedExpanded) "收起" else "展开"
                )
            }
            if (advancedExpanded) {
                OutlinedTextField(
                    value = state.normalMinText,
                    onValueChange = viewModel::updateNormalMinText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("正常间隔（分钟）") },
                    singleLine = true,
                    isError = state.advancedErrors.containsKey("normalInterval"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = state.advancedErrors["normalInterval"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = state.scheduleMinText,
                    onValueChange = viewModel::updateScheduleMinText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日程低频间隔（分钟）") },
                    singleLine = true,
                    isError = state.advancedErrors.containsKey("scheduleInterval"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = state.advancedErrors["scheduleInterval"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = state.movementSecText,
                    onValueChange = viewModel::updateMovementSecText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("运动观察间隔（秒）") },
                    singleLine = true,
                    isError = state.advancedErrors.containsKey("movementInterval"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = state.advancedErrors["movementInterval"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = state.recoveryMetersText,
                    onValueChange = viewModel::updateRecoveryMetersText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("恢复阈值（米）") },
                    singleLine = true,
                    isError = state.advancedErrors.containsKey("recoveryThreshold"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = state.advancedErrors["recoveryThreshold"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = state.accuracyMetersText,
                    onValueChange = viewModel::updateAccuracyMetersText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("精度阈值（米）") },
                    singleLine = true,
                    isError = state.advancedErrors.containsKey("accuracy"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = state.advancedErrors["accuracy"]?.let { { Text(it) } }
                )
                OutlinedTextField(
                    value = state.altitudeSecText,
                    onValueChange = viewModel::updateAltitudeSecText,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("高度等待（秒）") },
                    singleLine = true,
                    isError = state.advancedErrors.containsKey("altitudeWait"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = state.advancedErrors["altitudeWait"]?.let { { Text(it) } }
                )
                Button(
                    onClick = viewModel::saveAdvancedSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("保存高级参数")
                }
            }
        }

        PimSection("网络") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动同步仅限非流量网络")
                Switch(
                    checked = state.syncOnUnmeteredOnly,
                    onCheckedChange = viewModel::setSyncOnUnmeteredOnly
                )
            }
        }

        PimSection("日志") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("详细日志")
                Switch(
                    checked = state.verboseLoggingEnabled,
                    onCheckedChange = viewModel::setVerboseLoggingEnabled
                )
            }
            if (state.verboseLoggingEnabled && state.verboseLoggingUntilUtcMillis != null) {
                val localTime = Instant.ofEpochMilli(state.verboseLoggingUntilUtcMillis!!)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                Text("自动关闭时间：$localTime")
            }
            Text("保留天数")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 7, 14, 30).forEach { days ->
                    FilterChip(
                        selected = state.logRetentionDays == days,
                        onClick = { viewModel.setLogRetentionDays(days) },
                        label = { Text("${days}天") }
                    )
                }
            }
        }

        PimSection("权限") {
            val rows = permissionSettingRows(state.permissions)
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { StatusPermissionNavigator.open(context, row.issueCode) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title)
                        Text(
                            text = if (row.isHardBlock) "采集必需" else "建议",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (row.granted) "已授权" else "未授权",
                        color = if (row.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (index < rows.lastIndex) {
                    Divider()
                }
            }
        }

        PimSection("恢复默认") {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("恢复默认设置")
            }
        }
    }
}
