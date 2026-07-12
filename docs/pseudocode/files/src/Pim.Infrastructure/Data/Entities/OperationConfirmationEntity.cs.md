# src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：高风险操作二次确认持久化实体，映射表 `operation_confirmations`，记录请求、预览、状态与执行结果。
- 主要依赖：`Pim.Core.Operations`（`OperationConfirmationStatus` 默认状态字符串）；DataAnnotations/Schema
- 被谁使用：`PimDbContext.OperationConfirmations`；`OperationConfirmationService` 全生命周期；各模块经确认服务间接依赖

## 函数级结构化伪代码

### OperationConfirmationEntity
#### 属性组（密封 EF 实体，无业务方法）
- 输入：`OperationConfirmationService` 创建/更新字段
- 输出：表 `operation_confirmations` 一行
- 副作用：无；状态时间戳字段由服务在确认/拒绝/执行时写入
- 步骤：
  1. `Id`：主键，默认新 Guid，列 `id`
  2. `RequestedByUserId`：请求用户，可空，列 `requested_by_user_id`
  3. `OperationType`：操作类型，最长 128，列 `operation_type`
  4. `Summary`：摘要文本，列 `summary`
  5. `RiskLevel`：风险等级字符串，最长 32，列 `risk_level`
  6. `Source`：来源，最长 64，列 `source`
  7. `PayloadJson` / `PreviewJson`：jsonb 载荷与预览，默认 `{}`
  8. `Status`：状态字符串，最长 32，默认 `OperationConfirmationStatus.Pending.ToString()`（即 `"Pending"`）
  9. `ExpiresAt`：过期时间，列 `expires_at`
  10. `CreatedAt`：创建时间，默认 UtcNow
  11. `ConfirmedAt` / `RejectedAt` / `ExecutedAt`：可选时间戳
  12. `ResultJson`：可选 jsonb 执行结果
  13. `CorrelationId`：可选关联 Id，最长 128
- 分支与异常：本实体无状态机；状态转换与鉴权在 `OperationConfirmationService`
- 调用：服务 `Map` 到 `OperationConfirmationDto`

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`Pim.Core.Operations`
2. 命名空间：`Pim.Infrastructure.Data.Entities`
3. 表：`operation_confirmations`
4. 密封类 `OperationConfirmationEntity`
5. `Id` 主键默认新 Guid
6. `RequestedByUserId` 可空用户
7. `OperationType` MaxLength 128
8. `Summary` 摘要
9. `RiskLevel` MaxLength 32
10. `Source` MaxLength 64
11. `PayloadJson` jsonb 默认 `{}`
12. `PreviewJson` jsonb 默认 `{}`
13. `Status` 默认 Pending 枚举名字符串
14. `ExpiresAt` 过期时刻
15. `CreatedAt` 默认 UtcNow
16. `ConfirmedAt` / `RejectedAt` / `ExecutedAt` 可空
17. `ResultJson` 可空 jsonb
18. `CorrelationId` 可空 MaxLength 128

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs",
      "label": "OperationConfirmationEntity",
      "path": "src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Infrastructure/Data/Entities/OperationConfirmationEntity.cs", "type": "depends_on" }
  ]
}
```
