# src/Pim.Core/Planning/PlanningDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义规划域（项目、任务本、清单项、习惯例程/发生、可用窗口、AI 规划占位）的只读契约 DTO。
- 主要依赖：`Pim.Core.Planning.HabitCadence`（`PlanningEnums.cs`）
- 被谁使用：
  - `src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs`（实体映射与 CRUD 返回）

## 函数级结构化伪代码

### DomainProjectDto
#### DomainProjectDto(Id, Name, Description, Status)
- 输入：项目 Id、名称、可选描述、状态字符串
- 输出：领域项目快照
- 副作用：无
- 步骤：
  1. 以记录构造承载规划项目对外字段
- 分支与异常：无
- 调用：无

### TaskBookDto
#### TaskBookDto(Id, DomainProjectId, Name, Kind, Status)
- 输入：任务本 Id、可选所属项目 Id、名称、种类、状态
- 输出：任务本快照
- 副作用：无
- 步骤：
  1. 构造可挂靠领域项目的任务本 DTO；`DomainProjectId` 可空表示独立任务本
- 分支与异常：无
- 调用：无

### TaskChecklistItemDto
#### TaskChecklistItemDto(Id, TaskId, Title, IsDone, SortOrder)
- 输入：清单项 Id、所属任务 Id、标题、是否完成、排序
- 输出：任务清单项快照
- 副作用：无
- 步骤：
  1. 构造任务下可勾选清单项的传输结构
- 分支与异常：无
- 调用：无

### HabitRoutineDto
#### HabitRoutineDto(Id, Title, Cadence, Source, Status)
- 输入：习惯例程 Id、标题、节奏枚举、来源、状态
- 输出：习惯例程快照
- 副作用：无
- 步骤：
  1. 构造习惯模板 DTO；`Cadence` 使用 `HabitCadence` 枚举表达周期
- 分支与异常：无
- 调用：无

### HabitOccurrenceDto
#### HabitOccurrenceDto(Id, HabitRoutineId, StartsAt, EndsAt, Status)
- 输入：发生实例 Id、所属例程 Id、起止时间、状态
- 输出：习惯发生实例快照
- 副作用：无
- 步骤：
  1. 构造由例程展开或创建的具体时间窗发生记录
- 分支与异常：无
- 调用：无

### AvailabilityWindowDto
#### AvailabilityWindowDto(Id, StartsAt, EndsAt, Kind, Source)
- 输入：窗口 Id、起止时间、种类、来源
- 输出：可用时间窗快照
- 副作用：无
- 步骤：
  1. 构造用户可调度时间段的传输结构
- 分支与异常：无
- 调用：无

### AiPlanningPlaceholderDto
#### AiPlanningPlaceholderDto(Id, Title, StartsAt, EndsAt, Reason, ConfirmationId)
- 输入：占位 Id、标题、起止时间、原因、可选确认 Id
- 输出：AI 规划占位快照
- 副作用：无
- 步骤：
  1. 构造 AI 建议的时间块占位；可挂 `ConfirmationId` 走确认流
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Core.Planning`
2. 定义 `DomainProjectDto`：Id、Name、可空 Description、Status
3. 定义 `TaskBookDto`：Id、可空 DomainProjectId、Name、Kind、Status
4. 定义 `TaskChecklistItemDto`：Id、TaskId、Title、IsDone、SortOrder
5. 定义 `HabitRoutineDto`：Id、Title、HabitCadence 类型 Cadence、Source、Status
6. 定义 `HabitOccurrenceDto`：Id、HabitRoutineId、StartsAt、EndsAt、Status
7. 定义 `AvailabilityWindowDto`：Id、StartsAt、EndsAt、Kind、Source
8. 定义 `AiPlanningPlaceholderDto`：Id、Title、StartsAt、EndsAt、Reason、可空 ConfirmationId
9. 全部为密封 `record`，无业务逻辑；映射与持久化由 Calendar 模块 `PlanningModelService` 完成

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Planning/PlanningDtos.cs",
      "label": "PlanningDtos",
      "path": "src/Pim.Core/Planning/PlanningDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Planning/PlanningDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Planning/PlanningDtos.cs", "to": "src/Pim.Core/Planning/PlanningEnums.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/Pim.Core/Planning/PlanningDtos.cs", "type": "depends_on" }
  ]
}
```
