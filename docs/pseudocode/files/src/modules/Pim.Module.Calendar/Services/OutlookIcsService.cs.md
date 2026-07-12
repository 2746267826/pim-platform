# src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：解析 Outlook/ICS 文本为结构化事件列表；保留原始 VEVENT、时区、复发元数据与 Outlook 扩展属性。
- 主要依赖：`Ical.Net`（`Calendar`/`CalendarEvent`）、`System.Text.Json`
- 被谁使用：`CalendarModule` 注册与 ICS 导入端点；`CalendarService.ImportOutlookIcsAsync`；单元测试 `OutlookIcsServiceTests` 等

## 函数级结构化伪代码

### OutlookIcsService
#### OutlookIcsParseResult Parse(string icsContent)
- 输入：ICS 全文
- 输出：`OutlookIcsParseResult`（事件列表；解析失败可带 `ErrorReason=parse_error`）
- 副作用：无外部 I/O
- 步骤：
  1. 空白内容 → 空事件列表
  2. `IcalCalendar.Load`；捕获任意异常 → 空列表 + `parse_error`
  3. 无 Events → 空列表
  4. `ExtractRawEventComponents` 按顺序切出原始 VEVENT 块
  5. 对每个 `CalendarEvent`：对齐 raw；UTC 起止；raw 有 DTSTART/DTEND 但解析空则 `invalidReason=parse_error`
  6. 提取时区、RECURRENCE-ID、EXDATE、RRULE、全天标记；`BuildMetadata` 序列化为 ExternalMetadataJson
  7. 组装 `OutlookIcsParsedEvent` 列表返回
- 分支与异常：Load 失败吞异常；单事件可标 InvalidReason
- 调用：`IcalCalendar.Load`、raw/属性辅助方法、`JsonSerializer.Serialize`

#### static Dictionary BuildMetadata(string? method, CalendarEvent e, string rawComponent)
- 输入：日历 METHOD、事件、原始组件文本
- 输出：元数据字典（organizer/attendees/sequence/class/transp/priority/categories/htmlDescription/outlookProperties 等）
- 副作用：无
- 步骤：遍历 `e.Properties`；优先 raw 值；按属性名分类；收集 X-MICROSOFT/X-MS-OLK
- 分支与异常：空 name 跳过；int 解析失败保留字符串
- 调用：`GetRawPropertyValues`、`GetParameters`

#### static Dictionary GetParameters(ICalendarProperty property)
- 输入：iCal 属性
- 输出：参数名→值字典（忽略大小写键比较）
- 副作用：无
- 步骤：`ToDictionary` on Parameters
- 分支与异常：无
- 调用：无

#### static bool IsAllDay(CalendarEvent e)
- 输入：事件
- 输出：Start 存在且 `!HasTime`
- 副作用：无
- 步骤：检查 `e.Start`
- 分支与异常：Start 空 → false
- 调用：无

#### static string? GetSourceTimeZoneId(CalendarEvent e)
- 输入：事件
- 输出：时区 Id 或 null
- 副作用：无
- 步骤：优先 `Start.TzId`；否则 DTSTART 参数 TZID
- 分支与异常：均无则 null
- 调用：无

#### static string? GetPropertyValue / IEnumerable GetPropertyValues
- 输入：事件与属性名
- 输出：单个或全部非空白属性值
- 副作用：无
- 步骤：按名忽略大小写过滤 Properties
- 分支与异常：无匹配 → null/空
- 调用：无

#### static bool HasRawDateProperty(string rawComponent)
- 输入：原始 VEVENT
- 输出：是否含 DTSTART 或 DTEND 行
- 副作用：无
- 步骤：查 raw 属性行
- 分支与异常：无
- 调用：`GetRawPropertyValues`

#### static IEnumerable GetRawPropertyValues(string rawComponent, string name)
- 输入：原始组件、属性名
- 输出：冒号后的值序列
- 副作用：无
- 步骤：`UnfoldLines`；行以 name 开头且下一字符为 `:` 或 `;`；取冒号后子串
- 分支与异常：不匹配则 continue
- 调用：`UnfoldLines`

#### static IEnumerable UnfoldLines(string value)
- 输入：多行文本
- 输出：折叠续行后的逻辑行
- 副作用：无
- 步骤：统一换行；空格/Tab 开头拼接到上一行；产出完整行
- 分支与异常：末行非空则 yield
- 调用：无

#### static List ExtractRawEventComponents(string icsContent)
- 输入：ICS 全文
- 输出：每个 `BEGIN:VEVENT`…`END:VEVENT` 子串列表（内部用 CRLF）
- 副作用：无
- 步骤：规范化换行；循环 IndexOf BEGIN/END 切片
- 分支与异常：找不到 BEGIN 或 END 则结束
- 调用：无

### OutlookIcsParseResult / OutlookIcsParsedEvent
#### record 类型
- 输入：构造参数见源码字段
- 输出：不可变结果/事件快照
- 副作用：无
- 步骤：承载 Uid/Title/时间/RRule/全天/时区/raw/元数据 JSON/复发字段/InvalidReason
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、Ical.Net 组件；别名 `IcalCalendar`
2. 服务类；静态 Web 默认 Json 选项
3. Parse：空内容返回空；Load 失败 parse_error；无 Events 空列表
4. 切 raw VEVENT；逐事件映射字段与 invalidReason
5. BuildMetadata：METHOD、组织者/与会者、序列/类别/优先级/透明度/HTML 描述、微软扩展属性
6. 辅助：参数字典、全天、时区、属性读写、raw 行匹配、续行展开、VEVENT 切片
7. 记录类型 `OutlookIcsParseResult` 与 `OutlookIcsParsedEvent` 定义导出形状

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs",
      "label": "OutlookIcsService",
      "path": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs", "to": "Ical.Net", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookIcsServiceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookIcsService.cs", "type": "tests" }
  ]
}
```
