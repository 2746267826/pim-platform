# src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：运行时组件 `MobileHeartbeatReporter`：移动端采集/同步链路中的策略或上报单元。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### MobileHeartbeatReporter
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L16 声明 `MobileHeartbeatReporter`
- 分支与异常：无
- 调用：无

### hasPreciseLocationPermission
#### hasPreciseLocationPermission(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun hasPreciseLocationPermission(): Boolean {
  2. 返回 ContextCompat.checkSelfPermission(
  3. 执行：Manifest.permission.ACCESS_FINE_LOCATION
  4. 执行：) == PackageManager.PERMISSION_GRANTED
- 分支与异常：无显著分支
- 调用：hasPreciseLocationPermission、ContextCompat.checkSelfPermission

### locationCapabilitySummary
#### locationCapabilitySummary(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun locationCapabilitySummary(): String {
  2. 返回 if (hasPreciseLocationPermission()) {
  3. 执行："fine-location-granted"
  4. 执行："fine-location-missing"
- 分支与异常：无显著分支
- 调用：locationCapabilitySummary、hasPreciseLocationPermission

### appVersionName
#### appVersionName(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun appVersionName(): String {
  2. 返回 try {
  3. 执行：val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
  4. 执行：context.packageManager.getPackageInfo(
  5. 执行：context.packageName,
  6. 执行：PackageManager.PackageInfoFlags.of(0)
  7. 注解 @Suppress
  8. 执行：context.packageManager.getPackageInfo(context.packageName, 0)
  9. 执行：info.versionName ?: "unknown"
  10. 执行："unknown"
- 分支与异常：无显著分支
- 调用：appVersionName、context.packageManager.getPackageInfo、PackageManager.PackageInfoFlags.of、Suppress

## 近逐行中文伪代码

1. [L15] 注解 @Singleton
2. [L16] 定义类 `MobileHeartbeatReporter`
3. [L17] 注解 @ApplicationContext
4. [L18] 执行：private val api: ApiService
5. [L20] 挂起函数 `report`
6. [L21] 执行：deviceId: String,
7. [L22] 执行：serverUrl: String,
8. [L23] 执行：usagePermissionGranted: Boolean,
9. [L24] 执行：state: MobileSyncState
10. [L26] 执行：val statusJson = JSONObject()
11. [L27] 执行：.put("brand", Build.BRAND ?: "")
12. [L28] 执行：.put("manufacturer", Build.MANUFACTURER ?: "")
13. [L29] 执行：.put("model", Build.MODEL ?: "")
14. [L30] 执行：.put("androidVersion", Build.VERSION.RELEASE ?: "")
15. [L31] 执行：.put("sdkInt", Build.VERSION.SDK_INT)
16. [L32] 执行：.put("appVersion", appVersionName())
17. [L33] 执行：.put("usagePermissionGranted", usagePermissionGranted)
18. [L34] 执行：.put("preciseLocationPermissionGranted", hasPreciseLocationPermission())
19. [L35] 执行：.put("lastUsageSyncResult", state.phase)
20. [L36] 执行：.put("lastGapCheckWindowCount", state.gapWindowCount)
21. [L37] 执行：.put("pendingQueueCount", state.pendingQueueCount)
22. [L38] 执行：.put("acceptedCount", state.acceptedCount)
23. [L39] 执行：.put("skippedCount", state.skippedCount)
24. [L40] 执行：.put("rejectedCount", state.rejectedCount)
25. [L41] 执行：.put("failedCount", state.failedCount)
26. [L42] 执行：.put("lastError", state.lastError ?: JSONObject.NULL)
27. [L43] 执行：.put("locationCapability", locationCapabilitySummary())
28. [L44] 执行：.toString()
29. [L46] 执行：api.sendHeartbeat(
30. [L47] 执行：DaemonHeartbeatRequest(
31. [L48] 执行：deviceId,
32. [L49] 执行："android",
33. [L50] 执行：appVersionName(),
34. [L51] 执行：serverUrl,
35. [L52] 执行：state.lastSuccessfulUploadAt,
36. [L53] 执行：state.lastAttemptedUploadAt,
37. [L54] 执行：state.lastError,
38. [L55] 执行：state.pendingQueueCount,
39. [L56] 执行："Unknown",
40. [L57] 执行："Unknown",
41. [L58] 执行：!usagePermissionGranted,
42. [L59] 执行：statusJson
43. [L64] 执行：private fun hasPreciseLocationPermission(): Boolean {
44. [L65] 返回 ContextCompat.checkSelfPermission(
45. [L67] 执行：Manifest.permission.ACCESS_FINE_LOCATION
46. [L68] 执行：) == PackageManager.PERMISSION_GRANTED
47. [L71] 执行：private fun locationCapabilitySummary(): String {
48. [L72] 返回 if (hasPreciseLocationPermission()) {
49. [L73] 执行："fine-location-granted"
50. [L75] 执行："fine-location-missing"
51. [L79] 执行：private fun appVersionName(): String {
52. [L80] 返回 try {
53. [L81] 执行：val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
54. [L82] 执行：context.packageManager.getPackageInfo(
55. [L83] 执行：context.packageName,
56. [L84] 执行：PackageManager.PackageInfoFlags.of(0)
57. [L87] 注解 @Suppress
58. [L88] 执行：context.packageManager.getPackageInfo(context.packageName, 0)
59. [L90] 执行：info.versionName ?: "unknown"
60. [L92] 执行："unknown"

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt",
      "label": "MobileHeartbeatReporter",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileHeartbeatReporter.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
