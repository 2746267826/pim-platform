# tests/Pim.UnitTests/Services/IcsServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：ICS 导出/导入、往返保真、空列表、RRule 字段。
- 主要依赖：`IcsService`、`EventEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### IcsServiceTests
#### ExportEvents_SingleEvent_ProducesValidIcs()
- 输入：单事件实体
- 输出：无
- 副作用：无
- 步骤：ExportEvents 后断言 VCALENDAR/VEVENT/UID/SUMMARY/DESCRIPTION/LOCATION/STATUS
- 分支与异常：无
- 调用：`IcsService.ExportEvents`

#### ImportEvents_ValidIcsContent_ParsesCorrectly()
- 输入：手写 ICS 文本
- 输出：无
- 副作用：无
- 步骤：ImportEvents 单条；Uid/Title/Description/Location
- 分支与异常：无
- 调用：`IcsService.ImportEvents`

#### ExportThenImport_RoundTrip_PreservesEventData()
- 输入：完整 EventEntity
- 输出：无
- 副作用：无
- 步骤：Export→Import 字段一致
- 分支与异常：无
- 调用：Export + Import

#### ExportEvents_EmptyList_ProducesEmptyCalendar()
- 输入：空序列
- 输出：无
- 副作用：无
- 步骤：有日历壳无 VEVENT
- 分支与异常：无
- 调用：ExportEvents

#### ImportEvents_EmptyContent_ReturnsEmptyList()
- 输入：空串
- 输出：无
- 副作用：无
- 步骤：结果 Empty
- 分支与异常：无
- 调用：ImportEvents

#### ExportEvents_WithRRule_IncludesRecurrenceRule()
- 输入：带 RRule 事件
- 输出：无
- 副作用：无
- 步骤：ICS 含 `RRULE:FREQ=WEEKLY;BYDAY=MO`
- 分支与异常：无
- 调用：ExportEvents

## 近逐行中文伪代码

1. 单事件导出字段齐全
2. 合法 ICS 解析
3. 往返保真
4. 空导出/空导入
5. RRule 写出

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/IcsServiceTests.cs",
      "label": "IcsServiceTests",
      "path": "tests/Pim.UnitTests/Services/IcsServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/IcsServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/IcsServiceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/IcsService.cs", "type": "tests" }
  ]
}
```
