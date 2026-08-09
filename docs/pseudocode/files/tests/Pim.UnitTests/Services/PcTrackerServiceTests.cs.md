# tests/Pim.UnitTests/Services/PcTrackerServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证业务日起点查询辅助方法返回带 UTC Offset 的 `DateTimeOffset`，本地日历日对齐 4 点起算。
- 主要依赖：`PcTrackerService.GetBusinessDayStartForQuery`
- 被谁使用：xUnit

## 函数级结构化伪代码

### PcTrackerServiceTests
#### BusinessDayStart_ReturnsUtcOffsetForPostgresTimestamptzQueries()
- 输入：无（固定日期 2026-05-20）
- 输出：无
- 副作用：无
- 步骤：
  1. 调用 `GetBusinessDayStartForQuery(date)`
  2. 断言 `Offset == TimeSpan.Zero`（timestamptz 友好）
  3. 断言本地时间的 DateTime 等于 `date.Date.AddHours(4)`
- 分支与异常：无
- 调用：`PcTrackerService.GetBusinessDayStartForQuery`

## 近逐行中文伪代码

1. 构造日历日 2026-05-20
2. 取业务日起点
3. Offset 为零；本地墙钟为当日 04:00

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/PcTrackerServiceTests.cs",
      "label": "PcTrackerServiceTests",
      "path": "tests/Pim.UnitTests/Services/PcTrackerServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/PcTrackerServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/PcTrackerServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "tests" }
  ]
}
```
