# src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：将 `EventEntity` 列表在给定时间窗内展开为 `ExpandedEvent`（简单事件直通；含 RRule 的用 ical.net 展开；失败时回退为原实例），并记录展开统计日志。
- 主要依赖：Ical.Net（CalendarEvent、RecurrencePattern、EvaluationOptions）、MD5、ILogger、`EventEntity`
- 被谁使用：Calendar 查询/同步路径中需要区间内展开的服务

## 函数级结构化伪代码

### RecurrenceService
#### 构造函数 `(ILogger<RecurrenceService> logger)`
- 步骤：保存 `_logger`

#### `List<ExpandedEvent> ExpandEvents(IEnumerable<EventEntity> events, DateTimeOffset rangeStart, DateTimeOffset rangeEnd)`
- 输入：事件集合、查询区间
- 输出：展开后的 ExpandedEvent 列表
- 副作用：Information 日志
- 步骤：
  1. 初始化 results；计数 recurring/simple/errors。
  2. 对每个 entity：
     - RRule 空：simple++；若与区间相交（DtEnd>start 且 DtStart<end）则加入原 Id 的 ExpandedEvent。
     - 否则 recurring++；`ExpandRecurring`；若结果空且 RRule 非 null 则 errors++；AddRange。
  3. 打日志：事件数、recurring/simple/results/errors、区间。
  4. 返回 results。
- 分支与异常：ExpandRecurring 内部吞异常并回退
- 调用：`ExpandRecurring`、`_logger.LogInformation`

#### `private static List<ExpandedEvent> ExpandRecurring(entity, rangeStart, rangeEnd)`
- 输入：单实体与区间
- 输出：展开列表
- 副作用：无
- 步骤：
  1. duration = DtEnd - DtStart。
  2. try：建 CalendarEvent（UTC CalDateTime）；添加 RecurrencePattern(RRule)（CS0618 抑制）；EvaluationOptions MaxUnmatchedIncrementsLimit=500；`GetOccurrences` 从 rangeStart。
  3. 对每个 occurrence：start=UTC 偏移 0；start>=rangeEnd 则 break；end 用 Period.EndTime 或 start+duration；`DeriveOccurrenceId` 生成 OccurrenceId。
  4. catch：若原事件与区间相交，回退加入原 Id 实例。
- 调用：ical.net、`DeriveOccurrenceId`

#### `private static Guid DeriveOccurrenceId(Guid eventId, DateTimeOffset occurrenceStart)`
- 输入：主事件 Id、发生开始时间
- 输出：MD5 派生的确定性 Guid
- 步骤：UTF8 字符串 `"{eventId:D}|{occurrenceStart:yyyyMMddTHHmmssZ}"` → MD5 → `new Guid(hash)`
- 分支与异常：无
- 调用：MD5.HashData

### ExpandedEvent
#### 构造函数 `(entity, occurrenceId, occurrenceStart, occurrenceEnd)`
- 属性：Entity、OccurrenceId、OccurrenceStart、OccurrenceEnd（只读）

## 近逐行中文伪代码

1. 引入 Crypto、Text、ical.net 组件与 Evaluation、Logging、Entities。
2. `ExpandEvents` 遍历：无 RRule 做区间相交直通；有 RRule 调 ExpandRecurring 并统计失败空结果。
3. `ExpandRecurring` 用 ical CalendarEvent + RecurrencePattern 展开；越界 break；异常则回退原时间窗。
4. OccurrenceId = MD5(主事件Id|开始UTC字符串) 保证稳定可复现。
5. ExpandedEvent 包装 Entity 与发生时间窗。
6. 结束时 Info 日志汇总 recurring/simple/results/errors。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs",
      "label": "RecurrenceService",
      "path": "src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs", "to": "Ical.Net", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/RecurrenceService.cs", "to": "Microsoft.Extensions.Logging", "type": "depends_on" }
  ]
}
```
