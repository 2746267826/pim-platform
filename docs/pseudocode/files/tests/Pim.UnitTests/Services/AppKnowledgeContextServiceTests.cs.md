# tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：应用知识上下文 Save upsert/竞态/置信度；按 app 查询；知识应用列表统计。
- 主要依赖：AppKnowledgeContextService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### SaveAsync_CreatesDomainContextWithScopeSummaryAndDefaults
### SaveAsync_UpsertsByTrimmedAppPattern
### SaveAsync_WhenInsertHitsUniqueRace_UpdatesExistingAppPattern
### SaveAsync_RejectsConfidenceOutsideUnitInterval
### GetByAppAsync_ReturnsOnlyContextsForOneApp
### GetKnowledgeAppsAsync_ReturnsContextCountsAndRecentAffectedDuration

## 近逐行中文伪代码

1. 创建默认
2. trim upsert
3. 唯一竞态更新
4. 置信度区间
5. 按 app 过滤
6. 应用列表统计

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs",
      "label": "AppKnowledgeContextServiceTests.cs",
      "path": "tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs","to":"src/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs","type":"tests"}
}
```