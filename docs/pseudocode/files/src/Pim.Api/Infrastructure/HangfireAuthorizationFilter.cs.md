# src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：Hangfire Dashboard 授权过滤器，仅允许已认证且角色为 `admin` 的用户访问仪表盘。
- 主要依赖：`Hangfire.Dashboard`（`IDashboardAuthorizationFilter`、`DashboardContext`）
- 被谁使用：API 宿主配置 Hangfire Dashboard 时注册为授权过滤器

## 函数级结构化伪代码

### HangfireAuthorizationFilter
#### bool Authorize(DashboardContext context)
- 输入：`context` Hangfire Dashboard 上下文
- 输出：`true` 允许访问；`false` 拒绝
- 副作用：无（只读 HTTP 用户身份）
- 步骤：
  1. 从 `context` 取出 `HttpContext`
  2. 判断 `User.Identity?.IsAuthenticated == true`
  3. 且 `User.IsInRole("admin")`
  4. 两条件同时成立返回 `true`，否则 `false`
- 分支与异常：未认证或非 admin → `false`；`Identity` 为 null 时短路为 false
- 调用：`DashboardContext.GetHttpContext`、`ClaimsPrincipal.IsInRole`

## 近逐行中文伪代码

1. 引用 `Hangfire.Dashboard`
2. 命名空间 `Pim.Api.Infrastructure`
3. 声明密封类 `HangfireAuthorizationFilter`，实现 `IDashboardAuthorizationFilter`
4. 方法 `Authorize(context)`：
5.   从 `context` 获取 `HttpContext` 记为 `http`
6.   返回：`http.User.Identity` 已认证 **且** 用户属于角色 `"admin"`
7. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs",
      "label": "HangfireAuthorizationFilter",
      "path": "src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs.md",
      "layer": "api",
      "kind": "middleware"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs", "to": "Hangfire.Dashboard.IDashboardAuthorizationFilter", "type": "implements" },
    { "from": "src/Pim.Api/Infrastructure/HangfireAuthorizationFilter.cs", "to": "Hangfire.Dashboard.DashboardContext", "type": "depends_on" }
  ]
}
```
