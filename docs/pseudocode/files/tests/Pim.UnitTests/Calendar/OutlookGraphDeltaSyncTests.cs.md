# tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 Outlook Graph delta 分页跟随 nextLink/存储 deltaLink，以及核心 diff 在变更前提交 L3 确认不本地改写。
- 主要依赖：`OutlookSyncService`、`FakeMicrosoftGraphClient`、`OperationConfirmationService`、`OutlookTokenService`
- 被谁使用：xUnit

## 函数级结构化伪代码

### OutlookGraphDeltaSyncTests
#### DeltaSyncFollowsNextLinkAndStoresDeltaLink
- 入队两页 delta（nextLink → deltaLink）
- SyncAsync：ReadCount=2，步骤含 Follow nextLink / Store deltaLink，连接 DeltaLink 含 deltatoken
#### OutlookCoreDiffCreatesL3ConfirmationBeforeLocalMutation
- 本地事件 Location 旧值；远端同 id 新 Location
- Sync：ConfirmationCount=1、UpdatedCount=0，本地 Location 仍旧
#### helpers
- CreateService / SeedCalendarAndConnection / CreateDb / StubHttpClientFactory

## 近逐行中文伪代码

1. [L1-L15] using、UserId
2. [L16-L44] nextLink 分页与 deltaLink 存储
3. [L46-L82] 核心字段 diff → L3 确认、不更新本地
4. [L84-L130] 服务装配、种子连接、InMemory DB、Stub factory

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs",
      "label": "OutlookGraphDeltaSyncTests",
      "path": "tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookSyncService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs", "to": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Calendar/OutlookGraphDeltaSyncTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/OutlookTokenService.cs", "type": "depends_on" }
  ]
}
```
