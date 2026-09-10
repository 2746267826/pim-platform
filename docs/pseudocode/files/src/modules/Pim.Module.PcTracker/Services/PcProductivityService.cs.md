# src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：基于 `ActivityClassificationEntity` 聚合生产力看板、日期范围日趋势、日时间线 V2；用分类名关键词映射 productive/distracting/neutral。
- 主要依赖：`PimDbContext`、`ActivityClassificationEntity`、PcTracker DTOs
- 被谁使用：PcTracker 生产力/时间线相关端点

## 函数级结构化伪代码

### PcProductivityService
#### 构造(PimDbContext db)
- 输入：db
- 输出：实例
- 副作用：无
- 步骤：捕获 `_db`
- 分支与异常：无
- 调用：无

#### Task\<ProductivityDashboardDto\> GetDashboardAsync(date?, ct)
- 输入：可选目标日（默认 UtcNow.Date）
- 输出：今日得分/三类小时/目标 5h/是否达标/周趋势
- 副作用：只读查询
- 步骤：
  1. targetDate 规范化为 UTC Date；weekStart = 周日对齐（DayOfWeek）
  2. 拉取该自然周 StartedAt 落在 [weekStart, weekStart+7) 的分类
  3. 今日子集：按 CategoryName→GetProductivity 累加分钟 productive/distracting/neutral
  4. 循环 7 天建 DailyProductivityDto（分钟四舍五入 1 位、比例 4 位）
  5. TodayScore = productive/total*100；小时 = 分钟/60；TargetHours=5；GoalMet 比较
- 分支与异常：无
- 调用：GetProductivity、EF

#### Task\<List\<DailyProductivityDto\>\> GetRangeAsync(start, end, ct)
- 输入：起止 DateTime
- 输出：按日排序的生产力列表
- 副作用：只读
- 步骤：
  1. start/end 标为 UTC Kind
  2. 查询 StartedAt ∈ [utcStart, utcEnd+1day)
  3. GroupBy StartedAt.Date → 三类分钟与比例 → OrderBy Date
- 分支与异常：无
- 调用：GetProductivity

#### Task\<List\<TimelineV2Item\>\> GetTimelineV2Async(date, ct)
- 输入：日期
- 输出：当日时间线条目
- 副作用：只读
- 步骤：
  1. dayStart/dayEnd UTC 日界
  2. 分类按 StartedAt 排序
  3. 映射：AppName=RecordKey；Category 默认「其他」/色；Productivity=GetProductivity；Duration 分钟
- 分支与异常：无
- 调用：GetProductivity

#### private string GetProductivity(categoryName?)
- 输入：分类名
- 输出：`"productive"` | `"distracting"` | `"neutral"`
- 副作用：无
- 步骤：
  1. 空或「其他」→ neutral
  2. 含 工作/编程/文档/会议/设计/运维/学习/技术/外语/邮件 → productive
  3. 含 游戏/视频/娱乐/社交 → distracting
  4. 否则 neutral
- 分支与异常：无
- 调用：string.Contains

## 近逐行中文伪代码

1. 注入 db
2. GetDashboard：周数据拉全 → 今日三类时长 → 7 日趋势 → 5 小时目标与得分
3. GetRange：区间内按日聚合三类分钟与比例
4. GetTimelineV2：当日分类映射时间线项
5. GetProductivity：中文关键词启发式 productive/distracting/neutral

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs",
      "label": "PcProductivityService",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcProductivityService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
