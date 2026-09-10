# src/Pim.Infrastructure/Extensions/AuthExtensions.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：向 DI 注册 JWT Bearer 认证与授权，并用 `JwtService` 配置令牌校验参数。
- 主要依赖：`Microsoft.AspNetCore.Authentication.JwtBearer`；`Microsoft.Extensions.DependencyInjection` / `Options`；`Pim.Infrastructure.Auth.JwtService`
- 被谁使用：`Pim.Api/Program.cs` 调用 `builder.Services.AddPimAuth()`

## 函数级结构化伪代码

### AuthExtensions
#### `AddPimAuth(this IServiceCollection services) -> IServiceCollection`
- 输入：`services` 服务集合
- 输出：同一 `IServiceCollection`（链式）
- 副作用：注册认证方案、JwtBearer 选项配置委托、授权服务
- 步骤：
  1. `AddAuthentication(JwtBearerDefaults.AuthenticationScheme)` 并 `AddJwtBearer()`
  2. 对默认 JwtBearer 方案 `AddOptions<JwtBearerOptions>(...)`
  3. `Configure<JwtService>`：从 DI 取 `JwtService`，设 `options.TokenValidationParameters = jwtService.GetValidationParameters()`
  4. `AddAuthorization()`
  5. 返回 `services`
- 分支与异常：无显式分支；`JwtService` 未注册时运行期解析配置会失败
- 调用：`JwtService.GetValidationParameters()`

## 近逐行中文伪代码

1. 引入 JwtBearer、DI、Options、`Pim.Infrastructure.Auth`
2. 命名空间：`Pim.Infrastructure.Extensions`
3. 静态类 `AuthExtensions`
4. 扩展方法 `AddPimAuth(services)`
5. 注册默认方案为 JwtBearer 的 Authentication，并添加 JwtBearer 处理器
6. 为该方案配置 `JwtBearerOptions`
7. 注入 `JwtService`，把校验参数写入 `TokenValidationParameters`
8. 注册 Authorization
9. 返回 services

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Extensions/AuthExtensions.cs",
      "label": "AuthExtensions",
      "path": "src/Pim.Infrastructure/Extensions/AuthExtensions.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Extensions/AuthExtensions.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Extensions/AuthExtensions.cs", "to": "src/Pim.Infrastructure/Auth/JwtService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Extensions/AuthExtensions.cs", "type": "calls" }
  ]
}
```
