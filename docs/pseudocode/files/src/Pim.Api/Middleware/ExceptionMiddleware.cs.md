# src/Pim.Api/Middleware/ExceptionMiddleware.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：全局异常中间件；将领域异常与未处理异常统一写成 `ApiResponse` JSON 响应。
- 主要依赖：`RequestDelegate`、`ILogger<ExceptionMiddleware>`、`DomainException`、`ApiResponse`、`CorrelationIdMiddleware`、`System.Text.Json`
- 被谁使用：`Program.cs` 注册 `UseMiddleware<ExceptionMiddleware>`；单元测试 `ExceptionMiddlewareTests`

## 函数级结构化伪代码

### ExceptionMiddleware
#### ExceptionMiddleware(RequestDelegate next, ILogger<ExceptionMiddleware> logger)
- 输入：下游管道委托、日志
- 输出：中间件实例
- 副作用：保存字段
- 步骤：赋值 `_next`、`_logger`
- 分支与异常：无
- 调用：无

#### Task InvokeAsync(HttpContext context)
- 输入：当前 HTTP 上下文
- 输出：异步完成（可能已写响应体）
- 副作用：可能改写 `StatusCode`/`ContentType` 并写出 JSON；未处理异常打 Error 日志
- 步骤：
  1. `try` 调用 `_next(context)`
  2. 捕获 `DomainException`：按 `ErrorCode` 映射 HTTP 状态；`ContentType=application/json`；序列化 `ApiResponse.Error(code, message)` 写出
  3. 捕获其它 `Exception`：从 `context.Items` 取关联 Id；`LogError`；状态 500；写出 `ApiResponse.Error(01001, "内部服务器错误")`
- 分支与异常：领域异常不记 Error 日志；通用异常隐藏内部细节
- 调用：`_next`、`ResolveDomainStatusCode`、`ApiResponse.Error`、`JsonSerializer.Serialize`、`WriteAsync`、`LogError`

#### static int ResolveDomainStatusCode(int errorCode)
- 输入：领域错误码
- 输出：HTTP 状态码 404 或 400
- 副作用：无
- 步骤：若 errorCode 属于 `{4004,4006,5104,5300,5304,5305}` → 404；否则 400
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、Pim.Api.Infrastructure、ApiResponse、DomainException
2. 命名空间 `Pim.Api.Middleware`
3. 类 `ExceptionMiddleware`：字段 `_next`、`_logger`
4. 构造注入 next 与 logger
5. `InvokeAsync`：try 执行下游
6. DomainException：状态码由 `ResolveDomainStatusCode` 决定；JSON 错误响应含 ErrorCode 与 Message
7. 其它异常：读 CorrelationId 记日志；固定 500 与业务码 01001「内部服务器错误」
8. `ResolveDomainStatusCode`：若干资源不存在类错误码映射 404，其余 400

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Middleware/ExceptionMiddleware.cs",
      "label": "ExceptionMiddleware",
      "path": "src/Pim.Api/Middleware/ExceptionMiddleware.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Middleware/ExceptionMiddleware.cs.md",
      "layer": "api",
      "kind": "middleware"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "type": "calls" },
    { "from": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "to": "src/Pim.Core/Exceptions", "type": "depends_on" },
    { "from": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "to": "src/Pim.Api/Infrastructure", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Api/ExceptionMiddlewareTests.cs", "to": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "type": "tests" }
  ]
}
```
