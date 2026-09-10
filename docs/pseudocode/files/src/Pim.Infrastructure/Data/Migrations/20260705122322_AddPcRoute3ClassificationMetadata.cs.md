# src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移（原始 SQL）：为 PC 活动分类补 Route3 元数据列；创建分类审计、应用签名、分类树表并收紧列约束。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations.MigrationBuilder.Sql`
- 被谁使用：EF 迁移流水线

## 函数级结构化伪代码

### AddPcRoute3ClassificationMetadata
#### void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：改 `pc_activity_classifications`；建 audits/signatures/categories 表与索引
- 步骤：
  1. 分类表新增：interpretation_version、record_key_stability、record_key_version、source_bucket_ids(jsonb)、source_type（均 IF NOT EXISTS + 默认值）
  2. 建 record_key_version / source_type 索引
  3. 建 `pc_activity_classification_audits`（operation、rule/suggestion、range、影响计数与 keys、创建人/时间）及索引
  4. 建 `pc_app_signatures`（进程名唯一、展示名、类别路径、productivity、source/confidence 等）并 SET NOT NULL 加固
  5. 建 `pc_categories`（自引用 parent_id RESTRICT、name/color/icon/productivity/sort/builtin）及索引；用 LEFT 截断收紧 varchar 长度
- 分支与异常：IF NOT EXISTS / IF EXISTS 保证幂等；DDL 失败抛出
- 调用：`migrationBuilder.Sql`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：删除 audits 表；删除分类元数据列与相关索引（不回滚 signatures/categories）
- 步骤：
  1. DROP audits 表
  2. DROP 两索引
  3. DROP 五列
- 分支与异常：IF EXISTS 幂等
- 调用：`migrationBuilder.Sql`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；nullable disable
2. partial 类 `AddPcRoute3ClassificationMetadata` 继承 Migration
3. `Up` 单条多语句 SQL：
4.   分类表加解释版本/键稳定性/键版本/bucket ids/source_type 及索引
5.   建 classification_audits 与 created_at/rule/suggestion 索引
6.   建 app_signatures（process_name 唯一）并强制 source/confidence 非空默认
7.   建 categories 自引用树；截断 name/color/icon/productivity 长度
8. `Down`：删 audits；删元数据索引与列（不删 signatures/categories）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs",
      "label": "AddPcRoute3ClassificationMetadata",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs", "to": "pc_activity_classifications", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs", "to": "pc_activity_classification_audits", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs", "to": "pc_app_signatures", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260705122322_AddPcRoute3ClassificationMetadata.cs", "to": "pc_categories", "type": "depends_on" }
  ]
}
```
