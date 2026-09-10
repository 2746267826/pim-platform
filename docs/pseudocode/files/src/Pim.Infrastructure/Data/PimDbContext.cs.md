# src/Pim.Infrastructure/Data/PimDbContext.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：主 EF Core `DbContext`：核心实体 DbSet、软删过滤、索引/默认值配置、模块程序集 Fluent 配置合并、SaveChanges 时刷新 AI Provider 更新时间、按模块程序集签名换缓存键
- 主要依赖：`Microsoft.EntityFrameworkCore`、`Pim.Core.Data`、`Pim.Core.Operations`、`Pim.Infrastructure.Audit`、`Pim.Infrastructure.Data.Entities`、`Pim.Infrastructure.Endpoints`、`PimDbContextModelCacheKeyFactory`
- 被谁使用：API/Infrastructure 服务与各模块服务注入；`Program.cs` 启动迁移；模块 `RegisterModuleAssembly`；`ServiceCollectionExtensions.AddDbContext`

## 函数级结构化伪代码

### PimDbContext
#### static void RegisterModuleAssembly(Assembly assembly)
- 输入：模块程序集
- 输出：无
- 副作用：线程安全地将程序集加入 `_moduleAssemblies`（按 FullName 去重）
- 步骤：
  1. lock `_moduleAssembliesLock`
  2. 若无同名 FullName 则 Add
- 分支与异常：无
- 调用：无

#### static string ModuleAssemblySignature { get }
- 输入：无
- 输出：已注册模块 FullName 排序后用 `|` 拼接的签名字符串
- 副作用：无（只读锁）
- 步骤：lock → OrderBy Ordinal → Join
- 分支与异常：无
- 调用：无

#### PimDbContext(DbContextOptions<PimDbContext> options)
- 输入：EF 选项
- 输出：上下文实例
- 副作用：无
- 步骤：传给 `base(options)`
- 分支与异常：无
- 调用：`DbContext` 构造

#### DbSet 属性（Users, RefreshTokens, LoginAttempts, AuditLogs, AuditVersions, OperationConfirmations, DaemonHeartbeats, EndpointStatuses, EndpointNotificationActions, AiProviderSettings, AiRequestLogs）
- 输入：无
- 输出：对应实体集
- 副作用：无
- 步骤：`Set<TEntity>()`
- 分支与异常：无
- 调用：`DbContext.Set`

#### override int SaveChanges() / SaveChanges(bool) / Task SaveChangesAsync(...)
- 输入：可选 acceptAllChanges、CancellationToken
- 输出：受影响行数
- 副作用：写库前刷新 `AiProviderSettingEntity.UpdatedAt`；再 base 保存
- 步骤：
  1. `RefreshAiProviderSettingUpdatedAt()`
  2. 调用对应 `base.SaveChanges*`
- 分支与异常：委托 base
- 调用：`RefreshAiProviderSettingUpdatedAt`、`DbContext.SaveChanges*`

#### protected override void OnModelCreating(ModelBuilder modelBuilder)
- 输入：模型构建器
- 输出：无
- 副作用：配置核心实体映射
- 步骤：
  1. `UserEntity`：Username/Email 唯一索引；`DeletedAt == null` 全局查询过滤
  2. `RefreshTokenEntity`：TokenHash 索引；FK User
  3. `LoginAttemptEntity`：(IpAddress, AttemptedAt) 索引
  4. `AuditLogEntity`：MetadataJson 默认 `{}`、CreatedAt 默认 `now()`；多列索引
  5. `AuditVersionEntity`：Before/After/`[]` 默认值、CreatedAt now；复合索引与 ConfirmationId 索引
  6. `OperationConfirmationEntity`：Payload/Preview 默认 `{}`、Status 默认 Pending 字符串、CreatedAt now；多索引
  7. `DaemonHeartbeatEntity`：DaemonKind 默认 windows、源状态 Unknown、StatusJson `{}`、ReceivedAt now；DeviceId+DaemonKind 唯一
  8. `EndpointStatusEntity`：Platform/UploadStatus 默认、时间默认 now；UserId+DeviceId 唯一
  9. `EndpointNotificationActionEntity`：CreatedAt now；User/Device/CreatedAt/ConfirmationId 索引
  10. `AiProviderSettingEntity`：Provider litellm、Status disabled、时间 now；Provider 唯一
  11. `AiRequestLogEntity`：多 JSON 默认、EstimatedCost 精度 18,8、多索引含源对象与 CorrelationId
  12. 复制 `_moduleAssemblies` 快照，对每个程序集 `ApplyConfigurationsFromAssembly`
- 分支与异常：无
- 调用：`ModelBuilder.Entity`、`ApplyConfigurationsFromAssembly`

#### protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
- 输入：选项构建器
- 输出：无
- 副作用：替换模型缓存键工厂为 `PimDbContextModelCacheKeyFactory`（模块程序集变化时重建模型）
- 步骤：`ReplaceService<IModelCacheKeyFactory, PimDbContextModelCacheKeyFactory>`
- 分支与异常：无
- 调用：`ReplaceService`

#### private void RefreshAiProviderSettingUpdatedAt()
- 输入：无（读 ChangeTracker）
- 输出：无
- 副作用：将所有 `Modified` 的 `AiProviderSettingEntity.UpdatedAt` 设为 UtcNow
- 步骤：遍历 Entries；State==Modified 则写 UpdatedAt
- 分支与异常：无
- 调用：`ChangeTracker.Entries`

## 近逐行中文伪代码

1. 引入 Reflection、EF Core、Core.Data/Operations、Audit、Entities、Endpoints
2. 命名空间 `Pim.Infrastructure.Data`；类 `PimDbContext : DbContext`
3. 静态列表 `_moduleAssemblies` 与锁；`RegisterModuleAssembly` 去重注册
4. `ModuleAssemblySignature`：排序 FullName 用 `|` 连接
5. 构造传入 `DbContextOptions<PimDbContext>`
6. 声明核心 DbSet（用户/令牌/登录尝试/审计/确认/心跳/端点/AI 设置与日志）
7. 四个 SaveChanges 重载均先 `RefreshAiProviderSettingUpdatedAt` 再 base
8. `OnModelCreating`：逐实体配索引、默认值、FK、软删过滤；再应用各模块 Fluent 配置
9. `OnConfiguring`：替换 `IModelCacheKeyFactory`
10. `RefreshAiProviderSettingUpdatedAt`：Modified 的 AI Provider 行写当前 UTC 时间

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/PimDbContext.cs",
      "label": "PimDbContext",
      "path": "src/Pim.Infrastructure/Data/PimDbContext.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/PimDbContext.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "Microsoft.EntityFrameworkCore.DbContext", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Audit", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AuthEndpoints.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "calls" }
  ]
}
```
