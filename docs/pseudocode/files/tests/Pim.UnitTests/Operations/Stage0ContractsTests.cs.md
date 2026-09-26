# tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：锁定 Stage0 健康状态序、确认生命周期与侧栏摘要 DTO。
- 主要依赖：`PimHealthStatus`、`OperationConfirmationStatus`、`SystemStatusSummaryDto`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Health 枚举序 Unknown < Healthy < Warning < Critical
2. Confirmation 含 Pending/Confirmed/Rejected/Expired/Executed
3. SystemStatusSummaryDto 可承载中文 label 与 daemon 消息

## 近逐行中文伪代码

1. [L1-L14] 健康序
2. [L16-L26] 确认状态
3. [L28-L40] 摘要 DTO

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs",
      "label": "Stage0ContractsTests",
      "path": "tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/Stage0ContractsTests.cs", "to": "src/Pim.Core/Operations", "type": "tests" }
  ]
}
```
