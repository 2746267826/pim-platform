# src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.schedule
- 职责：从日历事件 API 加载并映射为定位策略用的 `ScheduleWindow`；提供当前/即将到来窗口选择。
- 主要依赖：ApiService、EventResponse、ScheduleWindow
- 被谁使用：定位策略/前台定位相关逻辑

## 函数级结构化伪代码

### ScheduleWindowSelector
#### current(windows, nowMillis)
- 输入：窗口列表、当前时间
- 输出：第一个「有地点且 now ∈ [start, end)」的窗口，否则 null
- 步骤：firstOrNull 匹配 location 非空且时间落在区间

#### upcoming(windows, nowMillis, limit=10)
- 输入：窗口列表、当前时间、上限
- 输出：未来有地点的窗口，按 startsAt 升序取 limit
- 步骤：filter startsAt > now → sortedBy startsAt → take(limit)

### ScheduleWindowRepository
#### loadWindows(startMillis, endMillis)
- 输入：起止毫秒
- 输出：`List<ScheduleWindow>`
- 副作用：HTTP GET events
- 步骤：
  1. Instant 转 ISO 字符串调 getEvents
  2. code != 0 则 error(message 或「加载日程失败」)
  3. mapEvents(data)
- 调用：ApiService.getEvents、mapEvents

#### currentWindow / upcomingWindows
- 委托 ScheduleWindowSelector.current / upcoming

#### mapEvents(events) [companion]
- 输入：EventResponse 列表
- 输出：有 location 且可解析 dtStart/dtEnd 的 ScheduleWindow
- 步骤：location trim 非空；dtStart/dtEnd Instant.parse；组装 id/title/location/时间
- 分支：缺 location 或时间解析失败 → mapNotNull 丢弃

#### String.toEpochMillisOrNull [private]
- Instant.parse 成功则 toEpochMilli，失败 null

## 近逐行中文伪代码

1. ScheduleWindowSelector.current：找当前进行中且有地点的窗口。
2. upcoming：startsAt 在未来、有地点，排序后取前 limit。
3. Repository 注入 ApiService。
4. loadWindows：毫秒转 Instant 字符串请求 getEvents。
5. 业务 code 非 0 抛 error。
6. mapEvents 映射成功项。
7. currentWindow/upcomingWindows 转调 Selector。
8. mapEvents：无地点或时间非法则跳过。
9. toEpochMillisOrNull：runCatching Instant.parse。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt",
      "label": "ScheduleWindowRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models",
      "type": "depends_on"
    }
  ]
}
```
