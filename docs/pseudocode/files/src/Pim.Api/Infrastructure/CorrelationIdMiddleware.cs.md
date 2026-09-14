# src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：HTTP 中间件：解析/生成 `X-Correlation-Id`，写入 `HttpContext.Items` 与响应头，并推入 Serilog `LogContext`。
- 主要依赖：`RequestDelegate`、`HttpContext`、`Serilog.Context.LogContext`
- 被谁使用：`Program.cs` 的 `UseMiddleware`；`ExceptionMiddleware` 读 `HeaderName`；单元测试 `CorrelationIdMiddlewareTests`

## 函数级结构化伪代码

### CorrelationIdMiddleware
#### CorrelationIdMiddleware(RequestDelegate next)
- 输入：下游管道委托
- 输出：中间件实例
- 副作用：保存 `_next`
- 步骤：1. 赋值 `_next = next`
- 分支与异常：无
- 调用：无

#### Task InvokeAsync(HttpContext context)
- 输入：当前 HTTP 上下文
- 输出：异步完成下游
- 副作用：写 `Items`/`Response.Headers`；PushProperty 作用域内执行下游
- 步骤：
  1. 若请求头有 `X-Correlation-Id` → `ResolveCorrelationId(首值)`，否则 `GenerateCorrelationId`
  2. `context.Items[HeaderName] = correlationId`
  3. `context.Response.Headers[HeaderName] = correlationId`
  4. `using LogContext.PushProperty("CorrelationId", correlationId)` 内 `await _next(context)`
- 分支与异常：无效入站 Id 在 Resolve 时被替换为新 Guid
- 调用：`ResolveCorrelationId`、`GenerateCorrelationId`、`LogContext.PushProperty`、`_next`

#### static string ResolveCorrelationId(string? value)
- 输入：可选原始字符串
- 输出：合法关联 Id（入站合法则原样，否则新生成）
- 副作用：无
- 步骤：Trim；`IsValidCorrelationId` 为真则返回，否则 `GenerateCorrelationId`
- 分支与异常：空/超长/非法字符 → 生成新 Id
- 调用：`IsValidCorrelationId`、`GenerateCorrelationId`

#### private static bool IsValidCorrelationId(string? value)
- 输入：候选字符串
- 输出：是否合法
- 副作用：无
- 步骤：
  1. 空/空白或长度 > 128 → false
  2. 逐字符：仅允许 ASCII 字母数字及 `-` `_` `.` `:`
- 分支与异常：任一非法字符 → false
- 调用：`char.IsAsciiLetterOrDigit`

#### private static string GenerateCorrelationId()
- 输入：无
- 输出：`Guid.NewGuid().ToString("N")`（32 位 hex，无连字符）
- 副作用：无
- 步骤：生成 Guid N 格式
- 分支与异常：无
- 调用：`Guid.NewGuid`

## 近逐行中文伪代码

1. 引入 `Serilog.Context`；命名空间 `Pim.Api.Infrastructure`
2. 类 `CorrelationIdMiddleware`：常量 `HeaderName = "X-Correlation-Id"`、`MaxCorrelationIdLength = 128`
3. 字段 `_next`；构造注入 `RequestDelegate`
4. `InvokeAsync`：从请求头取关联 Id 或生成；写入 Items 与响应头
5. 在 `LogContext.PushProperty("CorrelationId", ...)` 作用域内调用下游
6. `ResolveCorrelationId`：Trim 后校验，非法则生成
7. `IsValidCorrelationId`：非空、≤128、仅 [A-Za-z0-9-_.:]
8. `GenerateCorrelationId`：Guid 的 N 格式字符串

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs",
      "label": "CorrelationIdMiddleware",
      "path": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs.md",
      "layer": "api",
      "kind": "middleware"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs", "type": "calls" },
    { "from": "src/Pim.Api/Middleware/ExceptionMiddleware.cs", "to": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Api/CorrelationIdMiddlewareTests.cs", "to": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs", "type": "tests" },
    { "from": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs", "to": "Serilog.Context", "type": "depends_on" }
  ]
}
```
