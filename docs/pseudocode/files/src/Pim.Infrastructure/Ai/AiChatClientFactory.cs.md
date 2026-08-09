# src/Pim.Infrastructure/Ai/AiChatClientFactory.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：按模型名创建并缓存 `IChatClient`（OpenAI 兼容端点），提供可释放的工厂；支持测试覆盖 `CreateClientCore`。
- 主要依赖：`Microsoft.Extensions.AI`（`IChatClient`）、`Microsoft.Extensions.Options`（`IOptions<AiOptions>`）、`OpenAI`/`OpenAI.Chat`（`ChatClient`、`OpenAIClientOptions`）、`System.ClientModel`（`ApiKeyCredential`）；同程序集 `AiOptions`
- 被谁使用：DI 以 `IAiChatClientFactory` 单例注册；`Pim.Infrastructure.Ai.AiGateway` 调用 `Create` 获取客户端

## 函数级结构化伪代码

### IAiChatClientFactory
#### IChatClient Create(string model)
- 输入：`model` 模型标识
- 输出：对应模型的 `IChatClient` 实例
- 副作用：实现可能创建并缓存客户端
- 步骤：
  1. 由实现返回可用聊天客户端
- 分支与异常：契约不规定；实现可在已释放时抛出
- 调用：被 `AiGateway` 在发起 LLM 调用前调用

### AiChatClientFactory
#### AiChatClientFactory(IOptions<AiOptions> options)
- 输入：主构造参数 `options`（AI 配置）
- 输出：工厂实例
- 副作用：初始化锁、按序数字典缓存、未释放标志
- 步骤：
  1. 捕获 `options` 供后续创建客户端使用
  2. 初始化 `_lock`、`_clients`（`StringComparer.Ordinal`）、`_disposed = false`
- 分支与异常：无
- 调用：无

#### IChatClient Create(string model)
- 输入：`model` 模型名（缓存键）
- 输出：缓存或新创建的 `IChatClient`
- 副作用：可能向 `_clients` 新增条目
- 步骤：
  1. 进入 `_lock` 临界区
  2. 若已释放则抛 `ObjectDisposedException`
  3. 若 `_clients` 已有该 `model`，直接返回缓存实例
  4. 否则调用 `CreateClientCore(model)` 创建
  5. 将新客户端加入 `_clients` 并返回
- 分支与异常：已释放 → `ObjectDisposedException`；创建失败由 `CreateClientCore`/SDK 抛出
- 调用：`ObjectDisposedException.ThrowIf`；`CreateClientCore`

#### void Dispose()
- 输入：无
- 输出：无
- 副作用：标记已释放；释放可 `IDisposable` 的缓存客户端；清空字典
- 步骤：
  1. 进入 `_lock`
  2. 若 `_disposed` 已为真则直接返回（幂等）
  3. 置 `_disposed = true`
  4. 遍历 `_clients.Values`，对实现 `IDisposable` 的调用 `Dispose`
  5. 清空 `_clients`
- 分支与异常：二次 Dispose 无操作
- 调用：各客户端 `Dispose`（若适用）

#### protected virtual IChatClient CreateClientCore(string model)
- 输入：`model` 模型名
- 输出：新的 `IChatClient`（OpenAI ChatClient 适配）
- 副作用：构造网络客户端（底层可能持有 Http 资源）
- 步骤：
  1. 读取 `options.Value` 得到 `AiOptions`
  2. 用 `model`、`ApiKeyCredential(ai.ApiKey)`、`OpenAIClientOptions` 构造 `ChatClient`
  3. `Endpoint` 设为 `ai.BaseUrl` 去尾 `/` 后拼接 `"/v1"` 的 `Uri`
  4. 通过 `AsIChatClient()` 转为 `IChatClient` 并返回
- 分支与异常：`BaseUrl`/`ApiKey` 无效或 URI 非法时由构造抛出
- 调用：`ChatClient` 构造；`AsIChatClient` 扩展

## 近逐行中文伪代码

1. 引用 `Microsoft.Extensions.AI`、`Microsoft.Extensions.Options`、`OpenAI`、`OpenAI.Chat`、`System.ClientModel`
2. 命名空间：`Pim.Infrastructure.Ai`
3. 声明接口 `IAiChatClientFactory`，方法 `Create(model)` 返回 `IChatClient`
4. 声明类 `AiChatClientFactory`：主构造注入 `IOptions<AiOptions>`，实现 `IAiChatClientFactory` 与 `IDisposable`
5. 字段：`_lock` 同步对象；`_clients` 按序数字典缓存；`_disposed` 释放标志
6. 方法 `Create(model)`：
7.   - 加锁
8.   - 若已释放则 `ObjectDisposedException.ThrowIf`
9.   - 缓存命中则返回已有客户端
10.   - 否则 `CreateClientCore` 创建、加入字典并返回
11. 方法 `Dispose`：
12.   - 加锁；已释放则返回
13.   - 标记释放；遍历缓存，可释放则 `Dispose`；清空字典
14. 虚方法 `CreateClientCore(model)`：
15.   - 取 `AiOptions`
16.   - 构造 `ChatClient`：模型名、API Key 凭证、Endpoint = BaseUrl 去尾斜杠 + `/v1`
17.   - 返回 `chatClient.AsIChatClient()`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs",
      "label": "AiChatClientFactory",
      "path": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiChatClientFactory.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "to": "Microsoft.Extensions.AI", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "to": "OpenAI", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiChatClientFactory.cs", "type": "calls" }
  ]
}
```
