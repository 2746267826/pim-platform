# src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：提醒表 `reminders` 的 EF 实体；关联业务对象、渠道 JSON、勿扰窗口与投递导航。
- 主要依赖：`ISoftDeletable`、`DataAnnotations`/`Schema`、`ReminderDeliveryEntity`
- 被谁使用：提醒服务与投递流水；EF 迁移（含 AddReminderService）与快照

## 函数级结构化伪代码

### ReminderEntity
#### 属性集（无行为方法）
- 输入：各属性由调用方/EF 赋值
- 输出：行状态
- 副作用：无（纯 POCO）
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid`
  2. `UserId`：所属用户
  3. `RelatedObjectType`/`RelatedObjectId`：关联对象类型（最长 80）与 Id
  4. `Title`：标题，最长 255
  5. `Body`/`TriggerReason`：正文与触发原因（无 MaxLength 限制在注解层）
  6. `RiskLevel`：风险级别，最长 80，默认 `L1LowRiskAction`
  7. `ChannelsJson`：jsonb 渠道列表，默认 `[]`
  8. `DoNotDisturbStart`/`DoNotDisturbEnd`：勿扰起止（可空，最长 16）
  9. `ScheduledAt`：计划触发时间
  10. `Status`：状态，最长 40，默认 `Open`
  11. `CreatedAt`/`UpdatedAt`：默认 UTC 现在
  12. `DeletedAt`：软删除时间（可空）
  13. `Deliveries`：投递记录导航集合
- 分支与异常：无运行时分支
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`ISoftDeletable`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表注解 `reminders`；类实现 `ISoftDeletable`
4. 主键与 user_id；related_object_type/id；title/body/trigger_reason
5. risk_level 默认 L1LowRiskAction；channels_json 默认空数组
6. dnd_start/dnd_end 可选；scheduled_at；status 默认 Open
7. created_at/updated_at/deleted_at
8. Deliveries 集合导航初始化为空 List

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs",
      "label": "ReminderEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs", "to": "src/Pim.Core/Data", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs", "type": "depends_on" }
  ]
}
```
