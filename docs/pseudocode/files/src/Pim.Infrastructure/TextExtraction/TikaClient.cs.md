# src/Pim.Infrastructure/TextExtraction/TikaClient.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：通过 HTTP PUT `/tika` 调用 Apache Tika 服务，从流或字节数组提取纯文本。
- 主要依赖：`HttpClient`、`System.Net.Http.Headers`
- 被谁使用：DI `AddHttpClient<TikaClient>`；`TikaFileTextExtractionService`（Files 模块）调用

## 函数级结构化伪代码

### TikaClient
#### TikaClient(HttpClient httpClient)
- 输入：注入的 HttpClient
- 输出：客户端实例
- 副作用：设置 Timeout=2 分钟
- 步骤：保存 `_httpClient`；`Timeout = TimeSpan.FromMinutes(2)`
- 分支与异常：无
- 调用：无

#### Task<string> ExtractTextAsync(Stream fileStream, string fileName, CancellationToken ct = default)
- 输入：文件流、文件名（当前未用于请求体）、取消令牌
- 输出：Tika 返回的文本字符串
- 副作用：HTTP PUT 到 `/tika`；非成功状态抛异常
- 步骤：
  1. 用 `StreamContent` 包装流；Content-Type=`application/octet-stream`
  2. `PutAsync("/tika", content, ct)`
  3. `EnsureSuccessStatusCode`
  4. `ReadAsStringAsync`
- 分支与异常：HTTP 失败 → `HttpRequestException`（EnsureSuccess）；超时/取消按 HttpClient 行为
- 调用：`HttpClient.PutAsync`

#### Task<string> ExtractTextAsync(byte[] fileBytes, string fileName, CancellationToken ct = default)
- 输入：文件字节、文件名、取消令牌
- 输出：提取文本
- 副作用：同流重载
- 步骤：`MemoryStream(fileBytes)` 后委托流重载
- 分支与异常：同流重载
- 调用：`ExtractTextAsync(Stream, ...)`

## 近逐行中文伪代码

1. 引入 `System.Net.Http.Headers`
2. 命名空间 `Pim.Infrastructure.TextExtraction`
3. 类 `TikaClient` 持有 `_httpClient`
4. 构造：注入 HttpClient；超时 2 分钟
5. 流重载：StreamContent + octet-stream；PUT `/tika`；EnsureSuccess；读字符串
6. 字节重载：MemoryStream 包装后调用流重载
7. 注意：`fileName` 参数目前未写入请求头/路径

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/TextExtraction/TikaClient.cs",
      "label": "TikaClient",
      "path": "src/Pim.Infrastructure/TextExtraction/TikaClient.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/TextExtraction/TikaClient.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/TextExtraction/TikaClient.cs", "to": "System.Net.Http.HttpClient", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/TextExtraction/TikaClient.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/Pim.Infrastructure/TextExtraction/TikaClient.cs", "type": "calls" }
  ]
}
```
