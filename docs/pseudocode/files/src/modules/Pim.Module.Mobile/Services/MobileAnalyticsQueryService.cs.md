# src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：规范化移动端分析查询参数（时区、UTC 范围、粒度、分页、过滤串），产出 `MobileAnalyticsQueryContext`。
- 主要依赖：`TimeProvider`、`Pim.Module.Mobile.DTOs`（`MobileAnalyticsQueryRequest`/`Context`/`RangeDto`、`MobileAnalyticsDefaults`）
- 被谁使用：Mobile 分析查询端点/服务在执行聚合前调用 `Normalize`

## 函数级结构化伪代码

### MobileAnalyticsQueryService
#### MobileAnalyticsQueryService(TimeProvider timeProvider)
- 输入：时间提供者
- 输出：服务实例
- 副作用：保存字段
- 步骤：赋值 `_timeProvider`
- 分支与异常：无
- 调用：无

#### MobileAnalyticsQueryContext Normalize(MobileAnalyticsQueryRequest request)
- 输入：原始查询请求（可空字段）
- 输出：规范化后的查询上下文
- 副作用：无（纯计算）
- 步骤：
  1. 时区：空白则 `DefaultTimezone`，否则 Trim；`ResolveTimezone`
  2. `NormalizeRange` 得 `rangeStartUtc`/`rangeEndUtc`；若 end\<start 则交换
  3. 本地日起止：`ConvertTime` 到时区；end 用 `AddTicks(-1)` 再取 Date
  4. 粒度：空或不在 `{hour,30m,15m,day}` → `"hour"`
  5. pageSize：默认 `DefaultPageSize`，钳制 `[1, MaxPageSize]`；page ≥1
  6. minDurationSeconds：默认短事件阈值，≥0
  7. 组装 `MobileAnalyticsRangeDto` + 过滤字段（DeviceId/LifeCategory/PackageName/Source 规范化；IncludeSystemNoise 默认 false；Cursor/Page/PageSize）
- 分支与异常：时区解析见 `ResolveTimezone`
- 调用：`ResolveTimezone`、`NormalizeRange`、`FormatDate`、`NormalizeString`、`TimeZoneInfo.ConvertTime`

#### TimeZoneInfo ResolveTimezone(string timezone)
- 输入：时区 Id 字符串
- 输出：系统 `TimeZoneInfo`
- 副作用：无
- 步骤：
  1. `FindSystemTimeZoneById(timezone)`
  2. 若失败且 timezone 为默认时区：回退 `"China Standard Time"`（覆盖 `TimeZoneNotFoundException`/`InvalidTimeZoneException`）
- 分支与异常：非默认时区的异常向上抛出
- 调用：`TimeZoneInfo.FindSystemTimeZoneById`

#### (DateTimeOffset StartUtc, DateTimeOffset EndUtc) NormalizeRange(DateTimeOffset? startUtc, DateTimeOffset? endUtc, TimeZoneInfo timeZoneInfo)
- 输入：可选起止 UTC、时区
- 输出：闭开或业务约定的 UTC 起止
- 副作用：无
- 步骤：
  1. 两者皆有 → 原样返回
  2. 仅 start → end = start+7 天
  3. 仅 end → start = end-7 天
  4. 皆无：以 `timeProvider.GetUtcNow()` 转本地日；本地 start=今天-6 天；endExclusive=明天；转 UTC 日界
- 分支与异常：无
- 调用：`LocalDateStartUtc`、`_timeProvider.GetUtcNow`、`TimeZoneInfo.ConvertTime`

#### DateTimeOffset LocalDateStartUtc(DateTime localDate, TimeZoneInfo timeZoneInfo)
- 输入：本地日历日、时区
- 输出：该日 00:00 对应的 UTC `DateTimeOffset`（Offset 为零）
- 副作用：无
- 步骤：Unspecified Kind → `ConvertTimeToUtc` → 包装 Offset Zero
- 分支与异常：无
- 调用：`TimeZoneInfo.ConvertTimeToUtc`

#### string FormatDate(DateTime date)
- 输入：日期
- 输出：`yyyy-MM-dd`（不变区域性）
- 副作用：无
- 步骤：`ToString` + `CultureInfo.InvariantCulture`
- 分支与异常：无
- 调用：无

#### string? NormalizeString(string? value)
- 输入：可空字符串
- 输出：空白→null，否则 Trim
- 副作用：无
- 步骤：`IsNullOrWhiteSpace` 判断
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Globalization 与 Mobile DTOs
2. 命名空间 `Pim.Module.Mobile.Services`
3. 密封类；静态粒度集合 hour/30m/15m/day（忽略大小写）
4. 构造注入 `TimeProvider`
5. `Normalize`：解析时区与 UTC 范围；必要时交换起止
6. 计算本地起止日期字符串；规范化粒度与分页
7. 规范化过滤串与系统噪声开关；返回 `MobileAnalyticsQueryContext`
8. `ResolveTimezone`：FindById，默认时区失败则中国标准时间
9. `NormalizeRange`：双端/单端/默认近 7 本地日
10. `LocalDateStartUtc`：本地午夜转 UTC
11. `FormatDate` / `NormalizeString` 工具

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs",
      "label": "MobileAnalyticsQueryService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs", "to": "System.TimeProvider", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs", "type": "depends_on" }
  ]
}
```
