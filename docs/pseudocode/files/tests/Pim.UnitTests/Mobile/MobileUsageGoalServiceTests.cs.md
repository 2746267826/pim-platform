# tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：手机用量目标 Save/List 与按 scope+category 幂等更新。
- 主要依赖：`MobileUsageGoalService`、`MobileUsageGoalUpsertRequest`、`MobileLifeCategories`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### SaveAsync_StoresUserGlobalDailyGoalAndListAsyncReturnsIt
- 步骤：Save total-daily 全局日目标 14400s；List 单条且字段一致

### SaveAsync_UpdatesExistingScopePackageAndCategory
- 步骤：同 scope+category 二次 Save 更新 label/limit/enabled；仍单条

## 近逐行中文伪代码

1. [L9-31] 新建 total-daily 并 List
2. [L33-60] category-daily 短视频目标更新

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs",
      "label": "MobileUsageGoalServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/Pim.UnitTests/Mobile/MobileUsageGoalServiceTests.cs",
      "to": "src/Pim.Module.Mobile/Services/MobileUsageGoalService.cs",
      "type": "tests"
    }
  ]
}
```
