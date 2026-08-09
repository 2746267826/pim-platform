# src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移：创建提醒主表 `reminders` 与投递表 `reminder_deliveries`（级联删除）及查询索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations.MigrationBuilder`
- 被谁使用：EF 迁移流水线；提醒服务读写

## 函数级结构化伪代码

### AddReminderService
#### void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：建两表与索引、外键
- 步骤：
  1. 建 `reminders`：user_id、关联对象 type/id、title/body/trigger_reason、risk_level、channels_json、dnd 起止、scheduled_at、status 默认 Open、created/updated/deleted
  2. 建 `reminder_deliveries`：reminder_id FK Cascade、user_id、channel、status 默认 Created、action、payload_json、created/responded
  3. 索引：deliveries.reminder_id；deliveries(user_id, created_at)；reminders(related type+id)；reminders(user_id, status, scheduled_at)
- 分支与异常：DDL 失败抛出
- 调用：`CreateTable`、`CreateIndex`、FK Cascade

#### void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：先删 deliveries 再删 reminders
- 步骤：DropTable deliveries；DropTable reminders
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；nullable disable
2. partial 类 `AddReminderService` 继承 Migration
3. `Up`：建 reminders（软删除 deleted_at、channels_json 默认 []、status Open）
4. `Up`：建 reminder_deliveries，FK 指向 reminders.id 级联删除
5. `Up`：建 reminder_id、user+时间、关联对象、用户状态计划时间索引
6. `Down`：先 deliveries 后 reminders 删表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs",
      "label": "AddReminderService",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs", "to": "reminders", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070054_AddReminderService.cs", "to": "reminder_deliveries", "type": "depends_on" }
  ]
}
```
