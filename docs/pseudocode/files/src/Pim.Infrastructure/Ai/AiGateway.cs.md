# src/Pim.Infrastructure/Ai/AiGateway.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：实现 `IAiGateway`：带重试、超时、Schema 校验与修复轮次、请求日志的 AI 补全网关。
- 主要依赖：`IOptions<AiOptions>`、`IAiChatClientFactory`、`IAiSchemaRegistry`、`IAiRequestLogWriter`、`AiSchemaValidator`、`Microsoft.Extensions.AI`、`System.Text.Json`
- 被谁使用：DI 注册为 `IAiGateway`；`AiEndpoints`、`FileAiService` 等调用 `CompleteAsync`

## 函数级结构化伪代码

### AiGateway
#### Task<AiResult> CompleteAsync(AiGatewayRequest request, CancellationToken ct = default)
- 输入：网关请求（消息、模型、Schema 名/版本、尝试次数、输出 token、元数据等）；取消令牌
- 输出：`AiResult`（状态、文本、解析 JSON、校验错误、用量、日志 Id、错误消息）
- 副作用：调用外部 AI 服务商；写入 AI 请求日志；可能多次尝试
- 步骤：
  1. 读取 `AiOptions`；解析模型、有效最大尝试次数、生成 `correlationId`
  2. 若 AI 禁用：写 Blocked 日志并返回“AI 已禁用”
  3. `ResolveSchema`；将消息转为 `ChatMessage`（有 Schema 时插入“只返回 JSON”系统提示）
  4. 循环 `attempt = 1..maxAttempts`：
     a. 计时；`CallProviderAsync`
     b. 超时/失败：写日志；若还有重试则 continue，否则返回 TimedOut/Failed
     c. 提取文本与 token 用量
     d. 有 Schema：校验；失败则写 FailedValidation 日志，构造修复消息重试，耗尽则 `FailedValidation`
     e. 校验通过：写 Succeeded 日志，返回成功（含解析 JSON）
     f. 无 Schema：写 Succeeded 日志，返回纯文本成功
  5. 循环意外结束：返回 `FailedValidation(lastLogId, lastValidationErrors)`
- 分支与异常：
  - 禁用 → Blocked
  - Schema 未注册 → `InvalidOperationException`
  - 提供商返回成功但无 Response → `InvalidOperationException`
  - 调用方取消 → 向上抛出 `OperationCanceledException`
- 调用：`ResolveSchema`、`ToChatMessages`、`CallProviderAsync`、`WriteLogAsync`/`logWriter.WriteAsync`、`AiSchemaValidator.Validate`、`CreateRepairMessages`、`ExtractUsage`、`CreateLogModel`

#### AiSchemaDefinition? ResolveSchema(AiGatewayRequest request)
- 输入：请求中的 Schema 名/版本
- 输出：Schema 定义或 `null`（未要求 Schema）
- 副作用：无
- 步骤：
  1. 名或版本为空 → `null`
  2. `schemaRegistry.Get`；未注册 → 抛 `InvalidOperationException`
- 分支与异常：未注册抛异常
- 调用：`IAiSchemaRegistry.Get`

#### IReadOnlyList<ChatMessage> ToChatMessages(IReadOnlyList<AiMessage> messages, bool structured)
- 输入：领域消息列表；是否结构化输出
- 输出：`ChatMessage` 列表
- 副作用：无
- 步骤：
  1. 角色映射并转换内容
  2. 若 structured，在头部插入系统提示要求仅返回 JSON
- 分支与异常：无
- 调用：`ToChatRole`

#### ChatRole ToChatRole(AiMessageRole role)
- 输入：领域角色枚举
- 输出：`ChatRole`
- 副作用：无
- 步骤：System/Assistant 映射，其余当 User
- 分支与异常：默认 User
- 调用：无

#### IReadOnlyList<ChatMessage> CreateRepairMessages(string failedJson, IReadOnlyList<string> errors, string schemaJson)
- 输入：失败 JSON、错误列表、Schema JSON
- 输出：修复轮次消息（系统指令 + 用户载荷）
- 副作用：无
- 步骤：构造“只修 JSON”系统消息与含 failedJson/errors/schema 的用户消息
- 分支与异常：无
- 调用：`JsonSerializer.Serialize`

#### AiTokenUsage ExtractUsage(ChatResponse response)
- 输入：提供商响应
- 输出：`AiTokenUsage`（prompt/completion/total；cost/currency 为 null）
- 副作用：无
- 步骤：从 `response.Usage` 取 token 计数并 `ToNullableInt`
- 分支与异常：Usage 为空时字段为 null
- 调用：`ToNullableInt`

#### int? ToNullableInt(long? value)
- 输入：可空 long
- 输出：可空 int（超 `int.MaxValue` 钳制为 MaxValue）
- 副作用：无
- 步骤：null → null；否则钳制转换
- 分支与异常：无
- 调用：无

#### Task<ProviderCallResult> CallProviderAsync(string model, IReadOnlyList<ChatMessage> messages, int maxOutputTokens, int timeoutSeconds, CancellationToken ct)
- 输入：模型、消息、最大输出 token、超时秒、取消令牌
- 输出：`ProviderCallResult`（Succeeded/TimedOut/Failed）
- 副作用：创建聊天客户端并调用提供商
- 步骤：
  1. 建超时 CTS 与链接 CTS
  2. `chatClientFactory.Create(model)` → `GetResponseAsync`
  3. 成功包装 Succeeded
  4. 调用方取消 → 重抛
  5. 超时取消 / TimeoutException → TimedOut
  6. 其他异常 → Failed
- 分支与异常：见上
- 调用：`IAiChatClientFactory.Create`、`IChatClient.GetResponseAsync`

#### AiRequestLogWriteModel CreateLogModel(...)
- 输入：请求上下文、状态、尝试次数、起止时间、消息/载荷/原始/文本/解析 JSON、Schema、校验错误、用量、错误码消息
- 输出：日志写入模型
- 副作用：无（仅组装；持久化字段受 SaveFullPrompts/SaveFullResponses 开关裁剪）
- 步骤：从 options 与 request 填充；序列化 validationErrors 与 metadata；应用 Persist* 策略
- 分支与异常：无
- 调用：`PersistMessagesJson` 等

#### Task<Guid> WriteLogAsync(...)
- 输入：简化版日志字段（无起止时间差、无用量与校验错误列表）
- 输出：写入后的日志 Id
- 副作用：调用 `logWriter.WriteAsync`
- 步骤：用当前 UTC 作为 start/finish；校验错误写 `"[]"`；token 全 null；应用 Persist* 策略
- 分支与异常：委托给 logWriter
- 调用：`IAiRequestLogWriter.WriteAsync`

#### string PersistMessagesJson / PersistResponseRawJson / PersistResponseText / PersistParsedJson
- 输入：内容字符串与 `AiOptions`
- 输出：按开关保留全文或占位（`[]`/`{}`/null）
- 副作用：无
- 步骤：读 `SaveFullPrompts` 或 `SaveFullResponses`
- 分支与异常：无
- 调用：无

### ProviderCallResult（私有 record）
#### Succeeded / TimedOut / Failed 工厂
- 输入：响应或错误消息
- 输出：带状态的结果包装
- 副作用：无
- 步骤：构造对应 Status
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 JSON、MEAI、Options、`Pim.Core.Ai`
2. 命名空间 `Pim.Infrastructure.Ai`
3. 密封类 `AiGateway` 主构造注入 options、chatClientFactory、schemaRegistry、logWriter，实现 `IAiGateway`
4. `CompleteAsync`：取 options；模型默认值；最大尝试次数钳制到 `[1, MaxAttemptsPerRequest]`；生成 correlationId
5. 若未启用 AI：`WriteLogAsync` 记 Blocked，返回禁用结果
6. 解析 Schema；消息转 ChatMessage；初始化 lastLogId 与 lastValidationErrors
7. for 循环尝试：
8.   记录 started；确定 maxOutputTokens；调用提供商
9.   TimedOut：写日志；可重试则 continue，否则返回超时结果
10.  Failed：写日志；可重试则 continue，否则返回失败结果
11.  取 response（无则抛）；记 finished；取 text/usage/rawJson/payloadJson
12.  若有 schema：`AiSchemaValidator.Validate`
13.    无效：记 FailedValidation；可重试则换修复消息 continue；否则 `AiResult.FailedValidation`
14.    有效：写 Succeeded 日志并返回含 parsed JSON 的成功结果
15.  无 schema：写 Succeeded 日志并返回纯文本成功
16. 循环后兜底 `FailedValidation`
17. `ResolveSchema`：无名称/版本返回 null；Get 不到抛未注册
18. `ToChatMessages`：映射角色；结构化时插入系统 JSON 指令
19. `ToChatRole`：System/Assistant/默认 User
20. `CreateRepairMessages`：系统“只修 JSON”+ 用户序列化失败上下文
21. `ExtractUsage` / `ToNullableInt`：提取并钳制 token
22. `CallProviderAsync`：超时链接取消；成功/超时/失败分支
23. `CreateLogModel`：组装完整日志模型并按开关裁剪敏感/大体量字段
24. `WriteLogAsync`：即时起止时间的简化写日志路径
25. Persist* 四个方法：按 SaveFullPrompts/SaveFullResponses 决定是否落全文
26. 私有 `ProviderCallResult` 及 Succeeded/TimedOut/Failed 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiGateway.cs",
      "label": "AiGateway",
      "path": "src/Pim.Infrastructure/Ai/AiGateway.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiGateway.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Core/Ai/IAiSchemaRegistry.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaValidator.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Core/Ai", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Infrastructure/Ai/AiGateway.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Infrastructure/Ai/AiGateway.cs", "type": "calls" }
  ]
}
```
