# src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：协调器 `LocationUploadBatchResult`：编排多步骤同步或上传流程。
- 主要依赖：`src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`、`src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### LocationUploadBatchResult
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L20 声明 `LocationUploadBatchResult`
- 分支与异常：无
- 调用：无

### LocationUploadStatusUpdates
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L27 声明 `LocationUploadStatusUpdates`
- 分支与异常：无
- 调用：无

### LocationUploadPlanner
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L36 声明 `LocationUploadPlanner`
- 分支与异常：无
- 调用：无

### LocationUploadCoordinator
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L49 声明 `LocationUploadCoordinator`
- 分支与异常：无
- 调用：无

### planStatusUpdates
#### planStatusUpdates(result: LocationUploadBatchResult)
- 输入：result: LocationUploadBatchResult
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `planStatusUpdates` 参数：result: LocationUploadBatchResult
  2. 返回 LocationUploadStatusUpdates(
  3. 执行：syncedIds = result.syncedIds,
  4. 执行：failedIds = result.failedIds,
  5. 执行：failedReason = result.errorMessage,
  6. 执行：shouldRetry = result.retryableFailedIds.isNotEmpty(),
  7. 执行：retryableFailedIds = result.retryableFailedIds
- 分支与异常：无显著分支
- 调用：planStatusUpdates、LocationUploadStatusUpdates、result.retryableFailedIds.isNotEmpty

### uploadPending
#### uploadPending(limit: Int = DEFAULT_LIMIT)
- 输入：limit: Int = DEFAULT_LIMIT
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 挂起函数 `uploadPending`
  2. 执行：val rows = pendingRows(limit)
  3. 若 (rows.isEmpty()) 则
  4. 返回 LocationUploadPlanner.planStatusUpdates(
  5. 执行：LocationUploadBatchResult(emptyList(), emptyList(), null)
  6. 执行：val synced = mutableListOf<Long>()
  7. 执行：val retryableFailed = mutableListOf<Long>()
  8. 执行：val permanentFailed = mutableListOf<Long>()
  9. 执行：val perItemErrors = linkedMapOf<Long, String>()
  10. 执行：var lastError: String? = null
  11. 执行：val deviceId = deviceId()
  12. 循环 for (row in rows)
  13. 执行：val request = row.toRequest(deviceId)
  14. 若 (request == null) 则
  15. 执行：permanentFailed += row.id
  16. 执行：perItemErrors[row.id] = "missing-horizontal-accuracy"
  17. 执行：lastError = lastError ?: "missing-horizontal-accuracy"
  18. 进入 try
  19. 执行：val response = api.uploadMobileLocation(request)
  20. 若 (response.code == 0 && response.data != null) 则
  21. 执行：synced += row.id
  22. 执行：val msg = response.message.ifBlank { "location upload failed" }
  23. 执行：perItemErrors[row.id] = msg
  24. 执行：lastError = lastError ?: msg
  25. 执行：if (ex is CancellationException) throw ex
  26. 执行：val outcome = MobileSyncErrorClassifier.classify(ex)
  27. when 分支匹配
  28. 分支臂：MobileSyncOutcome.RETRY -> {
  29. 执行：retryableFailed += row.id
  30. 执行：val msg = ex.message ?: ex::class.java.simpleName
- 分支与异常：if (rows.isEmpty()) {；if (request == null) {；try {；if (response.code == 0 && response.data != null) {；if (ex is CancellationException) throw ex；when (outcome) {；else -> {
- 调用：uploadPending、pendingRows、rows.isEmpty、LocationUploadPlanner.planStatusUpdates、LocationUploadBatchResult、emptyList、deviceId、row.toRequest、api.uploadMobileLocation、MobileSyncErrorClassifier.classify、updates.copy、applyStatusUpdates

### pendingRows
#### pendingRows(limit: Int)
- 输入：limit: Int
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private suspend fun pendingRows(limit: Int): List<MobileLocationPointEntity> {
  2. 执行：val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING, limit)
  3. 执行：if (pending.size >= limit) return pending
  4. 执行：val failed = dao.getLocationPointsBySyncStatus(MobileSyncStatus.FAILED, limit - pending.size)
  5. 返回 pending + failed
- 分支与异常：if (pending.size >= limit) return pending
- 调用：pendingRows、dao.getLocationPointsBySyncStatus

### applyStatusUpdates
#### applyStatusUpdates(updates: LocationUploadStatusUpdates)
- 输入：updates: LocationUploadStatusUpdates
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private suspend fun applyStatusUpdates(updates: LocationUploadStatusUpdates) {
  2. 执行：applyLocationStatusUpdates(dao, updates)
- 分支与异常：无显著分支
- 调用：applyStatusUpdates、applyLocationStatusUpdates

### deviceId
#### deviceId(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun deviceId(): String {
  2. 执行：val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
  3. 执行：val seed = androidId ?: Build.FINGERPRINT ?: "android-device"
  4. 返回 "android-${sha256(seed).take(16)}"
- 分支与异常：无显著分支
- 调用：deviceId、Settings.Secure.getString、sha256、take

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

## 近逐行中文伪代码

1. [L20] 定义类 `LocationUploadBatchResult`
2. [L21] 执行：val syncedIds: List<Long>,
3. [L22] 执行：val failedIds: List<Long>,
4. [L23] 执行：val errorMessage: String?,
5. [L24] 执行：val retryableFailedIds: List<Long> = emptyList()
6. [L27] 定义类 `LocationUploadStatusUpdates`
7. [L28] 执行：val syncedIds: List<Long>,
8. [L29] 执行：val failedIds: List<Long>,
9. [L30] 执行：val failedReason: String?,
10. [L31] 执行：val shouldRetry: Boolean,
11. [L32] 执行：val perItemErrors: Map<Long, String> = emptyMap(),
12. [L33] 执行：val retryableFailedIds: List<Long> = emptyList()
13. [L36] 单例 object `LocationUploadPlanner`
14. [L37] 函数 `planStatusUpdates` 参数：result: LocationUploadBatchResult
15. [L38] 返回 LocationUploadStatusUpdates(
16. [L39] 执行：syncedIds = result.syncedIds,
17. [L40] 执行：failedIds = result.failedIds,
18. [L41] 执行：failedReason = result.errorMessage,
19. [L42] 执行：shouldRetry = result.retryableFailedIds.isNotEmpty(),
20. [L43] 执行：retryableFailedIds = result.retryableFailedIds
21. [L48] 注解 @Singleton
22. [L49] 定义类 `LocationUploadCoordinator`
23. [L50] 注解 @ApplicationContext
24. [L51] 执行：private val database: AppDatabase,
25. [L52] 执行：private val api: ApiService
26. [L54] 执行：private val dao: MobileDataDao = database.mobileDataDao()
27. [L56] 挂起函数 `uploadPending`
28. [L57] 执行：val rows = pendingRows(limit)
29. [L58] 若 (rows.isEmpty()) 则
30. [L59] 返回 LocationUploadPlanner.planStatusUpdates(
31. [L60] 执行：LocationUploadBatchResult(emptyList(), emptyList(), null)
32. [L64] 执行：val synced = mutableListOf<Long>()
33. [L65] 执行：val retryableFailed = mutableListOf<Long>()
34. [L66] 执行：val permanentFailed = mutableListOf<Long>()
35. [L67] 执行：val perItemErrors = linkedMapOf<Long, String>()
36. [L68] 执行：var lastError: String? = null
37. [L69] 执行：val deviceId = deviceId()
38. [L71] 循环 for (row in rows)
39. [L72] 执行：val request = row.toRequest(deviceId)
40. [L73] 若 (request == null) 则
41. [L74] 执行：permanentFailed += row.id
42. [L75] 执行：perItemErrors[row.id] = "missing-horizontal-accuracy"
43. [L76] 执行：lastError = lastError ?: "missing-horizontal-accuracy"
44. [L80] 进入 try
45. [L81] 执行：val response = api.uploadMobileLocation(request)
46. [L82] 若 (response.code == 0 && response.data != null) 则
47. [L83] 执行：synced += row.id
48. [L85] 执行：val msg = response.message.ifBlank { "location upload failed" }
49. [L86] 执行：permanentFailed += row.id
50. [L87] 执行：perItemErrors[row.id] = msg
51. [L88] 执行：lastError = lastError ?: msg
52. [L91] 执行：if (ex is CancellationException) throw ex
53. [L92] 执行：val outcome = MobileSyncErrorClassifier.classify(ex)
54. [L93] when 分支匹配
55. [L94] 分支臂：MobileSyncOutcome.RETRY -> {
56. [L95] 执行：retryableFailed += row.id
57. [L96] 执行：val msg = ex.message ?: ex::class.java.simpleName
58. [L97] 执行：perItemErrors[row.id] = msg
59. [L98] 执行：lastError = lastError ?: msg
60. [L100] 分支臂：MobileSyncOutcome.BLOCKED -> {
61. [L101] 执行：permanentFailed += row.id
62. [L102] 执行：val msg = ex.message ?: ex::class.java.simpleName
63. [L103] 执行：perItemErrors[row.id] = msg
64. [L104] 执行：lastError = lastError ?: msg
65. [L106] when 默认 else
66. [L107] 执行：permanentFailed += row.id
67. [L108] 执行：val msg = ex.message ?: ex::class.java.simpleName
68. [L109] 执行：perItemErrors[row.id] = msg
69. [L110] 执行：lastError = lastError ?: msg
70. [L116] 执行：val allFailed = retryableFailed + permanentFailed
71. [L117] 执行：val updates = LocationUploadPlanner.planStatusUpdates(
72. [L118] 执行：LocationUploadBatchResult(synced, allFailed, lastError, retryableFailed)
73. [L120] 执行：val fullUpdates = updates.copy(perItemErrors = perItemErrors, retryableFailedIds = retryableFailed)
74. [L121] 执行：applyStatusUpdates(fullUpdates)
75. [L122] 返回 fullUpdates
76. [L125] 执行：private suspend fun pendingRows(limit: Int): List<MobileLocationPointEntity> {
77. [L126] 执行：val pending = dao.getLocationPointsBySyncStatus(MobileSyncStatus.PENDING, limit)
78. [L127] 执行：if (pending.size >= limit) return pending
79. [L128] 执行：val failed = dao.getLocationPointsBySyncStatus(MobileSyncStatus.FAILED, limit - pending.size)
80. [L129] 返回 pending + failed
81. [L132] 执行：private suspend fun applyStatusUpdates(updates: LocationUploadStatusUpdates) {
82. [L133] 执行：applyLocationStatusUpdates(dao, updates)
83. [L136] 执行：private fun MobileLocationPointEntity.toRequest(deviceId: String): MobileLocationPointRequest? {
84. [L137] 执行：val accuracy = accuracyMeters ?: return null
85. [L138] 返回 MobileLocationPointRequest(
86. [L139] 执行：deviceId = deviceId,
87. [L140] 执行：recordedAtUtc = Instant.ofEpochMilli(recordedAtUtc).toString(),
88. [L141] 执行：latitude = latitude,
89. [L142] 执行：longitude = longitude,
90. [L143] 执行：horizontalAccuracyMeters = accuracy.toDouble(),
91. [L144] 执行：provider = provider ?: "unknown",
92. [L145] 执行：sourceKind = source,
93. [L146] 执行：altitudeMeters = altitudeMeters,
94. [L147] 执行：speedMetersPerSecond = speedMetersPerSecond?.toDouble(),
95. [L148] 执行：bearingDegrees = bearingDegrees?.toDouble(),
96. [L149] 执行：isAutoSubmitted = source != "manual",
97. [L150] 执行：rawJson = rawJson
98. [L154] 执行：private fun deviceId(): String {
99. [L155] 执行：val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
100. [L156] 执行：val seed = androidId ?: Build.FINGERPRINT ?: "android-device"
101. [L157] 返回 "android-${sha256(seed).take(16)}"
102. [L160] 执行：private fun sha256(value: String): String {
103. [L161] 执行：val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
104. [L162] 返回 bytes.joinToString("") { "%02x".format(Locale.US, it) }
105. [L165] 执行：private companion object {
106. [L166] 执行：const val DEFAULT_LIMIT = 100
107. [L170] 执行：internal fun LocationUploadStatusUpdates.retryableFirstError(): String? {
108. [L171] 返回 retryableFailedIds.firstOrNull()?.let { perItemErrors[it] }
109. [L174] 执行：internal suspend fun applyLocationStatusUpdates(
110. [L175] 执行：dao: MobileDataDao,
111. [L176] 执行：updates: LocationUploadStatusUpdates
112. [L178] 若 (updates.syncedIds.isNotEmpty()) 则
113. [L179] 执行：dao.deleteLocationPointByIds(updates.syncedIds)
114. [L181] 执行：val retryableSet = updates.retryableFailedIds.toSet()
115. [L182] 执行：val permanentIds = updates.failedIds.filter { it !in retryableSet && it !in updates.syncedIds.toSet() }
116. [L183] 执行：val retryableIds = updates.failedIds.filter { it in retryableSet && it !in updates.syncedIds.toSet() }
117. [L184] 若 (permanentIds.isNotEmpty()) 则
118. [L185] 分支臂：permanentIds.forEach { id ->
119. [L186] 执行：dao.updateLocationPointSyncStatus(
120. [L187] 执行：ids = listOf(id),
121. [L188] 执行：syncStatus = MobileSyncStatus.REJECTED,
122. [L189] 执行：lastError = updates.perItemErrors[id] ?: updates.failedReason ?: "permanent-failure"
123. [L193] 若 (retryableIds.isNotEmpty()) 则
124. [L194] 分支臂：retryableIds.forEach { id ->
125. [L195] 执行：dao.updateLocationPointSyncStatus(
126. [L196] 执行：ids = listOf(id),
127. [L197] 执行：syncStatus = MobileSyncStatus.PENDING,
128. [L198] 执行：lastError = updates.perItemErrors[id] ?: updates.failedReason ?: "transient-failure"

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt",
      "label": "LocationUploadBatchResult",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/LocationUploadCoordinator.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "type": "depends_on"
    }
  ]
}
```
