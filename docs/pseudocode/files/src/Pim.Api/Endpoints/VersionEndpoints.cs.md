# src/Pim.Api/Endpoints/VersionEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册匿名 `GET /api/version`，返回程序集 InformationalVersion 与能力列表（当前含 `mobileItemResultsV1`）。
- 主要依赖：`typeof(Program).Assembly`、`AssemblyInformationalVersionAttribute`、Minimal API `Results`
- 被谁使用：`Program.cs` 映射版本端点；移动端/客户端探测能力

## 函数级结构化伪代码

### ApiVersionResponse
#### `record ApiVersionResponse(string Version, IReadOnlyList<string> Capabilities)`
- 输入：版本字符串、能力标识列表
- 输出：响应 DTO
- 副作用：无
- 步骤：
  1. 承载 API 版本与能力集合
- 分支与异常：无
- 调用：无

### VersionEndpoints
#### 常量 `MobileItemResultsV1` / 属性 `Capabilities`
- 输入：无
- 输出：能力名 `"mobileItemResultsV1"`；只读列表含该能力
- 副作用：无
- 步骤：
  1. 定义能力常量
  2. 静态集合初始化为单元素列表
- 分支与异常：无
- 调用：无

#### `static IEndpointRouteBuilder MapVersionEndpoints(this IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：同一 `endpoints`（链式）
- 副作用：注册 `GET /api/version` 且 `AllowAnonymous`
- 步骤：
  1. `MapGet("/api/version", handler)`
  2. handler：反射 `Program` 程序集自定义属性，取 `AssemblyInformationalVersionAttribute.InformationalVersion`，缺省 `"0.0.0(unknown)"`
  3. `Results.Ok(new ApiVersionResponse(version, Capabilities))`
  4. 返回 `endpoints`
- 分支与异常：无属性时用默认版本串
- 调用：反射 `GetCustomAttributes`/`OfType`/`FirstOrDefault`、`Results.Ok`

## 近逐行中文伪代码

1. 命名空间 `Pim.Api.Endpoints`
2. 记录类型 `ApiVersionResponse(Version, Capabilities)`
3. 静态类 `VersionEndpoints`：常量 `MobileItemResultsV1 = "mobileItemResultsV1"`
4. 静态只读 `Capabilities = [MobileItemResultsV1]`
5. 扩展方法 `MapVersionEndpoints`：映射 `GET /api/version`
6. 处理器：从 `typeof(Program).Assembly` 取 InformationalVersion，否则 `"0.0.0(unknown)"`
7. 返回 200 + `ApiVersionResponse`；路由允许匿名
8. 返回 `endpoints`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/VersionEndpoints.cs",
      "label": "VersionEndpoints",
      "path": "src/Pim.Api/Endpoints/VersionEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/VersionEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/VersionEndpoints.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/VersionEndpoints.cs", "to": "src/Pim.Api/Program.cs", "type": "depends_on" }
  ]
}
```
