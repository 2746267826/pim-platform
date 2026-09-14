# src/Pim.Infrastructure/Ai/AiOptions.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：AI 网关/客户端配置选项（配置节 `Ai`），控制开关、提供商、限流与日志落库粒度。
- 主要依赖：无代码依赖；由 `IOptions<AiOptions>` / `IConfiguration` 绑定
- 被谁使用：`ServiceCollectionExtensions.Configure<AiOptions>`；`AiGateway`、`AiChatClientFactory`、`AiUsageService`、`AiProviderHealthService` 注入读取

## 函数级结构化伪代码

### AiOptions
#### 属性组（配置 POCO，无方法）
- 输入：配置系统从 `Ai` 节反序列化到属性
- 输出：运行时可读的选项实例
- 副作用：无（纯数据）；消费者据此决定是否调用外部 AI、超时与落库策略
- 步骤：
  1. `Enabled`：是否启用 AI 能力
  2. `Provider`：提供商标识，默认 `"litellm"`
  3. `BaseUrl`：API 基址，默认 `http://litellm:4000`
  4. `ApiKey`：访问密钥，默认空串
  5. `DefaultModel`：默认模型名，默认 `"pim-default"`
  6. `TimeoutSeconds`：单次请求超时秒数，默认 30
  7. `MaxOutputTokensPerRequest`：单请求最大输出 token，默认 1000
  8. `MaxAttemptsPerRequest`：单请求最大尝试次数，默认 2
  9. `SaveFullPrompts` / `SaveFullResponses`：是否完整持久化提示与响应，默认 true
- 分支与异常：无；非法配置由消费者或绑定校验处理
- 调用：无主动调用；被 AI 基础设施组件读取

## 近逐行中文伪代码

1. 命名空间：`Pim.Infrastructure.Ai`
2. 声明密封类 `AiOptions`
3. 属性 `Enabled`：布尔，AI 总开关
4. 属性 `Provider`：字符串，默认 litellm
5. 属性 `BaseUrl`：字符串，默认 litellm 容器地址
6. 属性 `ApiKey`：字符串，默认空
7. 属性 `DefaultModel`：字符串，默认 pim-default
8. 属性 `TimeoutSeconds`：整型，默认 30
9. 属性 `MaxOutputTokensPerRequest`：整型，默认 1000
10. 属性 `MaxAttemptsPerRequest`：整型，默认 2
11. 属性 `SaveFullPrompts`：布尔，默认 true，控制是否保存完整 prompt
12. 属性 `SaveFullResponses`：布尔，默认 true，控制是否保存完整响应

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiOptions.cs",
      "label": "AiOptions",
      "path": "src/Pim.Infrastructure/Ai/AiOptions.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiOptions.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" }
  ]
}
```
