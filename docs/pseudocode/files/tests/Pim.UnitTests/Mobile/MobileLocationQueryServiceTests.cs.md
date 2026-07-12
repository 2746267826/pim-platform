# tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：位置查询 Normalize 默认近 7 个北京日；夹紧 pageSize 与时间范围顺序。
- 主要依赖：`MobileLocationQueryService`、MobileTestHelpers
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Normalize_DefaultsToLastSevenBeijingDays
- 步骤：Timezone Asia/Shanghai；本地 07-02..07-08；UTC 区间；MaxAccuracy=50；IncludeRejected=false；PageSize=50

### Normalize_ClampsPageSizeAndReordersRange
- 步骤：起止颠倒重排；PageSize 500→200；负精度→50

## 近逐行中文伪代码

1. [L9-24] 默认七天
2. [L26-41] 夹紧与重排

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs",
      "label": "MobileLocationQueryServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileLocationQueryServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileLocationQueryService.cs", "type": "tests" }
  ]
}
```
