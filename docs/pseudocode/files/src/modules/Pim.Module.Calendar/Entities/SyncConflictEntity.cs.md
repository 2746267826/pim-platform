# src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：外部日历（默认 Outlook）与 PIM 对象双向同步冲突记录，映射表 `sync_conflicts`，保存双方 jsonb 快照与解决确认引用。
- 主要依赖：DataAnnotations / Schema（无软删除接口）
- 被谁使用：Outlook 同步/冲突解决服务、`PimDbContext`、AddOutlookSyncConflicts 等迁移

## 函数级结构化伪代码

### SyncConflictEntity
#### 属性（无方法）
- 输入/输出：持久化字段
- 副作用：无
- 步骤（字段语义）：
  1. `Id`：主键 Guid，默认 NewGuid。
  2. `UserId`：冲突所属用户。
  3. `Provider`：默认 `"outlook"`，MaxLength 40。
  4. `ObjectType`：默认 `"event"`，MaxLength 80。
  5. `ObjectId`：本地对象 Id。
  6. `GraphEventId`：可选 Graph 事件 Id，MaxLength 255。
  7. `ConflictKind`：默认 `"both_sides_changed"`，MaxLength 120。
  8. `Status`：默认 `"open"`，MaxLength 40。
  9. `PimSnapshotJson` / `ExternalSnapshotJson`：jsonb，默认 `"{}"`。
  10. `ResolvedConfirmationId`：解决时关联确认 Id。
  11. `CreatedAt` / `UpdatedAt`：默认 UtcNow。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema。
2. 表 `sync_conflicts`。
3. 列：id、user_id、provider、object_type、object_id、graph_event_id、conflict_kind、status、两侧 snapshot jsonb、resolved_confirmation_id、时间戳。
4. 默认：outlook / event / both_sides_changed / open / 空 JSON 对象。
5. 无导航属性；无软删除。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs",
      "label": "SyncConflictEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/SyncConflictEntity.cs", "to": "sync_conflicts", "type": "depends_on" }
  ]
}
```
