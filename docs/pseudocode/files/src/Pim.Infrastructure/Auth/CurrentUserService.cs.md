# src/Pim.Infrastructure/Auth/CurrentUserService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：从当前 HTTP 请求的 ClaimsPrincipal 解析当前用户 Id 与角色，供业务服务做租户/鉴权过滤。
- 主要依赖：`IHttpContextAccessor`、`System.Security.Claims`、`Microsoft.AspNetCore.Http`
- 被谁使用：DI 注册为 `ICurrentUserService`（Scoped）；Calendar/Files/QuickNotes/PcTracker/Endpoints/Today 等服务构造注入

## 函数级结构化伪代码

### ICurrentUserService
#### 属性 `Guid? UserId { get; }` / `string? Role { get; }`
- 输入：无（只读属性）
- 输出：当前用户 Id（可空）与角色字符串（可空）
- 副作用：无
- 步骤：
  1. 实现方提供当前请求上下文下的用户标识与角色
- 分支与异常：无
- 调用：无

### CurrentUserService
#### CurrentUserService(IHttpContextAccessor httpContextAccessor)
- 输入：HTTP 上下文访问器
- 输出：构造完成的服务实例；属性 `UserId`/`Role` 已填充
- 副作用：读取 `HttpContext.User` Claims（只读）
- 步骤：
  1. 取 `httpContextAccessor.HttpContext?.User`
  2. 查找 `ClaimTypes.NameIdentifier` 的 Value
  3. `Guid.TryParse` 成功则赋给 `UserId`，否则 `null`
  4. 查找 `ClaimTypes.Role` 的 Value 赋给 `Role`（可为 null）
- 分支与异常：
  - 无 HttpContext 或无 User → 两属性均为 null
  - NameIdentifier 非合法 Guid → `UserId` 为 null
- 调用：`ClaimsPrincipal.FindFirst`、`Guid.TryParse`

#### 属性 `Guid? UserId` / `string? Role`
- 输入：构造时写入
- 输出：只读属性
- 副作用：无
- 步骤：暴露构造解析结果
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 `System.Security.Claims`、`Microsoft.AspNetCore.Http`
2. 命名空间 `Pim.Infrastructure.Auth`
3. 声明接口 `ICurrentUserService`：只读 `UserId`、`Role`
4. 类 `CurrentUserService` 实现该接口
5. 构造：从 `IHttpContextAccessor` 取 `HttpContext?.User`
6. 取 `NameIdentifier` claim 值；能解析为 Guid 则作为 `UserId`，否则 null
7. 取 `Role` claim 值作为 `Role`
8. 属性 `UserId`、`Role` 为 get-only 自动属性

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Auth/CurrentUserService.cs",
      "label": "CurrentUserService",
      "path": "src/Pim.Infrastructure/Auth/CurrentUserService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Auth/CurrentUserService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "to": "ICurrentUserService", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "to": "IHttpContextAccessor", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" }
  ]
}
```
