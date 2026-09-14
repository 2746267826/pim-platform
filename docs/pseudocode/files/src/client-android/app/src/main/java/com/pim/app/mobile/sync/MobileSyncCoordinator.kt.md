# src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：协调器 `MobileSyncState`：编排多步骤同步或上传流程。
- 主要依赖：`src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`、`src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`、`src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt`、`src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt`、`src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt`、`src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### MobileSyncState
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L47 声明 `MobileSyncState`
- 分支与异常：无
- 调用：无

### MobileSyncCoordinator
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L74 声明 `MobileSyncCoordinator`
- 分支与异常：无
- 调用：无

### UploadWindow
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L916 声明 `UploadWindow`
- 分支与异常：无
- 调用：无

### PendingUsageBatch
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L1097 声明 `PendingUsageBatch`
- 分支与异常：无
- 调用：无

### syncOnOpen
#### syncOnOpen(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 挂起函数 `syncOnOpen`
  2. 若 (!syncMutex.tryLock()) 则
  3. 执行：val running = _state.value.copy(
  4. 执行：isInProgress = true,
  5. 执行：progressText = "同步正在进行中。"
  6. 执行：persistState(running)
  7. 返回 running
  8. 返回 try {
  9. 执行：runSyncOnOpen()
  10. 执行：syncMutex.unlock()
- 分支与异常：if (!syncMutex.tryLock()) {
- 调用：syncOnOpen、syncMutex.tryLock、_state.value.copy、persistState、runSyncOnOpen、syncMutex.unlock

### refreshPersistedState
#### refreshPersistedState(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `refreshPersistedState` 参数：无
  2. 执行：_state.value = readPersistedState()
- 分支与异常：无显著分支
- 调用：refreshPersistedState、readPersistedState

### runSyncOnOpen
#### runSyncOnOpen(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private suspend fun runSyncOnOpen(): MobileSyncState {
  2. 执行：val attemptedAt = nowIso()
  3. 执行：val serverUrl = configuredServerUrl()
  4. 执行：val deviceIdentity = deviceIdentity()
  5. 执行：val hasToken = !tokenManager
  6. 执行：.getAccessTokenForServer(serverSettingsStore.getBaseUrl())
  7. 执行：.isNullOrBlank()
  8. 执行：val hasUsageAccess = usageAccessChecker.hasUsageAccess()
  9. 若 (serverUrl.isBlank()) 则
  10. 返回 finishWithLocalError(
  11. 执行：deviceId = deviceIdentity.deviceId,
  12. 执行：serverUrl = serverUrl,
  13. 执行：usagePermissionGranted = hasUsageAccess,
  14. 执行：attemptedAt = attemptedAt,
  15. 执行：phase = "server-missing",
  16. 执行：message = "服务器地址未配置，已跳过同步。"
  17. 若 (!hasToken) 则
  18. 执行：logs.warn("mobile-sync", "缺少登录令牌，已跳过同步。")
  19. 执行：val authMissing = state(
  20. 执行：phase = "auth-missing",
  21. 执行：progressText = "缺少登录令牌，已跳过同步。请登录后重新同步。",
  22. 执行：outcome = MobileSyncOutcome.BLOCKED,
  23. 执行：lastError = "缺少登录令牌",
  24. 执行：lastAttemptedUploadAt = attemptedAt
  25. 执行：persistState(authMissing)
  26. 返回 authMissing
  27. 若 (!hasUsageAccess) 则
  28. 执行：logs.warn("mobile-sync", "缺少应用使用情况权限，已跳过使用记录同步。")
  29. 执行：val missingPermissionState = state(
  30. 执行：phase = "usage-permission-missing",
- 分支与异常：if (serverUrl.isBlank()) {；if (!hasToken) {；if (!hasUsageAccess) {；if (oldQueueState != null) {；if (oldQueueState.outcome == MobileSyncOutcome.RETRY || pendingUsageRemaining(mobileDataDao) > 0) {；if (gapResponse.code != 0 || gapData == null) {；if (clamped.windowStartUtc != originalStart || clamped.windowEndUtc != originalEnd) {；if (appMetadata.isNotEmpty()) {
- 调用：runSyncOnOpen、nowIso、configuredServerUrl、deviceIdentity、getAccessTokenForServer、serverSettingsStore.getBaseUrl、isNullOrBlank、usageAccessChecker.hasUsageAccess、serverUrl.isBlank、finishWithLocalError、logs.warn、state、persistState、uploadQueuedLocations、sendHeartbeat

### pendingQueueCount
#### pendingQueueCount(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private suspend fun pendingQueueCount(): Int {
  2. 返回 mobileDataDao.pendingUsageEventCount().first() +
  3. 执行：mobileDataDao.pendingUsageSummaryCount().first() +
  4. 执行：mobileDataDao.pendingAppMetadataCount().first() +
  5. 执行：mobileDataDao.pendingLocationPointCount().first()
- 分支与异常：无显著分支
- 调用：pendingQueueCount、mobileDataDao.pendingUsageEventCount、first、mobileDataDao.pendingUsageSummaryCount、mobileDataDao.pendingAppMetadataCount、mobileDataDao.pendingLocationPointCount

### deviceIdentity
#### deviceIdentity(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun deviceIdentity(): DeviceIdentity {
  2. 执行：val androidId = Settings.Secure.getString(
  3. 执行：context.contentResolver,
  4. 执行：Settings.Secure.ANDROID_ID
  5. 执行：val seed = androidId ?: Build.FINGERPRINT ?: "android-device"
  6. 执行：val hash = sha256(seed)
  7. 返回 DeviceIdentity(
  8. 执行：deviceId = "android-${hash.take(16)}",
  9. 执行：androidIdHash = androidId?.let { sha256(it) }
- 分支与异常：无显著分支
- 调用：deviceIdentity、Settings.Secure.getString、sha256、DeviceIdentity、hash.take

### configuredServerUrl
#### configuredServerUrl(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun configuredServerUrl(): String {
  2. 返回 normalizeServerUrl(serverSettingsStore.getBaseUrl())
- 分支与异常：无显著分支
- 调用：configuredServerUrl、normalizeServerUrl、serverSettingsStore.getBaseUrl

### normalizeServerUrl
#### normalizeServerUrl(value: String)
- 输入：value: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun normalizeServerUrl(value: String): String {
  2. 执行：val trimmed = value.trim().trimEnd('/')
  3. 返回 try {
  4. 执行：val uri = URI(trimmed)
  5. 若 (uri.host.equals("localhost", ignoreCase = true)) 则
  6. 执行：URI(uri.scheme, uri.userInfo, "127.0.0.1", uri.port, uri.path, uri.query, uri.fragment)
  7. 执行：.toString()
  8. 执行：.trimEnd('/')
- 分支与异常：if (uri.host.equals("localhost", ignoreCase = true)) {
- 调用：normalizeServerUrl、value.trim、trimEnd、URI、uri.host.equals、toString

### capabilityJson
#### capabilityJson(usagePermissionGranted: Boolean)
- 输入：usagePermissionGranted: Boolean
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun capabilityJson(usagePermissionGranted: Boolean): String {
  2. 返回 JSONObject()
  3. 执行：.put("usageEvents", usagePermissionGranted)
  4. 执行：.put("usageStatsFallback", usagePermissionGranted)
  5. 执行：.put("appMetadata", true)
  6. 执行：.put("maxBackfillDays", 14)
  7. 执行：.put("client", "android")
  8. 执行：.toString()
- 分支与异常：无显著分支
- 调用：capabilityJson、JSONObject、put、toString

### persistState
#### persistState(state: MobileSyncState)
- 输入：state: MobileSyncState
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun persistState(state: MobileSyncState) {
  2. 执行：prefs.edit()
  3. 执行：.putString("phase", state.phase)
  4. 执行：.putString("progress_text", state.progressText)
  5. 执行：.putBoolean("is_in_progress", state.isInProgress)
  6. 执行：.putString("outcome", state.outcome.name)
  7. 执行：.putInt("accepted_count", state.acceptedCount)
  8. 执行：.putInt("skipped_count", state.skippedCount)
  9. 执行：.putInt("rejected_count", state.rejectedCount)
  10. 执行：.putInt("failed_count", state.failedCount)
  11. 执行：.putString("last_error", state.lastError)
  12. 执行：.putString("last_error_detail", state.lastErrorDetail)
  13. 执行：.putInt("pending_queue_count", state.pendingQueueCount)
  14. 执行：.putInt("gap_window_count", state.gapWindowCount)
  15. 执行：.putInt("current_window_index", state.currentWindowIndex)
  16. 执行：.putString("current_window_start_utc", state.currentWindowStartUtc)
  17. 执行：.putString("current_window_end_utc", state.currentWindowEndUtc)
  18. 执行：.putInt("current_event_count", state.currentEventCount)
  19. 执行：.putInt("current_summary_count", state.currentSummaryCount)
  20. 执行：.putInt("current_app_metadata_count", state.currentAppMetadataCount)
  21. 执行：.putString("last_batch_id", state.lastBatchId)
  22. 执行：.putString("last_batch_status", state.lastBatchStatus)
  23. 执行：.putString("heartbeat_status", state.heartbeatStatus)
  24. 执行：.putString("last_attempted_upload_at", state.lastAttemptedUploadAt)
  25. 执行：.putString("last_successful_upload_at", state.lastSuccessfulUploadAt)
  26. 执行：.commit()
  27. 执行：_state.value = state
- 分支与异常：无显著分支
- 调用：persistState、prefs.edit、putString、putBoolean、putInt、commit

### readPersistedState
#### readPersistedState(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun readPersistedState(): MobileSyncState {
  2. 返回 MobileSyncState(
  3. 执行：phase = prefs.getString("phase", null) ?: "waiting",
  4. 执行：progressText = prefs.getString("progress_text", null) ?: "打开 App 后会自动同步一次。",
  5. 执行：isInProgress = prefs.getBoolean("is_in_progress", false),
  6. 执行：outcome = try { MobileSyncOutcome.valueOf(prefs.getString("outcome", "SUCCESS") ?: "SUCCESS") } catch (_: Exce
  7. 执行：acceptedCount = prefs.getInt("accepted_count", 0),
  8. 执行：skippedCount = prefs.getInt("skipped_count", 0),
  9. 执行：rejectedCount = prefs.getInt("rejected_count", 0),
  10. 执行：failedCount = prefs.getInt("failed_count", 0),
  11. 执行：lastError = prefs.getString("last_error", null),
  12. 执行：lastErrorDetail = prefs.getString("last_error_detail", null),
  13. 执行：pendingQueueCount = prefs.getInt("pending_queue_count", 0),
  14. 执行：gapWindowCount = prefs.getInt("gap_window_count", 0),
  15. 执行：currentWindowIndex = prefs.getInt("current_window_index", 0),
  16. 执行：currentWindowStartUtc = prefs.getString("current_window_start_utc", null),
  17. 执行：currentWindowEndUtc = prefs.getString("current_window_end_utc", null),
  18. 执行：currentEventCount = prefs.getInt("current_event_count", 0),
  19. 执行：currentSummaryCount = prefs.getInt("current_summary_count", 0),
  20. 执行：currentAppMetadataCount = prefs.getInt("current_app_metadata_count", 0),
  21. 执行：lastBatchId = prefs.getString("last_batch_id", null),
  22. 执行：lastBatchStatus = prefs.getString("last_batch_status", null),
  23. 执行：heartbeatStatus = prefs.getString("heartbeat_status", null),
  24. 执行：lastAttemptedUploadAt = prefs.getString("last_attempted_upload_at", null),
  25. 执行：lastSuccessfulUploadAt = prefs.getString("last_successful_upload_at", null)
- 分支与异常：无显著分支
- 调用：readPersistedState、MobileSyncState、prefs.getString、prefs.getBoolean、MobileSyncOutcome.valueOf、prefs.getInt

### previousSuccessfulUploadAt
#### previousSuccessfulUploadAt(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun previousSuccessfulUploadAt(): String? {
  2. 返回 prefs.getString("last_successful_upload_at", null)
- 分支与异常：无显著分支
- 调用：previousSuccessfulUploadAt、prefs.getString

### displayName
#### displayName(profile: MobileDeviceProfileEntity)
- 输入：profile: MobileDeviceProfileEntity
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun displayName(profile: MobileDeviceProfileEntity): String {
  2. 返回 listOf(profile.manufacturer, profile.model)
  3. 执行：.filter { it.isNotBlank() }
  4. 执行：.joinToString(" ")
  5. 执行：.ifBlank { "Android device" }
- 分支与异常：无显著分支
- 调用：displayName、listOf、it.isNotBlank、joinToString

### appVersion
#### appVersion(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun appVersion(): Pair<String?, Long?> {
  2. 返回 try {
  3. 执行：val info = packageInfo(context.packageManager, context.packageName)
  4. 执行：info.versionName to versionCode(info)
  5. 执行：null to null
- 分支与异常：无显著分支
- 调用：appVersion、packageInfo、versionCode

### packageInfo
#### packageInfo(packageManager: PackageManager, packageName: String)
- 输入：packageManager: PackageManager, packageName: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun packageInfo(packageManager: PackageManager, packageName: String): PackageInfo {
  2. 返回 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
  3. 执行：packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
  4. 注解 @Suppress
  5. 执行：packageManager.getPackageInfo(packageName, 0)
- 分支与异常：无显著分支
- 调用：packageInfo、packageManager.getPackageInfo、PackageManager.PackageInfoFlags.of、Suppress

### versionCode
#### versionCode(packageInfo: PackageInfo)
- 输入：packageInfo: PackageInfo
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun versionCode(packageInfo: PackageInfo): Long {
  2. 返回 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
  3. 执行：packageInfo.longVersionCode
  4. 注解 @Suppress
  5. 执行：packageInfo.versionCode.toLong()
- 分支与异常：无显著分支
- 调用：versionCode、Suppress、packageInfo.versionCode.toLong

### sha256
#### sha256(value: String)
- 输入：value: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun sha256(value: String): String {
  2. 执行：val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
  3. 返回 bytes.joinToString("") { "%02x".format(Locale.US, it) }
- 分支与异常：无显著分支
- 调用：sha256、MessageDigest.getInstance、digest、value.toByteArray、bytes.joinToString、format

### stableBatchId
#### stableBatchId(deviceId: String, windowStartUtc: String, windowEndUtc: String)
- 输入：deviceId: String, windowStartUtc: String, windowEndUtc: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun stableBatchId(deviceId: String, windowStartUtc: String, windowEndUtc: String): String {
  2. 返回 "android-${sha256("$deviceId|$windowStartUtc|$windowEndUtc").take(24)}"
- 分支与异常：无显著分支
- 调用：stableBatchId、sha256、take

### sortedMergeOutcome
#### sortedMergeOutcome(a: MobileSyncOutcome, b: MobileSyncOutcome)
- 输入：a: MobileSyncOutcome, b: MobileSyncOutcome
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：internal fun sortedMergeOutcome(a: MobileSyncOutcome, b: MobileSyncOutcome): MobileSyncOutcome {
  2. 返回 when {
  3. 分支臂：a == MobileSyncOutcome.RETRY || b == MobileSyncOutcome.RETRY -> MobileSyncOutcome.RETRY
  4. 分支臂：a == MobileSyncOutcome.BLOCKED || b == MobileSyncOutcome.BLOCKED -> MobileSyncOutcome.BLOCKED
  5. when 默认 else
- 分支与异常：else -> MobileSyncOutcome.SUCCESS
- 调用：sortedMergeOutcome

### androidCategoryName
#### androidCategoryName(category: Int?)
- 输入：category: Int?
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun androidCategoryName(category: Int?): String? {
  2. 执行：if (category == null) return null
  3. 返回 when (category) {
  4. 分支臂：ApplicationInfo.CATEGORY_GAME -> "game"
  5. 分支臂：ApplicationInfo.CATEGORY_AUDIO -> "audio"
  6. 分支臂：ApplicationInfo.CATEGORY_VIDEO -> "video"
  7. 分支臂：ApplicationInfo.CATEGORY_IMAGE -> "camera"
  8. 分支臂：ApplicationInfo.CATEGORY_SOCIAL -> "social"
  9. 分支臂：ApplicationInfo.CATEGORY_NEWS -> "news"
  10. 分支臂：ApplicationInfo.CATEGORY_MAPS -> "maps"
  11. 分支臂：ApplicationInfo.CATEGORY_PRODUCTIVITY -> "productivity"
  12. when 默认 else
- 分支与异常：if (category == null) return null；return when (category) {；else -> null
- 调用：androidCategoryName

### mergeCategoryName
#### mergeCategoryName(rawJson: String, categoryName: String?)
- 输入：rawJson: String, categoryName: String?
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun mergeCategoryName(rawJson: String, categoryName: String?): String {
  2. 执行：if (categoryName.isNullOrBlank()) return rawJson
  3. 返回 try {
  4. 执行：JSONObject(rawJson)
  5. 执行：.put("categoryName", categoryName)
  6. 执行：.toString()
  7. 执行：JSONObject()
- 分支与异常：if (categoryName.isNullOrBlank()) return rawJson
- 调用：mergeCategoryName、categoryName.isNullOrBlank、JSONObject、put、toString

### nowIso
#### nowIso(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun nowIso(): String = iso(System.currentTimeMillis())
- 分支与异常：无显著分支
- 调用：nowIso、iso、System.currentTimeMillis

### iso
#### iso(epochMillis: Long)
- 输入：epochMillis: Long
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun iso(epochMillis: Long): String {
  2. 返回 Instant.ofEpochMilli(epochMillis).toString()
- 分支与异常：无显著分支
- 调用：iso、Instant.ofEpochMilli、toString

### parseIsoMillis
#### parseIsoMillis(value: String)
- 输入：value: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun parseIsoMillis(value: String): Long {
  2. 返回 Instant.parse(value).toEpochMilli()
- 分支与异常：无显著分支
- 调用：parseIsoMillis、Instant.parse、toEpochMilli

### pendingUsageRemaining
#### pendingUsageRemaining(dao: MobileDataDao)
- 输入：dao: MobileDataDao
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：internal suspend fun pendingUsageRemaining(dao: MobileDataDao): Int {
  2. 返回 dao.pendingUsageEventCount().first() +
  3. 执行：dao.pendingUsageSummaryCount().first() +
  4. 执行：dao.pendingAppMetadataCount().first()
- 分支与异常：无显著分支
- 调用：pendingUsageRemaining、dao.pendingUsageEventCount、first、dao.pendingUsageSummaryCount、dao.pendingAppMetadataCount

## 近逐行中文伪代码

1. [L47] 定义类 `MobileSyncState`
2. [L48] 执行：val phase: String,
3. [L49] 执行：val progressText: String,
4. [L50] 执行：val isInProgress: Boolean = false,
5. [L51] 执行：val outcome: MobileSyncOutcome = MobileSyncOutcome.SUCCESS,
6. [L52] 执行：val acceptedCount: Int = 0,
7. [L53] 执行：val skippedCount: Int = 0,
8. [L54] 执行：val rejectedCount: Int = 0,
9. [L55] 执行：val failedCount: Int = 0,
10. [L56] 执行：val lastError: String? = null,
11. [L57] 执行：val lastErrorDetail: String? = null,
12. [L58] 执行：val pendingQueueCount: Int = 0,
13. [L59] 执行：val gapWindowCount: Int = 0,
14. [L60] 执行：val currentWindowIndex: Int = 0,
15. [L61] 执行：val currentWindowStartUtc: String? = null,
16. [L62] 执行：val currentWindowEndUtc: String? = null,
17. [L63] 执行：val currentEventCount: Int = 0,
18. [L64] 执行：val currentSummaryCount: Int = 0,
19. [L65] 执行：val currentAppMetadataCount: Int = 0,
20. [L66] 执行：val lastBatchId: String? = null,
21. [L67] 执行：val lastBatchStatus: String? = null,
22. [L68] 执行：val heartbeatStatus: String? = null,
23. [L69] 执行：val lastAttemptedUploadAt: String? = null,
24. [L70] 执行：val lastSuccessfulUploadAt: String? = null
25. [L73] 注解 @Singleton
26. [L74] 定义类 `MobileSyncCoordinator`
27. [L75] 注解 @ApplicationContext
28. [L76] 执行：private val api: ApiService,
29. [L77] 执行：private val tokenManager: TokenManager,
30. [L78] 执行：private val usageAccessChecker: UsageAccessChecker,
31. [L79] 执行：private val usageEventCollector: UsageEventCollector,
32. [L80] 执行：private val appMetadataCollector: AppMetadataCollector,
33. [L81] 执行：private val database: AppDatabase,
34. [L82] 执行：private val logs: StructuredLogRepository,
35. [L83] 执行：private val heartbeatReporter: MobileHeartbeatReporter,
36. [L84] 执行：private val serverSettingsStore: ServerSettingsStore,
37. [L85] 执行：private val locationUploadCoordinator: LocationUploadCoordinator
38. [L87] 执行：private val mobileDataDao = database.mobileDataDao()
39. [L88] 执行：private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
40. [L89] 执行：private val syncMutex = Mutex()
41. [L90] 执行：private val _state = MutableStateFlow(readPersistedState())
42. [L91] 执行：val currentState: StateFlow<MobileSyncState> = _state.asStateFlow()
43. [L93] 挂起函数 `syncOnOpen`
44. [L94] 若 (!syncMutex.tryLock()) 则
45. [L95] 执行：val running = _state.value.copy(
46. [L96] 执行：isInProgress = true,
47. [L97] 执行：progressText = "同步正在进行中。"
48. [L99] 执行：persistState(running)
49. [L100] 返回 running
50. [L103] 返回 try {
51. [L104] 执行：runSyncOnOpen()
52. [L106] 执行：syncMutex.unlock()
53. [L110] 函数 `refreshPersistedState` 参数：无
54. [L111] 执行：_state.value = readPersistedState()
55. [L114] 执行：private suspend fun runSyncOnOpen(): MobileSyncState {
56. [L115] 执行：val attemptedAt = nowIso()
57. [L116] 执行：val serverUrl = configuredServerUrl()
58. [L117] 执行：val deviceIdentity = deviceIdentity()
59. [L118] 执行：val hasToken = !tokenManager
60. [L119] 执行：.getAccessTokenForServer(serverSettingsStore.getBaseUrl())
61. [L120] 执行：.isNullOrBlank()
62. [L121] 执行：val hasUsageAccess = usageAccessChecker.hasUsageAccess()
63. [L123] 若 (serverUrl.isBlank()) 则
64. [L124] 返回 finishWithLocalError(
65. [L125] 执行：deviceId = deviceIdentity.deviceId,
66. [L126] 执行：serverUrl = serverUrl,
67. [L127] 执行：usagePermissionGranted = hasUsageAccess,
68. [L128] 执行：attemptedAt = attemptedAt,
69. [L129] 执行：phase = "server-missing",
70. [L130] 执行：message = "服务器地址未配置，已跳过同步。"
71. [L134] 若 (!hasToken) 则
72. [L135] 执行：logs.warn("mobile-sync", "缺少登录令牌，已跳过同步。")
73. [L136] 执行：val authMissing = state(
74. [L137] 执行：phase = "auth-missing",
75. [L138] 执行：progressText = "缺少登录令牌，已跳过同步。请登录后重新同步。",
76. [L139] 执行：outcome = MobileSyncOutcome.BLOCKED,
77. [L140] 执行：lastError = "缺少登录令牌",
78. [L141] 执行：lastAttemptedUploadAt = attemptedAt
79. [L143] 执行：persistState(authMissing)
80. [L144] 返回 authMissing
81. [L147] 若 (!hasUsageAccess) 则
82. [L148] 执行：logs.warn("mobile-sync", "缺少应用使用情况权限，已跳过使用记录同步。")
83. [L149] 执行：val missingPermissionState = state(
84. [L150] 执行：phase = "usage-permission-missing",
85. [L151] 执行：progressText = "缺少应用使用情况权限，已跳过同步。",
86. [L152] 执行：outcome = MobileSyncOutcome.BLOCKED,
87. [L153] 执行：skippedCount = 1,
88. [L154] 执行：lastError = "缺少应用使用情况权限",
89. [L155] 执行：lastAttemptedUploadAt = attemptedAt
90. [L157] 执行：persistState(missingPermissionState)
91. [L158] 执行：val locationState = uploadQueuedLocations(missingPermissionState, attemptedAt)
92. [L159] 执行：sendHeartbeat(deviceIdentity.deviceId, serverUrl, false, locationState)
93. [L160] 返回 locationState
94. [L163] 返回 try {
95. [L164] 执行：logs.info(
96. [L165] 执行："mobile-sync",
97. [L166] 执行："开始执行打开 App 后的手机同步。",
98. [L167] 执行：mapOf("deviceId" to deviceIdentity.deviceId, "serverUrl" to serverUrl)
99. [L170] 执行：val preparing = state(
100. [L171] 执行：phase = "preparing",
101. [L172] 执行：progressText = "正在注册设备并准备同步。",
102. [L173] 执行：isInProgress = true,
103. [L174] 执行：pendingQueueCount = pendingQueueCount(),
104. [L175] 执行：lastAttemptedUploadAt = attemptedAt
105. [L177] 执行：persistState(preparing)
106. [L179] 执行：val profile = buildDeviceProfile(deviceIdentity, nowUtc = System.currentTimeMillis())
107. [L180] 执行：mobileDataDao.upsertDeviceProfile(profile)
108. [L181] 执行：registerDevice(deviceIdentity, profile)
109. [L182] 执行：mobileDataDao.updateDeviceProfileSyncStatus(syncStatus = MobileSyncStatus.SYNCED)
110. [L184] 执行：val oldQueueState = uploadQueuedUsage(deviceIdentity.deviceId, attemptedAt)
111. [L185] 若 (oldQueueState != null) 则
112. [L186] 若 (oldQueueState.outcome == MobileSyncOutcome.RETRY || pendingUsageRemaining(mobileDataDao) > 0) 则
113. [L187] 返回 oldQueueState
114. [L191] 执行：val rangeEndUtc = System.currentTimeMillis()
115. [L192] 执行：val rangeStartUtc = rangeEndUtc - FOURTEEN_DAYS_MS
116. [L193] 执行：val gapChecking = state(
117. [L194] 执行：phase = "gap-checking",
118. [L195] 执行：progressText = "正在询问服务器缺失时间窗。",
119. [L196] 执行：isInProgress = true,
120. [L197] 执行：pendingQueueCount = pendingQueueCount(),
121. [L198] 执行：lastAttemptedUploadAt = attemptedAt
122. [L200] 执行：persistState(oldQueueState?.let { gapChecking.merge(it) } ?: gapChecking)
123. [L201] 执行：val gapResponse = api.getMobileGaps(
124. [L202] 执行：MobileGapRequest(
125. [L203] 执行：deviceIdentity.deviceId,
126. [L204] 执行：iso(rangeStartUtc),
127. [L205] 执行：iso(rangeEndUtc),
128. [L206] 执行：capabilityJson(hasUsageAccess)
129. [L210] 执行：val gapData = gapResponse.data
130. [L211] 若 (gapResponse.code != 0 || gapData == null) 则
131. [L212] 抛出 IllegalStateException(gapResponse.message.ifBlank { "服务器缺口查询失败。" })
132. [L215] 分支臂：val serverWindows = gapData.windows.mapNotNull { window ->
133. [L216] 执行：val originalStart = parseIsoMillis(window.windowStartUtc)
134. [L217] 执行：val originalEnd = parseIsoMillis(window.windowEndUtc)
135. [L218] 执行：clampGapWindow(
136. [L219] 执行：windowStartUtc = originalStart,
137. [L220] 执行：windowEndUtc = originalEnd,
138. [L221] 执行：maxBackfillStartUtc = parseIsoMillis(gapData.maxBackfillStartUtc),
139. [L222] 执行：nowUtc = rangeEndUtc
140. [L223] 分支臂：)?.also { clamped ->
141. [L224] 若 (clamped.windowStartUtc != originalStart || clamped.windowEndUtc != originalEnd) 则
142. [L225] 执行：logs.warn(
143. [L226] 执行："mobile-sync",
144. [L227] 执行："服务器缺口窗口已按 Android 14 天补全上限裁剪。",
145. [L229] 执行："originalStartUtc" to window.windowStartUtc,
146. [L230] 执行："originalEndUtc" to window.windowEndUtc,
147. [L231] 执行："clampedStartUtc" to iso(clamped.windowStartUtc),
148. [L232] 执行："clampedEndUtc" to iso(clamped.windowEndUtc)
149. [L238] 分支臂：val windows = serverWindows.flatMap { window ->
150. [L239] 执行：splitGapWindowForUpload(window.windowStartUtc, window.windowEndUtc)
151. [L241] 执行：logs.info(
152. [L242] 执行："mobile-sync",
153. [L243] 执行："服务器返回 ${serverWindows.size} 个缺口窗口，已拆为 ${windows.size} 个上传窗口。",
154. [L244] 执行：mapOf("serverWindowCount" to serverWindows.size, "uploadWindowCount" to windows.size)
155. [L247] 赋值 `current` = state(
156. [L248] 执行：phase = "collecting",
157. [L249] 执行：progressText = "正在采集服务器要求补全的窗口。",
158. [L250] 执行：isInProgress = true,
159. [L251] 执行：gapWindowCount = windows.size,
160. [L252] 执行：pendingQueueCount = pendingQueueCount(),
161. [L253] 执行：lastAttemptedUploadAt = attemptedAt
162. [L255] 执行：current = oldQueueState?.let { current.merge(it) } ?: current
163. [L256] 执行：persistState(current)
164. [L258] 循环 for ((index, window) in windows.withIndex())
165. [L259] 执行：val windowStartUtc = iso(window.windowStartUtc)
166. [L260] 执行：val windowEndUtc = iso(window.windowEndUtc)
167. [L261] 执行：current = current.copy(
168. [L262] 执行：phase = "collecting",
169. [L263] 执行：progressText = "正在采集第 ${index + 1}/${windows.size} 个窗口。",
170. [L264] 执行：isInProgress = true,
171. [L265] 执行：currentWindowIndex = index + 1,
172. [L266] 执行：currentWindowStartUtc = windowStartUtc,
173. [L267] 执行：currentWindowEndUtc = windowEndUtc,
174. [L268] 执行：gapWindowCount = windows.size,
175. [L269] 执行：pendingQueueCount = pendingQueueCount(),
176. [L270] 执行：lastAttemptedUploadAt = attemptedAt
177. [L272] 执行：persistState(current)
178. [L274] 执行：logs.info(
179. [L275] 执行："mobile-sync",
180. [L276] 执行："正在采集服务器缺口窗口的使用记录。",
181. [L278] 执行："windowStartUtc" to windowStartUtc,
182. [L279] 执行："windowEndUtc" to windowEndUtc
183. [L283] 执行：val collection = usageEventCollector.collectUsage(window.windowStartUtc, window.windowEndUtc)
184. [L284] 执行：val eventIds = mobileDataDao.insertUsageEvents(collection.events)
185. [L285] 执行：val summaryIds = mobileDataDao.insertUsageSummaries(collection.summaries)
186. [L286] 执行：val packageNames = packageNames(collection.events, collection.summaries)
187. [L287] 执行：val appMetadata = appMetadataCollector.collectForPackages(packageNames)
188. [L288] 若 (appMetadata.isNotEmpty()) 则
189. [L289] 执行：mobileDataDao.upsertAppMetadata(appMetadata)
190. [L292] 执行：current = current.copy(
191. [L293] 执行：phase = "uploading",
192. [L294] 执行：progressText = "正在上传第 ${index + 1}/${windows.size} 个窗口。",
193. [L295] 执行：isInProgress = true,
194. [L296] 执行：currentWindowIndex = index + 1,
195. [L297] 执行：currentWindowStartUtc = windowStartUtc,
196. [L298] 执行：currentWindowEndUtc = windowEndUtc,
197. [L299] 执行：currentEventCount = collection.events.size,
198. [L300] 执行：currentSummaryCount = collection.summaries.size,
199. [L301] 执行：currentAppMetadataCount = appMetadata.size,
200. [L302] 执行：pendingQueueCount = pendingQueueCount(),
201. [L303] 执行：lastAttemptedUploadAt = attemptedAt
202. [L305] 执行：persistState(current)
203. [L307] 执行：val uploadState = uploadWindow(
204. [L308] 执行：deviceId = deviceIdentity.deviceId,
205. [L309] 执行：windowStartUtc = windowStartUtc,
206. [L310] 执行：windowEndUtc = windowEndUtc,
207. [L311] 执行：events = collection.events,
208. [L312] 执行：summaries = collection.summaries,
209. [L313] 执行：apps = appMetadata,
210. [L314] 执行：eventIds = eventIds,
211. [L315] 执行：summaryIds = summaryIds
212. [L318] 执行：val merged = current.merge(uploadState)
213. [L319] 执行：val hasUploadErrors = merged.failedCount > 0 || merged.lastError != null
214. [L320] 执行：current = merged.copy(
215. [L321] 执行：phase = if (hasUploadErrors) {
216. [L322] 执行："upload-failed"
217. [L324] 执行："uploading"
218. [L326] 执行：progressText = if (hasUploadErrors) {
219. [L327] 执行：merged.lastError ?: uploadState.progressText
220. [L329] 执行："第 ${index + 1}/${windows.size} 个窗口上传完成。"
221. [L331] 执行：isInProgress = true,
222. [L332] 执行：gapWindowCount = windows.size,
223. [L333] 执行：currentWindowIndex = index + 1,
224. [L334] 执行：currentWindowStartUtc = windowStartUtc,
225. [L335] 执行：currentWindowEndUtc = windowEndUtc,
226. [L336] 执行：currentEventCount = collection.events.size,
227. [L337] 执行：currentSummaryCount = collection.summaries.size,
228. [L338] 执行：currentAppMetadataCount = appMetadata.size,
229. [L339] 执行：lastBatchId = uploadState.lastBatchId,
230. [L340] 执行：lastBatchStatus = uploadState.lastBatchStatus,
231. [L341] 执行：pendingQueueCount = pendingQueueCount(),
232. [L342] 执行：lastAttemptedUploadAt = attemptedAt
233. [L344] 执行：persistState(current)
234. [L347] 执行：current = uploadQueuedLocations(current, attemptedAt)
235. [L349] 执行：val completed = current.copy(
236. [L350] 执行：phase = if (current.failedCount == 0) "completed" else "completed-with-errors",
237. [L351] 执行：progressText = if (current.failedCount == 0) {
238. [L352] 执行："手机同步已完成。"
239. [L354] 执行："手机同步已完成，但部分上传失败。"
240. [L356] 执行：isInProgress = false,
241. [L357] 执行：pendingQueueCount = pendingQueueCount(),
242. [L358] 执行：lastSuccessfulUploadAt = if (current.failedCount == 0) nowIso() else current.lastSuccessfulUploadAt
243. [L360] 执行：persistState(completed)
244. [L361] 执行：sendHeartbeat(deviceIdentity.deviceId, serverUrl, true, completed)
245. [L362] 执行：logs.info(
246. [L363] 执行："mobile-sync",
247. [L364] 执行："手机同步已完成。",
248. [L366] 执行："acceptedCount" to completed.acceptedCount,
249. [L367] 执行："skippedCount" to completed.skippedCount,
250. [L368] 执行："rejectedCount" to completed.rejectedCount,
251. [L369] 执行："failedCount" to completed.failedCount
252. [L372] 执行：completed
253. [L374] 抛出 ex
254. [L376] 执行：val previous = _state.value
255. [L377] 执行：val detail = ex.toCauseChainMessage()
256. [L378] 执行：val outcome = MobileSyncErrorClassifier.classify(ex)
257. [L379] 执行：val failed = previous.copy(
258. [L380] 执行：phase = "failed",
259. [L381] 执行：progressText = "手机同步失败。",
260. [L382] 执行：isInProgress = false,
261. [L383] 执行：outcome = outcome,
262. [L384] 执行：failedCount = maxOf(1, previous.failedCount),
263. [L385] 执行：lastError = ex.message ?: ex::class.java.simpleName,
264. [L386] 执行：lastErrorDetail = detail,
265. [L387] 执行：pendingQueueCount = pendingQueueCount(),
266. [L388] 执行：lastAttemptedUploadAt = attemptedAt
267. [L390] 执行：logs.error("mobile-sync", "手机同步失败：$detail", ex)
268. [L391] 执行：persistState(failed)
269. [L392] 执行：sendHeartbeat(deviceIdentity.deviceId, serverUrl, true, failed)
270. [L397] 执行：private suspend fun uploadQueuedLocations(
271. [L398] 执行：current: MobileSyncState,
272. [L399] 执行：attemptedAt: String
273. [L400] 执行：): MobileSyncState {
274. [L401] 执行：val updates = locationUploadCoordinator.uploadPending()
275. [L402] 若 (updates.syncedIds.isEmpty() && updates.failedIds.isEmpty()) 则
276. [L403] 执行：val idle = current.copy(
277. [L404] 执行：pendingQueueCount = pendingQueueCount(),
278. [L405] 执行：lastAttemptedUploadAt = attemptedAt
279. [L407] 执行：persistState(idle)
280. [L408] 返回 idle
281. [L411] 执行：val syncedCount = updates.syncedIds.size
282. [L412] 执行：val retryableCount = updates.retryableFailedIds.size
283. [L413] 执行：val rejectedCount = updates.failedIds.size - retryableCount
284. [L414] 执行：val hasRetryable = updates.shouldRetry || retryableCount > 0
285. [L415] 执行：val next = current.copy(
286. [L416] 执行：phase = when {
287. [L417] 分支臂：hasRetryable -> "location-upload-failed"
288. [L418] 分支臂：current.phase == "usage-permission-missing" -> current.phase
289. [L419] when 默认 else
290. [L421] 执行：progressText = when {
291. [L422] 分支臂：hasRetryable -> "定位队列上传失败，已安排网络重试。"
292. [L423] 分支臂：current.phase == "usage-permission-missing" ->
293. [L424] 执行："${current.progressText} 定位队列已同步 $syncedCount 条。"
294. [L425] when 默认 else
295. [L427] 执行：outcome = if (hasRetryable) MobileSyncOutcome.RETRY else current.outcome,
296. [L428] 执行：acceptedCount = current.acceptedCount + syncedCount,
297. [L429] 执行：rejectedCount = current.rejectedCount + rejectedCount,
298. [L430] 执行：failedCount = current.failedCount + retryableCount,
299. [L431] 执行：lastError = if (hasRetryable) {
300. [L432] 执行：updates.retryableFirstError() ?: updates.failedReason ?: current.lastError
301. [L434] 执行：lastErrorDetail = if (hasRetryable) {
302. [L435] 执行：updates.retryableFirstError() ?: updates.failedReason ?: current.lastErrorDetail
303. [L437] 执行：pendingQueueCount = pendingQueueCount(),
304. [L438] 执行：lastAttemptedUploadAt = attemptedAt
305. [L440] 执行：persistState(next)
306. [L442] 执行：val details = mapOf(
307. [L443] 执行："syncedCount" to syncedCount,
308. [L444] 执行："rejectedCount" to rejectedCount,
309. [L445] 执行："retryableCount" to retryableCount
310. [L447] 若 (hasRetryable) 则
311. [L448] 执行：logs.warn("mobile-location-sync", "定位队列上传未完成，已安排 WorkManager 重试。", details)
312. [L450] 执行：logs.info("mobile-location-sync", "定位队列上传完成。", details)
313. [L452] 返回 next
314. [L455] 执行：private suspend fun uploadWindow(
315. [L456] 执行：deviceId: String,
316. [L457] 执行：windowStartUtc: String,
317. [L458] 执行：windowEndUtc: String,
318. [L459] 执行：events: List<MobileUsageEventEntity>,
319. [L460] 执行：summaries: List<MobileUsageSummaryEntity>,
320. [L461] 执行：apps: List<MobileAppMetadataEntity>,
321. [L462] 执行：eventIds: List<Long>,
322. [L463] 执行：summaryIds: List<Long>
323. [L464] 执行：): MobileSyncState {
324. [L465] 执行：val batchId = stableBatchId(deviceId, windowStartUtc, windowEndUtc)
325. [L466] 执行：val request = MobileUsageEventsUploadRequest(
326. [L467] 执行：deviceId,
327. [L469] 执行：windowStartUtc,
328. [L470] 执行：windowEndUtc,
329. [L471] 执行：apps.map { it.toDto() },
330. [L472] 分支臂：events.mapIndexed { index, event -> event.toDto(eventIds[index].toString()) },
331. [L473] 分支臂：summaries.mapIndexed { index, summary -> summary.toDto(summaryIds[index].toString()) }
332. [L476] 执行：val response = api.uploadMobileUsage(request)
333. [L477] 执行：val ingest = response.data
334. [L478] 若 (response.code != 0 || ingest == null) 则
335. [L479] 执行：val message = response.message.ifBlank { "Usage upload failed." }
336. [L480] 执行：markUsageFailed(eventIds, summaryIds, apps, message)
337. [L481] 执行：logs.warn(
338. [L482] 执行："mobile-sync",
339. [L483] 执行："Usage upload failed.",
340. [L484] 执行：mapOf("windowStartUtc" to windowStartUtc, "windowEndUtc" to windowEndUtc, "message" to message)
341. [L486] 返回 state(
342. [L487] 执行：phase = "upload-failed",
343. [L488] 执行：progressText = message,
344. [L489] 执行：outcome = MobileSyncOutcome.RETRY,
345. [L490] 执行：failedCount = maxOf(1, events.size + summaries.size),
346. [L491] 执行：lastError = message,
347. [L492] 执行：lastErrorDetail = message,
348. [L493] 执行：currentWindowStartUtc = windowStartUtc,
349. [L494] 执行：currentWindowEndUtc = windowEndUtc,
350. [L495] 执行：currentEventCount = events.size,
351. … 其余约 488 条有效逻辑行同序压缩（源文件共 1147 行）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "label": "MobileSyncState",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/AppMetadataCollector.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt",
      "type": "depends_on"
    }
  ]
}
```
