# src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：Stage0 运维相关表迁移：创建 `audit_logs`、`daemon_heartbeats`、`operation_confirmations` 及索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`（`Migration`、`MigrationBuilder`）
- 被谁使用：EF Core 迁移流水线（`dotnet ef database update` / 应用启动迁移）；Designer/Snapshot 关联

## 函数级结构化伪代码

### Stage0OperationsTables
#### `void Up(MigrationBuilder migrationBuilder)`
- 输入：迁移构建器
- 输出：无
- 副作用：在目标库创建三张表与多条索引
- 步骤：
  1. 创建 `audit_logs`：审计字段（actor/action/resource/source/result、IP、UA、correlation、jsonb metadata、错误码文案、`created_at` 默认 now）
  2. 创建 `daemon_heartbeats`：设备心跳（device_id、daemon_kind 默认 windows、版本与 server_url、上传时间/错误/队列、ActivityWatch/KeyStats 状态默认 Unknown、paused、status_json、received_at）
  3. 创建 `operation_confirmations`：高风险操作确认（operation_type、summary、risk、payload/preview jsonb、status 默认 Pending、过期与确认/拒绝/执行时间、result_json、correlation_id）
  4. 为 audit 建 action/correlation/created_at/resource_type/user_id 索引
  5. 为 heartbeat 建 `(device_id, daemon_kind)` 唯一索引与 `received_at` 索引
  6. 为 confirmation 建 expires_at/operation_type/requested_by_user_id/status 索引
- 分支与异常：由 EF 迁移运行时处理 SQL 失败
- 调用：`migrationBuilder.CreateTable` / `CreateIndex`

#### `void Down(MigrationBuilder migrationBuilder)`
- 输入：迁移构建器
- 输出：无
- 副作用：按序删除三张表
- 步骤：
  1. Drop `audit_logs`
  2. Drop `daemon_heartbeats`
  3. Drop `operation_confirmations`
- 分支与异常：表不存在或依赖冲突时迁移失败
- 调用：`migrationBuilder.DropTable`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations
2. 禁用可空引用上下文（`#nullable disable`）
3. 命名空间 `Pim.Infrastructure.Data.Migrations`
4. 分部类 `Stage0OperationsTables` 继承 `Migration`
5. `Up`：
6.   建表 `audit_logs` 与主键 `PK_audit_logs`
7.   建表 `daemon_heartbeats` 与主键，daemon_kind/状态默认值，status_json 默认 `{}`
8.   建表 `operation_confirmations` 与主键，payload/preview 默认 `{}`，status 默认 Pending
9.   创建 audit 五条单列索引
10.  创建 heartbeat 设备+类型唯一索引与 received_at 索引
11.  创建 confirmation 四条索引
12. `Down`：依次 Drop 三张表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs",
      "label": "Stage0OperationsTables",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260524170037_Stage0OperationsTables.cs", "type": "depends_on" }
  ]
}
```
