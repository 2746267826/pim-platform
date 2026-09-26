# src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer 快照）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260706161439_AddMobileModule` 的目标模型快照；在既有模型上纳入 Mobile 模块七表及当时全库实体（含 AI/Files/扩展 PcTracker）。
- 主要依赖：EF Core Migrations、Npgsql、`PimDbContext`、Mobile/Files/Calendar/PcTracker 实体类型名
- 被谁使用：EF 迁移工具；与 `AddMobileModule.cs` 配对

## 函数级结构化伪代码

### AddMobileModule（partial）
#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：配置该迁移完成后的完整目标模型
- 步骤：
  1. 模型注解与 Npgsql Identity 默认（同系列 Designer）
  2. 平台：`AiProviderSettingEntity`/`AiRequestLogEntity`、`AuditLog`、`DaemonHeartbeat`、`LoginAttempt`、`OperationConfirmation`、`RefreshToken`、`User`
  3. Calendar 核心六实体 + Files 七实体（FileAiResult/Chunk/IndexJob/Item/Provider/Suggestion/Version）
  4. **本迁移引入的 Mobile 实体**（配套 Up 建表）：
     - `MobileAppCatalogEntity` → `mobile_app_catalog`
     - `MobileDeviceEntity` → `mobile_devices`
     - `MobileLocationPointEntity` → `mobile_location_points`
     - `MobileSyncBatchEntity` → `mobile_sync_batches`
     - `MobileUsageEventEntity` → `mobile_usage_events`
     - `MobileUsageSessionEntity` → `mobile_usage_sessions`
     - `MobileUsageSummaryEntity` → `mobile_usage_summaries`
     及按 user/device/package/时间窗的复合索引
  5. PcTracker 扩展：含 `ActivityClassificationAudit`、`AppKnowledgeContext`、`AppSignature`、`PcCategory` 等；另有 `pc_activity_classifications.explanation` 列变更
  6. QuickNotes；关系块（User FK、Calendar、Files 子实体、Keystats 等）
- 分支与异常：无运行时分支
- 调用：`modelBuilder.Entity` 系列 Fluent API

## 近逐行中文伪代码

1. auto-generated 头与 EF/Npgsql/Data 引用；`#nullable disable`
2. 命名空间 Migrations；`[DbContext(PimDbContext)]` + `[Migration("20260706161439_AddMobileModule")]`
3. partial `AddMobileModule`；`BuildTargetModel` 开始
4. 注解 ProductVersion 8.0.11 / MaxIdentifierLength 63 / IdentityByDefault
5. 配置 AI 设置/请求日志表实体
6. 配置审计、心跳、登录、确认、刷新令牌、用户
7. 配置 Calendar、Files 全套实体与表
8. 配置 Mobile 七实体：设备、应用目录、位置点、同步批次、用量事件/会话/摘要
9. 配置 PcTracker（含审计/知识上下文/签名/分类）与 QuickNotes
10. 关系：Login/Refresh→User；Calendar 导航；Files 子表 FK；Keystats 级联；Note 附件等
11. 结束 pragma 与方法

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs",
      "label": "AddMobileModule.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs", "to": "src/modules/Pim.Module.Mobile", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.Designer.cs", "to": "Microsoft.EntityFrameworkCore", "type": "depends_on" }
  ]
}
```
