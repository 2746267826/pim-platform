# src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：基于 ASP.NET Core Data Protection 的服务端密钥保护器，实现 `ISecretProtector` 的 Protect/Unprotect。
- 主要依赖：`Microsoft.AspNetCore.DataProtection`（`IDataProtectionProvider`、`IDataProtector`）；`ISecretProtector`
- 被谁使用：DI 单例 `ISecretProtector`；`FileProviderBindingService`、`OutlookTokenService` 等存取敏感令牌/密钥

## 函数级结构化伪代码

### DataProtectionSecretProtector
#### 构造 `DataProtectionSecretProtector(IDataProtectionProvider provider)`
- 输入：Data Protection 提供方
- 输出：实例
- 副作用：创建目的字符串为 `Pim.ServerSideSecrets.v1` 的 protector
- 步骤：
  1. `_protector = provider.CreateProtector("Pim.ServerSideSecrets.v1")`
- 分支与异常：provider 异常向上抛
- 调用：`CreateProtector`

#### `string Protect(string value)`
- 输入：明文
- 输出：受保护字符串
- 副作用：无（加密结果由 DP API 产生）
- 步骤：委托 `_protector.Protect(value)`
- 分支与异常：null/无效输入由底层抛出
- 调用：`IDataProtector.Protect`

#### `string Unprotect(string protectedValue)`
- 输入：受保护字符串
- 输出：明文
- 副作用：无
- 步骤：委托 `_protector.Unprotect(protectedValue)`
- 分支与异常：载荷损坏/密钥不匹配时底层抛出
- 调用：`IDataProtector.Unprotect`

## 近逐行中文伪代码

1. 引入 `Microsoft.AspNetCore.DataProtection`
2. 命名空间 `Pim.Infrastructure.Secrets`
3. 密封类 `DataProtectionSecretProtector` 实现 `ISecretProtector`
4. 字段 `_protector`：`IDataProtector`
5. 构造：用 provider 创建目的 `Pim.ServerSideSecrets.v1` 的 protector
6. `Protect`：表达式体调用 `_protector.Protect`
7. `Unprotect`：表达式体调用 `_protector.Unprotect`
8. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs",
      "label": "DataProtectionSecretProtector",
      "path": "src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs", "to": "Microsoft.AspNetCore.DataProtection", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "calls" }
  ]
}
```
