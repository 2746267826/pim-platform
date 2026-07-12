# tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖移动端应用目录覆盖与分类规则 CRUD，以及分析陈旧标记范围。
- 主要依赖：`MobileAppCatalogOverrideService`、`MobileTestHelpers`、Mobile 实体/DTO
- 被谁使用：xUnit

## 函数级结构化伪代码

### MobileAppCatalogOverrideServiceTests
#### UpsertOverrideAsync_CreatesAndUpdatesUserGlobalOverrideByPackageName
- 包名 trim/大小写归一；二次 upsert 更新字段且仅一行
#### DeleteAndClearOverrides_RemoveOnlyCurrentUserOverrides
- 删除当前用户一条；Clear 仅清当前用户，他用户保留
#### CategoryRuleCrud_ListsCreatesUpdatesAndDeletesRules
- 创建高低优先级规则；列表按 priority 排序；Update/Delete
#### MarkAnalyticsStaleAsync_MarksAffectedAggregatesAndTimelineBlocksForPackageAndRange
- 仅标记包名+时间窗内 aggregate/timeline
#### helpers：Service/Override/Aggregate/TimelineBlock

## 近逐行中文伪代码

1. [L1-L10] using 与 sealed 类
2. [L11-L38] upsert 归一与更新
3. [L40-L65] 删除与清空范围
4. [L67-L113] 规则 CRUD
5. [L115-L143] MarkAnalyticsStale
6. [L145-L198] 工厂辅助

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs",
      "label": "MobileAppCatalogOverrideServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppCatalogOverrideService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileAppCatalogOverrideServiceTests.cs", "to": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs", "type": "depends_on" }
  ]
}
```
