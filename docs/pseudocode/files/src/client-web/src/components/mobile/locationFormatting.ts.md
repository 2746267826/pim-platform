# src/client-web/src/components/mobile/locationFormatting.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：移动端定位相关展示文案与数值格式化（精度、坐标、提供商、质量、距离、速度、时长、片段类型、质量标志）。
- 主要依赖：无
- 被谁使用：HistoricalLocation*、Location* 面板与地图相关组件

## 函数级结构化伪代码

### `formatAccuracyLabel(value)`
- 输入：精度米数（可空）
- 输出：如 `1.2 m` 或 `-`
- 副作用：无
- 步骤：null/NaN → `-`；四舍五入到 0.1；整数用 0 位否则 1 位小数 + ` m`
- 分支与异常：无

### `formatCoordinate(latitude, longitude)`
- 输入：纬经度
- 输出：`lat, lon` 各 6 位小数
- 副作用：无

### `providerLabel(provider)`
- 输入：provider 字符串
- 输出：中文/固定标签
- 步骤：小写 trim；gps/network/fused/passive 映射；否则原值或「未知」

### `sourceKindLabel(source)`
- 输入：来源
- 输出：Android/手动/自动/原值/未知

### `locationQualityLabel(quality)`
- 输入：质量枚举字符串
- 输出：可信/可用/需复核/已拒绝/原值/未知

### `formatDistanceMeters(meters)`
- 输入：米
- 输出：≥1000 用 `x.x km`，否则整米

### `formatSpeedMetersPerSecond(value)`
- 输入：m/s
- 输出：转 km/h 一位小数

### `formatDurationSeconds(seconds)`
- 输入：秒
- 输出：天/小时/分钟/秒中文组合
- 步骤：取整；按 86400/3600/60 拆分；优先显示较大单位

### `segmentKindLabel(kind)`
- 输入：片段类型
- 输出：移动/停留/缺口/低可信/原值/未知

### `qualityFlagLabel(flag)`
- 输入：质量标志
- 输出：低精度聚集、包含拒绝点、时间缺口等中文；空则「正常」

## 近逐行中文伪代码

1. 精度：无效显示 `-`，否则圆整并加单位 m。
2. 坐标固定 6 位小数。
3. 提供商/来源/质量/片段/标志：小写匹配后中文标签。
4. 距离 km/m；速度 m/s→km/h；时长拆分天时分秒。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/locationFormatting.ts",
      "label": "locationFormatting",
      "path": "src/client-web/src/components/mobile/locationFormatting.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/locationFormatting.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "calls" }
  ]
}
```
