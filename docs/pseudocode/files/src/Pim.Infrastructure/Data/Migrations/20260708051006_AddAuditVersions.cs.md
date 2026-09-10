# src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `AddAuditVersions`——创建 `audit_versions` 表及按对象/确认 Id 的索引，支撑版本审计。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移；`AuditVersionService` 读写该表

## 函数级结构化伪代码

### AddAuditVersions : Migration
#### void Up(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：建表 + 索引
- 步骤：
  1. 创建 `audit_versions`：id、object_type(80)、object_id、confirmation_id 可空、source(80)、actor(255)、before/after/changed_fields JSON 文本默认 `{}`/`{}`/`[]`、created_at 默认 now()
  2. PK `PK_audit_versions`
  3. 索引 `IX_audit_versions_confirmation_id`
  4. 复合索引 `(object_type, object_id, created_at)`
- 分支与异常：迁移失败回滚
- 调用：`CreateTable` / `CreateIndex`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：删表
- 步骤：`DropTable("audit_versions")`
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. partial 类 `AddAuditVersions` 继承 Migration
2. Up：CreateTable audit_versions，列含对象键、确认、来源、操作者、三份 JSON 文本、created_at
3. PrimaryKey 在 id
4. 建 confirmation_id 索引与 object_type+object_id+created_at 索引
5. Down：DropTable audit_versions

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs",
      "label": "AddAuditVersions",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708051006_AddAuditVersions.cs", "type": "depends_on" }
  ]
}
```
