# src/Pim.Api/Endpoints/AuthEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册 `/api/v1/auth` 下 register/login/refresh 最小 API 端点：用户注册、登录（含 IP 失败限流与计时日志）、刷新令牌轮换。
- 主要依赖：
  - `PimDbContext`、`UserEntity`/`RefreshTokenEntity`/`LoginAttemptEntity`
  - `JwtService`、`PasswordHasher`
  - `Pim.Core.Common.ApiResponse`、`Pim.Api.DTOs`（Register/Login/Refresh/AuthResponse/UserInfo）
  - SHA256、Stopwatch
- 被谁使用：`Program.cs` → `app.MapAuthEndpoints()`

## 函数级结构化伪代码

### AuthEndpoints
#### `static void MapAuthEndpoints(this IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：void（副作用注册路由）
- 副作用：挂载三组 POST 处理程序
- 步骤：
  1. `MapGroup("/api/v1/auth")`。
  2. 注册 `POST /register`、`POST /login`、`POST /refresh` 内联异步委托。
- 分支与异常：各 handler 内处理
- 调用：Minimal API MapPost

#### 内联 `POST /register`
- 输入：`RegisterRequest`、db、jwt、ct
- 输出：201 Created + AuthResponse，或 409 Conflict
- 副作用：写 User、RefreshToken
- 步骤：
  1. 用户名已存在 → Conflict 01003「用户名已存在」。
  2. 邮箱已存在 → Conflict 01004「邮箱已存在」。
  3. 建 UserEntity：用户名/邮箱/`PasswordHasher.Hash`/DisplayName 默认用户名/Role=user。
  4. Save 用户；生成 access + refresh；refresh 的 SHA256 Base64 存 RefreshTokens，7 天过期；再 Save。
  5. Created `/api/v1/users/{id}` + ApiResponse.Ok(AuthResponse：access、refresh、15 分钟过期、UserInfo)。
- 分支与异常：冲突 409；DB 异常向上
- 调用：`PasswordHasher.Hash`、`jwt.GenerateAccessToken`/`GenerateRefreshToken`、SHA256

#### 内联 `POST /login`
- 输入：`LoginRequest`、db、jwt、HttpContext、logger、ct
- 输出：200 AuthResponse / 401 / 429
- 副作用：LoginAttempts；成功时写 RefreshToken；性能日志
- 步骤：
  1. 记 total/step 秒表；取 RemoteIp 或 `"unknown"`。
  2. 统计该 IP 近 15 分钟失败次数；≥5 → Retry-After=900，429。
  3. 按 Username 或 Email 查用户；`PasswordHasher.Verify`。
  4. 失败：写 LoginAttempt Success=false；Info 日志（bcrypt/userLookup/rateLimit/total）；Unauthorized。
  5. 成功：写 LoginAttempt Success=true + UserId；发 access/refresh；存 refresh hash（7 天）；Save；Info 日志含 jwt/dbSave；Ok AuthResponse。
- 分支与异常：限流 429；凭证错误 401
- 调用：EF Count/FirstOrDefault、PasswordHasher、JwtService、SHA256

#### 内联 `POST /refresh`
- 输入：`RefreshRequest`、db、jwt、ct
- 输出：200 AuthResponse 或 401
- 副作用：吊销旧 refresh、写入新 refresh
- 步骤：
  1. 对请求 refresh 做 SHA256 Base64 得 tokenHash。
  2. 查未 Revoked 的 RefreshToken；null 或已过期 → Unauthorized。
  3. 旧 token RevokedAt=UtcNow；Find 用户，null → Unauthorized。
  4. 新 access + 新 refresh（hash 入库 7 天）；Save；Ok AuthResponse。
- 分支与异常：无效/过期 refresh → 401
- 调用：JwtService、SHA256、EF

## 近逐行中文伪代码

1. 引入诊断、加密、文本、EF、ApiResponse、Auth、Data、Entities、DTOs。
2. 静态类 `AuthEndpoints`；`MapAuthEndpoints` 建组 `/api/v1/auth`。
3. **register**：重名/重邮箱 Conflict → 建用户哈希密码 → 发 token → 存 refresh hash → 201。
4. **login**：计时 + IP；15 分钟内失败≥5 → 429；查用户+验密；失败记 attempt 并 401；成功记 attempt、发 token、存 refresh、记耗时日志、200。
5. **refresh**：hash 查库；无效或过期 401；吊销旧令牌；用户不存在 401；发新双令牌并入库、200。
6. Access 响应过期时间统一 `UtcNow.AddMinutes(15)`；Refresh 实体过期 `AddDays(7)`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/AuthEndpoints.cs",
      "label": "AuthEndpoints",
      "path": "src/Pim.Api/Endpoints/AuthEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/AuthEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Auth/JwtService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Auth/PasswordHasher.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Core/Common/ApiResponse.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Data/Entities", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" }
  ]
}
```
