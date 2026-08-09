# src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PcTracker Phase2 API 传输模型——分类树节点/保存/重排、日生产力与目标、生产力看板、时间线 V2 条目。
- 主要依赖：无外部业务类型（纯 POCO）
- 被谁使用：`PcCategoryService`、`PcProductivityService` 及对应模块端点

## 函数级结构化伪代码

### CategoryTreeNode
#### 属性
- 输入/输出：树节点 Id/ParentId/Name/Color/Icon/Productivity/SortOrder/IsBuiltin/Children
- 副作用：默认 Color `#64748b`、Productivity `neutral`、Children 空列表
- 步骤：递归树载体
- 分支与异常：无
- 调用：被 `PcCategoryService.BuildTree/MapToNode` 填充

### CategorySaveRequest / ReorderCategoriesRequest / ReorderItem
#### 属性
- 输入：客户端保存/重排负载
- 输出：服务层写入分类实体
- 副作用：无
- 步骤：
  1. Save：可选 Id；ParentId/Name/Color/Icon/Productivity/SortOrder。
  2. Reorder：Items 列表，每项 Id/ParentId/SortOrder。
- 分支与异常：无
- 调用：`PcCategoryService.SaveAsync` 等

### DailyProductivityDto / ProductivityGoalDto / ProductivityDashboardDto
#### 属性
- 输入：服务聚合结果
- 输出：看板 API
- 副作用：Goal 默认 DailyProductiveHours=5.0；Dashboard 含 WeeklyTrend 列表
- 步骤：
  1. Daily：Date 与 productive/neutral/distracting/total 分钟及 ProductiveRatio。
  2. Dashboard：TodayScore、三类小时、TargetHours、GoalMet、周趋势。
- 分支与异常：无
- 调用：`PcProductivityService`

### TimelineV2Item
#### 属性
- 输入：分类后的活动时段
- 输出：Start/End、AppName、WindowTitle、CategoryName/Color、Productivity、Confidence、DurationMinutes
- 副作用：默认 Productivity neutral
- 步骤：时间线条目投影
- 分支与异常：无
- 调用：`PcProductivityService.GetTimelineV2Async`

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.DTOs`。
2. CategoryTreeNode 与 CategorySaveRequest 分类树读写形状。
3. ReorderCategoriesRequest + ReorderItem 批量改父级与排序。
4. DailyProductivityDto / ProductivityGoalDto / ProductivityDashboardDto 生产力指标。
5. TimelineV2Item 应用活动时间线条目字段。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs",
      "label": "Phase2Dtos",
      "path": "src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs.md",
      "layer": "module.pctracker",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker", "to": "src/modules/Pim.Module.PcTracker/DTOs/Phase2Dtos.cs", "type": "depends_on" }
  ]
}
```
