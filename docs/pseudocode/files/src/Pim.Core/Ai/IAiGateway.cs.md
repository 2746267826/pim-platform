# src/Pim.Core/Ai/IAiGateway.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义 AI 网关统一完成接口，将模块/API 与具体 LLM 提供方实现解耦。
- 主要依赖：同层 `AiGatewayRequest`、`AiResult`（`AiDtos.cs`）
- 被谁使用：`Pim.Infrastructure.Ai.AiGateway`、`DisabledAiGateway` 实现；`Pim.Api.Endpoints.AiEndpoints` 与 `Pim.Module.Files.Services.FileAiService` 注入调用；DI 在 `ServiceCollectionExtensions` 注册

## 函数级结构化伪代码

### IAiGateway
#### Task<AiResult> CompleteAsync(AiGatewayRequest request, CancellationToken ct = default)
- 输入：`request` 网关请求（模块、用途、消息、可选模型/Schema/重试等）；`ct` 取消令牌
- 输出：`Task<AiResult>` 一次 AI 完成调用的结果（状态、文本、解析 JSON、用量、日志 Id 等）
- 副作用：无（契约层）；实现侧可能写请求日志、调用外部 LLM、更新健康状态
- 步骤：
  1. 由实现按 `request` 选择模型与客户端
  2. 向 LLM 发起补全/聊天完成
  3. 可选 Schema 校验与重试
  4. 组装并返回 `AiResult`
- 分支与异常：契约不规定；实现可在禁用、超时、校验失败、提供方错误时返回失败态 `AiResult` 或抛出
- 调用：被 `AiEndpoints` 测试接口、`FileAiService` 等业务服务调用

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Ai`
2. 声明公开接口 `IAiGateway`
3. 方法 `CompleteAsync`：
4.   - 参数 `request` 类型 `AiGatewayRequest`
5.   - 可选参数 `ct` 类型 `CancellationToken`，默认 `default`
6.   - 返回 `Task<AiResult>`
7. 接口无默认实现体，由基础设施层提供

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Ai/IAiGateway.cs",
      "label": "IAiGateway",
      "path": "src/Pim.Core/Ai/IAiGateway.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Ai/IAiGateway.cs.md",
      "layer": "core",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Ai/IAiGateway.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "calls" }
  ]
}
```
