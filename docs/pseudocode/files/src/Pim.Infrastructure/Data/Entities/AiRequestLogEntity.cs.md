# src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 实体，映射表 `ai_request_logs`，持久化单次 AI 网关请求的上下文、请求/响应 JSON、Schema 校验、Token/费用与错误信息。
- 主要依赖：
  - `System.ComponentModel.DataAnnotations`（`Key`/`MaxLength`）
  - `System.ComponentModel.DataAnnotations.Schema`（`Table`/`Column`）
- 被谁使用：
  - `PimDbContext.AiRequestLogs`
  - `AiRequestLogWriter` 写入
  - `AiUsageService` 查询统计
  - 迁移/快照中的 `AiRequestLogEntity` 模型配置
  - 单元测试 `AiRequestLogWriterTests`、`AiPersistenceModelTests`、`AiUsageServiceTests`

## 函数级结构化伪代码

### AiRequestLogEntity
#### 属性集合（无自定义方法；密封 POCO）
- 输入：由写入器/测试赋值
- 输出：可被 EF 跟踪的行状态
- 副作用：无运行时逻辑；列映射决定落库形状
- 步骤：
  1. 主键 `Id`（Guid，默认 `NewGuid`）→ 列 `id`。
  2. 调用上下文：`UserId`、`Module`、`Purpose`、`SourceObjectType`/`SourceObjectId`。
  3. 提供商：`Provider`（默认 `"litellm"`）、`Model`、`LiteLlmRequestId`、`CorrelationId`。
  4. 执行状态：`Status`、`AttemptNumber`/`MaxAttempts`、`StartedAt`/`FinishedAt`/`DurationMs`。
  5. 载荷：`RequestMessagesJson`/`RequestPayloadJson`/`ResponseRawJson`（jsonb）、`ResponseText`、`ParsedOutputJson`。
  6. Schema：`SchemaName`/`SchemaVersion`/`SchemaJsonSnapshot`/`SchemaValidationErrorsJson`。
  7. 用量：`PromptTokens`/`CompletionTokens`/`TotalTokens`、`EstimatedCost`/`Currency`、`InputChars`/`OutputChars`、`InputHash`/`OutputHash`。
  8. 错误与扩展：`ErrorCode`/`ErrorMessage`、`MetadataJson`。
- 分支与异常：无；长度约束由 `[MaxLength]` 与 DB 配置共同约束。
- 调用：无方法调用；仅被 ORM 与业务写入/查询使用。

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema 命名空间。
2. 命名空间 `Pim.Infrastructure.Data.Entities`。
3. `[Table("ai_request_logs")]` 标注密封类 `AiRequestLogEntity`。
4. `Id`：主键 Guid，列 `id`，默认新 Guid。
5. `UserId`：可空 Guid，列 `user_id`。
6. `Module`/`Purpose`：字符串默认空，MaxLength 128。
7. `SourceObjectType` MaxLength 128；`SourceObjectId` MaxLength 256。
8. `Provider` 默认 `"litellm"`，MaxLength 32；`Model` MaxLength 128。
9. `LiteLlmRequestId` 可空 MaxLength 128；`CorrelationId` 必填语义字符串 MaxLength 128。
10. `Status` MaxLength 32；`AttemptNumber`/`MaxAttempts` 整型。
11. `StartedAt` 默认 UtcNow；`FinishedAt`/`DurationMs` 可空。
12. `RequestMessagesJson` jsonb 默认 `"[]"`；`RequestPayloadJson`/`ResponseRawJson` jsonb 默认 `"{}"`。
13. `ResponseText`/`ParsedOutputJson` 可空；后者 jsonb。
14. `SchemaName`/`SchemaVersion` 可空；`SchemaJsonSnapshot` jsonb 可空。
15. `SchemaValidationErrorsJson` jsonb 默认 `"[]"`。
16. Token 三字段与 `EstimatedCost`/`Currency` 可空。
17. `InputChars`/`OutputChars` 整型；`InputHash`/`OutputHash` MaxLength 128。
18. `ErrorCode` 可空 MaxLength 128；`ErrorMessage` 可空文本。
19. `MetadataJson` jsonb 默认 `"{}"`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs",
      "label": "AiRequestLogEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiPersistenceModelTests.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiRequestLogEntity.cs", "type": "tests" }
  ]
}
```
