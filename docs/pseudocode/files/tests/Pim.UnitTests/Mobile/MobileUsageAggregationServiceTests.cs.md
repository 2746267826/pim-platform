# tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：使用聚合 overview/heatmap/charts：覆盖/目标/北京小时、中文桶、fallback 比例与质量标志。
- 主要依赖：`MobileUsageAggregationService`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Overview 用 override/目标/PeakLocalHour 北京
2. Heatmap/Charts 中文分类与 top-apps
3. fallback 摘要按重叠比例
4. fallback 跨小时拆分
5. 噪声隐藏与缺失元数据 quality flags

## 近逐行中文伪代码

1. 五 Fact + SeedSession/SeedSummary helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs",
      "label": "MobileUsageAggregationServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileUsageAggregationServiceTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "type": "tests" }
  ]
}
```
