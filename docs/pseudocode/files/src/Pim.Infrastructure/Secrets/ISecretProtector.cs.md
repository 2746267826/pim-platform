# src/Pim.Infrastructure/Secrets/ISecretProtector.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：密钥/敏感字符串保护抽象：明文保护为密文、密文还原为明文。
- 主要依赖：无（纯接口）
- 被谁使用：实现 `DataProtectionSecretProtector`；DI 在 `ServiceCollectionExtensions` 注册；`FileProviderBindingService`、`OutlookTokenService` 注入调用

## 函数级结构化伪代码

### ISecretProtector
#### `Protect(string value) -> string`
- 输入：明文 `value`
- 输出：受保护密文字符串
- 副作用：实现可依赖 Data Protection 密钥环
- 步骤：
  1. 由实现将明文加密/保护后返回
- 分支与异常：由实现定义（空值、损坏密钥等）
- 调用：调用方在落库前保护 AppPassword、OAuth token 等

#### `Unprotect(string protectedValue) -> string`
- 输入：已保护密文 `protectedValue`
- 输出：还原明文
- 副作用：实现可读密钥环
- 步骤：
  1. 由实现解密/解保护后返回
- 分支与异常：密文无效或密钥轮换失败时由实现抛出
- 调用：读取连接/令牌时解保护

## 近逐行中文伪代码

1. 命名空间：`Pim.Infrastructure.Secrets`
2. 接口 `ISecretProtector`
3. 方法 `Protect(value)`：明文 → 密文
4. 方法 `Unprotect(protectedValue)`：密文 → 明文

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs",
      "label": "ISecretProtector",
      "path": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Secrets/ISecretProtector.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Secrets/DataProtectionSecretProtector.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "to": "src/Pim.Infrastructure/Secrets/ISecretProtector.cs", "type": "calls" }
  ]
}
```
