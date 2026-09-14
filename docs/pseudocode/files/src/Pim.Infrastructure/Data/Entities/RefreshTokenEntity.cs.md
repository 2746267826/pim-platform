# src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：刷新令牌持久化实体，映射表 `refresh_tokens`，关联用户并记录哈希与吊销时间。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`UserEntity`
- 被谁使用：`PimDbContext`、认证/Token 刷新流程

## 函数级结构化伪代码

### RefreshTokenEntity
#### 属性与表映射（无实例方法）
- 输入：无（POCO 属性由 EF/调用方赋值）
- 输出：实体实例字段
- 副作用：无逻辑副作用；持久化由 DbContext 负责
- 步骤：
  1. 表名 `refresh_tokens`（`[Table]`）
  2. `Id`：Guid PK，默认 `NewGuid()`
  3. `UserId`：所属用户
  4. `TokenHash`：令牌哈希，MaxLength 255
  5. `ExpiresAt`：过期时间
  6. `RevokedAt`：可空吊销时间
  7. `CreatedAt`：默认 UtcNow
  8. `User`：导航属性，FK → `UserEntity`
- 分支与异常：本类型无行为逻辑
- 调用：被 Auth 服务写入/查询

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Infrastructure.Data.Entities`
3. `[Table("refresh_tokens")]` 类 `RefreshTokenEntity`
4. `Id` 主键列 id，默认 NewGuid
5. `UserId` 列 user_id
6. `TokenHash` 列 token_hash，最长 255，默认空串
7. `ExpiresAt` 列 expires_at
8. `RevokedAt` 可空列 revoked_at
9. `CreatedAt` 列 created_at，默认 UtcNow
10. `User` 导航属性，ForeignKey(UserId) 指向 `UserEntity`，非空引用

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs",
      "label": "RefreshTokenEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs", "to": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/RefreshTokenEntity.cs", "type": "depends_on" }
  ]
}
```
