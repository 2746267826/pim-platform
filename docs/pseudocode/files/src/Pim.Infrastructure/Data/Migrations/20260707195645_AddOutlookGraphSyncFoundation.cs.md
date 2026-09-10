# src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移：扩展 `outlook_connections` Graph 同步字段；创建 `outlook_sync_batches` 批次元数据表与索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations.MigrationBuilder`
- 被谁使用：EF 迁移流水线；Outlook/Calendar 同步模块

## 函数级结构化伪代码

### AddOutlookGraphSyncFoundation
#### void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：改连接表；建批表与索引
- 步骤：
  1. `outlook_connections` 增加：client_id、delta_link、last_error、provider(默认 outlook)、scopes(默认 Calendars.ReadWrite...)、status(默认 not-connected)、tenant_id(默认 common)、token_health(默认 missing)
  2. 创建 `outlook_sync_batches`：user_id、provider、status、读写/冲突/确认/失败计数、steps_json/errors_json、error_summary、started/finished
  3. 索引：user_id；user_id+provider+started_at；user_id+started_at
- 分支与异常：DDL 失败抛出
- 调用：`AddColumn`、`CreateTable`、`CreateIndex`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：删批表；删连接表新增列
- 步骤：DropTable batches；依次 DropColumn 八个新列
- 分支与异常：无
- 调用：`DropTable`、`DropColumn`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；nullable disable
2. partial 类 `AddOutlookGraphSyncFoundation` 继承 Migration
3. `Up`：为 outlook_connections 加 client_id/delta_link/last_error/provider/scopes/status/tenant_id/token_health
4. `Up`：建 outlook_sync_batches（计数与 jsonb 步骤/错误、运行状态默认 running）
5. `Up`：建 user_id 相关三个索引
6. `Down`：删 batches 表；回滚八列

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs",
      "label": "AddOutlookGraphSyncFoundation",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs", "to": "outlook_connections", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs", "to": "outlook_sync_batches", "type": "depends_on" }
  ]
}
```
