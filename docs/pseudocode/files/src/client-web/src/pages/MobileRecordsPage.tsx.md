# src/client-web/src/pages/MobileRecordsPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：手机使用分析总页：日期范围、设备/分类/包名筛选、热力图、图表、时间线、异常与应用目录管理。
- 主要依赖：`../api/mobile` 全套、mobile 组件族、react-query
- 被谁使用：路由「手机记录」

## 函数级结构化伪代码

### errorMessage / catalogMutationKeys
- 错误文案；目录变更需失效的 query key 列表

### MobileRecordsPage
- 状态：range 快捷/自定义、device、category、package、系统噪声、heatmap 粒度、选中 bucket、展开 block/session、分页
- 派生：utcRange → analyticsQuery
- 查询：devices、overview、heatmap、charts、timeline blocks、overrides、rules；按需 sessions/events
- mutations：save/delete override 与 rule（create 或 update）
- 交互 handler：改范围/筛选/热力选中/图表下钻/分页/refresh
- 渲染：Header + InsightStrip + Heatmap|Detail + Charts + Timeline + Anomaly|CatalogManager

## 近逐行中文伪代码

1. 默认 7 天范围与大量筛选 state。
2. 组装 MobileAnalyticsQuery（含 minDuration、timezone）。
3. 多 query 并行，部分 30s 刷新。
4. 目录 mutation 成功统一 invalidate 分析与规则缓存。
5. heatmap 矩阵选中 cell；展开 block 拉 sessions，展开 session 拉 events。
6. 筛选变化清空展开与 bucket。
7. 页面聚合 loading/fetching/error 传给 Header。
8. 主区自上而下组合各移动分析子组件。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/MobileRecordsPage.tsx",
      "label": "MobileRecordsPage",
      "path": "src/client-web/src/pages/MobileRecordsPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/MobileRecordsPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/components/mobile/MobileChartsGrid.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/components/mobile/MobileAppCatalogManager.tsx", "type": "depends_on" }
  ]
}
```
