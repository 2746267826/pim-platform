# src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：Outlook Graph 连接持久化实体（令牌密文、订阅、delta、同步状态）。
- 主要依赖：`System.ComponentModel.DataAnnotations`、EF 列映射
- 被谁使用：`PimDbContext`、Outlook 同步/令牌相关服务

## 函数级结构化伪代码

### OutlookConnectionEntity
#### 属性模型（无方法）
- 输入：ORM 读写字段
- 输出：表 `outlook_connections` 行
- 副作用：无运行时逻辑
- 步骤：
  1. 主键 `Id` 默认 `NewGuid`
  2. 归属 `UserId`；`Provider` 默认 `"outlook"`
  3. OAuth：`ClientId`、`TenantId`(common)、`Scopes`(Calendars.ReadWrite 等)
  4. 状态：`Status`=`not-connected`，`TokenHealth`=`missing`
  5. 密文：`AccessTokenEncrypted` / `RefreshTokenEncrypted`，过期时间
  6. Graph：`SubscriptionId`/`ExpiresAt`、`DeltaLink`、`LastSyncedAt`/`LastError`
  7. 审计时间：`CreatedAt`/`UpdatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表名 `outlook_connections`
4. 定义连接实体全部列：用户、提供方、客户端/租户/范围、状态与令牌健康
5. 存加密 access/refresh 令牌字节与过期时刻
6. 存订阅 ID/过期、delta 链接、最近同步与错误
7. 创建/更新时间戳默认 UTC 现在

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs",
      "label": "OutlookConnectionEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/OutlookConnectionEntity.cs", "type": "depends_on" }
  ]
}
```
