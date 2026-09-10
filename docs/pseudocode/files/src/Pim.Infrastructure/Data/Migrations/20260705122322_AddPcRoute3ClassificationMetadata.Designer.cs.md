# src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `AddPcRoute3ClassificationMetadata` 的目标模型：PC 分类 Route3 元数据列、审计表、应用签名与分类树。
- 主要依赖：EF Core / Npgsql / `PimDbContext`；此时模型已含 AI、Files、QuickNotes 等前序迁移实体
- 被谁使用：EF 迁移工具链；与同名 `.cs` raw SQL `Up` 配对

## 函数级结构化伪代码

### AddPcRoute3ClassificationMetadata（partial）
#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：完整目标模型（约 37 表 / 58 实体配置块）
- 步骤：
  1. 注解 EF 8.0.11 + Npgsql identity
  2. 配置基础设施：`AiProviderSettingEntity`、`AiRequestLogEntity`、Audit/Daemon/Login/Confirmation/Token/User
  3. Calendar 模块实体；Files 七实体；QuickNotes 两实体
  4. PcTracker 既有 + **Route3 焦点**：
     - `ActivityClassificationEntity` 增列：`InterpretationVersion`（默认 interpreted-aw-v1）、`RecordKeyStability`（low）、`RecordKeyVersion`（pc-fallback-v1）、`SourceBucketIdsJson`、`SourceType`；索引 record_key_version、source_type
     - `ActivityClassificationAuditEntity`→`pc_activity_classification_audits`：operation/range/affected* / rule_id/suggestion_id
     - `AppSignatureEntity`→`pc_app_signatures`：process_name 唯一；display/category_path/productivity/source/confidence
     - `PcCategoryEntity`→`pc_categories`：自引用 Parent（Restrict）；name/color/icon/productivity/sort_order/is_builtin
  5. 配置全部 FK/Navigation（含 PcCategory.Children、Files 导航、Keystats 等）
- 分支与异常：无
- 调用：Fluent API

## 近逐行中文伪代码

1. auto-generated；DbContext+Migration 特性；partial `AddPcRoute3ClassificationMetadata`
2. `BuildTargetModel`：产品版本注解、identity 列
3. 依次配置 AI 设置/请求日志与运维/用户表
4. 配置 Calendar、Files、QuickNotes 全表
5. 配置规则/建议等既有 PcTracker 表
6. **Audit 实体**：affected 计数与 keys jsonb、created_at 索引
7. **Classification 实体**：在原字段上追加 interpretation/record_key_* / source_bucket_ids / source_type
8. **AppSignature**：process_name 唯一索引
9. **PcCategory**：parent_id FK Restrict；Children 导航
10. 其余 Keystats/AW 与关系收尾
11. （业务增量见同名非 Designer：ALTER 分类列 + CREATE audits/signatures/categories）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs",
      "label": "AddPcRoute3ClassificationMetadata.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.Designer.cs", "to": "src/modules/Pim.Module.PcTracker", "type": "depends_on" }
  ]
}
```
