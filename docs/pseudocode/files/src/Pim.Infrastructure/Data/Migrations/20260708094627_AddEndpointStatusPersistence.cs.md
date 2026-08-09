# src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `AddEndpointStatusPersistence`——持久化端点（守护进程）状态与通知动作。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移；`EndpointStatusService` 等

## 函数级结构化伪代码

### AddEndpointStatusPersistence : Migration
#### void Up(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：建两表与索引
- 步骤：
  1. 表 `endpoint_notification_actions`：user/device、action/risk/result、可选 detail_url/message/confirmation_id/related_object_*、created_at
  2. 表 `endpoint_statuses`：user/device 唯一维度、platform 默认 windows、app_version、upload_status 默认 Unknown、缓存计数、online_only_blocked_count、last_heartbeat_at、created/updated
  3. 索引：notification 按 confirmation_id/created_at/device_id/user_id；status 按 last_heartbeat_at 与 (user_id, device_id) unique
- 分支与异常：迁移失败回滚
- 调用：`CreateTable` / `CreateIndex`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：删两表
- 步骤：Drop `endpoint_notification_actions` 与 `endpoint_statuses`
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. partial 类 `AddEndpointStatusPersistence` 继承 Migration
2. Up：CreateTable endpoint_notification_actions（动作审计字段 + created_at）
3. CreateTable endpoint_statuses（心跳与上传状态字段，默认 platform/upload_status）
4. 为 notification 建 4 个索引；为 statuses 建 last_heartbeat 与 user+device 唯一索引
5. Down：依次 Drop 两表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs",
      "label": "AddEndpointStatusPersistence",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708094627_AddEndpointStatusPersistence.cs", "type": "depends_on" }
  ]
}
```
