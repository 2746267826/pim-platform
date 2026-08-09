# src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.mobile.usage
- 职责：从系统 `UsageStatsManager` 采集前台使用事件；无事件时回退到 usage stats 汇总；无权限/无服务时返回空集合与来源标记。
- 主要依赖：`UsageAccessChecker`、`MobileUsageEventEntity`、`MobileUsageSummaryEntity`、Android `UsageStatsManager`/`UsageEvents`
- 被谁使用：移动端 usage 同步/采集管线

## 函数级结构化伪代码

### UsageEventCollection
#### data class UsageEventCollection(...)
- 输入：events、summaries、source、时间窗与 collectedAtUtc
- 输出：不可变结果载体
- 副作用：无
- 步骤：
  1. 持有事件列表或汇总列表（二者通常互斥）
  2. 记录数据来源字符串与采集时间窗
- 分支与异常：无
- 调用：无

### UsageEventCollector
#### collectUsage(windowStartUtc, windowEndUtc, collectedAtUtc = now): UsageEventCollection
- 输入：UTC 毫秒时间窗；可选采集时刻
- 输出：`UsageEventCollection`
- 副作用：读取系统 usage 统计服务
- 步骤：
  1. 校验 `windowEndUtc > windowStartUtc`，否则 `require` 抛错
  2. 取 `USAGE_STATS_SERVICE`；失败则 `emptyCollection(..., SOURCE_UNAVAILABLE)`
  3. 若 `usageAccessChecker.hasUsageAccess()` 为假，返回 `SOURCE_NO_ACCESS` 空集合
  4. 调用 `queryUsageEvents`；若非空，以 `SOURCE_USAGE_EVENTS` 返回事件、空 summaries
  5. 否则 `queryUsageStatsFallback`，以 `SOURCE_USAGE_STATS_FALLBACK` 返回汇总、空 events
- 分支与异常：窗口非法抛 `IllegalArgumentException`；系统服务缺失/无权限走空集合
- 调用：`queryUsageEvents`、`queryUsageStatsFallback`、`emptyCollection`、`UsageAccessChecker.hasUsageAccess`

#### queryUsageEvents(...): List\<MobileUsageEventEntity\>
- 输入：manager、时间窗、collectedAtUtc
- 输出：事件实体列表
- 副作用：`queryEvents`
- 步骤：
  1. try `queryEvents`；`SecurityException` 返回空列表
  2. 循环 `hasNextEvent`/`getNextEvent`
  3. packageName 为空则 skip
  4. 映射为 `MobileUsageEventEntity`（含 eventName、rawJson）
- 分支与异常：SecurityException → 空
- 调用：`eventName`、`eventRawJson`

#### queryUsageStatsFallback(...): List\<MobileUsageSummaryEntity\>
- 输入：manager、时间窗、collectedAtUtc
- 输出：汇总实体列表
- 副作用：`queryUsageStats`
- 步骤：
  1. API ≥ R 用 `INTERVAL_BEST`，否则 `INTERVAL_DAILY`
  2. try 查询；SecurityException → 空
  3. 过滤 packageName 非空并 map 为 `MobileUsageSummaryEntity`
- 分支与异常：SecurityException → 空
- 调用：`usageStatsRawJson`

#### emptyCollection(...): UsageEventCollection
- 输入：时间窗、collectedAtUtc、source
- 输出：空 events/summaries 的集合
- 副作用：无
- 步骤：1. 构造并返回
- 分支与异常：无
- 调用：无

#### eventRawJson / usageStatsRawJson / eventName
- 输入：系统事件或 UsageStats、元数据
- 输出：JSON 字符串或事件名
- 副作用：无
- 步骤：组装 JSONObject 字段；eventType 映射为可读名，未知为 `UNKNOWN_$eventType`
- 分支与异常：when 分支
- 调用：`JSONObject.put` / `putNullable`

### putNullable (file-private extension)
#### JSONObject.putNullable(name, value): JSONObject
- 输入：键与可空值
- 输出：同一 JSONObject
- 副作用：写字段
- 步骤：value 为 null 时写 `JSONObject.NULL`
- 分支与异常：无
- 调用：`put`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.mobile.usage`
2. [L15-22] 定义结果数据类 `UsageEventCollection`
3. [L24-28] `@Singleton` 注入 `Context` 与 `UsageAccessChecker`
4. [L29-36] `collectUsage`：校验时间窗
5. [L38-39] 获取 `UsageStatsManager`，失败 → `SOURCE_UNAVAILABLE` 空结果
6. [L41-43] 无 usage 权限 → `SOURCE_NO_ACCESS`
7. [L45-61] 查询 events；非空则返回 `SOURCE_USAGE_EVENTS`
8. [L63-77] 否则 fallback stats，返回 `SOURCE_USAGE_STATS_FALLBACK`
9. [L80-122] `queryUsageEvents`：遍历系统事件并建实体
10. [L124-166] `queryUsageStatsFallback`：按 SDK 选 interval 并 map 汇总
11. [L168-182] `emptyCollection` 工厂
12. [L184-234] rawJson 与 eventName 映射
13. [L236-241] 来源常量
14. [L244-246] `putNullable` 扩展

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt",
      "label": "UsageEventCollector",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileUsageEventEntity.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageEventCollector.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileUsageSummaryEntity.kt",
      "type": "depends_on"
    }
  ]
}
```
