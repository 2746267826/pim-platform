# src/Pim.Infrastructure/Audit/AuditVersionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：对象版本审计持久化实体，映射表 `audit_versions`，保存变更前后 JSON 快照与变更字段。
- 主要依赖：`System.ComponentModel.DataAnnotations` / `Schema`（`[Table]`、`[Key]`、`[Column]`、`[MaxLength]`）
- 被谁使用：`PimDbContext.AuditVersions`；`AuditVersionService` 创建与映射；`DataCenterQueryService` 查询；EF 迁移/快照（如 `AddAuditVersions`、`AddReportArtifacts.Designer`）

## 函数级结构化伪代码

### AuditVersionEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务层构造/赋值后由 EF 持久化
- 输出：表 `audit_versions` 一行
- 副作用：无逻辑副作用；默认值在属性初始化时给出
- 步骤：
  1. `Id`：主键 Guid，默认 `Guid.NewGuid()`，列 `id`
  2. `ObjectType`：被审计对象类型，最长 80，默认空串，列 `object_type`
  3. `ObjectId`：被审计对象 Id，列 `object_id`
  4. `ConfirmationId`：可选关联操作确认 Id，列 `confirmation_id`
  5. `Source`：来源，最长 80，默认 `"pim"`，列 `source`
  6. `Actor`：操作者，最长 255，默认 `"system"`，列 `actor`
  7. `BeforeJson` / `AfterJson`：变更前后 JSON，默认 `"{}"`，列 `before_json` / `after_json`
  8. `ChangedFieldsJson`：变更字段列表 JSON，默认 `"[]"`，列 `changed_fields_json`
  9. `CreatedAt`：创建时间，默认 `DateTimeOffset.UtcNow`，列 `created_at`
- 分支与异常：本类型无校验逻辑；长度/非空约束由 EF/数据库与服务层保证
- 调用：被 `AuditVersionService` 写入与 `Map` 到 `AuditVersionDto`

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间：`Pim.Infrastructure.Audit`
3. 表映射：`[Table("audit_versions")]`
4. 声明类 `AuditVersionEntity`
5. `Id`：Key，列 id，默认新 Guid
6. `ObjectType`：列 object_type，MaxLength 80，默认空
7. `ObjectId`：列 object_id
8. `ConfirmationId`：可空 Guid，列 confirmation_id
9. `Source`：列 source，MaxLength 80，默认 pim
10. `Actor`：列 actor，MaxLength 255，默认 system
11. `BeforeJson`：列 before_json，默认 `{}`
12. `AfterJson`：列 after_json，默认 `{}`
13. `ChangedFieldsJson`：列 changed_fields_json，默认 `[]`
14. `CreatedAt`：列 created_at，默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs",
      "label": "AuditVersionEntity",
      "path": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Audit/AuditVersionEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Audit/AuditVersionEntity.cs", "to": "src/Pim.Core/Audit/AuditVersionDtos.cs", "type": "depends_on" }
  ]
}
```
