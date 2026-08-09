# src/client-web/src/components/mobile/MobileInsightStrip.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：移动分析概览指标条：总时长、日均、目标、App 数、完整度、最近同步。
- 主要依赖：`MobileAnalyticsOverview`、`mobileFormatting`、`MobileMetricGrid`
- 被谁使用：移动分析仪表盘头部区域

## 函数级结构化伪代码

### MobileInsightStrip
#### default function MobileInsightStrip({ overview, isLoading = false })
- 输入：可选概览 DTO、加载中标志
- 输出：`MobileMetricGrid` 六格指标
- 副作用：无
- 步骤：
  1. 取 `goalProgress`；超限 tone=warning，有目标 good，否则 default
  2. 构造 6 个 `MobileMetricItem`：
     - 总使用时长 + 较上期百分比；加载中显示「加载中」
     - 日均 + 峰值日
     - 目标 used/limit 或「未设置」
     - App 数 + 切换次数
     - 完整度 + 事件覆盖；≥0.9 good 否则 warning
     - 最近同步日期 + stale/生成时间
  3. 包在 max-w section 中渲染 `MobileMetricGrid`
- 分支与异常：overview 缺失时用占位 helper
- 调用：`formatDuration`、`formatSignedPercent`、`formatCompactDuration`、`formatNumber`、`formatPercent`、`formatDateTime`、`MobileMetricGrid`

## 近逐行中文伪代码

1. 导入 overview 类型、格式化函数、MobileMetricGrid
2. Props：overview 可选、isLoading 默认 false
3. goal 与 goalTone 计算
4. items 数组六项指标 value/helper/tone
5. 返回 section + MobileMetricGrid

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileInsightStrip.tsx",
      "label": "MobileInsightStrip",
      "path": "src/client-web/src/components/mobile/MobileInsightStrip.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileInsightStrip.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileInsightStrip.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileInsightStrip.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileInsightStrip.tsx", "to": "src/client-web/src/components/mobile/MobileMetricGrid.tsx", "type": "calls" }
  ]
}
```
