# src/Pim.Infrastructure/Data/Entities/UserEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：映射表 `users` 的用户实体，含凭证哈希、角色、激活状态与软删除时间戳。
- 主要依赖：`ISoftDeletable`、`System.ComponentModel.DataAnnotations`、EF 列/表注解
- 被谁使用：`PimDbContext.Users`；登录/刷新令牌实体外键；迁移快照与认证流程

## 函数级结构化伪代码

### UserEntity
#### 属性组（EF 实体 POCO，无方法）
- 输入：由 EF / 业务代码读写属性
- 输出：持久化到 `users` 行
- 副作用：无自身逻辑；参与软删除过滤（`DeletedAt`）
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid()`，列 `id`
  2. `Username`：最长 50，列 `username`，默认空串
  3. `Email`：最长 255，列 `email`，默认空串
  4. `PasswordHash`：最长 255，列 `password_hash`，默认空串
  5. `DisplayName`：可空，最长 100，列 `display_name`
  6. `Role`：最长 20，默认 `"user"`，列 `role`
  7. `IsActive`：布尔，默认 true，列 `is_active`
  8. `CreatedAt` / `UpdatedAt`：`DateTimeOffset`，默认 UtcNow
  9. `DeletedAt`：可空，实现 `ISoftDeletable` 软删
- 分支与异常：无控制流；唯一索引等在 Fluent/迁移中定义
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`Pim.Core.Data`
2. 命名空间 `Pim.Infrastructure.Data.Entities`
3. `[Table("users")]` 映射表名
4. 类 `UserEntity` 实现 `ISoftDeletable`
5. `Id` 主键 + 列 `id`，默认新 Guid
6. `Username`/`Email`/`PasswordHash` 带 MaxLength 与列名
7. `DisplayName` 可空；`Role` 默认 user；`IsActive` 默认 true
8. `CreatedAt`/`UpdatedAt` 默认 UtcNow
9. `DeletedAt` 可空软删时间戳

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs",
      "label": "UserEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/UserEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs", "to": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs", "to": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "type": "depends_on" }
  ]
}
```
