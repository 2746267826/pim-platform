# src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历模块回收站：列出已软删的日历本/任务本/日程/任务；预览恢复冲突；执行恢复（含按操作批次连带恢复与「恢复为副本」）。
- 主要依赖：
  - `PimDbContext`、`ICurrentUserService`、`CalendarAuditWriter`
  - 实体 `CalendarEntity`/`EventEntity`/`TaskEntity`
  - DTO：`CalendarRecycleBinItem`、`CalendarRestorePreviewResponse`、`CalendarRestoreRequest`、`CalendarOperationResult` 等
  - `DomainException`、`PagedResult`
- 被谁使用：日历模块回收站 API 端点

## 函数级结构化伪代码

### CalendarRecycleBinService
#### 构造与 `UserId`
- 输入：db、currentUser、audit
- 输出：服务实例；`UserId` 属性
- 副作用：无
- 步骤：
  1. 注入依赖赋值。
  2. `UserId`：`_currentUser.UserId` 空则 `DomainException(1002, "未登录")`。
- 分支与异常：未登录 1002
- 调用：无

#### `ListAsync(type, search, deletedFrom, deletedTo, page, pageSize, ct)`
- 输入：类型过滤、搜索、删除时间窗、分页
- 输出：`PagedResult<CalendarRecycleBinItem>`
- 副作用：读库（IgnoreQueryFilters）
- 步骤：
  1. 规范化 list 类型（空→all）；page≥1；pageSize clamp 1–100。
  2. 若类型 all/calendar/task-book：查用户已删 Calendar；按 Kind 映射 item 类型（task→task-book），Source=manual。
  3. 若 all/event：查已删 Event（含 Calendar，用户匹配）映射为 event 项（含时间与 Source）。
  4. 若 all/task：查已删 Task 映射为 task 项。
  5. 可选 search：Title 或 BookName 忽略大小写包含。
  6. 可选 deletedFrom/To 过滤 DeletedAt。
  7. 按 DeletedAt 降序分页；返回 PagedResult。
- 分支与异常：类型组合跳过对应查询
- 调用：EF Set/IgnoreQueryFilters/Include/Where

#### `PreviewRestoreAsync` / `BuildPreviewAsync`
- 输入：type、id
- 输出：`CalendarRestorePreviewResponse`
- 副作用：读库
- 步骤：
  1. `NormalizeType` 后按 event/task/calendar|task-book 分派。
  2. 事件/任务：加载已删实体 + 冲突列表 + sample；CanRestore = 无冲突。
  3. 日历本：加载本 + 样本（同删除操作批次最多 5）+ 恢复计数（子项数+1）；冲突空、CanRestore=true。
- 分支与异常：未知类型 02021
- 调用：Load*、Find*Conflicts、BuildBookRestoreSamples、CountBookRestore

#### `RestoreAsync(type, id, request, ct)`
- 输入：类型、id、`CalendarRestoreRequest`
- 输出：`CalendarOperationResult`
- 副作用：清软删字段、可能改 Uid；Save；审计
- 步骤：
  1. 规范化类型；若 RestoreAsCopy 且类型为 calendar/task-book → 02022。
  2. 预览：有冲突且非副本 → 02020。
  3. 生成 operationId；operation 名 restore 或 restore_copy。
  4. 分派 RestoreEvent/Task/CalendarAsync。
  5. SaveChanges；`CalendarAuditWriter.RecordSuccessAsync`（task-book 记为 task_book）。
  6. 返回 result。
- 分支与异常：02020/02021/02022 及加载类错误码
- 调用：BuildPreview、Restore*、Save、audit

#### `RestoreEventAsync` / `RestoreTaskAsync`
- 输入：id、是否副本、operation 元数据、now
- 输出：单条恢复的 OperationResult
- 副作用：改实体删除标记；副本时重置 Uid/SourceUid
- 步骤：
  1. 加载已删；`EnsureParentBookActive`（任务仅当有 Calendar）。
  2. 副本：新 Uid `@pim`；事件另清 SourceUid。
  3. `ClearDelete`；返回 AffectedCount=1 与 sample。
- 分支与异常：父本仍删 02023；不存在 02001/02004
- 调用：Load*、ClearDelete、Sample

#### `RestoreCalendarAsync`
- 输入：type、id、operation 元数据、now
- 输出：本 + 同批次子项恢复结果
- 副作用：清本与匹配 DeletedByOperationId 的子项删除标记
- 步骤：
  1. 加载已删本；记下 deletedOperationId；取 samples。
  2. ClearDelete 本。
  3. 若有 operationId：Kind=task 则恢复同操作删除的 Tasks；否则 Events。
  4. 返回 affectedIds 与 samples。
- 分支与异常：本不存在/类型不匹配 02002
- 调用：LoadDeletedCalendar、ClearDelete

#### 加载与冲突辅助
- `LoadDeletedEvent/Task/CalendarAsync`：IgnoreQueryFilters + 用户与 DeletedAt 条件；日历校验 Kind 与 type。
- `FindEventConflictsAsync`：同用户其他事件 Uid/SourceUid/标题+时间相同。
- `FindTaskConflictsAsync`：同用户其他任务 标题+Due+DtStart。
- `BuildBookRestoreSamplesAsync`/`CountBookRestoreAsync`：按 DeletedByOperationId 拉同批子项。
- `NormalizeListType`/`NormalizeType`：小写；calendar-book→calendar。
- `ClearDelete` 三态重载：清 DeletedAt/ByOperation*，UpdatedAt=now。
- `Sample`/`EventConflictReason`/`EnsureParentBookActive`/`Metadata`：映射与校验辅助。

## 近逐行中文伪代码

1. 注入 Db、当前用户、审计；UserId 未登录抛 1002。
2. List：规范化类型与分页；按需拉取已删 calendar/event/task 拼 RecycleBinItem；内存 search 与时间过滤；按删除时间分页。
3. Preview：NormalizeType → 事件/任务查冲突，日历本汇总同批子项样本与数量。
4. Restore：禁日历本副本；有冲突且非副本拒绝；分派恢复；Save + 成功审计。
5. 恢复事件/任务：父本须未删；副本换 Uid；ClearDelete。
6. 恢复日历本：先恢复本，再按原删除操作 ID 连带恢复 tasks 或 events。
7. 冲突原因：same-uid / same-source-uid / same-title-time。
8. 错误码：02001 日程、02002 日历、02004 任务、02020 冲突、02021 类型、02022 副本限制、02023 先恢复本。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs",
      "label": "CalendarRecycleBinService",
      "path": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" }
  ]
}
```
