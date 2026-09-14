# tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Outlook Graph 同步地基：默认设置、设备码占位、无连接失败批、写回 L3、核心 diff 确认不改事件。
- 主要依赖：`OutlookSyncService`、`IOperationConfirmationService` 替身、StubHttp
- 被谁使用：xUnit

## 函数级结构化伪代码

1. GetSettingsAsync 默认 outlook/common/not-connected
2. CreateDeviceCodeRequest 占位 URI/code 与 15min 过期
3. Sync 无连接 → failed 批可列表
4. CreateOutlookWritebackConfirmation L3 且无 PendingConfirmationEntity
5. Sync 核心 diff：标题不变、Confirmation/Conflict 计数、ChangedFields、SyncConflict pending
### CapturingConfirmationService / StubHttpClientFactory

## 近逐行中文伪代码

1. [L1-L69] 设置/设备码/失败批
2. [L71-L177] 写回与 core-diff
3. [L179-L300] 工厂与 Capturing 替身

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs",
      "label": "OutlookGraphSyncFoundationTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphSyncFoundationTests.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" }
  ]
}
```
