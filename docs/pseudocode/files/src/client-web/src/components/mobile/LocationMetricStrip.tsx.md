# src/client-web/src/components/mobile/LocationMetricStrip.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：将定位分析 overview 转为六格指标条（点数、跨度、里程、停留、精度、质量）。
- 主要依赖：`MobileLocationAnalyticsOverview`、`MobileMetricGrid`、`locationFormatting` 格式化函数
- 被谁使用：移动定位分析仪表盘

## 函数级结构化伪代码

### LocationMetricStrip（默认导出）
#### render({ overview })
- 输入：`MobileLocationAnalyticsOverview | null | undefined`
- 输出：`MobileMetricGrid` 节点
- 副作用：无
- 步骤：
  1. qualityHelper：有 qualityFlags 则 map `qualityFlagLabel` 用顿号拼接，否则「质量正常」
  2. 组装 6 项：
     - 定位点：pointCount；helper 保留/拒绝数
     - 活跃跨度：formatDurationSeconds(activeSpanSeconds)
     - 估算里程：formatDistanceMeters(distanceMeters)
     - 停留点：stayCount；helper 最长停留
     - 平均误差：formatAccuracyLabel(averageAccuracyMeters)
     - 质量提示：qualityIssueCount；tone 有问题 warning 否则 good
  3. 交给 `MobileMetricGrid`
- 分支与异常：overview 空时数值用 0/格式化函数对 undefined 的默认
- 调用：`MobileMetricGrid`、`formatAccuracyLabel`、`formatDistanceMeters`、`formatDurationSeconds`、`qualityFlagLabel`

## 近逐行中文伪代码

1. 引入 overview 类型、MobileMetricGrid、locationFormatting
2. 组件接收 overview
3. 质量 helper 文案
4. 返回 MobileMetricGrid，items 六项指标与 helper/tone

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/LocationMetricStrip.tsx",
      "label": "LocationMetricStrip",
      "path": "src/client-web/src/components/mobile/LocationMetricStrip.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/LocationMetricStrip.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/LocationMetricStrip.tsx", "to": "src/client-web/src/components/mobile/MobileMetricGrid.tsx", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/LocationMetricStrip.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationMetricStrip.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" }
  ]
}
```
