# src/Pim.Infrastructure/Ai/AiProviderHealthService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：探测 LiteLLM 提供商健康状态，将配置与检查结果持久化到 `ai_provider_settings`。
- 主要依赖：`PimDbContext`、`AiOptions`、`IHttpClientFactory`、`AiProviderSettingEntity`、EF Core、`System.Text.Json`
- 被谁使用：DI 注册于 `ServiceCollectionExtensions`；`AiEndpoints` 的 `/health-check` 调用 `IAiProviderHealthService.CheckAsync`

## 函数级结构化伪代码

### IAiProviderHealthService
#### Task CheckAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：无（Task）
- 副作用：更新/插入提供商设置行
- 步骤：
  1. 由实现执行健康检查并落库
- 分支与异常：契约不规定
- 调用：API 健康检查端点

### AiProviderHealthService
#### 构造函数（主构造）
- 输入：`PimDbContext db`、`IOptions<AiOptions> options`、`IHttpClientFactory httpClientFactory`
- 输出：服务实例
- 副作用：无
- 步骤：
  1. 捕获依赖供 `CheckAsync` 使用
- 分支与异常：无
- 调用：无

#### Task CheckAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：无
- 副作用：读写 `AiProviderSettings`；可能对 LiteLLM 发 HTTP GET
- 步骤：
  1. 读取 `AiOptions`
  2. 查询 Provider=`"litellm"` 的设置行；不存在则新建内存实体
  3. 同步 `BaseUrl`、`DefaultModel`、状态（enabled/disabled）、`LastHealthCheckAt`、`UpdatedAt`
  4. 若未启用 AI：清空 `LastError`，必要时 Add，SaveChanges，返回
  5. 若已启用：创建命名客户端 `litellm-health`，GET `{BaseUrl}/v1/models`，Bearer `ApiKey`
  6. 要求成功状态码；解析响应，校验默认模型是否出现在 `data[].id`
  7. 模型缺失：Status=`error`，写入 LastError，保存并返回
  8. 成功：LastError=null
  9. 捕获取消：若 `ct` 已请求取消则重新抛出
  10. 其它异常：Status=`error`，LastError=异常消息
  11. AddIfNeeded 后 SaveChanges
- 分支与异常：
  - AI 关闭：不访问网络
  - 模型列表不含默认模型：error 状态
  - 网络/HTTP/解析失败：error 状态
  - 调用方取消：传播 `OperationCanceledException`
- 调用：`db.AiProviderSettings`、`httpClientFactory.CreateClient`、`ContainsDefaultModel`、`AddIfNeeded`、`SaveChangesAsync`

#### static bool ContainsDefaultModel(string modelsJson, string defaultModel)
- 输入：`/v1/models` 响应 JSON；配置的默认模型 Id
- 输出：是否在 `data` 数组中找到匹配 `id`
- 副作用：无（解析临时 JsonDocument）
- 步骤：
  1. `JsonDocument.Parse` 响应
  2. 取根属性 `data`，须为数组
  3. 遍历元素，比较 `id` 与 `defaultModel`（Ordinal）
  4. 命中返回 true，否则 false
- 分支与异常：无 `data` 或非数组 → false；JSON 非法由调用方 catch
- 调用：`JsonDocument` / `JsonElement`

#### void AddIfNeeded(AiProviderSettingEntity settings)
- 输入：设置实体
- 输出：无
- 副作用：可能 `db.AiProviderSettings.Add`
- 步骤：
  1. 若 `Id` 为空 Guid 或 EF 状态为 Detached，则加入 DbSet
- 分支与异常：无
- 调用：EF `Entry`/`Add`

## 近逐行中文伪代码

1. 引用：EF Core、`IOptions`、`PimDbContext`、实体命名空间、`System.Text.Json`
2. 命名空间：`Pim.Infrastructure.Ai`
3. 定义接口 `IAiProviderHealthService`，方法 `CheckAsync(ct)`
4. 定义密封类 `AiProviderHealthService`，主构造注入 `db`、`options`、`httpClientFactory`，实现接口
5. `CheckAsync` 开始：
6.   取 `ai = options.Value`
7.   查询 `AiProviderSettings` 中 Provider 为 `"litellm"` 的单行；没有则 `new AiProviderSettingEntity { Provider = "litellm" }`
8.   写入 BaseUrl、DefaultModel
9.   Status = ai.Enabled ? `"enabled"` : `"disabled"`
10.  LastHealthCheckAt / UpdatedAt = UtcNow
11.  若未启用：LastError=null → AddIfNeeded → SaveChanges → return
12.  try：
13.    创建 HttpClient 名 `"litellm-health"`
14.    GET `BaseUrl` 去尾斜杠 + `"/v1/models"`
15.    Authorization: Bearer `ai.ApiKey`
16.    SendAsync；EnsureSuccessStatusCode
17.    读响应字符串
18.    若 `ContainsDefaultModel` 为 false：
19.      Status=`"error"`，LastError 说明默认模型未返回
20.      AddIfNeeded + SaveChanges + return
21.    否则 LastError=null
22.  catch OperationCanceledException 且 ct 已取消：rethrow
23.  catch 其它 Exception：Status=`"error"`，LastError=ex.Message
24.  AddIfNeeded + SaveChanges
25. `ContainsDefaultModel`：
26.  解析 JSON；无 `data` 数组则 false
27.  遍历 `data`，`id` 与 defaultModel 序数相等则 true
28.  否则 false
29. `AddIfNeeded`：
30.  Id 为空或 Entry 为 Detached 时 `Add(settings)`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs",
      "label": "AiProviderHealthService",
      "path": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiProviderHealthService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "src/Pim.Infrastructure/Data/Entities/AiProviderSettingEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "IAiProviderHealthService", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "to": "litellm:/v1/models", "type": "http" }
  ]
}
```
