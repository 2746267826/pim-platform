# src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：将一次 AI 请求的写入模型脱敏、映射为 `AiRequestLogEntity` 并持久化，返回新日志 Id。
- 主要依赖：
  - `Pim.Core.Ai`（`AiRequestStatus`）
  - `Pim.Infrastructure.Data`（`PimDbContext`）
  - `Pim.Infrastructure.Data.Entities`（`AiRequestLogEntity`）
  - 同目录 `AiRedactor`（JSON/明文脱敏）
  - `System.Security.Cryptography` / `System.Text`（SHA-256 哈希）
- 被谁使用：
  - `AiGateway` 经 `IAiRequestLogWriter` 写成功/失败/超时等日志
  - `ServiceCollectionExtensions` 注册 `AddScoped<IAiRequestLogWriter, AiRequestLogWriter>`
  - 单元测试 `AiRequestLogWriterTests`、`AiGatewayTests`

## 函数级结构化伪代码

### AiRequestLogWriteModel
#### 记录主构造（写库入参 DTO）
- 输入：用户/模块/目的/源对象、提供商与模型、关联 Id、状态与重试、起止时间、请求/响应/解析结果/Schema、Token 与费用、错误与元数据 JSON 等字段（见近逐行）
- 输出：不可变写入模型
- 副作用：无
- 步骤：1. 仅承载字段，无行为
- 分支与异常：无
- 调用：无

### IAiRequestLogWriter
#### `Task<Guid> WriteAsync(AiRequestLogWriteModel model, CancellationToken ct = default)`
- 输入：写入模型；可选取消令牌
- 输出：新建日志实体主键 `Guid`
- 副作用：由实现决定（持久化）
- 步骤：1. 契约方法，无体
- 分支与异常：无
- 调用：无

### AiRequestLogWriter
#### 主构造 `AiRequestLogWriter(PimDbContext db)`
- 输入：EF Core 上下文
- 输出：写入器实例（主键构造注入）
- 副作用：无
- 步骤：1. 保存 `db` 依赖
- 分支与异常：无
- 调用：无

#### `Task<Guid> WriteAsync(AiRequestLogWriteModel model, CancellationToken ct = default)`
- 输入：`model` 全量请求日志字段；`ct` 取消
- 输出：`entity.Id`（新增后的日志 Guid）
- 副作用：
  - 对多段 JSON/明文做脱敏
  - `db.AiRequestLogs.Add` + `SaveChangesAsync` 写库
- 步骤：
  1. 对 `RequestMessagesJson`、`RequestPayloadJson`、`ResponseRawJson`、`MetadataJson`、`SchemaValidationErrorsJson` 调用 `AiRedactor.RedactJson`。
  2. 对 `ResponseText`、`ErrorMessage` 调用 `AiRedactor.RedactPlainText`。
  3. 若 `ParsedOutputJson` / `SchemaJsonSnapshot` 非 null，再 `RedactJson`；否则保持 null。
  4. 拼输入串 `input = redactedMessages + redactedPayload`；输出串 `output = (redactedResponseText ?? "") + redactedResponseRaw`。
  5. new `AiRequestLogEntity`，拷贝业务字段；`Status = ToStorageStatus(model.Status)`；`DurationMs = (FinishedAt - StartedAt).TotalMilliseconds` 转 long。
  6. 写入脱敏后的 JSON/文本；`InputChars`/`OutputChars` 取串长；`InputHash`/`OutputHash` = `Sha256(...)`。
  7. `db.AiRequestLogs.Add(entity)`；`await db.SaveChangesAsync(ct)`；返回 `entity.Id`。
- 分支与异常：
  - 可选字段 null 时跳过对应 RedactJson 或明文用空串参与拼接。
  - 未知 `AiRequestStatus` 在 `ToStorageStatus` 落到 `"failed"`。
  - DB/取消异常向上抛出。
- 调用：
  - `AiRedactor.RedactJson` / `RedactPlainText`
  - `ToStorageStatus`、`Sha256`
  - `PimDbContext.AiRequestLogs.Add`、`SaveChangesAsync`

#### `private static string ToStorageStatus(AiRequestStatus status)`
- 输入：领域枚举状态
- 输出：库内字符串状态
- 副作用：无
- 步骤：
  1. switch：Succeeded→`succeeded`；Failed→`failed`；Blocked→`blocked`；TimedOut→`timed_out`；FailedValidation→`failed_validation`；其他→`failed`。
- 分支与异常：默认分支强制 `failed`
- 调用：无

#### `private static string Sha256(string value)`
- 输入：待哈希字符串
- 输出：小写十六进制 SHA-256
- 副作用：无
- 步骤：
  1. UTF-8 编码 → `SHA256.HashData` → `Convert.ToHexString` → `ToLowerInvariant`。
- 分支与异常：无
- 调用：`SHA256.HashData`、`Encoding.UTF8.GetBytes`、`Convert.ToHexString`

## 近逐行中文伪代码

1. 引入加密、文本、`Pim.Core.Ai`、`PimDbContext`、实体命名空间。
2. 命名空间 `Pim.Infrastructure.Ai`。
3. 定义密封记录 `AiRequestLogWriteModel`，字段含：UserId、Module、Purpose、SourceObjectType/Id、Provider、Model、LiteLlmRequestId、CorrelationId、Status、AttemptNumber、MaxAttempts、StartedAt、FinishedAt、RequestMessagesJson、RequestPayloadJson、ResponseRawJson、ResponseText、ParsedOutputJson、SchemaName/Version/JsonSnapshot、SchemaValidationErrorsJson、Prompt/Completion/TotalTokens、EstimatedCost、Currency、ErrorCode、ErrorMessage、MetadataJson。
4. 定义接口 `IAiRequestLogWriter`，方法 `WriteAsync(model, ct)` 返回 `Task<Guid>`。
5. 定义密封类 `AiRequestLogWriter(PimDbContext db)` 实现接口。
6. `WriteAsync` 开始：
7. 脱敏 messages / payload / responseRaw / metadata（JSON）。
8. 脱敏 responseText（明文）。
9. 若有 ParsedOutputJson 则脱敏，否则 null。
10. 若有 SchemaJsonSnapshot 则脱敏，否则 null。
11. 脱敏 SchemaValidationErrorsJson；脱敏 ErrorMessage。
12. `input = redactedMessages + redactedPayload`。
13. `output = (redactedResponseText 或空串) + redactedResponseRaw`。
14. 构造 `AiRequestLogEntity`：拷贝 UserId 至 Model 等标识字段。
15. Status 经 `ToStorageStatus`；Attempt/Max/Started/Finished 原样。
16. `DurationMs = (FinishedAt - StartedAt)` 的总毫秒 long。
17. 填入已脱敏的请求/响应/解析/Schema 相关字段。
18. SchemaName/Version 原样；Token 与费用字段原样。
19. InputChars/OutputChars = 输入/输出串长度。
20. InputHash/OutputHash = 对 input/output 做 Sha256。
21. ErrorCode 原样；ErrorMessage 用脱敏值；MetadataJson 用脱敏值。
22. `db.AiRequestLogs.Add(entity)`。
23. `await db.SaveChangesAsync(ct)`。
24. 返回 `entity.Id`。
25. `ToStorageStatus`：枚举映射到 snake/小写存储串，未知→`failed`。
26. `Sha256`：UTF-8 字节 → SHA256 → 小写 hex 字符串。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs",
      "label": "AiRequestLogWriter",
      "path": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Data/Entities", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Ai/AiRedactor.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs", "to": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "type": "tests" }
  ]
}
```
