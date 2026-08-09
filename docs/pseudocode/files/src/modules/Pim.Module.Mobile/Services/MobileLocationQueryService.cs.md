# src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：规范化移动端位置查询参数（时区、UTC 范围、精度阈值、分页与游标），输出 `MobileLocationQueryContext`。
- 主要依赖：`TimeProvider`、`Pim.Module.Mobile.DTOs`（含 `MobileAnalyticsDefaults`）
- 被谁使用：Mobile 位置查询端点/服务

## 函数级结构化伪代码

### MobileLocationQueryService
#### 常量
- `DefaultMaxAccuracyMeters = 50`、`DefaultPageSize = 50`、`MaxPageSize = 200`

#### 构造 `MobileLocationQueryService(TimeProvider timeProvider)`
- 输入：时间提供者
- 输出：服务实例
- 副作用：保存 `_timeProvider`
- 步骤：赋值字段
- 分支与异常：无
- 调用：无

#### `MobileLocationQueryContext Normalize(MobileLocationQueryRequest request)`
- 输入：原始查询请求
- 输出：规范化上下文（范围 DTO、设备、精度、是否含拒绝点、游标、页大小）
- 副作用：无
- 步骤：
  1. 时区：空白则用 `MobileAnalyticsDefaults.DefaultTimezone`，否则 Trim；`ResolveTimezone`。
  2. `NormalizeRange` 得 UTC 起止；若 end < start 则交换。
  3. 本地日起止：start 转本地日期；end 用 `rangeEndUtc.AddTicks(-1)` 转本地日期（半开区间）。
  4. pageSize = Clamp(请求值或默认 50, 1, 200)。
  5. maxAccuracyMeters：请求 >0 用请求值，否则 50。
  6. 组装 `MobileAnalyticsRangeDto` + DeviceId/IncludeRejected/Cursor/pageSize。
- 分支与异常：时区解析见 `ResolveTimezone`
- 调用：`ResolveTimezone`、`NormalizeRange`、`FormatDate`、`NormalizeString`

#### `static TimeZoneInfo ResolveTimezone(string timezone)`
- 输入：时区 ID
- 输出：`TimeZoneInfo`
- 副作用：无
- 步骤：`FindSystemTimeZoneById`；若为默认时区且抛 `TimeZoneNotFoundException`/`InvalidTimeZoneException`，回退 `"China Standard Time"`
- 分支与异常：非默认时区异常向上抛出
- 调用：`TimeZoneInfo.FindSystemTimeZoneById`

#### `(StartUtc, EndUtc) NormalizeRange(startUtc?, endUtc?, timeZoneInfo)`
- 输入：可选起止 UTC、时区
- 输出：闭开或显式 UTC 对
- 副作用：无
- 步骤：
  1. 两者皆有 → 原样返回。
  2. 仅 start → end = start+7 天。
  3. 仅 end → start = end-7 天。
  4. 皆无 → 本地今天往前 6 天 00:00 到明天 00:00（本地日界转 UTC）。
- 分支与异常：无
- 调用：`_timeProvider.GetUtcNow`、`LocalDateStartUtc`

#### `static DateTimeOffset LocalDateStartUtc(DateTime localDate, TimeZoneInfo)`
- 输入：本地日期、时区
- 输出：该本地日 00:00 对应 UTC（Offset 0）
- 步骤：Unspecified Kind → ConvertTimeToUtc → DateTimeOffset(utc, Zero)

#### `static string FormatDate` / `NormalizeString`
- FormatDate：`yyyy-MM-dd` InvariantCulture。
- NormalizeString：空白 → null，否则 Trim。

## 近逐行中文伪代码

1. 引入 Globalization 与 Mobile DTOs。
2. 常量：默认精度 50m、页 50、最大页 200；注入 TimeProvider。
3. Normalize：补默认时区 → 解析时区 → 规范化 UTC 范围并保证 start≤end。
4. 用本地日历日填 RangeDto 的 LocalStart/LocalEnd 日期字符串。
5. Clamp 页大小；精度默认 50；IncludeRejected 默认 false；字符串字段 Trim 或 null。
6. ResolveTimezone：默认时区在 Windows/IANA 找不到时回退 China Standard Time。
7. NormalizeRange：双端/单端/默认近 7 本地日。
8. LocalDateStartUtc 将本地日零点转为 UTC；FormatDate/NormalizeString 辅助。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs",
      "label": "MobileLocationQueryService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileLocationQueryService.cs", "to": "System.TimeProvider", "type": "depends_on" }
  ]
}
```
