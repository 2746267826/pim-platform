# src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs

## 元信息
- 语言：C#（auto-generated）
- 程序集或包：Pim.Infrastructure
- 职责：`PimDbContext` 的 EF Core 模型快照：固化当前实体→表/列/索引/关系映射，供迁移 diff。
- 主要依赖：`Microsoft.EntityFrameworkCore`、`Npgsql` 元数据、`PimDbContext` 与各模块 Entity 类型名字符串
- 被谁使用：EF 设计时工具（`dotnet ef migrations add`）；不由运行时业务直接调用

## 函数级结构化伪代码

### PimDbContextModelSnapshot
#### protected override void BuildModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：在内存中完整配置当前数据库模型
- 步骤：
  1. 注解：ProductVersion `8.0.11`、MaxIdentifierLength 63；启用 Npgsql IdentityByDefault
  2. 对每个实体类型 `modelBuilder.Entity("<clr-name>", b => { ... })`：
     - Property：列名 snake_case、类型、长度、默认值、jsonb 等
     - HasKey / HasIndex（含唯一与过滤索引）
     - ToTable 表名
  3. 实体覆盖域（表名摘要）：
     - 核心/运维：`users`、`refresh_tokens`、`login_attempts`、`audit_logs`、`audit_versions`、`operation_confirmations`、`daemon_heartbeats`、`endpoint_statuses`、`endpoint_notification_actions`
     - AI：`ai_provider_settings`、`ai_request_logs`
     - Calendar：`calendars`、`events`、`tasks`、`task_books`、`task_checklist_items`、`task_execution_segments`、`domain_projects`、`habit_*`、`reminders`/`reminder_deliveries`、`outlook_*`、`pending_confirmations`、`report_*`、`scheduling_feedback`、`sync_conflicts`、`availability_windows`、`ai_planning_placeholders`
     - Files：`file_providers`、`file_items`、`file_versions`、`file_chunks`、`file_index_jobs`、`file_ai_results`、`file_suggestions`
     - Mobile：`mobile_devices`、`mobile_app_catalog`/`_overrides`、`mobile_app_category_rules`、`mobile_location_points`、`mobile_sync_batches`、`mobile_usage_*`、`mobile_timeline_blocks`
     - PcTracker：`pc_aw_*`、`pc_keystats_*`、`pc_activity_*`、`pc_app_*`、`pc_categories`
     - QuickNotes：`quick_notes`、`quick_note_attachments`
  4. 后半段配置导航/FK：如 Event→Calendar、Task 自引用与 DomainProject/TaskBook、File* 版本链、Keystats 子表、PcCategory 父子、QuickNoteAttachment 等
  5. 声明反向 Navigation 集合
- 分支与异常：无运行时分支（生成代码）
- 调用：`ModelBuilder` Fluent API

## 近逐行中文伪代码

1. 文件头 auto-generated；using EF/Npgsql/Pim.Infrastructure.Data
2. `[DbContext(typeof(PimDbContext))]` partial `PimDbContextModelSnapshot : ModelSnapshot`
3. `BuildModel`：全局注解 + Identity 列策略
4. 数百段 `modelBuilder.Entity`：属性→列映射与索引
5. 约 60+ 张表 `ToTable(...)` 覆盖全平台域
6. 关系段 `HasOne`/`WithMany`/`HasForeignKey`/`OnDelete`
7. Navigation 收尾；`#pragma warning restore`
8. 变更本文件须通过 `dotnet ef migrations add`，勿手改业务逻辑

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs",
      "label": "PimDbContextModelSnapshot",
      "path": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260527025542_AddAiGateway.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs", "type": "depends_on" }
  ]
}
```
