# tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Api/UnitTests/Solution 引用 Mobile 模块。
- 主要依赖：csproj / sln XML
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ApiProject_ReferencesMobileModule
### UnitTestsProject_ReferencesMobileModule
### Solution_IncludesMobileModule

## 近逐行中文伪代码

1. 解析 ProjectReference Include
2. 断言路径
3. sln 含模块

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs",
      "label": "MobileModuleProjectReferenceTests.cs",
      "path": "tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Mobile/MobileModuleProjectReferenceTests.cs","to":"src/modules/Pim.Module.Mobile/Pim.Module.Mobile.csproj","type":"tests"}]
}
```