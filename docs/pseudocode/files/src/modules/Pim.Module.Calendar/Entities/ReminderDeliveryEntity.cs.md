# src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：提醒投递记录实体，映射表 `reminder_deliveries`，记录某次提醒在某通道上的状态、动作与载荷。
- 主要依赖：`System.ComponentModel.DataAnnotations`、导航至 `ReminderEntity`
- 被谁使用：EF 映射；提醒服务创建/更新投递状态

## 函数级结构化伪代码

### ReminderDeliveryEntity
#### 属性集（实体字段）
- 输入：无（POCO 属性读写）
- 输出：各列对应 CLR 属性
- 副作用：由 EF 持久化到 `reminder_deliveries`
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid()`
  2. `ReminderId`：关联提醒 Id（外键）
  3. `UserId`：目标用户
  4. `Channel`：通道，默认 `"Web"`，最长 40
  5. `Status`：状态，默认 `"Created"`，最长 40
  6. `Action`：用户动作，可空，最长 80
  7. `PayloadJson`：jsonb 载荷字符串，默认 `"{}"`
  8. `CreatedAt`：创建时间，默认 UTC 现在
  9. `RespondedAt`：响应时间，可空
  10. 导航属性 `Reminder`：指向 `ReminderEntity`（`ForeignKey(ReminderId)`）
- 分支与异常：无方法体控制流
- 调用：无

## 近逐行中文伪代码

1. 引用数据注解与 Schema 注解
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表映射 `[Table("reminder_deliveries")]`
4. 类 `ReminderDeliveryEntity`（非软删）
5. `Id` 主键；`ReminderId`、`UserId` 关联字段
6. `Channel` 默认 `"Web"`；`Status` 默认 `"Created"`
7. `Action` 可空；`PayloadJson` 类型 jsonb，默认空对象 JSON
8. `CreatedAt` 默认 UTC；`RespondedAt` 可空
9. 导航 `Reminder` 外键绑定 `ReminderId`，非空引用
10. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs",
      "label": "ReminderDeliveryEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/ReminderDeliveryEntity.cs", "type": "depends_on" }
  ]
}
```
