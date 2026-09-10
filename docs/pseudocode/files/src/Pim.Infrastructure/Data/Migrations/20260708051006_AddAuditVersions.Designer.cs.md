# src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer 快照）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260708051006_AddAuditVersions` 的目标模型快照；新增 `AuditVersionEntity`→`audit_versions`，并包含当时完整库模型（规划对象扩展、Mobile 分析表等）。
- 主要依赖：EF Core、Npgsql、`PimDbContext`、`Pim.Infrastructure.Audit.AuditVersionEntity`
- 被谁使用：EF 迁移工具；与 `AddAuditVersions.cs` 配对

## 函数级结构化伪代码

### AddAuditVersions（partial）
#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：描述含审计版本表后的全量模型
- 步骤：
  1. 模型注解与 Npgsql 默认
  2. **本迁移焦点**：`AuditVersionEntity` 映射 `audit_versions`
     - 列：id、actor、before_json/after_json（默认 `{}`）、changed_fields_json（默认 `[]`）、confirmation_id、created_at（now()）、object_id、object_type、source
     - 索引：`ConfirmationId`；复合 `(ObjectType, ObjectId, CreatedAt)`
  3. 既有平台实体：AI 设置/日志、AuditLog、DaemonHeartbeat、LoginAttempt、OperationConfirmation、RefreshToken、User
  4. Calendar 扩展：AiPlanningPlaceholder、AvailabilityWindow、DomainProject、HabitOccurrence/Routine、OutlookSyncBatch、TaskBook、TaskChecklistItem、TaskExecutionSegment 等 + 原有 Calendar/Event/Task/Outlook/Pending/Feedback
  5. Files 七实体；Mobile 含 catalog override、category rule、timeline block、usage aggregate/goal 等扩展表
  6. PcTracker 与 QuickNotes；关系与 Navigation 注册
- 分支与异常：无运行时分支
- 调用：Fluent `modelBuilder.Entity` API

## 近逐行中文伪代码

1. auto-generated 头与引用；nullable disable
2. `[DbContext]` + `[Migration("20260708051006_AddAuditVersions")]`
3. partial `AddAuditVersions`；`BuildTargetModel`
4. 注解 8.0.11 / 标识符长度 / IdentityByDefault
5. 首先配置 `AuditVersionEntity`：属性、HasKey、两索引、ToTable(`audit_versions`)
6. 配置 AI、审计日志、心跳、登录、确认、令牌、用户
7. 配置 Calendar 规划/习惯/任务书/执行段等扩展实体
8. 配置 Files、Mobile（含分析扩展）、PcTracker、QuickNotes
9. 关系块：User FK、Calendar/Task 树、Files 子表、Keystats、Note 附件等
10. 恢复警告；结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs",
      "label": "AddAuditVersions.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.Designer.cs", "to": "Microsoft.EntityFrameworkCore", "type": "depends_on" }
  ]
}
```
