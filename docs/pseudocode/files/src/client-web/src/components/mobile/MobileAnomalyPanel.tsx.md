# src/client-web/src/components/mobile/MobileAnomalyPanel.tsx

## 元信息
- 语言：TypeScript / React
- 程序集或包：client-web
- 职责：手机分析「异常与建议」展示面板：异常列表、建议列表、质量摘要（覆盖/回退）与加载提示。
- 主要依赖：`../../api/mobile` 异常/建议/质量类型；`mobileFormatting`（formatDateTime、formatPercent、healthToneClass）
- 被谁使用：Mobile 分析仪表盘

## 函数级结构化伪代码

### emptyQuality（常量）
- 输入：无
- 输出：全零/空数组的 `MobileAnalyticsQuality` 默认值
- 副作用：无
- 步骤：覆盖率等数值 0；lastSyncAt null；qualityFlags []。
- 分支与异常：无
- 调用：无

### MobileAnomalyPanel(props)
- 输入：anomalies、suggestions、quality?、isLoading?
- 输出：section JSX
- 副作用：无（纯展示）
- 步骤：
  1. `qualitySummary = quality ?? emptyQuality`。
  2. 标题「异常与建议」+ 最近同步时间。
  3. 三列网格：异常（severity 色）、建议（蓝框文本）、质量（事件覆盖/回退占比百分比）。
  4. 空列表显示「暂无…」；`isLoading` 时底部「正在分析异常」。
- 分支与异常：quality 缺省走 emptyQuality
- 调用：`formatDateTime`、`healthToneClass`、`formatPercent`

## 近逐行中文伪代码

1. 引入 mobile API 类型与格式化工具。
2. 默认 emptyQuality 全零。
3. 渲染三栏：异常/建议/质量指标。
4. 无数据占位；加载中提示。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileAnomalyPanel.tsx",
      "label": "MobileAnomalyPanel",
      "path": "src/client-web/src/components/mobile/MobileAnomalyPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileAnomalyPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileAnomalyPanel.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileAnomalyPanel.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" }
  ]
}
```
