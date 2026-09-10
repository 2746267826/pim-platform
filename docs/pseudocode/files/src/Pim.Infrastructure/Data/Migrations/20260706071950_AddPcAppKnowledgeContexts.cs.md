# src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `AddPcAppKnowledgeContexts`：创建 PC 应用知识上下文表 `pc_app_knowledge_contexts` 及唯一/辅助索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移管道；PcTracker 应用知识/分类相关持久化

## 函数级结构化伪代码

### AddPcAppKnowledgeContexts
#### `void Up(MigrationBuilder migrationBuilder)`
- 输入：`migrationBuilder`
- 输出：无
- 副作用：建表与索引
- 步骤：
  1. 创建 `pc_app_knowledge_contexts`：`id` PK；可空 `app_signature_id` FK→`pc_app_signatures.id`（`SetNull`）
  2. 模式字段：`process_name`、`pattern_type`、`pattern_value`、`target_category_name`、`project_tag`、`scope_summary`、`source`、`confidence`、`enabled`
  3. 统计：`affected_record_count`（默认 0）、`affected_duration_seconds`（默认 0）、`last_matched_at`
  4. 溯源：`source_rule_id`、`source_suggestion_id`；时间戳 `created_at`/`updated_at`
  5. 唯一索引 `(process_name, pattern_type, pattern_value)`；索引 `app_signature_id`、`target_category_name`、`source_suggestion_id`
- 分支与异常：迁移失败由 EF 抛出
- 调用：`CreateTable`、`CreateIndex`、`ForeignKey`

#### `void Down(MigrationBuilder migrationBuilder)`
- 输入：`migrationBuilder`
- 输出：无
- 副作用：删除表
- 步骤：
  1. `DropTable pc_app_knowledge_contexts`
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；`#nullable disable`
2. 分部类 `AddPcAppKnowledgeContexts : Migration`
3. `Up`：建 `pc_app_knowledge_contexts`，PK `PK_pc_app_knowledge_contexts`
4. FK 到 `pc_app_signatures`，删除签名时 SetNull
5. 唯一索引 `ix_pc_app_knowledge_contexts_app_pattern` 覆盖进程名+模式类型+模式值
6. 再建签名、分类、来源建议索引
7. `Down`：删除该表
8. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs",
      "label": "AddPcAppKnowledgeContexts",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706071950_AddPcAppKnowledgeContexts.cs", "to": "src/modules/Pim.Module.PcTracker", "type": "depends_on" }
  ]
}
```
