package com.pim.app.ui

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.pim.app.data.AppDatabase
import com.pim.app.data.MobileLogEntity
import com.pim.app.location.LocationCaptureRepository
import com.pim.app.location.LocationCaptureState
import com.pim.app.location.LocationSnapshot
import com.pim.app.location.LocationSubmissionPolicy
import com.pim.app.mobile.logs.StructuredLogRepository
import com.pim.core.auth.TokenManager
import com.pim.core.models.LoginRequest
import com.pim.core.network.ApiClientProvider
import com.pim.core.settings.ServerSettingsStore
import com.pim.core.util.toCauseChainMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Composable
fun PimAppScaffold(
    locationViewModel: LocationCaptureViewModel = hiltViewModel(),
    statusViewModel: MobileStatusViewModel = hiltViewModel()
) {
    val locationState by locationViewModel.state.collectAsStateWithLifecycle()
    val uiState by statusViewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(PimTab.Status) }
    val context = LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationViewModel.startCapture()
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PIM Android",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "手机采集、同步与诊断",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(imageVector = selectedTab.icon, contentDescription = null)
                        }
                    }
                    TabRow(selectedTabIndex = selectedTab.ordinal) {
                        PimTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) },
                                icon = { Icon(tab.icon, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    PimTab.Status -> StatusTab(
                        locationState = locationState,
                        uiState = uiState,
                        usagePermissionGranted = hasUsageAccess(context),
                        preciseLocationGranted = hasFineLocationPermission(context),
                        onRefresh = statusViewModel::refresh
                    )
                    PimTab.Usage -> UsageTab(context)
                    PimTab.Location -> LocationTab(
                        state = locationState,
                        onRequestPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onStart = locationViewModel::startCapture,
                        onStop = locationViewModel::stopCapture,
                        onSubmit = locationViewModel::submitCurrentLocationManually
                    )
                    PimTab.Settings -> SettingsTab(
                        uiState = uiState,
                        onSaveServerUrl = statusViewModel::saveServerUrl,
                        onLogin = statusViewModel::login,
                        onClearLogin = statusViewModel::clearLogin
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusTab(
    locationState: LocationCaptureState,
    uiState: MobileUiState,
    usagePermissionGranted: Boolean,
    preciseLocationGranted: Boolean,
    onRefresh: () -> Unit
) {
    Section(title = "运行状态") {
        StatusRow("Android 客户端", "已打开")
        StatusRow("版本", uiState.appVersion)
        StatusRow("服务器", uiState.serverUrl)
        StatusRow("登录", if (uiState.isLoggedIn) "已登录" else "未登录")
        StatusRow("使用权限", if (usagePermissionGranted) "已授权" else "未授权")
        StatusRow("精确定位", if (preciseLocationGranted) "已授权" else "未授权")
        StatusRow("同步方式", "打开 App 后单次同步")
        StatusRow("后台保活", "未启用")
        StatusRow("定位", if (locationState.isCapturing) "采集中" else "待机")
        StatusRow("定位提交", locationState.submitStatus)
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("刷新状态")
        }
    }

    Section(title = "同步与传输") {
        StatusRow("阶段", uiState.phase)
        StatusRow("进度", uiState.progressText)
        StatusRow("已接收", uiState.acceptedCount.toString())
        StatusRow("已跳过", uiState.skippedCount.toString())
        StatusRow("已拒绝", uiState.rejectedCount.toString())
        StatusRow("失败", uiState.failedCount.toString())
        StatusRow("待传队列", uiState.pendingQueueCount.toString())
        StatusRow("最近尝试", uiState.lastAttemptedUploadAt ?: "无")
        StatusRow("最近成功", uiState.lastSuccessfulUploadAt ?: "无")
        StatusRow("最近错误", uiState.lastError ?: "无")
    }

    Section(title = "最近日志") {
        if (uiState.recentLogs.isEmpty()) {
            Text("暂无日志。")
        } else {
            uiState.recentLogs.forEach { log ->
                StatusRow("${log.level}/${log.tag}", "${formatTime(log.occurredAtUtc)}  ${log.message}")
            }
        }
    }
}

@Composable
private fun UsageTab(context: Context) {
    var hasUsageAccess by remember { mutableStateOf(hasUsageAccess(context)) }

    Section(title = "应用使用权限") {
        StatusRow("权限", if (hasUsageAccess) "已授权" else "未授权")
        Text("打开 App 后会请求服务器缺失窗口，只采集服务器返回的最近 14 天内数据。")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            ) {
                Icon(Icons.Filled.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("打开权限设置")
            }
            OutlinedButton(onClick = { hasUsageAccess = hasUsageAccess(context) }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("刷新")
            }
        }
    }
}

@Composable
private fun LocationTab(
    state: LocationCaptureState,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSubmit: () -> Unit
) {
    val snapshot = state.latest
    val decision = LocationSubmissionPolicy.decide(
        horizontalAccuracyMeters = snapshot?.horizontalAccuracyMeters,
        autoAlreadySubmitted = state.autoSubmitted
    )
    val inlineReason = state.inlineReason ?: if (snapshot != null) decision.reason else null

    Section(title = "手动定位") {
        StatusRow("状态", state.statusMessage)
        StatusRow("精度规则", if (snapshot == null) "等待位置" else decision.statusLabel)
        StatusRow("等待时长", formatDuration(state.waitDurationMs))
        Divider()
        LocationSnapshotRows(snapshot)
        Divider()
        StatusRow("提交状态", state.submitStatus)
        if (inlineReason != null) {
            Text(
                text = inlineReason,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onRequestPermission) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("授权定位")
            }
            OutlinedButton(onClick = onStart, enabled = !state.isCapturing) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("开始")
            }
            OutlinedButton(onClick = onStop, enabled = state.isCapturing) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("停止")
            }
        }
        Button(
            onClick = onSubmit,
            enabled = decision.canSubmitManually && snapshot != null && !state.isSubmitting
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("手动提交")
        }
    }
}

@Composable
private fun LocationSnapshotRows(snapshot: LocationSnapshot?) {
    StatusRow("纬度", snapshot?.latitude?.let { "%.6f".format(Locale.US, it) } ?: "等待")
    StatusRow("经度", snapshot?.longitude?.let { "%.6f".format(Locale.US, it) } ?: "等待")
    StatusRow("水平误差", snapshot?.horizontalAccuracyMeters?.let { "%.1f m".format(Locale.US, it) } ?: "等待")
    StatusRow("来源", snapshot?.provider ?: "等待")
    StatusRow("海拔", snapshot?.altitudeMeters?.let { "%.1f m".format(Locale.US, it) } ?: "无")
    StatusRow("速度", snapshot?.speedMetersPerSecond?.let { "%.2f m/s".format(Locale.US, it) } ?: "无")
    StatusRow("方向", snapshot?.bearingDegrees?.let { "%.1f deg".format(Locale.US, it) } ?: "无")
    StatusRow("时间", snapshot?.timeMillis?.let(::formatTime) ?: "等待")
}

@Composable
private fun SettingsTab(
    uiState: MobileUiState,
    onSaveServerUrl: (String) -> Unit,
    onLogin: (String, String) -> Unit,
    onClearLogin: () -> Unit
) {
    var serverUrl by rememberSaveable(uiState.serverUrl) { mutableStateOf(uiState.serverUrl) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var savedText by rememberSaveable { mutableStateOf("") }

    Section(title = "服务器设置") {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("服务器 URL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
        )
        Button(
            onClick = {
                onSaveServerUrl(serverUrl)
                savedText = "已保存"
            }
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("保存")
        }
        if (savedText.isNotEmpty()) {
            Text(savedText, color = MaterialTheme.colorScheme.primary)
        }
    }

    Section(title = "登录") {
        StatusRow("当前状态", if (uiState.isLoggedIn) "已保存令牌" else "未登录")
        StatusRow("应用版本", uiState.appVersion)
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onLogin(username, password) },
                enabled = !uiState.isLoginInProgress
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isLoginInProgress) "登录中" else "登录")
            }
            OutlinedButton(onClick = onClearLogin) {
                Text("清除登录")
            }
        }
        uiState.loginStatus?.let {
            Text(it, color = if (uiState.isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@HiltViewModel
class LocationCaptureViewModel @Inject constructor(
    private val repository: LocationCaptureRepository
) : ViewModel() {
    val state = repository.state

    fun startCapture() = repository.startCapture()

    fun stopCapture() = repository.stopCapture()

    fun submitCurrentLocationManually() = repository.submitCurrentLocationManually()

    override fun onCleared() {
        repository.stopCapture()
        super.onCleared()
    }
}

data class MobileUiState(
    val serverUrl: String = ServerSettingsStore.DEFAULT_BASE_URL,
    val appVersion: String = "未知",
    val isLoggedIn: Boolean = false,
    val phase: String = "等待同步",
    val progressText: String = "打开 App 后会自动同步一次。",
    val acceptedCount: Int = 0,
    val skippedCount: Int = 0,
    val rejectedCount: Int = 0,
    val failedCount: Int = 0,
    val lastError: String? = null,
    val pendingQueueCount: Int = 0,
    val lastAttemptedUploadAt: String? = null,
    val lastSuccessfulUploadAt: String? = null,
    val recentLogs: List<MobileLogLine> = emptyList(),
    val loginStatus: String? = null,
    val isLoginInProgress: Boolean = false
)

data class MobileLogLine(
    val level: String,
    val tag: String,
    val message: String,
    val occurredAtUtc: Long
)

@HiltViewModel
class MobileStatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val apiClientProvider: ApiClientProvider,
    private val serverSettingsStore: ServerSettingsStore,
    private val database: AppDatabase,
    private val logs: StructuredLogRepository
) : ViewModel() {
    private val prefs = context.getSharedPreferences("pim_mobile_sync_state", Context.MODE_PRIVATE)
    private val mobileDataDao = database.mobileDataDao()

    private val _state = MutableStateFlow(MobileUiState())
    val state: StateFlow<MobileUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            mobileDataDao.recentLogs().collect { logs ->
                _state.update { current -> current.copy(recentLogs = logs.map { it.toLine() }) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val pending = pendingQueueCount()
            _state.update { current ->
                current.copy(
                    serverUrl = serverSettingsStore.getBaseUrl(),
                    appVersion = appVersionDisplay(),
                    isLoggedIn = !tokenManager.getAccessToken().isNullOrBlank(),
                    phase = prefs.getString("phase", null) ?: current.phase,
                    progressText = prefs.getString("progress_text", null) ?: current.progressText,
                    acceptedCount = prefs.getInt("accepted_count", current.acceptedCount),
                    skippedCount = prefs.getInt("skipped_count", current.skippedCount),
                    rejectedCount = prefs.getInt("rejected_count", current.rejectedCount),
                    failedCount = prefs.getInt("failed_count", current.failedCount),
                    lastError = prefs.getString("last_error", current.lastError),
                    lastAttemptedUploadAt = prefs.getString("last_attempted_upload_at", current.lastAttemptedUploadAt),
                    lastSuccessfulUploadAt = prefs.getString("last_successful_upload_at", current.lastSuccessfulUploadAt),
                    pendingQueueCount = pending
                )
            }
        }
    }

    fun saveServerUrl(value: String) {
        val normalized = serverSettingsStore.setBaseUrl(value)
        _state.update { it.copy(serverUrl = normalized) }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.update { it.copy(loginStatus = "请输入用户名和密码。") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoginInProgress = true, loginStatus = "正在登录...") }
            runCatching {
                val response = apiClientProvider.refreshApiService().login(LoginRequest(username.trim(), password))
                val auth = response.data
                if (response.code != 0 || auth == null) {
                    error(response.message.ifBlank { "登录失败。" })
                }
                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
            }.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isLoggedIn = true,
                            isLoginInProgress = false,
                            loginStatus = "登录成功，已保存令牌。"
                        )
                    }
                },
                onFailure = { error ->
                    val failureMessage = error.toCauseChainMessage()
                    runCatching {
                        logs.error("mobile-auth", "登录失败：$failureMessage", error)
                    }
                    _state.update {
                        it.copy(
                            isLoggedIn = false,
                            isLoginInProgress = false,
                            loginStatus = "登录失败：$failureMessage",
                            lastError = failureMessage
                        )
                    }
                }
            )
        }
    }

    fun clearLogin() {
        tokenManager.clear()
        _state.update {
            it.copy(
                isLoggedIn = false,
                loginStatus = "已清除登录令牌。"
            )
        }
    }

    private suspend fun pendingQueueCount(): Int {
        return mobileDataDao.pendingUsageEventCount().first() +
            mobileDataDao.pendingUsageSummaryCount().first() +
            mobileDataDao.pendingAppMetadataCount().first() +
            mobileDataDao.pendingLocationPointCount().first() +
            mobileDataDao.pendingSyncBatchCount().first() +
            mobileDataDao.pendingLogCount().first() +
            mobileDataDao.pendingDeviceProfileCount().first()
    }

    private fun MobileLogEntity.toLine(): MobileLogLine {
        return MobileLogLine(
            level = level,
            tag = tag ?: "mobile",
            message = message,
            occurredAtUtc = occurredAtUtc
        )
    }

    private fun appVersionDisplay(): String {
        return try {
            val info = packageInfo(context.packageManager, context.packageName)
            "${info.versionName ?: "unknown"} (${versionCode(info)})"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun packageInfo(packageManager: PackageManager, packageName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }

    private fun versionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}

private enum class PimTab(
    val title: String,
    val icon: ImageVector
) {
    Status("状态", Icons.Filled.CheckCircle),
    Usage("使用", Icons.Filled.Security),
    Location("定位", Icons.Filled.LocationOn),
    Settings("设置", Icons.Filled.Settings)
}

private fun hasUsageAccess(context: Context): Boolean {
    val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return false
    val now = System.currentTimeMillis()
    return try {
        manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            (now - 24 * 60 * 60 * 1000L).coerceAtLeast(0L),
            now
        ).orEmpty().isNotEmpty()
    } catch (_: SecurityException) {
        false
    }
}

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).coerceAtLeast(0L)
    val minutes = seconds / 60
    val remainder = seconds % 60
    return if (minutes > 0) {
        "${minutes}分${remainder}秒"
    } else {
        "${remainder}秒"
    }
}

private fun formatTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}
