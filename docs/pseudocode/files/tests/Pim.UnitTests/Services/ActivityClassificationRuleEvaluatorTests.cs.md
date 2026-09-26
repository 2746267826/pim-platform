# tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖活动分类规则求值器运算符、边界与畸形条件，以及 schema SQL 迁移约束片段。
- 主要依赖：`ActivityClassificationRuleEvaluator`、`ActivityClassificationContext`/`Result`、`PcTrackerSchemaInitializer`
- 被谁使用：xUnit

## 函数级结构化伪代码

### ActivityClassificationRuleEvaluatorTests
#### Matches_ReturnsTrueForWebPageDomainSuffixAndTitleContainsAny / FalseWhenOneAllConditionFails
- all 组合成功/失败
#### Matches_SupportsStringOperators (Theory)
- equals/contains/startsWith/endsWith/pathPrefix/regex 等字段
#### Matches_DomainSuffixRequiresDomainBoundary
- notactivitywatch.net 不匹配后缀边界
#### Matches_PathPrefixIgnoresQueryAndFragmentBoundaries
- /docs? /docs# 仍匹配 pathPrefix /docs
#### Matches_ReturnsFalseForMalformedOrUnsupportedConditions
- null/空/坏 JSON/空 all/坏 op/未知 field/坏 regex
#### ActivityClassificationResult_HasFallbackDefaults
- 其他/#64748b/fallback/低 confidence
#### SchemaSql_* 系列
- 旧规则 priority+1000；时间戳 DEFAULT 在 seed 前；pending cluster 唯一索引；exe 后缀去敏；jsonb Format 安全

## 近逐行中文伪代码

1. [L1-L10] using 与类
2. [L11-L63] all 条件真/假
3. [L65-L98] Theory 字符串算子
4. [L100-L152] domain 边界与 pathPrefix
5. [L154-L180] 畸形条件
6. [L182-L192] Fallback 默认
7. [L194-L254] SchemaSql 静态片段断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs",
      "label": "ActivityClassificationRuleEvaluatorTests",
      "path": "tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "type": "tests" }
  ]
}
```
