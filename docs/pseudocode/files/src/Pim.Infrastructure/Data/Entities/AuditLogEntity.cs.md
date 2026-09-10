# src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：映射表 `audit_logs` 的审计日志持久化实体（操作者、动作、资源、结果、关联与错误信息）。
- 主要依赖：`System.ComponentModel.DataAnnotations` / `Schema`（Table/Key/Column/MaxLength）
- 被谁使用：`PimDbContext` 的 `DbSet`；审计写入与查询服务

## 函数级结构化伪代码

### AuditLogEntity
#### 属性集（无行为方法）
- 输入：各属性赋值（由调用方/EF 填充）
- 输出：行状态
- 副作用：无（纯 POCO）；持久化由 DbContext 负责
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid`
  2. `UserId`：可空用户 Id
  3. `ActorType`：操作者类型，最长 32
  4. `Action`：动作名，最长 128
  5. `ResourceType` / `ResourceId`：资源类型与可选资源 Id
  6. `Source`：来源，最长 64
  7. `Result`：结果，最长 32
  8. `IpAddress` / `UserAgent`：可选客户端信息
  9. `CorrelationId`：可选关联 Id
  10. `MetadataJson`：jsonb 元数据，默认 `"{}"`
  11. `ErrorCode` / `ErrorMessage`：可选错误
  12. `CreatedAt`：创建时间，默认 UTC 现在
- 分支与异常：无运行时分支；字符串字段默认空串
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Infrastructure.Data.Entities`
3. 表名注解 `audit_logs`；密封类 `AuditLogEntity`
4. `Id` 主键列 id，默认新 Guid
5. `UserId` 可空用户列
6. `ActorType`/`Action`/`ResourceType`/`ResourceId`/`Source`/`Result` 字符串列带 MaxLength
7. `IpAddress`/`UserAgent`/`CorrelationId` 可选字符串
8. `MetadataJson` jsonb，默认空对象 JSON
9. `ErrorCode` 可空 int；`ErrorMessage` 可空字符串
10. `CreatedAt` 时间戳，默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs",
      "label": "AuditLogEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs", "type": "depends_on" }
  ]
}
```
