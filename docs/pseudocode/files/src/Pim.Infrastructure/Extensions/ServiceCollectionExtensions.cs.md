# src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：扩展方法 `AddPimInfrastructure`——集中注册 EF、审计/运维/AI/Hangfire/DataProtection/Auth/存储/Tika 等基础设施服务。
- 主要依赖：`IServiceCollection`、`IConfiguration`、Hangfire、EF Npgsql、DataProtection、各 Infrastructure 服务类型
- 被谁使用：API 宿主 `Program`/`Startup` 调用

## 函数级结构化伪代码

### ServiceCollectionExtensions
#### static IServiceCollection AddPimInfrastructure(this IServiceCollection services, IConfiguration configuration)
- 输入：服务集合；配置根
- 输出：同一 `IServiceCollection`（链式）
- 副作用：向 DI 注册大量服务；可能创建 DataProtection 密钥目录；条件注册 MinIO
- 步骤：
  1. **EF**：`PimDbContext` + Npgsql，连接串 `DefaultConnection`，失败重试 3 次
  2. Scoped：`PimMigrationAdoptionService`、`IAuditLogService`→`AuditLogService`、`AuditVersionService`、`IOperationConfirmationService`、`IDaemonHeartbeatService`、`EndpointStatusService`、`ISystemStatusService`
  3. **AI**：`Configure<AiOptions>("Ai")`；Scoped Gateway/Usage/Health/RequestLogWriter；Singleton SchemaRegistry、ChatClientFactory；命名 HttpClient `litellm-health`
  4. **Hangfire**：PostgreSql 存储（同连接串）、HangfireServer；监控/作业状态服务；`Stage0DiagnosticJob`
  5. **DataProtection**：密钥路径配置或默认 `/data/keys/data-protection`；`Directory.CreateDirectory`；PersistKeys + ApplicationName=`Pim`；`ISecretProtector`→`DataProtectionSecretProtector`
  6. **Auth**：Singleton `JwtService`；`HttpContextAccessor`；Scoped `ICurrentUserService`
  7. **MinIO**：若 `Minio:Endpoint` 非空，Singleton `MinioStorage`
  8. Singleton `KopiaService`（RepositoryPath + Password 配置）
  9. HttpClient `TikaClient`，BaseAddress=`Tika:BaseUrl`
  10. return services
- 分支与异常：
  - MinIO 未配置 → 跳过注册
  - 配置键缺失时 `!` 强制可能在解析时抛
  - CreateDirectory 失败抛 IO 异常
- 调用：`AddDbContext`、`AddScoped`/`AddSingleton`、`Configure`、`AddHangfire`/`AddHangfireServer`、`AddDataProtection`、`AddHttpClient`

## 近逐行中文伪代码

1. 引入 Hangfire、DataProtection、EF、Configuration、DI 与各 Infrastructure 命名空间
2. 静态类 `ServiceCollectionExtensions`
3. `AddPimInfrastructure`：注册 DbContext(UseNpgsql + EnableRetryOnFailure(3))
4. 注册迁移采纳、审计日志、AuditVersion、确认、心跳、EndpointStatus、SystemStatus
5. Configure AiOptions；注册 AI 网关/用量/健康/日志/Schema/ChatClient 与 litellm-health 客户端
6. Hangfire PostgreSql + Server；监控客户端、JobStatus、Stage0 诊断作业
7. 解析 DataProtection KeysPath；创建目录；PersistKeysToFileSystem；SetApplicationName("Pim")；注册 SecretProtector
8. JwtService + HttpContextAccessor + CurrentUserService
9. 若 Minio Endpoint 有值：注册 MinioStorage(endpoint, access, secret)
10. 注册 KopiaService(repoPath, password)
11. 注册 TikaClient HttpClient 与 BaseAddress
12. 返回 services

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs",
      "label": "ServiceCollectionExtensions",
      "path": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Ai/AiGateway.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Storage/KopiaService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Storage/MinioStorage.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Auth/JwtService.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "type": "calls" }
  ]
}
```
