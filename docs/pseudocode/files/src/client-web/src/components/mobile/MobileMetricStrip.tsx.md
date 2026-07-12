# src/client-web/src/components/mobile/MobileMetricStrip.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：手机使用摘要指标条：总前台时长、切换次数、App 数、完整度、质量问题、最后同步；区分事件明细/回退汇总模式。
- 主要依赖：`mobileFormatting`（`formatDateTime`/`formatDuration`/`formatPercent`）
- 被谁使用：`MobileRecordsDashboard`

## 函数级结构化伪代码

### MetricItem({ label, value, helper? })
- 输入：标签、主值、可选辅助文案
- 输出：`<div>` 内 dt/dd/[p]
- 副作用：无
- 步骤：白底卡片；有 helper 时渲染灰色小字
- 分支与异常：helper 可选
- 调用：无

### MobileMetricStrip(props)
- 输入：`totalForegroundSeconds`、`appSwitchCount`、`appsUsed`、`completeness`、`qualityIssueCount`、`lastSyncAt`、可选 `fallbackForegroundSeconds`（默认 0）
- 输出：`<dl>` 网格指标条
- 副作用：无
- 步骤：
  1. `summaryMode = fallbackForegroundSeconds > 0 ? 'fallback' : 'events'`，写入 `data-summary-mode`。
  2. 总前台时长：`formatDuration`；helper 为「回退汇总 …」或「事件明细」。
  3. 其余五项：切换次数、使用 App 数、完整度%、质量问题数、最后同步时间。
- 分支与异常：回退秒数 >0 切换 helper 文案
- 调用：`formatDuration`/`formatPercent`/`formatDateTime`

## 近逐行中文伪代码

1. 导出 props 接口与默认组件。
2. 内部 MetricItem 渲染单指标卡片。
3. 根据 fallback 秒数设 summaryMode。
4. 六格响应式网格展示汇总指标。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileMetricStrip.tsx",
      "label": "MobileMetricStrip",
      "path": "src/client-web/src/components/mobile/MobileMetricStrip.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileMetricStrip.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileMetricStrip.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/MobileMetricStrip.tsx", "type": "depends_on" }
  ]
}
```
