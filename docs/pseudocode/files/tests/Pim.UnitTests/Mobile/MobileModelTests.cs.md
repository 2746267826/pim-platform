# tests/Pim.UnitTests/Mobile/MobileModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Mobile 实体注册；坐标精度；唯一索引。
- 主要依赖：Mobile 实体 / PimDbContext
- 被谁使用：dotnet test

## 函数级结构化伪代码

### MobileModule_RegistersExpectedEntities
### MobileLocation_UsesPreciseCoordinateAndAccuracyMappings
### MobileModel_DefinesRequiredUniqueIndexes

## 近逐行中文伪代码

1. 七类实体
2. lat/lon 10,7 accuracy 9,2
3. device/app/event/summary 唯一索引

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileModelTests.cs",
      "label": "MobileModelTests.cs",
      "path": "tests/Pim.UnitTests/Mobile/MobileModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Mobile/MobileModelTests.cs","to":"src/Pim.Module.Mobile/Entities","type":"tests"}
}
```