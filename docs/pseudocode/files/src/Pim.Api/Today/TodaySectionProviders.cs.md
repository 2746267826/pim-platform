# src/Pim.Api/Today/TodaySectionProviders.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：实现「今日」面板各区块的 `ITodaySectionProvider`：日程、任务、习惯、可用性、AI 占位、待确认、Outlook 同步占位、提醒队列、报告、端点状态、PC 活动/质量、运维健康、分类建议；并提供区块数据 record 与结果构建辅助。
- 主要依赖：
  - `CalendarService`、`PlanningModelService`、`ReminderService`、`ReportService`
  - `IOperationConfirmationService`、`EndpointStatusService`、`ISystemStatusService`
  - `PcTrackerService`、`PcTrackerQualityService`、`ActivitySuggestionService`
  - `Pim.Core.Today`、`Pim.Core.Operations`、Calendar/PcTracker DTOs
- 被谁使用：DI 注册后由 Today 聚合端点/服务按 SectionId 拉取

## 函数级结构化伪代码

### 数据 record（无行为）
- `CalendarScheduleTodayData`：当日 Events + ScheduledTasks
- `CalendarTasksTodayData`：IncompleteCount + DueToday/Overdue/Unscheduled 任务列表
- `TodayLayerCountData`：Count + CalendarLayerItem 列表
- `PendingConfirmationTodayData`：PendingCount + 确认 DTO 列表
- `TodayPlaceholderData`：Kind + Count
- `EndpointStatusTodayData`：EndpointCount、OnlineOnlyBlockedCount、Items
- `ReportsAvailableTodayData`：AvailableCount + ReportArtifactDto
- `PcActivityTodayData`：PcSummaryResponse
- `PcQualityTodayData`：PcQualityResponse + IssueCount
- `OperationsHealthTodayData`：SystemStatusDetailDto；属性 Summary 来自 Detail.Summary
- `ClassificationSuggestionsTodayData`：PendingCount + 建议预览列表

### CalendarScheduleTodaySectionProvider
#### 属性 `SectionId` / `Kind` → `"calendar.schedule"`
#### `Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)`
- 输入：query.Date、ct
- 输出：TodaySectionDto
- 副作用：读日历服务
- 步骤：
  1. 本地午夜 start → end=start+1 天。
  2. `GetEventsAsync(start,end)`；`GetTasksAsync(null)`。
  3. 未完成且 DtStart 本地日 = query.Date 的任务，按 DtStart 排序。
  4. 事件与排期任务皆空 → Empty，否则 Normal。
  5. 数据 `CalendarScheduleTodayData`，链接 `/calendar`。
- 分支与异常：服务异常向上
- 调用：`calendarService.GetEventsAsync`/`GetTasksAsync`、`LocalMidnight`

#### `LocalMidnight(DateOnly date)`
- 输入：date
- 输出：带本地偏移的 DateTimeOffset 午夜
- 步骤：ToDateTime(MinValue) + Local.GetUtcOffset

### CalendarTasksTodaySectionProvider
#### SectionId/Kind → `"calendar.tasks"`
#### `BuildAsync`
- 步骤：
  1. 全部任务，过滤非 COMPLETED。
  2. Due 日 = 今日 → dueToday；Due 日 < 今日 → overdue；DtStart null → unscheduled。
  3. 有逾期或今日到期 → Warning；无未完成 → Empty；否则 Normal。
  4. 数据含 IncompleteCount 与三列表；链接 `/tasks`、`/calendar`。
- 调用：`GetTasksAsync`

### CalendarHabitsTodaySectionProvider
#### SectionId/Kind → `"calendar.habits"`
#### `BuildAsync`
- 步骤：`GetCalendarLayersAsync(LayerQuery(query,"habits"))`；无项 Empty 否则 Normal；`TodayLayerCountData`；链接 `/habits`、`/calendar`。
#### `static LayerQuery(TodayQuery, string layer)`
- 步骤：当日本地 00:00 带偏移 → from；返回 `CalendarLayerQuery(from, from+1d, [layer])`。

### CalendarAvailabilityTodaySectionProvider
#### SectionId/Kind → `"calendar.availability"`
#### `BuildAsync`
- 步骤：复用 LayerQuery(..., "availability")；Empty/Normal；链接 `/calendar`。

### CalendarAiPlaceholdersTodaySectionProvider
#### SectionId/Kind → `"calendar.ai_placeholders"`
#### `BuildAsync`
- 步骤：LayerQuery(..., "ai-placeholders")；有项 Warning 无项 Empty；链接 `/calendar`、`/confirmations`。

### OperationsConfirmationsTodaySectionProvider
#### SectionId/Kind → `"operations.confirmations"`
#### `BuildAsync`
- 步骤：`ListPendingForUserAsync(currentUser.UserId)`；0→Empty 否则 Warning；`PendingConfirmationTodayData`；链接 `/confirmations`。

### OutlookSyncTodaySectionProvider
#### SectionId/Kind → `"sync.outlook"`
#### `BuildAsync`
- 步骤：固定 Empty + `TodayPlaceholderData(Kind,0)`；链接 `/sync`（占位实现）。

### RemindersQueueTodaySectionProvider
#### SectionId/Kind → `"reminders.queue"`
#### `BuildAsync`
- 步骤：`ListAsync`；Status 为 Open/Snoozed 且 ScheduledAt ≤ UtcNow+1d；0→Empty 否则 Warning；Placeholder 计数；链接 `/reminders`。

### ReportsAvailableTodaySectionProvider
#### SectionId/Kind → `"reports.available"`
#### `BuildAsync`
- 步骤：`ListAsync`；Status=Active 且 GeneratedAt 本地日=query.Date，Take(5)；Empty/Normal；`ReportsAvailableTodayData`；链接 `/reports`。

### EndpointsStatusTodaySectionProvider
#### SectionId/Kind → `"endpoints.status"`
#### `BuildAsync`
- 步骤：`endpointStatus.ListAsync`；累加 OnlineOnlyBlockedCount；任一 UploadStatus 为 Warning/Critical 或 blocked>0 → hasWarning；无端点 Empty / Warning / Normal；`EndpointStatusTodayData`；链接 `/endpoint-shell`。

### PcActivityTodaySectionProvider
#### SectionId/Kind → `"pc.activity"`
#### `BuildAsync`
- 步骤：`GetSummaryAsync(PcBusinessDate 午夜)`；热图/排行/时间线/会话/Keystats 任一有数据 → Normal 否则 Empty；`PcActivityTodayData`；链接 `/pc-tracker`。

### PcQualityTodaySectionProvider
#### SectionId/Kind → `"pc.quality"`
#### `BuildAsync`
- 步骤：`GetQualityAsync(PcBusinessDate 午夜, null, null)`；`MapStatus(OverallStatus)`；`PcQualityTodayData`；链接 `/pc-tracker`。

### OperationsHealthTodaySectionProvider
#### SectionId/Kind → `"operations.health"`
#### `BuildAsync`
- 步骤：`GetDetailAsync`；MapStatus(Summary.Status)；`OperationsHealthTodayData`；链接 `/status`。

### ClassificationSuggestionsTodaySectionProvider
#### SectionId/Kind → `"pc.classification_suggestions"`
#### `BuildAsync`
- 步骤：`GetSuggestionsAsync`；Take(5) 预览；有建议 Warning 否则 Empty；链接 `/pc-tracker`。

### TodaySectionProviderResult（internal static）
#### `Build(sectionId, kind, status, data, links)`
- 输出：`TodaySectionDto(sectionId, kind, status, UtcNow, data, links, null)`
#### `Details(params string[] hrefs)`
- 输出：每项 `TodayLinkDto(Details, href)` 列表
#### `MapStatus(PimHealthStatus)`
- Healthy→Normal；Warning→Warning；Critical→Critical；其它→Unavailable

## 近逐行中文伪代码

1. 引入 Operations、Today、Auth、Endpoints、Calendar/PcTracker 服务与 DTO。
2. 定义多个 sealed record 作为各区块 data 载荷。
3. **schedule**：本地日窗口取事件 + 当日未完成排期任务 → Empty/Normal → `/calendar`。
4. **tasks**：未完成拆分今日到期/逾期/未排期 → Warning/Empty/Normal → `/tasks`+`/calendar`。
5. **habits/availability/ai_placeholders**：Planning 日历层查询对应 layer → 计数与 Items；AI 占位有数据为 Warning。
6. **confirmations**：当前用户待确认列表 → 有则 Warning。
7. **sync.outlook**：空占位。
8. **reminders**：Open/Snoozed 且 24h 内 → 计数 Warning。
9. **reports**：当日 Active 报告最多 5 条。
10. **endpoints**：列表 + 阻断计数 + 上传状态 → Warning/Empty/Normal。
11. **pc.activity/quality**：业务日摘要与质量；健康态映射。
12. **operations.health**：系统状态详情与 Summary 状态映射。
13. **classification_suggestions**：建议全量计数 + 前 5 预览。
14. 辅助类统一构造 SectionDto、Details 链接、PimHealthStatus 映射。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Today/TodaySectionProviders.cs",
      "label": "TodaySectionProviders",
      "path": "src/Pim.Api/Today/TodaySectionProviders.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Today/TodaySectionProviders.cs.md",
      "layer": "api",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReportService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "type": "calls" }
  ]
}
```
