# src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `AddReminderService` 目标模型：提醒主表 `reminders` 与投递表 `reminder_deliveries`（含 FK Cascade）；并快照当时全库（含 Planning 对象、AuditVersion、SyncConflict 等）。
- 主要依赖：EF Core / Npgsql / `PimDbContext` / Calendar Reminder 实体类型
- 被谁使用：EF 迁移工具链；与同名 `.cs` 配对

## 函数级结构化伪代码

### AddReminderService（partial）
#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：完整目标模型（约 63 表 / 94 实体配置块）
- 步骤：
  1. 注解 + identity
  2. 先配置 `AuditVersionEntity` 等前序实体，再铺开 AI/运维/Calendar Planning/Mobile/Files/PcTracker/QuickNotes
  3. **提醒焦点**：
     - `ReminderEntity`→`reminders`：user_id、related_object_type/id、title/body、trigger_reason、risk_level、channels_json、dnd_start/end、scheduled_at、status(默认 Open)、created/updated/deleted_at；索引 (related_object_type, related_object_id)、(user_id, status, scheduled_at)
     - `ReminderDeliveryEntity`→`reminder_deliveries`：reminder_id、user_id、channel、status(Created)、action、payload_json、created_at、responded_at；索引 reminder_id、(user_id, created_at)
     - 关系：Delivery HasOne Reminder WithMany，FK Cascade
  4. 其余 Navigation 收尾
- 分支与异常：无
- 调用：Fluent API

## 近逐行中文伪代码

1. auto-generated；Migration Id `20260708070054_AddReminderService`
2. `BuildTargetModel` 写全库快照（含 audit_versions、domain_projects、habits、task_books 等）
3. 配置 `ReminderDelivery` 列与索引，表 `reminder_deliveries`
4. 配置 `Reminder` 列与复合索引，表 `reminders`
5. 后部 `HasOne(Reminder).WithMany` + Cascade
6. 其它实体关系；pragma restore
7. （业务增量见同名非 Designer：CreateTable reminders/reminder_deliveries + FK + 索引）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs",
      "label": "AddReminderService.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.Designer.cs", "to": "src/modules/Pim.Module.Calendar", "type": "depends_on" }
  ]
}
```
