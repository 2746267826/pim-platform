# tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：活动分类建议构建/列表/拒绝/禁止直接接受；URL 脱敏；rejected 抑制重建。
- 主要依赖：`ActivitySuggestionService`、`AppSignatureService`、PcTracker 实体/DTO、InMemory Db
- 被谁使用：dotnet test

## 函数级结构化伪代码

### BuildSuggestionsAsync_GroupsFallbackWebRecordsByDomain
- 步骤：同域 fallback 聚合 clusterKey web:domain；时长合计；JSON 脱敏含 [redacted]

### BuildSuggestionsAsync_IgnoresTinyFallbackRecordsBelowRecommendedDuration
- 步骤：低于 recommendedMinimumMinutes 不生成

### GetSuggestionsAsync_ReturnsOnlyPendingSuggestionsOrderedByDuration
- 步骤：仅 pending；按时长降序

### BuildSuggestionsAsync_RejectedSuggestionSuppressesRecreation
- 步骤：已 rejected 同 cluster 不重建

### BuildSuggestionsAsync_UpdatesPendingSuggestionWhenHistoricalRowExists
- 步骤：存在 rejected+pending 时更新 pending 统计

### GetRecentProjectTagsAsync_ReturnsTagsFromRulesAndSnapshots
- 步骤：规则与快照 ProjectTag 并集

### AcceptSuggestionAsync_RejectsDirectAcceptWithoutCreatingRule
- 步骤：直接 Accept 抛「预览/应用」；不建规则

### AcceptSuggestionAsync_RepeatedDirectAcceptDoesNotCreateDuplicateRule
- 步骤：两次直接 Accept 均失败；规则数 0

### RejectSuggestionAsync_MarksSuggestionRejected
- 步骤：pending→rejected

### RejectSuggestionAsync_ThrowsForAcceptedSuggestionWithoutChangingStatus
### AcceptSuggestionAsync_ThrowsForRejectedSuggestionWithoutCreatingRule
- 步骤：非 pending 状态错误

### 工厂：CreateDbContext / CreateService / NewSuggestion / NewAcceptRequest / NewWebRecord / AssertNoSensitiveUrlMaterial
- 步骤：种子与敏感串断言

## 近逐行中文伪代码

1. [L12-39] 域名聚合与脱敏
2. [L41-61] 最小时长过滤
3. [L63-89] pending 排序
4. [L91-119] rejected 抑制
5. [L121-156] 更新 pending
6. [L158-188] 最近项目标签
7. [L190-300] accept/reject 状态机
8. [L302-391] 辅助方法

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs",
      "label": "ActivitySuggestionServiceTests",
      "path": "tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs", "to": "src/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs", "to": "src/Pim.Module.PcTracker/Services/AppSignatureService.cs", "type": "depends_on" }
  ]
}
```
