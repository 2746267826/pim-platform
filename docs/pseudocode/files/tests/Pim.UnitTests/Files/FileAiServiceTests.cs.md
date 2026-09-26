# tests/Pim.UnitTests/Files/FileAiServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：文件 AI 摘要/标签与组织建议经治理网关；失败不写建议。
- 主要依赖：`FileAiService`、FakeAiGateway、File 实体
- 被谁使用：dotnet test

## 函数级结构化伪代码

### GenerateSummaryAndTagsAsync_SendsGovernedGatewayRequestAndStoresResult
- 步骤：seed 索引文件；gateway 返回 summary/tags；断言 Module/Purpose/Schema/证据 chunk；不泄漏 protected；落库 FileAiResult

### GenerateOrganizationSuggestionsAsync_StoresPendingSuggestions
- 步骤：rename 建议 pending；purpose organization_suggestions

### GenerateOrganizationSuggestionsAsync_WhenGatewayFailsCreatesNoSuggestions
- 步骤：Failed 结果 → 无建议行

## 近逐行中文伪代码

1. [L16-65] 摘要标签
2. [L67-106] 组织建议
3. [L108+] 失败路径与 Seed/Fake

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileAiServiceTests.cs",
      "label": "FileAiServiceTests",
      "path": "tests/Pim.UnitTests/Files/FileAiServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileAiServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/FileAiServiceTests.cs", "to": "src/Pim.Module.Files/Services/FileAiService.cs", "type": "tests" }
  ]
}
```
