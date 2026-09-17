# tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Nextcloud 绑定：密码保护、URL 校验、规范化 upsert、测试连接解保护。
- 主要依赖：`FileProviderBindingService`、`ISecretProtector`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Bind 保护 AppPassword，DTO 不回显
2. 非 http(s) → 5101
3. 用户信息/query 不安全 URL → 5101
4. 等价 URL 规范化后 upsert 同 Id 更新密码
5. TestProvider 使用明文密码并置 connected

## 近逐行中文伪代码

1. [L1-L114] 五场景
2. 后续 Fake 适配器与 CreateService

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs",
      "label": "FileProviderBindingServiceTests",
      "path": "tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/FileProviderBindingServiceTests.cs", "to": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "type": "tests" }
  ]
}
```
