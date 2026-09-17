# src/client-web/src/components/pc-tracker/CategoryTimeline.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：将 TimelineItem 渲染为按小时拆分的甘特时间线，含分类图例、生产性占比与悬停 tooltip。
- 主要依赖：`TimelineItem`（types）
- 被谁使用：PC Tracker 时间线区域

## 函数级结构化伪代码

### getIcon(category)
- 按 CATEGORY_ICONS 子串匹配返回 emoji，默认沙漏

### CategoryTimeline
- 输入：timeline 数组
- useMemo：解析起止分钟、累加分类时长、按小时切段 leftPct/widthPct、算 stats/legend/hourRange
- useMemo：hours 列表；segmentsByHour 再按小时重叠重算百分比
- 渲染：顶栏图例+生产性%；逐小时行绝对定位色条；底栏图例；fixed tooltip

## 近逐行中文伪代码

1. 定义 GanttSegment/ActiveTooltip 与分类图标表。
2. 过滤有起止的事件，拆小时段并统计。
3. 生产性分类关键字累加 prodMin。
4. 空数据提示；有数据画左轴时刻与色条。
5. 悬停设置 tooltip 坐标与文案，离开清空。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/CategoryTimeline.tsx",
      "label": "CategoryTimeline",
      "path": "src/client-web/src/components/pc-tracker/CategoryTimeline.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/CategoryTimeline.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-tracker/CategoryTimeline.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
