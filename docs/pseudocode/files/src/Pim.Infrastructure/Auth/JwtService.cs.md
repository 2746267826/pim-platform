# src/Pim.Infrastructure/Auth/JwtService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：基于 RSA 的 JWT 签发与校验参数提供；支持 PEM 私钥文件或 Development 环境临时内存密钥。
- 主要依赖：`IConfiguration`、`IHostEnvironment`、`ILogger<JwtService>`、`System.IdentityModel.Tokens.Jwt`、`Microsoft.IdentityModel.Tokens`、`System.Security.Cryptography`
- 被谁使用：DI 单例注册；`AuthEndpoints` 调用 `GenerateAccessToken`/`GenerateRefreshToken`；`AuthExtensions` 用 `GetValidationParameters` 配置 JWT Bearer

## 函数级结构化伪代码

### JwtService
#### JwtService(IConfiguration configuration, IHostEnvironment environment, ILogger<JwtService> logger)
- 输入：配置（`Jwt:PrivateKeyPath`）、宿主环境、日志
- 输出：已初始化 RSA 的服务实例
- 副作用：创建 RSA；可能读 PEM 文件；Development 下可能打 Warning；非 Development 且缺密钥抛异常
- 步骤：
  1. 计时；`RSA.Create()`；保存 logger
  2. 读 `Jwt:PrivateKeyPath`：路径非空且文件存在 → `ImportFromPem`
  3. 否则若 `IsDevelopment()` → 使用临时内存密钥并 Warning（含 keySize、耗时、生产配置提示）
  4. 否则 → `InvalidOperationException` 要求配置有效 PEM 路径
- 分支与异常：生产缺密钥抛异常；开发缺密钥继续但令牌重启失效
- 调用：`RSA.Create`、`File.Exists`/`ReadAllText`、`ImportFromPem`、`LogWarning`

#### string GenerateAccessToken(Guid userId, string username, string role)
- 输入：用户 Id、用户名、角色
- 输出：JWT 字符串（issuer=`pim`，audience=`pim-client`，15 分钟过期，RS256）
- 副作用：Debug 日志耗时
- 步骤：
  1. 校验 userId 非 Empty、username/role 非空白
  2. 在 `_rsaLock` 下用 `_rsa` 构造 `SigningCredentials`（RsaSha256）
  3. claims：`NameIdentifier`、`Name`、`Role`、`jti`（新 Guid）
  4. 构造 `JwtSecurityToken` 并 `WriteToken`
- 分支与异常：参数非法 → `ArgumentException`
- 调用：`JwtSecurityTokenHandler.WriteToken`

#### string GenerateRefreshToken()
- 输入：无
- 输出：64 字节密码学随机数的 Base64 字符串
- 副作用：无（仅本地 RNG）
- 步骤：`RandomNumberGenerator.GetBytes(64)` → Base64
- 分支与异常：无
- 调用：`RandomNumberGenerator.Create`

#### TokenValidationParameters GetValidationParameters()
- 输入：无
- 输出：与签发一致的校验参数（issuer/audience/lifetime/signing key，ClockSkew 30s）
- 副作用：无
- 步骤：锁内取 `RsaSecurityKey(_rsa)`；填充 `TokenValidationParameters`
- 分支与异常：无
- 调用：无

#### void Dispose()
- 输入：无
- 输出：无
- 副作用：释放 RSA；`GC.SuppressFinalize`
- 步骤：若未 dispose → `_rsa.Dispose()` 并标记
- 分支与异常：幂等
- 调用：`RSA.Dispose`

## 近逐行中文伪代码

1. 引入 Diagnostics、Jwt、Claims、Cryptography、Configuration、Hosting、Logging、IdentityModel
2. 命名空间 `Pim.Infrastructure.Auth`
3. 类 `JwtService` 实现 `IDisposable`：字段 `_rsa`、`_logger`、`_rsaLock`、`_disposed`
4. 构造：启动 Stopwatch；创建 RSA；读 `Jwt:PrivateKeyPath`
5. 有 PEM 文件则 ImportFromPem
6. 否则 Development：Warning 使用临时密钥并提示配置路径
7. 否则抛 InvalidOperationException
8. `GenerateAccessToken`：校验 userId/username/role
9. 锁内建 RsaSha256 SigningCredentials
10. claims 含 NameIdentifier、Name、Role、Jti
11. issuer=pim、audience=pim-client、过期 UTC+15 分钟；写 token；Debug 耗时
12. `GenerateRefreshToken`：64 随机字节转 Base64
13. `GetValidationParameters`：锁内 RsaSecurityKey；校验 issuer/audience/lifetime/signing key；ClockSkew 30s
14. `Dispose`：释放 RSA 一次并 SuppressFinalize

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Auth/JwtService.cs",
      "label": "JwtService",
      "path": "src/Pim.Infrastructure/Auth/JwtService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Auth/JwtService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Auth/JwtService.cs", "to": "Microsoft.IdentityModel.Tokens", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Auth/JwtService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/AuthExtensions.cs", "to": "src/Pim.Infrastructure/Auth/JwtService.cs", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Auth/JwtService.cs", "type": "calls" }
  ]
}
```
