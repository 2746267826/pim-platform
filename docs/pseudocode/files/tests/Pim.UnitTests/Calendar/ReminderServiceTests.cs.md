# tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证提醒创建字段与低/高风险动作分支。
- 主要依赖：`ReminderService`、`CreateReminderRequest`
- 被谁使用：xUnit

## 函数级结构化伪代码

### ReminderStoresTriggerRiskChannelsDndHistoryAndRelatedObject
- CreateAsync 存 title/related/channels/status Open/DND
### LowRiskActionExecutesAndHighRiskActionReturnsOpenDetail
- dismiss L1 → Executed/Dismissed；confirm L3 → OpenDetailRequired + /confirmations/

## 近逐行中文伪代码

1. [L1-L40] 创建提醒字段
2. [L42-L57] 动作风险分支
3. [L59-L88] Request/Service/Db helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs",
      "label": "ReminderServiceTests",
      "path": "tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/ReminderServiceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "type": "tests" }
  ]
}
```
