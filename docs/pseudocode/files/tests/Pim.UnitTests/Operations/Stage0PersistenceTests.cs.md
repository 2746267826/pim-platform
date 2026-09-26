# tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Stage0 实体 EF 默认值元数据与可保存性。
- 主要依赖：AuditLog/OperationConfirmation/DaemonHeartbeat 实体
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 元数据：默认 `{}`/`now()`/Pending/windows/Unknown 等
2. SaveChanges 三类各 1 行

## 近逐行中文伪代码

1. [L1-L39] 元数据断言
2. [L41-L91] 保存断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs",
      "label": "Stage0PersistenceTests",
      "path": "tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Operations/Stage0PersistenceTests.cs", "to": "src/Pim.Infrastructure/Data/Entities", "type": "tests" }
  ]
}
```
