# tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：基线迁移采纳判断与 BaselineMigrationId 稳定。
- 主要依赖：`PimMigrationAdoptionService`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### NeedsBaselineAdoption
- 无 users 表 → false
- 有 users 无 history → true
- 已有 history → false

### BaselineMigrationId_IsStable
- 等于 `20260524000000_BaselineExistingSchema`

## 近逐行中文伪代码

1. [L9-25] 三组布尔
2. [L27-31] 常量

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs",
      "label": "PimMigrationAdoptionServiceTests",
      "path": "tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/PimMigrationAdoptionServiceTests.cs", "to": "src/Pim.Infrastructure/Data/PimMigrationAdoptionService.cs", "type": "tests" }
  ]
}
```
