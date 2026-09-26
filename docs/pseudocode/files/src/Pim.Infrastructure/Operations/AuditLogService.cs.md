# src/Pim.Infrastructure/Operations/AuditLogService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：实现 `IAuditLogService`，将操作审计请求落库为 `AuditLogEntity` 并返回 `AuditLogDto`。
- 主要依赖：`PimDbContext`、`AuditLogEntity`、`Pim.Core.Operations`（请求/DTO/枚举）、`System.Text.Json`
- 被谁使用：DI 注册为 `IAuditLogService`；`QuickNoteService`、`FileOperationService`、`CalendarAuditWriter` 等调用 `RecordAsync`

## 函数级结构化伪代码

### AuditLogService
#### AuditLogService(PimDbContext db)
- 输入：EF 上下文
- 输出：服务实例
- 副作用：无
- 步骤：保存 `_db` 引用
- 分支与异常：无
- 调用：无

#### Task<AuditLogDto> RecordAsync(CreateAuditLogRequest request, CancellationToken ct = default)
- 输入：创建审计日志请求；取消令牌
- 输出：持久化后的 `AuditLogDto`（含生成的 Id 与 CreatedAt）
- 副作用：向 `AuditLogs` 插入一行并 `SaveChangesAsync`
- 步骤：
  1. 从 request 映射 `AuditLogEntity`：UserId、ActorType/Result 转字符串、Action/Resource*/Source、IP/UA、CorrelationId
  2. `Metadata` 为空则用空字典，再 `JsonSerializer.Serialize` 到 `MetadataJson`
  3. 写入 ErrorCode/ErrorMessage；`CreatedAt = UtcNow`
  4. `_db.AuditLogs.Add(entity)`；`SaveChangesAsync`
  5. 组装 DTO：将 ActorType/Result 字符串 `Enum.Parse` 回枚举后返回
- 分支与异常：
  - 枚举字符串非法时 `Enum.Parse` 抛异常（写入时用 ToString，正常可逆）
  - DB 失败向上抛
- 调用：`JsonSerializer.Serialize`、`DbSet.Add`、`SaveChangesAsync`、`Enum.Parse`

## 近逐行中文伪代码

1. 引入 JSON、`Pim.Core.Operations`、Data 与 Entities
2. 命名空间 `Pim.Infrastructure.Operations`
3. 密封类 `AuditLogService` 实现 `IAuditLogService`
4. 构造注入 `PimDbContext` 存为 `_db`
5. `RecordAsync`：new `AuditLogEntity`，逐字段从 request 赋值
6. ActorType/Result 用 `.ToString()`；Metadata 序列化为 JSON（null 当空字典）
7. CreatedAt 取 UtcNow；Add 到 `AuditLogs`；SaveChanges
8. 返回 `AuditLogDto`，ActorType/Result 再 Parse 回枚举

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/AuditLogService.cs",
      "label": "AuditLogService",
      "path": "src/Pim.Infrastructure/Operations/AuditLogService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/AuditLogService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "to": "src/Pim.Infrastructure/Data/Entities/AuditLogEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "to": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "type": "calls" }
  ]
}
```
