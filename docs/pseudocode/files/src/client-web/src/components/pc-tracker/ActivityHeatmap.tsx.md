# src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：GitHub 风格活动热力图，按 hour/day/month/year 维度渲染强度格子，支持日期点击与生产性筛选 pill。
- 主要依赖：`HeatmapGridResponse`（types）
- 被谁使用：PC Tracker 活动概览

## 函数级结构化伪代码

### normalizeCell / parseDate / gitHubColor
- 安全解析 cell；解析日期；按 value/max 五档绿色

### ActivityHeatmap
- 输入：data、isLoading、onDateClick?
- 状态：filterProductivity（当前未真正过滤 cells，仅 UI 切换）
- 步骤：
  1. loading / 空数据空态
  2. flatMap grid 得 SafeCell，排序
  3. 按 dimension 分发 renderHour/Day/Month/YearGrid

### renderHourGrid / renderDayGrid / renderMonthGrid / renderYearGrid
- 小时：24 格；日：自适应方格；月：按年月分组周历；年：密网格

## 近逐行中文伪代码

1. 定义绿色谱、生产性色/标签、星期标签。
2. 规范化未知 cell 结构。
3. loading 或无 cell 返回占位。
4. 顶栏维度标签 + 生产性 pill + 少/多图例。
5. 按 dimension 渲染对应网格；可点格子回调 ISO 日期。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx",
      "label": "ActivityHeatmap",
      "path": "src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
