# tests/Pim.UnitTests/Ai/AiGatewayTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 `AiGateway` 禁用/成功/校验重试/超时/调用方取消/服务商失败/持久化开关与 `AiChatClientFactory` 缓存并发。
- 主要依赖：`AiGateway`、`AiSchemaRegistry`、`AiRequestLogWriter`、`AiChatClientFactory`、`PimDbContext`、`Microsoft.Extensions.AI`
- 被谁使用：xUnit

## 函数级结构化伪代码

### AiGatewayTests
#### CompleteAsync_ReturnsBlockedAndDoesNotCallProvider_WhenAiDisabled
- 输入：enabled=false
- 输出：Blocked，不调 provider，日志 status=blocked
#### CompleteAsync_LogsSuccessWithTokenUsage
- 成功纯文本，TotalTokens=10，日志 succeeded
#### CompleteAsync_RetriesValidationOnceWithoutExpandingOriginalContext
- 首次坏 JSON、二次修复；重试消息含 Fix only JSON，不含原 user 文本；2 条日志
#### CompleteAsync_UsesConfiguredTimeoutAndLogsTimedOut
- waitForCancellation + 1s → TimedOut
#### CompleteAsync_RetriesTimeoutAndLogsEachAttempt_WhenRetrySucceeds
- 先超时后成功，日志 attempt 1/2
#### CompleteAsync_PropagatesCallerCancellationAndDoesNotLogAttempt
- 已取消 Token；抛 OperationCanceledException；WriteCount=0
#### CompleteAsync_LogsFailed_WhenProviderFactoryThrows
- factory 抛错 → Failed / provider_unavailable
#### CompleteAsync_RetriesProviderFailureAndLogsEachAttempt_WhenRetrySucceeds
- 先抛后成功，双日志
#### CompleteAsync_ClampsConfiguredMaxAttemptsToAtLeastOne
- MaxAttemptsPerRequest=0 → 实际 1 次
#### CompleteAsync_HonorsPromptAndResponsePersistenceSwitches
- saveFull* false → 日志清空 prompt/response
#### AiChatClientFactory_CachesClientByModel / Disposes... / ConcurrentCreate...
- 同模型缓存；Dispose 后拒绝 Create；并发只建一次
#### helpers
- BasicRequest / CreateDb / CreateGateway 重载 / Fixed/Throwing factory / FailingAiRequestLogWriter

### FakeChatClient / FakeChatClientStep / DisposableFakeChatClient / CountingAiChatClientFactory
- 队列步进：Respond / WaitUntilCanceled / Throw；记录 CallCount 与 Requests

## 近逐行中文伪代码

1. [L1-L12] using 与测试类
2. [L13-L26] 禁用 AI：Blocked、CallCount=0、中文错误、日志 blocked
3. [L28-L41] 成功与 token usage
4. [L43-L64] schema 校验重试，上下文不膨胀
5. [L66-L80] 超时日志
6. [L82-L115] 超时后重试成功，Collection 校验两日志
7. [L117-L132] 调用方取消不写日志
8. [L134-L151] factory 抛错失败日志
9. [L153-L187] provider 失败重试成功
10. [L189-L208] maxAttempts 下限钳制
11. [L210-L240] 持久化开关清空字段
12. [L242-L285] factory 缓存/Dispose/并发
13. [L287-L388] 请求与网关工厂辅助
14. [L390-L555] 测试替身与 FakeChatClient 实现

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs",
      "label": "AiGatewayTests",
      "path": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiGatewayTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs", "to": "src/Pim.Infrastructure/Ai/AiGateway.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs", "to": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs", "to": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs", "to": "src/Pim.Core/Ai", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Ai/AiGatewayTests.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" }
  ]
}
```
