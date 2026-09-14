# tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：从分类建议构建规则草稿：web 域后缀条件、app 归一名条件、确定性有界 RuleName。
- 主要依赖：`ClassificationRuleDraftService`、`ActivityClassificationSuggestionEntity`、`PcCategoryEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### ClassificationRuleDraftServiceTests
#### BuildSuggestionDraftAsync_CreatesDomainRuleForWebCluster()
- 输入：cluster `web:docs.example.com`、预览分类 Learning/Docs
- 输出：无
- 副作用：InMemory 写 suggestion
- 步骤：
  1. 保存 pending suggestion
  2. BuildSuggestionDraftAsync
  3. Scope=activity；ConditionsJson 含 domain + domainSuffix + docs.example.com
- 分支与异常：无
- 调用：`ClassificationRuleDraftService.BuildSuggestionDraftAsync`

#### BuildSuggestionDraftAsync_CreatesAppRuleForAppCluster()
- 输入：cluster `app:code`、已有 Programming 分类色
- 输出：无
- 副作用：写分类与 suggestion
- 步骤：
  1. ConditionsJson 含 appNameNormalized=code
  2. Color 继承 `#2563eb`
- 分支与异常：无
- 调用：同上

#### BuildSuggestionDraftAsync_UsesDeterministicBoundedRuleName()
- 输入：超长 web 域名 cluster
- 输出：无
- 副作用：无
- 步骤：
  1. 两次 Build 同 RuleName
  2. 长度 ≤128 且含 suggestion.Id 的 N 格式
- 分支与异常：无
- 调用：同上

#### NewSuggestion / CreateDb
- 输入：clusterKey
- 输出：实体 / DbContext
- 副作用：注册 PcTracker 程序集
- 步骤：pending suggestion 默认字段；InMemory DB
- 分支与异常：无
- 调用：`PimDbContext`

## 近逐行中文伪代码

1. web 簇 → domainSuffix 条件
2. app 簇 → appNameNormalized + 分类色
3. 长域名 RuleName 确定且截断
4. 辅助：NewSuggestion、CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs",
      "label": "ClassificationRuleDraftServiceTests",
      "path": "tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ClassificationRuleDraftServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs", "type": "tests" }
  ]
}
```
