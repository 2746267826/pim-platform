# tests/Pim.UnitTests/Calendar/ReminderNotificationPayloadTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证提醒通知 payload 含风险等级、关联对象、详情 URL 与动作列表。
- 主要依赖：`ReminderService`、`ICurrentUserService`、`CreateReminderRequest`
- 被谁使用：xUnit

## 函数级结构化伪代码

### ReminderNotificationPayloadTests
#### NotificationPayloadIncludesRiskRelatedObjectDetailUrlAndActions
- CreateAsync 确认类提醒（L3ExternalSourceOrWriteback）
- BuildNotificationPayloadAsync(WindowsToast)
- 断言 ReminderId/Title/Risk/RelatedObject/DetailUrl=/confirmations/{id}/Actions open|snooze|dismiss
#### helpers：CreateDb、FixedCurrentUserService

## 近逐行中文伪代码

1. [L1-L14] using、UserId
2. [L15-L42] 创建提醒并构建 payload 断言
3. [L44-L57] InMemory DB 与固定用户

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/ReminderNotificationPayloadTests.cs",
      "label": "ReminderNotificationPayloadTests",
      "path": "tests/Pim.UnitTests/Calendar/ReminderNotificationPayloadTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/ReminderNotificationPayloadTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/ReminderNotificationPayloadTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/ReminderService.cs", "type": "tests" }
  ]
}
```
