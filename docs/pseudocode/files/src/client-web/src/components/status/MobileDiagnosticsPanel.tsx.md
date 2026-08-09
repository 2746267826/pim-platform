# src/client-web/src/components/status/MobileDiagnosticsPanel.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：移动端质量诊断面板：总状态、五类诊断卡、下一步建议。
- 主要依赖：StatusBadge、PimHealthStatus
- 被谁使用：状态中心/移动诊断入口

## 函数级结构化伪代码

### normalizeStatus / formatCheckedAt / formatDetailKey/Value / findComponent
- 数字/字符串状态归一；时间与 details 展示；按 key 列表找 component

### DiagnosticCard / PanelShell
- 单卡：名称、检查时间、badge、message、最多 6 条 details

### MobileDiagnosticsPanel
- loading / error 分支
- 否则 overall + 统计 + diagnostics.map 找组件 + nextSteps 前 3 条

## 近逐行中文伪代码

1. 定义状态映射、detail 中文标签、五类 diagnostics keys。
2. 规范化 overallStatus。
3. 加载中/错误红框。
4. 正常：总览 + 双列诊断卡 + amber 下一步。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/status/MobileDiagnosticsPanel.tsx",
      "label": "MobileDiagnosticsPanel",
      "path": "src/client-web/src/components/status/MobileDiagnosticsPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/status/MobileDiagnosticsPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/status/MobileDiagnosticsPanel.tsx", "to": "src/client-web/src/ui/StatusBadge.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/status/MobileDiagnosticsPanel.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
