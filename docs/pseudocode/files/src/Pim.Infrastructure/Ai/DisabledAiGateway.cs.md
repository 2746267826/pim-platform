# src/Pim.Infrastructure/Ai/DisabledAiGateway.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：`IAiGateway` 的禁用/未配置替身：任意补全请求立即返回 `Blocked`，不调用外部 AI。
- 主要依赖：`Pim.Core.Ai`（`IAiGateway`、`AiGatewayRequest`、`AiResult`、`AiRequestStatus`、`AiTokenUsage`）
- 被谁使用：可作为未配置 AI 时的网关实现；当前 DI 默认注册为 `AiGateway`，本类供显式替换或测试替身

## 函数级结构化伪代码

### DisabledAiGateway
#### Task<AiResult> CompleteAsync(AiGatewayRequest request, CancellationToken ct = default)
- 输入：`AiGatewayRequest`（忽略内容）；可选 `CancellationToken`（未使用）
- 输出：已完成的 `Task<AiResult>`，状态固定 `AiRequestStatus.Blocked`
- 副作用：生成新的 `Guid` 作为日志/关联 Id；不访问网络、不写库
- 步骤：
  1. 构造 `AiResult`：`Blocked`、文本/解析 JSON 为 null、校验错误空列表、全 null 的 `AiTokenUsage`、新 `Guid`、消息 `"AI gateway is not configured."`
  2. `Task.FromResult` 包装并返回
- 分支与异常：无分支；不抛异常；不检查 `ct`
- 调用：`Task.FromResult`、`Guid.NewGuid`

## 近逐行中文伪代码

1. 引入 `Pim.Core.Ai`
2. 命名空间 `Pim.Infrastructure.Ai`
3. 密封类 `DisabledAiGateway` 实现 `IAiGateway`
4. 方法 `CompleteAsync`：表达式体
5. 用 `Task.FromResult` 同步完成
6. 新建 `AiResult`：状态 `Blocked`
7. 第二/三参为 null（无响应文本、无解析 JSON）
8. 第四参空集合（无校验错误）
9. 第五参 `AiTokenUsage` 五个字段均为 null
10. 第六参 `Guid.NewGuid()` 作为请求日志/关联 Id
11. 第七参固定中文说明：网关未配置
12. （文件结束；无字段、无构造注入）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs",
      "label": "DisabledAiGateway",
      "path": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/DisabledAiGateway.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs", "to": "src/Pim.Core/Ai/IAiGateway.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" }
  ]
}
```
