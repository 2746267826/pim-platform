# src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：映射表 `pending_confirmations` 的待用户确认操作实体（类型、摘要、jsonb 载荷、状态）。
- 主要依赖：`System.ComponentModel.DataAnnotations`/`Schema`
- 被谁使用：Calendar 模块确认流与 EF 模型（与运维侧 `OperationConfirmation` 不同表）

## 函数级结构化伪代码

### PendingConfirmationEntity
#### 属性集（无行为方法）
- 输入：各属性赋值
- 输出：行状态
- 副作用：无（纯 POCO）
- 步骤：
  1. `Id`：主键 Guid，默认 `NewGuid`
  2. `UserId`：所属用户
  3. `Type`：确认类型，最长 50
  4. `Summary`：摘要文本
  5. `Payload`：jsonb 载荷，默认 `"{}"`
  6. `Status`：状态，默认 `"pending"`，最长 20
  7. `ConfirmedAt`：确认时间，可空
  8. `CreatedAt`：创建时间，默认 UTC 现在
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Module.Calendar.Entities`
3. 表 `pending_confirmations`；类 `PendingConfirmationEntity`
4. 主键、用户、类型、摘要
5. `payload` 列类型 jsonb，默认空对象
6. 状态默认 pending；可选 `confirmed_at`；`created_at` 默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs",
      "label": "PendingConfirmationEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/PendingConfirmationEntity.cs", "type": "depends_on" }
  ]
}
```
