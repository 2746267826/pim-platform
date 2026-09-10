# src/Pim.Core/Operations/AuditDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义审计日志写入请求 DTO、查询/返回 DTO，以及 `IAuditLogService` 记录契约。
- 主要依赖：`AuditActorType`、`AuditResult`（`OperationEnums`）
- 被谁使用：`AuditLogService`（实现）、`QuickNoteService`、`FileOperationService`、`CalendarAuditWriter`、`ActivityClassificationRecomputeService` 等业务审计写入点

## 函数级结构化伪代码

### CreateAuditLogRequest
#### 记录构造（位置参数 record）
- 输入：
  - `UserId`：可选用户 Id
  - `ActorType`：操作者类型（用户/系统等）
  - `Action`：动作名
  - `ResourceType` / `ResourceId`：资源类型与可选资源 Id
  - `Source`：来源标识
  - `Result`：成功/失败等结果枚举
  - `IpAddress` / `UserAgent` / `CorrelationId`：可选环境与关联信息
  - `Metadata`：可选键值元数据
  - `ErrorCode` / `ErrorMessage`：可选错误信息
- 输出：不可变请求对象
- 副作用：无
- 步骤：
  1. 调用方组装字段
  2. 传给 `IAuditLogService.RecordAsync`
- 分支与异常：本类型无逻辑
- 调用：被各模块审计写入点构造

### AuditLogDto
#### 记录构造（位置参数 record）
- 输入：`Id`、`UserId`、`ActorType`、`Action`、`ResourceType`、`ResourceId`、`Source`、`Result`、`CorrelationId`、`CreatedAt`
- 输出：审计日志对外 DTO（相对请求少了 IP/UA/Metadata/错误详情等字段）
- 副作用：无
- 步骤：
  1. 由服务实现从持久化实体映射而来
- 分支与异常：无
- 调用：作为 `RecordAsync` 返回值

### IAuditLogService
#### Task<AuditLogDto> RecordAsync(CreateAuditLogRequest request, CancellationToken ct = default)
- 输入：`request` 审计写入请求；`ct` 取消令牌
- 输出：写入后的 `AuditLogDto`
- 副作用：持久化一条审计记录（实现侧）
- 步骤：
  1. 接收请求
  2. 由实现写入存储并映射为 DTO 返回
- 分支与异常：契约不规定具体异常；取消时遵循 `ct`
- 调用：业务服务在关键操作后调用

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Operations`
2. 定义密封 record `CreateAuditLogRequest`，包含用户、操作者类型、动作、资源、来源、结果、网络/关联、元数据与错误字段
3. 定义密封 record `AuditLogDto`，包含 Id、用户、操作者类型、动作、资源、来源、结果、关联 Id、创建时间
4. 定义接口 `IAuditLogService`
5. 接口方法 `RecordAsync`：接收 `CreateAuditLogRequest` 与可选 `CancellationToken`，异步返回 `AuditLogDto`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Operations/AuditDtos.cs",
      "label": "AuditDtos",
      "path": "src/Pim.Core/Operations/AuditDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Operations/AuditDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Operations/AuditDtos.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/AuditLogService.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarAuditWriter.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/Pim.Core/Operations/AuditDtos.cs", "type": "calls" }
  ]
}
```
