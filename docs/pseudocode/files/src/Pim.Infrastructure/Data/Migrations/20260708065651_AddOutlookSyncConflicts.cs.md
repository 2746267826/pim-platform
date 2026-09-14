# src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：创建 Outlook/Graph 同步冲突表 `sync_conflicts`，保存 PIM 与外部快照及解决确认引用。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移流水线；Calendar Outlook 同步冲突处理服务

## 函数级结构化伪代码

### AddOutlookSyncConflicts
#### `void Up(MigrationBuilder migrationBuilder)`
- 输入：迁移构建器
- 输出：无
- 副作用：建表与四条索引
- 步骤：
  1. CreateTable `sync_conflicts`：user_id、provider 默认 outlook、object_type 默认 event、object_id、可选 graph_event_id、conflict_kind、status 默认 open、两侧 jsonb 快照默认 `{}`、resolved_confirmation_id、created_at 默认 now、updated_at
  2. 索引：graph_event_id；`(object_type, object_id)`；resolved_confirmation_id；`(user_id, provider, status)`
- 分支与异常：SQL 失败中止
- 调用：`CreateTable` / `CreateIndex`

#### `void Down(MigrationBuilder migrationBuilder)`
- 输入：迁移构建器
- 输出：无
- 副作用：Drop `sync_conflicts`
- 步骤：1. DropTable
- 分支与异常：依赖残留则失败
- 调用：`DropTable`

## 近逐行中文伪代码

1. 分部类 `AddOutlookSyncConflicts` 继承 `Migration`
2. `Up`：建 `sync_conflicts` 全列与默认值（provider/object_type/status/jsonb）
3. 主键 `PK_sync_conflicts`
4. 四条索引覆盖 Graph 事件、对象定位、确认引用、用户+提供商+状态查询
5. `Down`：DropTable `sync_conflicts`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs",
      "label": "AddOutlookSyncConflicts",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065651_AddOutlookSyncConflicts.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs", "type": "depends_on" }
  ]
}
```
