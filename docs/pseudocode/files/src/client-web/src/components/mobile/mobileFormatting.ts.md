# src/client-web/src/components/mobile/mobileFormatting.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：移动端分析 UI 的上海时区日期范围、时长/百分比/数字/时间格式化，以及分类/来源/状态/健康状态中文标签与 tone class。
- 主要依赖：`../../api/mobile`（`MobileLifeCategory`、`MOBILE_DEFAULT_TIMEZONE`）、`PimHealthStatus`
- 被谁使用：LocationPointList、MobileUsageHeatmap 及多数 mobile 组件

## 函数级结构化伪代码

### 类型导出
#### MobileRangeShortcut / MobileAnalyticsDateRange / MobileAnalyticsUtcRange
- 输入：无
- 输出：快捷范围与 UTC 查询范围类型
- 副作用：无
- 步骤：类型别名/接口
- 分支与异常：无
- 调用：无

### 内部辅助
#### pad2 / dateInputFromUtcDate / parseShanghaiDate
- 输入：数字或 Date / `YYYY-MM-DD`
- 输出：补零串、UTC 日期输入、带 +08:00 的 Date
- 副作用：无
- 步骤：padStart；拼 ISO 日期；`new Date(valueT00:00:00+08:00)`
- 分支与异常：无
- 调用：无

### 日期范围
#### formatShanghaiDateInput(now?)
- 输入：当前时刻
- 输出：上海时区 `YYYY-MM-DD`
- 副作用：无
- 步骤：Intl en-CA formatToParts 取年/月/日；失败回退 UTC 拼装
- 分支与异常：parts 缺失回退
- 调用：`Intl.DateTimeFormat`

#### addShanghaiDays(dateInput, days)
- 输入：日期输入与天数偏移
- 输出：偏移后上海日期输入
- 副作用：无
- 步骤：parse → +days*DAY_MS → formatShanghaiDateInput
- 分支与异常：无
- 调用：`parseShanghaiDate`、`formatShanghaiDateInput`

#### buildMobileAnalyticsDateRange(shortcut?, now?)
- 输入：today/7d/30d/custom，当前时间
- 输出：`{shortcut,startDate,endDate}`
- 副作用：无
- 步骤：today 单日；30d 回溯 29 天；其余（含 custom 默认）回溯 6 天作 7d
- 分支与异常：custom 仍给 7 日窗但 shortcut 标 custom
- 调用：`formatShanghaiDateInput`、`addShanghaiDays`

#### toMobileAnalyticsUtcRange(range)
- 输入：startDate/endDate
- 输出：rangeStartUtc/rangeEndUtc/timezone
- 副作用：无
- 步骤：纠正起止顺序；start 日 0 点 ISO；end 次日 0 点 ISO；timezone 默认上海
- 分支与异常：起止颠倒自动交换
- 调用：`parseShanghaiDate`、`addShanghaiDays`

### 数值与时间格式
#### formatDuration / formatCompactDuration
- 输入：秒数
- 输出：中文「X小时Y分钟」或紧凑 `Xh Ym`/`Xs`
- 副作用：无
- 步骤：max(0,round)；拆小时分钟；优先小时再分钟再秒
- 分支与异常：null/undefined 当 0
- 调用：无

#### formatPercent / formatSignedPercent / formatNumber
- 输入：比例或数字
- 输出：百分比/带符号百分比/zh-CN 整数
- 副作用：无
- 步骤：非有限 → 0% 或 `-`；round*100；toLocaleString
- 分支与异常：NaN 处理
- 调用：无

#### formatDateTime / formatShortTime / formatLocalDate
- 输入：ISO 或日期字符串
- 输出：上海时区本地化日期时间/时分/月日
- 副作用：无
- 步骤：空 `-`；非法原值；formatLocalDate 用 +08:00 午夜
- 分支与异常：Invalid Date
- 调用：`Date`、`toLocale*`

### 标签
#### formatCategoryLabel / sourceLabel / statusLabel / healthStatusLabel / healthToneClass
- 输入：分类/来源/状态枚举或字符串
- 输出：中文文案或 Tailwind tone class
- 副作用：无
- 步骤：映射 events/fallback；succeeded/partial/failed/pending；Healthy/Warning/Critical/Info；对应颜色 class
- 分支与异常：未知回退「未分类/未知来源/未知」与 slate 样式
- 调用：无

## 近逐行中文伪代码

1. 引入 MobileLifeCategory、MOBILE_DEFAULT_TIMEZONE、PimHealthStatus
2. 导出快捷范围与日期/UTC 范围类型
3. 常量上海偏移 +08:00 与 DAY_MS
4. pad2、UTC 日期串、parseShanghaiDate
5. formatShanghaiDateInput 用 Intl 上海日历日
6. addShanghaiDays 按天偏移
7. buildMobileAnalyticsDateRange：today/30d/默认 7d
8. toMobileAnalyticsUtcRange：半开区间 [start, end+1day)
9. formatDuration/CompactDuration 中英文时长
10. formatPercent/SignedPercent/Number
11. formatDateTime/ShortTime/LocalDate 上海时区
12. formatCategoryLabel 默认「未分类」
13. sourceLabel/statusLabel 中文映射
14. healthStatusLabel 与 healthToneClass 配色

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/mobileFormatting.ts",
      "label": "mobileFormatting",
      "path": "src/client-web/src/components/mobile/mobileFormatting.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/mobileFormatting.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/mobileFormatting.ts", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/mobileFormatting.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationPointList.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" }
  ]
}
```
