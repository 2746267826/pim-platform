# tests/Pim.UnitTests/Operations/InfrastructureServiceCollectionTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：`AddPimInfrastructure` 配置持久 DataProtection 密钥环。
- 主要依赖：`ServiceCollectionExtensions.AddPimInfrastructure`
- 被谁使用：xUnit

## 函数级结构化伪代码

### AddPimInfrastructure_ConfiguresDurableDataProtectionKeyRing
- 内存配置 Connection/Minio/Kopia/Tika/KeysPath
- 目录创建；XmlRepository=FileSystemXmlRepository；Protect 成功写 xml；finally 删目录

## 近逐行中文伪代码

1. [L1-L51] 单 Fact

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/InfrastructureServiceCollectionTests.cs",
      "label": "InfrastructureServiceCollectionTests",
      "path": "tests/Pim.UnitTests/Operations/InfrastructureServiceCollectionTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/InfrastructureServiceCollectionTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/InfrastructureServiceCollectionTests.cs", "to": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "type": "tests" }
  ]
}
```
