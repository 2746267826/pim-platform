# tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：确认后的 Outlook 写回 Patch Graph 带 changeKey 并记审计版本。
- 主要依赖：`OutlookSyncService`、`OperationConfirmationService`、FakeGraph
- 被谁使用：xUnit

## 函数级结构化伪代码

### ConfirmedWritebackPatchesGraphWithChangeKeyAndRecordsAudit
- 种子连接与事件 → CreateWritebackConfirmation → ConfirmSecondLevel → ExecuteConfirmedWrite
- PatchRequests 含 eventId/changeKey/location；AuditVersion 非空

## 近逐行中文伪代码

1. [L1-L74] 主路径
2. [L76-L88] CreateDb/StubHttp

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs",
      "label": "OutlookGraphWritebackTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphWritebackTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "type": "tests" }
  ]
}
```
