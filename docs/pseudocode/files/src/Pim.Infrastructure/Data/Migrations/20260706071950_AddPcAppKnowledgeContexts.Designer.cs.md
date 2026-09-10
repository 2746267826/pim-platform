# src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `20260706071950_AddPcAppKnowledgeContexts` 的目标模型快照；在既有 AI/Files/QuickNotes/Calendar/PcTracker 模型上固化 PC 应用知识上下文与签名等表。
- 主要依赖：EF Core Migrations/Npgsql；`PimDbContext`
- 被谁使用：EF 迁移管线；与 `20260706071950_AddPcAppKnowledgeContexts.cs` partial 配对

## 函数级结构化伪代码

### AddPcAppKnowledgeContexts（partial）
#### 特性与类头
- 输入：无
- 输出：Migration Id `20260706071950_AddPcAppKnowledgeContexts` 绑定 `PimDbContext`
- 副作用：无
- 步骤：DbContext/Migration 特性 + partial 类
- 分支与异常：无
- 调用：EF

#### `BuildTargetModel(ModelBuilder modelBuilder)`
- 输入：`modelBuilder`
- 输出：该迁移完成后的全库模型（约 38 实体表 + 关系）
- 副作用：仅内存模型
- 步骤：
  1. 注解 EF 8.0.11、标识符长度 63；Npgsql Identity
  2. 配置基础设施与 AI：`ai_provider_settings`、`ai_request_logs`、`audit_logs`、`daemon_heartbeats`、`login_attempts`、`operation_confirmations`、`refresh_tokens`、`users`
  3. Calendar / Files / QuickNotes / PcTracker 既有表
  4. **本阶段关键实体**：
     - `AppKnowledgeContextEntity` → `pc_app_knowledge_contexts`（ProcessName+PatternType+PatternValue 唯一；索引 AppSignatureId/SourceSuggestionId/TargetCategoryName）
     - `AppSignatureEntity` → `pc_app_signatures`（ProcessName 唯一）
     - 以及 Files 模块全套、`pc_activity_classification_audits`、`pc_categories` 等已纳入快照
  5. 关系：User 系；Calendar Event/Task；Files 树（Provider/Item/Version/Chunk/AiResult/IndexJob/Suggestion）；AppKnowledgeContext → AppSignature；Keystats 子表 Cascade；PcCategory 自引用；QuickNoteAttachment → QuickNote
- 分支与异常：无
- 调用：EF

## 近逐行中文伪代码

1. auto-generated 头与 using（EF、Npgsql、Pim.Infrastructure.Data）
2. Migration 特性 `AddPcAppKnowledgeContexts` + partial 类
3. `BuildTargetModel`：模型注解
4. 逐实体 Fluent 配置（Property/Key/Index/ToTable）
5. 新增重点：`pc_app_knowledge_contexts` 字段含 PatternType/Value、ProcessName、TargetCategoryName、AppSignatureId、SourceRuleId/SuggestionId、Enabled、Confidence 等
6. 新增重点：`pc_app_signatures` 字段含 DisplayName、ProcessName 唯一、Productivity 默认 neutral
7. 关系段配置 HasOne/WithMany/FK/DeleteBehavior
8. Navigation；结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs",
      "label": "AddPcAppKnowledgeContexts.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.Designer.cs", "to": "src/modules/Pim.Module.PcTracker", "type": "depends_on" }
  ]
}
```
