# tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Today 各 SectionProvider 的 BuildAsync 行为：日程窗口、任务逾期、PC 质量/活动、运维健康、分类建议。
- 主要依赖：`Calendar*TodaySectionProvider`、`Pc*TodaySectionProvider`、`OperationsHealthTodaySectionProvider`、`ClassificationSuggestionsTodaySectionProvider`、`CalendarService`、`PcTrackerService`
- 被谁使用：xUnit

## 函数级结构化伪代码

### TodaySectionProviderTests
#### CalendarScheduleProvider_ReturnsEventsAndScheduledTasks()
- 输入：查询日 2026-05-25
- 输出：无
- 副作用：写日历/事件/任务
- 步骤：Build → Id=`calendar.schedule`、Normal；Data 含事件与已排期任务
- 分支与异常：无
- 调用：`CalendarScheduleTodaySectionProvider.BuildAsync`

#### CalendarScheduleProvider_UsesLocalDateWindow()
- 输入：本地 0:30 与次日 0:30 事件
- 输出：无
- 副作用：写事件
- 步骤：仅当日本地窗口内事件入选
- 分支与异常：无
- 调用：同上

#### CalendarScheduleProvider_ExcludesCompletedScheduledTasks()
- 输入：COMPLETED 已排期任务
- 输出：无
- 副作用：UpdateTask
- 步骤：Status=Empty、ScheduledTasks 空
- 分支与异常：无
- 调用：同上

#### CalendarTasksProvider_ReturnsWarning_WhenOverdueTasksExist()
- 输入：过期任务
- 输出：无
- 副作用：写任务
- 步骤：Warning；OverdueTasks 一条；IncompleteCount=1
- 分支与异常：无
- 调用：`CalendarTasksTodaySectionProvider.BuildAsync`

#### PcQualityProvider_UsesQualityService()
- 输入：空 PC 库
- 输出：无
- 副作用：无
- 步骤：Id=`pc.quality`；IssueCount≥1；链接 `/pc-tracker`
- 分支与异常：无
- 调用：`PcQualityTodaySectionProvider` + `PcTrackerQualityService`

#### PcActivityProvider_ReturnsEmpty_WhenNoPcDataExists()
- 输入：无 AW 数据
- 输出：无
- 副作用：无
- 步骤：Empty + PcActivityTodayData
- 分支与异常：无
- 调用：`PcActivityTodaySectionProvider`

#### OperationsHealthProvider_ReturnsHealthSummary()
- 输入：FakeSystemStatusService Warning
- 输出：无
- 副作用：无
- 步骤：Warning；Summary 状态；链接 `/status`
- 分支与异常：无
- 调用：`OperationsHealthTodaySectionProvider`

#### ClassificationSuggestionsProvider_ReturnsWarning_WhenPendingSuggestionsExist()
- 输入：pending suggestion
- 输出：无
- 副作用：写 suggestion
- 步骤：Warning；PendingCount=1
- 分支与异常：无
- 调用：`ClassificationSuggestionsTodaySectionProvider` + `ActivitySuggestionService`

#### 辅助 CreateDb / CreateCalendarService / CreatePcTrackerService / 替身
- 输入：是否注册 PC 模块
- 输出：DB 与服务
- 副作用：RegisterModuleAssembly
- 步骤：FixedCurrentUserService；FakeSystemStatusService
- 分支与异常：无
- 调用：Calendar/PcTracker 服务构造

## 近逐行中文伪代码

1. 日程：事件+任务、本地日窗、完成任务排除
2. 任务：逾期 Warning
3. PC 质量/空活动
4. 运维健康 Fake
5. 待处理分类建议 Warning
6. Query 固定 2026-05-25；LocalOffsetTime 用本机偏移

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs",
      "label": "TodaySectionProviderTests",
      "path": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs", "to": "src/Pim.Api/Today/TodaySectionProviders.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "type": "tests" }
  ]
}
```
