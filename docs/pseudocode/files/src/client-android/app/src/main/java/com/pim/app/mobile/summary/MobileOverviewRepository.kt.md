# src/client-android/app/src/main/java/com/pim/app/mobile/summary/MobileOverviewRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：数据仓库 `MobileOverview`：封装本地/远程数据访问。
- 主要依赖：`src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### MobileOverview
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L14 声明 `MobileOverview`
- 分支与异常：无
- 调用：无

### MobileOverviewRepository
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L21 声明 `MobileOverviewRepository`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L14] 定义类 `MobileOverview`
2. [L15] 执行：val usageSummary: MobileUsageSummaryResponse,
3. [L16] 执行：val locationOverview: MobileLocationAnalyticsOverviewResponse,
4. [L17] 执行：val tracks: List<MobileLocationTrackDto>,
5. [L18] 执行：val pendingLocationCount: Int
6. [L21] 定义类 `MobileOverviewRepository`
7. [L22] 执行：private val apiService: ApiService,
8. [L23] 执行：private val mobileDataDao: MobileDataDao
9. [L25] 挂起函数 `loadToday`
10. [L26] 执行：date: LocalDate = LocalDate.now(ZoneOffset.UTC),
11. [L27] 执行：deviceId: String? = null
12. [L28] 执行：): MobileOverview {
13. [L29] 执行：val start = date.atStartOfDay().toInstant(ZoneOffset.UTC)
14. [L30] 执行：val end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
15. [L31] 返回 loadRange(
16. [L32] 执行：date = date,
17. [L33] 执行：rangeStartUtc = start,
18. [L34] 执行：rangeEndUtc = end,
19. [L35] 执行：deviceId = deviceId
20. [L39] 挂起函数 `loadRange`
21. [L40] 执行：date: LocalDate,
22. [L41] 执行：rangeStartUtc: Instant,
23. [L42] 执行：rangeEndUtc: Instant,
24. [L43] 执行：deviceId: String? = null
25. [L44] 执行：): MobileOverview {
26. [L45] 执行：val usage = apiService.getMobileSummary(date = date.toString(), deviceId = deviceId).data
27. [L46] 执行：?: error("移动端使用摘要为空")
28. [L47] 执行：val location = apiService.getMobileLocationOverview(
29. [L48] 执行：rangeStartUtc = rangeStartUtc.toString(),
30. [L49] 执行：rangeEndUtc = rangeEndUtc.toString(),
31. [L50] 执行：deviceId = deviceId,
32. [L51] 执行：maxAccuracyMeters = 50.0
33. [L52] 执行：).data ?: error("位置概览为空")
34. [L53] 执行：val tracks = apiService.getMobileLocationTracks(
35. [L54] 执行：rangeStartUtc = rangeStartUtc.toString(),
36. [L55] 执行：rangeEndUtc = rangeEndUtc.toString(),
37. [L56] 执行：deviceId = deviceId,
38. [L57] 执行：maxAccuracyMeters = 50.0
39. [L58] 执行：).data.orEmpty()
40. [L60] 返回 MobileOverview(
41. [L61] 执行：usageSummary = usage,
42. [L62] 执行：locationOverview = location,
43. [L63] 执行：tracks = tracks,
44. [L64] 执行：pendingLocationCount = mobileDataDao.pendingLocationPointCount().first()

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/summary/MobileOverviewRepository.kt",
      "label": "MobileOverview",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/summary/MobileOverviewRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/summary/MobileOverviewRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/summary/MobileOverviewRepository.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "type": "depends_on"
    }
  ]
}
```
