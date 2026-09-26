# src/Pim.Core/Ai/AiEnums.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义 AI 消息角色与请求结果状态枚举，供 DTO、网关与日志/用量层共享
- 主要依赖：`System.Text.Json.Serialization`（`JsonStringEnumConverter`）
- 被谁使用：`AiDtos`（`AiMessage`/`AiResult` 等）、`AiGateway`、`DisabledAiGateway`、`AiRequestLogWriter`、`AiUsageService`

## 函数级结构化伪代码

### AiMessageRole
#### enum AiMessageRole
- 输入：无（枚举类型定义）
- 输出：消息角色取值：`System` | `User` | `Assistant`
- 副作用：无
- 步骤：
  1. 声明对话消息的三种标准角色
- 分支与异常：无
- 调用：被 `AiMessage.Role`、`AiGateway.ToChatRole` 等消费

### AiRequestStatus
#### enum AiRequestStatus
- 输入：无（枚举类型定义）
- 输出：请求终态：`Succeeded` | `Failed` | `Blocked` | `TimedOut` | `FailedValidation`
- 副作用：无
- 步骤：
  1. 声明 AI 调用结果状态集合
  2. 通过 `[JsonConverter(typeof(JsonStringEnumConverter))]` 以字符串形式序列化/反序列化
- 分支与异常：无
- 调用：被 `AiResult.Status`、网关结果构造、日志/用量状态映射消费

## 近逐行中文伪代码

1. 引用 `System.Text.Json.Serialization`
2. 声明命名空间 `Pim.Core.Ai`
3. 定义公共枚举 `AiMessageRole`
4.   - 成员 `System`：系统提示角色
5.   - 成员 `User`：用户消息角色
6.   - 成员 `Assistant`：助手回复角色
7. 在 `AiRequestStatus` 上标注 `JsonConverter(JsonStringEnumConverter)`，JSON 中写字符串而非数字
8. 定义公共枚举 `AiRequestStatus`
9.   - `Succeeded`：调用成功
10.   - `Failed`：调用失败（服务商/网络等）
11.   - `Blocked`：被策略阻止或 AI 禁用
12.   - `TimedOut`：超时
13.   - `FailedValidation`：Schema/格式校验失败
14. 文件结束（无方法体、无字段）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Ai/AiEnums.cs",
      "label": "AiEnums",
      "path": "src/Pim.Core/Ai/AiEnums.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Ai/AiEnums.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Ai/AiDtos.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/DisabledAiGateway.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" }
  ]
}
```
