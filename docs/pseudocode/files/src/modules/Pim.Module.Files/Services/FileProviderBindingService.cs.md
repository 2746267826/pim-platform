# src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：当前用户文件提供方绑定（Nextcloud 绑定/列表/连通性测试/取连接），应用密码经 `ISecretProtector` 保护。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`ISecretProtector`、`IFileProviderAdapter`、Files DTO/Entity
- 被谁使用：Files 模块端点与其它文件服务；`FileProviderBindingServiceTests` 等

## 函数级结构化伪代码

### FileProviderBindingService
#### FileProviderBindingService(PimDbContext, ICurrentUserService, ISecretProtector, IFileProviderAdapter)
- 输入：依赖
- 输出：实例
- 副作用：保存字段
- 步骤：赋值
- 分支与异常：无
- 调用：无

#### private Guid UserId
- 输入：无
- 输出：用户 Guid
- 副作用：未登录 `DomainException(1002, "未登录")`
- 步骤：读 `_currentUser.UserId`
- 分支与异常：null → 抛
- 调用：无

#### Task<IReadOnlyList<FileProviderDto>> ListProvidersAsync(CancellationToken ct)
- 输入：ct
- 输出：当前用户提供方 DTO 列表
- 副作用：读库 AsNoTracking
- 步骤：按 UserId 过滤，OrderBy Provider/Username，MapProvider
- 分支与异常：未登录
- 调用：`MapProvider`

#### Task<FileProviderDto> BindNextcloudAsync(BindNextcloudProviderRequest request, CancellationToken ct)
- 输入：BaseUrl/InternalBaseUrl/Username/AppPassword
- 输出：绑定后的 `FileProviderDto`
- 副作用：新增或更新 `FileProviderEntity`；Protect 密码；Status=pending；SaveChanges
- 步骤：
  1. 规范化外/内 URL、用户名、应用密码
  2. 按 user+provider=nextcloud+baseUrl+username 查找已有
  3. 无则 Add 新实体；写 InternalBaseUrl、密文密码、pending、清 LastError、UpdatedAt
  4. 保存并 Map
- 分支与异常：缺字段 5100；URL 非法 5101
- 调用：`NormalizeHttpUrl`、`_secretProtector.Protect`

#### Task<FileProviderTestDto> TestProviderAsync(Guid providerId, CancellationToken ct)
- 输入：提供方 Id
- 输出：测试结果 DTO
- 副作用：调用适配器测连；更新 Status connected/error、LastError、UpdatedAt；保存
- 步骤：Load → TestConnectionAsync → 写状态 → 返回
- 分支与异常：不存在 5104
- 调用：`_adapter.TestConnectionAsync`、`ToConnection`

#### Task<FileProviderConnection> GetConnectionAsync(Guid providerId, CancellationToken ct)
- 输入：提供方 Id
- 输出：含解密密码的连接信息
- 副作用：读库；Unprotect
- 步骤：Load → ToConnection
- 分支与异常：5104
- 调用：`ToConnection`

#### private Task<FileProviderEntity> LoadProviderAsync(Guid providerId, CancellationToken ct)
- 输入：Id
- 输出：属主匹配的实体
- 副作用：读库
- 步骤：Id+UserId 查询，否则 5104
- 分支与异常：不存在
- 调用：EF

#### private FileProviderConnection ToConnection(FileProviderEntity provider)
- 输入：实体
- 输出：连接 record（含明文密码）
- 副作用：Unprotect
- 步骤：组装 Id/BaseUrl/InternalBaseUrl/Username/Unprotect(AppPasswordSecret)
- 分支与异常：保护器异常上抛
- 调用：`_secretProtector.Unprotect`

#### private static FileProviderDto MapProvider(FileProviderEntity provider)
- 输入：实体
- 输出：DTO（不含密码）
- 副作用：无
- 步骤：投影字段
- 分支与异常：无
- 调用：无

#### private static string NormalizeRequired(string? value, string label)
- 输入：值与中文标签
- 输出：Trim 后非空串
- 副作用：空 → 5100
- 步骤：Trim 校验
- 分支与异常：空白
- 调用：无

#### private static string NormalizeHttpUrl(string? value, string label)
- 输入：URL 与标签
- 输出：规范化绝对 http(s) URL（小写 scheme/host，无 userinfo/query/fragment，路径去尾 /）
- 副作用：非法 → 5101
- 步骤：NormalizeRequired → Uri 绝对且 http/https → 禁 UserInfo/Query/Fragment → UriBuilder 重建
- 分支与异常：非绝对/非 http(s)/含敏感部件
- 调用：`Uri.TryCreate`、`UriBuilder`

## 近逐行中文伪代码

1. 引入 EF、DomainException、Auth、Data、Secrets、Files DTO/Entity/Providers
2. 密封服务；构造注入 db/用户/密钥保护/适配器
3. UserId 未登录抛 1002
4. ListProvidersAsync：当前用户提供方排序列表
5. BindNextcloudAsync：规范化输入；upsert nextcloud 提供方；Protect 密码；pending
6. TestProviderAsync：适配器测连并更新 connected/error
7. GetConnectionAsync：解密后返回连接
8. LoadProvider/ToConnection/MapProvider 辅助
9. NormalizeRequired/NormalizeHttpUrl：校验必填与 http(s) URL 形状

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs",
      "label": "FileProviderBindingService",
      "path": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/Pim.Core/Exceptions/DomainException.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs", "to": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "type": "tests" }
  ]
}
```
