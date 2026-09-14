# tests/Pim.UnitTests/Files/FileModuleProjectReferenceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：断言 Pim.Api.csproj 引用 Files 模块。
- 主要依赖：XDocument 读 csproj
- 被谁使用：xUnit

## 函数级结构化伪代码

### ApiProject_ReferencesFilesModule
- 相对 BaseDirectory 定位 csproj，收集 ProjectReference Include，含 Pim.Module.Files.csproj

## 近逐行中文伪代码

1. [L1-L31] 路径解析与 Contains 断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileModuleProjectReferenceTests.cs",
      "label": "FileModuleProjectReferenceTests",
      "path": "tests/Pim.UnitTests/Files/FileModuleProjectReferenceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileModuleProjectReferenceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/FileModuleProjectReferenceTests.cs", "to": "src/Pim.Api/Pim.Api.csproj", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Files/FileModuleProjectReferenceTests.cs", "to": "src/modules/Pim.Module.Files/Pim.Module.Files.csproj", "type": "depends_on" }
  ]
}
```
