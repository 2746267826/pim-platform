# tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：分析查询 Normalize 默认近 7 北京日；夹紧 pageSize 保留筛选。
- 主要依赖：MobileAnalyticsQueryService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Normalize_DefaultsToLastSevenBeijingDays
### Normalize_ClampsPageSizeAndKeepsFilters

## 近逐行中文伪代码

1. 默认时区/区间/噪声/分页
2. PageSize 999→200；保留 device/category/cursor 等

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs",
      "label": "MobileAnalyticsQueryServiceTests.cs",
      "path": "tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Mobile/MobileAnalyticsQueryServiceTests.cs","to":"src/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs","type":"tests"}]
}
```