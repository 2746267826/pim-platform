# src/Pim.Api/Today/TodaySectionService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：聚合全部 `ITodaySectionProvider`，构建今日分区注册表与单分区内容；解析日期（含 PC 业务日 4 点切日）；分区失败时降级为 Unavailable。
- 主要依赖：
  - `ITodaySectionProvider`、`TodayQuery`、`TodaySectionRegistryDto` / `TodaySectionDto` 等（`Pim.Core.Today`）
  - `TodayEndpointPaths`（`Pim.Api.Endpoints`）
  - `ILogger<TodaySectionService>`
- 被谁使用：`Program.cs` DI 注册；`TodayEndpoints` 注入调用

## 函数级结构化伪代码

### TodaySectionService
#### 构造 `TodaySectionService(IEnumerable<ITodaySectionProvider> providers, ILogger logger)`
- 输入：全部分区提供者、日志
- 输出：实例
- 副作用：缓存去重排序后的提供者列表
- 步骤：
  1. 按 `SectionId`（Ordinal）分组，每组取 `First()`。
  2. 按 `SectionId` 升序排成数组。
- 分支与异常：无
- 调用：LINQ GroupBy/Select/OrderBy

#### `Task<TodaySectionRegistryDto> GetRegistryAsync(string? date, CancellationToken ct)`
- 输入：可选日期字符串
- 输出：注册表 DTO（日期、PC 业务日、UTC 现在、各分区元数据）
- 副作用：无 IO；可能因日期非法抛 `FormatException`
- 步骤：
  1. `BuildQuery(date)`。
  2. 格式化 `query.Date`。
  3. 每个 provider → `TodaySectionRegistryItemDto`（Id、Kind、`Available`、Self 链接含 date 查询串）。
  4. 组装 `TodaySectionRegistryDto`（含 `PcBusinessDate`、`UtcNow`）。
- 分支与异常：日期解析失败见 `BuildQuery`
- 调用：`BuildQuery`、`FormatDate`、`TodayEndpointPaths.Section`

#### `async Task<TodaySectionDto?> GetSectionAsync(string sectionId, string? date, CancellationToken ct)`
- 输入：分区 Id、可选日期
- 输出：分区 DTO；未知 Id 返回 null
- 副作用：调用 provider；失败记 Warning 日志
- 步骤：
  1. 按 `SectionId` Ordinal 匹配 provider；无则 null。
  2. `BuildQuery(date)`。
  3. try：`provider.BuildAsync(query, ct)`。
  4. 若取消且 `ct` 已取消：重抛 `OperationCanceledException`。
  5. 其他异常：日志 + 返回 Unavailable 分区（空 payload、错误码 `section_unavailable`、中文消息）。
- 分支与异常：未知分区 null；取消向上；其他降级
- 调用：`ITodaySectionProvider.BuildAsync`

#### `static TodayQuery BuildQuery(string? date)`
- 输入：可选日期/日期时间字符串
- 输出：`TodayQuery`
- 副作用：无
- 步骤：
  1. 空白 → `BuildQuery(DateTime.Now, hasExplicitTime: true)`。
  2. 精确 `yyyy-MM-dd` → `TodayQuery(dateOnly, dateOnly)`。
  3. 可解析 DateTime → `BuildQuery(dateTime, HasExplicitTime(date))`。
  4. 否则 `FormatException`（中文提示）。
- 分支与异常：非法格式抛异常
- 调用：`DateOnly.TryParseExact`、`DateTime.TryParse`、`HasExplicitTime`

#### `static TodayQuery BuildQuery(DateTime dateTime, bool hasExplicitTime)`
- 输入：本地/解析出的 DateTime、是否显式含时间
- 输出：`TodayQuery(todayDate, pcBusinessDate)`
- 副作用：无
- 步骤：
  1. `todayDate = DateOnly.FromDateTime(dateTime.Date)`。
  2. 若有显式时间且小时 < 4 → PC 业务日 = 前一天，否则 = todayDate。
- 分支与异常：无
- 调用：无

#### `static bool HasExplicitTime(string date)`
- 输入：原始字符串
- 输出：是否含 `:` 或 ` AM`/` PM`（忽略大小写）
- 副作用：无
- 步骤：字符串 Contains 检查
- 分支与异常：无
- 调用：无

#### `static string FormatDate(DateOnly date)`
- 输入：DateOnly
- 输出：`yyyy-MM-dd`（InvariantCulture）
- 副作用：无
- 步骤：`ToString(DateFormat, InvariantCulture)`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Globalization、Logging、Endpoints、Core.Today。
2. 常量日期格式 `yyyy-MM-dd`；持有 logger 与去重排序后的 providers。
3. 构造：按 SectionId 分组取首、再按 SectionId 排序成数组。
4. `GetRegistryAsync`：解析查询 → 格式化日期 → 为每个 provider 建 Available 注册项（Self 链接带 date）→ 返回注册表（含 PC 业务日与 UtcNow）。
5. `GetSectionAsync`：匹配 provider；无则 null；BuildQuery；BuildAsync 成功则返回；取消则重抛；其他异常记 Warning 并返回 Unavailable + 错误 DTO。
6. `BuildQuery(string?)`：空用本机 Now（视为有时间）；严格日期则日=业务日；可解析 DateTime 则看是否显式时间；否则 FormatException。
7. `BuildQuery(DateTime, bool)`：有显式时间且 <4 点则 PC 业务日减一天。
8. `HasExplicitTime`：含冒号或 AM/PM。
9. `FormatDate`：Invariant `yyyy-MM-dd`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Today/TodaySectionService.cs",
      "label": "TodaySectionService",
      "path": "src/Pim.Api/Today/TodaySectionService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Today/TodaySectionService.cs.md",
      "layer": "api",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Today/TodaySectionService.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionService.cs", "to": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "to": "src/Pim.Api/Today/TodaySectionService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Today/TodaySectionService.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionService.cs", "to": "src/Pim.Api/Today/TodaySectionProviders.cs", "type": "calls" }
  ]
}
```
