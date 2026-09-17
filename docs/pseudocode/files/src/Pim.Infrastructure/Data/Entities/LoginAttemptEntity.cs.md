# src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：登录尝试审计实体，映射表 `login_attempts`，记录用户、IP、成败与时间。
- 主要依赖：`System.ComponentModel.DataAnnotations` / `Schema`；导航 `UserEntity`
- 被谁使用：`PimDbContext.LoginAttempts`；`AuthEndpoints` 登录成功/失败写入；EF 迁移与模型快照

## 函数级结构化伪代码

### LoginAttemptEntity
#### 属性映射（表 `login_attempts`）
- 输入：无（POCO 属性）
- 输出：持久化字段
- 副作用：无（纯实体定义）
- 步骤：
  1. `Id`：主键 Guid，默认 `Guid.NewGuid()`
  2. `UserId`：可空 Guid，关联用户
  3. `IpAddress`：最大 45 字符（兼容 IPv6），默认空串
  4. `Success`：是否登录成功
  5. `AttemptedAt`：尝试时间，默认 `DateTimeOffset.UtcNow`
  6. `User`：可选导航到 `UserEntity`，FK 指向 `UserId`
- 分支与异常：无运行时逻辑
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Infrastructure.Data.Entities`
3. 类标注 `[Table("login_attempts")]`
4. 定义 `LoginAttemptEntity`
5. `Id`：主键列 `id`，默认新 Guid
6. `UserId`：列 `user_id`，可空
7. `IpAddress`：列 `ip_address`，MaxLength 45，默认空
8. `Success`：列 `success`，布尔
9. `AttemptedAt`：列 `attempted_at`，默认 UTC 现在
10. `User`：外键导航 `UserId` → `UserEntity?`
11. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs",
      "label": "LoginAttemptEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs", "to": "src/Pim.Infrastructure/Data/Entities/UserEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Data/Entities/LoginAttemptEntity.cs", "type": "depends_on" }
  ]
}
```
