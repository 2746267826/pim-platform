# src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer 快照）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260708094627_AddEndpointStatusPersistence` 的目标模型快照；新增 Endpoint 状态持久化实体，并包含当时全库模型（提醒/报告/同步冲突等）。
- 主要依赖：EF Core、Npgsql、`PimDbContext`、`EndpointStatusEntity`/`EndpointNotificationActionEntity`
- 被谁使用：EF 迁移工具；与 `AddEndpointStatusPersistence.cs` 配对

## 函数级结构化伪代码

### AddEndpointStatusPersistence（partial）
#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：配置含 Endpoint 持久化表后的完整目标模型
- 步骤：
  1. 模型注解与 Npgsql 默认
  2. 既有 `AuditVersionEntity` 与平台/AI 实体
  3. **本迁移焦点**：
     - `EndpointNotificationActionEntity` → `endpoint_notification_actions`（索引：confirmation_id、created_at、device_id、user_id）
     - `EndpointStatusEntity` → `endpoint_statuses`（索引：last_heartbeat_at、`(user_id, device_id)`）
  4. Calendar 进一步扩展：Reminder/ReminderDelivery、ReportArtifact/ReportSuggestion、SyncConflict 等 + 规划/习惯/任务书全套
  5. Files、Mobile 全套、PcTracker、QuickNotes
  6. 关系与 Navigation：User、Calendar 树、Files 子实体、Keystats、Note 等
- 分支与异常：无运行时分支
- 调用：Fluent `modelBuilder.Entity` API

## 近逐行中文伪代码

1. auto-generated 头与引用；nullable disable
2. `[DbContext]` + `[Migration("20260708094627_AddEndpointStatusPersistence")]`
3. partial `AddEndpointStatusPersistence`；`BuildTargetModel`
4. 注解与 IdentityByDefault
5. 配置 AuditVersion、AI、平台操作/用户实体
6. 配置 `EndpointNotificationActionEntity`、`EndpointStatusEntity` 及表/索引
7. 配置 Calendar（含提醒、报告、冲突、规划对象）
8. 配置 Files / Mobile / PcTracker / QuickNotes 全部实体与表
9. 配置外键与导航集合
10. 恢复 pragma；结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs",
      "label": "AddEndpointStatusPersistence.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointNotificationActionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.Designer.cs", "to": "Microsoft.EntityFrameworkCore", "type": "depends_on" }
  ]
}
```
