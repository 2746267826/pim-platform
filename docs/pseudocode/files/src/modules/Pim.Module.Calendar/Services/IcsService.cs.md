# src/modules/Pim.Module.Calendar/Services/IcsService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：用 Ical.Net 在 `EventEntity` 与 ICS 文本之间导入/导出。
- 主要依赖：`Ical.Net`（Calendar/CalendarEvent/RecurrencePattern/CalendarSerializer）、`EventEntity`
- 被谁使用：`CalendarModule` 导出 ICS 端点；导入场景可能配合其它服务

## 函数级结构化伪代码

### IcsService
#### string ExportEvents(IEnumerable\<EventEntity\> events)
- 输入：事件实体集合
- 输出：ICS 序列化字符串
- 副作用：无持久化
- 步骤：
  1. 新建 `Ical.Net.Calendar`，时区 `Asia/Shanghai`
  2. 对每个实体建 `CalendarEvent`：Uid/Summary/Description/Location/Start/End/DtStamp/Status（时间为 UTC）
  3. 若有 `RRule`，加入 `RecurrencePattern`（抑制过时 API 警告）
  4. 加入 `calendar.Events`，`CalendarSerializer.SerializeToString`
- 分支与异常：无 RRule 则跳过
- 调用：Ical.Net 类型

#### List\<ParsedEvent\> ImportEvents(string icsContent)
- 输入：ICS 文本
- 输出：解析后的 `ParsedEvent` 列表
- 副作用：无
- 步骤：
  1. 空/空白 → 空列表
  2. `IcalCalendar.Load`；无 Events → 空列表
  3. 映射每事件：Uid（缺则 NewGuid）、Summary 默认 Untitled、描述地点、Start/End 的 AsUtc 转 `DateTimeOffset`（缺则 MinValue）、首条 RecurrenceRules 字符串
- 分支与异常：空内容/无日历
- 调用：`IcalCalendar.Load`

### ParsedEvent
#### record ParsedEvent(...)
- 输入：Uid/Title/Description/Location/Start/End/RRule
- 输出：不可变记录
- 副作用：无
- 步骤：位置记录定义
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Ical.Net 组件与 `EventEntity`，别名 `IcalCalendar`
2. 类 `IcsService`
3. `ExportEvents`：建日历加上海时区；循环实体填 VEVENT 字段；有 RRule 则加循环规则；序列化为字符串返回
4. `ImportEvents`：空白返回空；Load ICS；无事件返回空；Select 为 ParsedEvent（UTC 时间、可选 RRule）
5. 记录类型 `ParsedEvent` 承载解析结果字段

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/IcsService.cs",
      "label": "IcsService",
      "path": "src/modules/Pim.Module.Calendar/Services/IcsService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/IcsService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/IcsService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/IcsService.cs", "to": "Ical.Net", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/IcsService.cs", "type": "calls" }
  ]
}
```
