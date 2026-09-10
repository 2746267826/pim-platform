# src/client-web/src/components/mobile/MobileUsageHeatmap.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：移动端使用热力图：按粒度切换 hour/30m/15m；用 `buildHeatmapMatrix` 渲染日×小时格子；支持选中与加载/空态。
- 主要依赖：`MobileHeatmapBucket` 类型、`formatDuration`、`buildHeatmapMatrix`/`HeatmapMatrixCell`
- 被谁使用：移动分析 Dashboard

## 函数级结构化伪代码

### 类型与常量
#### MobileHeatmapGranularity / MobileUsageHeatmapProps / granularities
- 输入：props 与粒度键
- 输出：类型约束与 UI 选项 hour/30m/15m
- 副作用：无
- 步骤：从 MobileAnalyticsGranularity 抽取；定义 props 与选项数组
- 分支与异常：无
- 调用：无

### cellBackground(cell, maxSeconds)
- 输入：矩阵单元格、最大秒数
- 输出：rgba 背景色
- 副作用：无
- 步骤：前台秒≤0 → 浅灰；否则 intensity=max(0.12, seconds/max) 且 α≤0.88 的 teal
- 分支与异常：无用量浅色
- 调用：无

### MobileUsageHeatmap(props)
- 输入：buckets、granularity、selectedBucketStartUtc、isLoading、回调
- 输出：热力图 section
- 副作用：点击格子/粒度按钮触发父回调
- 步骤：
  1. `matrix = buildHeatmapMatrix(buckets)`
  2. 标题与图例；粒度 segmented 按钮
  3. CSS grid：92px 日期列 + 24 小时列
  4. 表头小时；按 day 渲染 label + 每小时 cell 按钮
  5. primaryBucket=sourceBuckets[0]；无 bucket 则 disabled
  6. 选中 ring；qualityFlags 琥珀色底条；title/aria 含时长
  7. 加载/空数据提示
- 分支与异常：无 primaryBucket 不可点；空 buckets 提示
- 调用：`buildHeatmapMatrix`、`formatDuration`、`onGranularityChange`、`onBucketSelect`

## 近逐行中文伪代码

1. 引入 MobileAnalyticsGranularity/MobileHeatmapBucket、formatDuration、buildHeatmapMatrix
2. 导出粒度类型与 Props
3. granularities 三档中文/缩写标签
4. cellBackground 按前台秒映射 teal 透明度
5. 组件：buildHeatmapMatrix(buckets)
6. 头部：标题说明 + 少→多图例 + 粒度切换
7. 横向滚动网格 25 列（标签+24h）
8. 渲染小时表头
9. 按天 contents：日期标签 + 24 个 cell 按钮
10. 选中比较 selectedBucketStartUtc；点击首个 sourceBucket
11. qualityFlags 非空显示琥珀色指示条
12. isLoading / 空 buckets 底部提示

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx",
      "label": "MobileUsageHeatmap",
      "path": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileUsageHeatmap.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "to": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts", "type": "calls" }
  ]
}
```
