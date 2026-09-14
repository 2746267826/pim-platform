# tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：从建议构建/保存应用知识上下文；签名学习；失败不落库；重复规则不写知识。
- 主要依赖：AppKnowledgeSuggestionService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### BuildRecommendedContextAsync_PrefersDomain / UsesTitle / WildcardSignature
### SaveRecommendedContextAsync_Persists / CreatesSignature / RaceReuse
### ApplySuggestionWithSideEffectAsync_WhenAppKnowledgeSaveFails_DoesNotPersistApply
### ApplyEndpoint_DuplicateRuleFailureDoesNotPersistAppKnowledgeContext

## 近逐行中文伪代码

1. 推荐上下文来源 domain/title/签名
2. 保存与竞态
3. 失败回滚
4. 重复规则不写知识

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs",
      "label": "AppKnowledgeSuggestionServiceTests.cs",
      "path": "tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs","to":"src/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs","type":"tests"}]
}
```