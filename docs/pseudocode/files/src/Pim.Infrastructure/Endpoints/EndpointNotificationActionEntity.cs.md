# src/Pim.Infrastructure/Endpoints/EndpointNotificationActionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：映射表 `endpoint_notification_actions`，记录端点通知动作（设备、风险、结果、确认与关联对象）。
- 主要依赖：`System.ComponentModel.DataAnnotations` / `Schema`
- 被谁使用：`PimDbContext` 或端点通知相关仓储/服务

## 函数级结构化伪代码

### EndpointNotificationActionEntity
#### 属性集（无行为方法）
- 输入：各属性赋值
- 输出：行状态
- 副作用：无（纯实体）
- 步骤：
  1. `Id` 主键默认 NewGuid
  2. `UserId` 用户
  3. `DeviceId` 设备 Id，最长 160
  4. `Action` / `RiskLevel` / `Result` 动作与风险与结果
  5. `DetailUrl` / `Message` 可选详情与消息
  6. `ConfirmationId` 可选确认 Id
  7. `RelatedObjectType` / `RelatedObjectId` 可选关联对象
  8. `CreatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Infrastructure.Endpoints`
3. 表 `endpoint_notification_actions`；类 `EndpointNotificationActionEntity`（非 sealed）
4. Id/UserId/DeviceId/Action/RiskLevel/Result 必填风格字段
5. DetailUrl/Message/ConfirmationId/RelatedObjectType/RelatedObjectId 可空
6. CreatedAt 默认 UtcNow

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Endpoints/EndpointNotificationActionEntity.cs",
      "label": "EndpointNotificationActionEntity",
      "path": "src/Pim.Infrastructure/Endpoints/EndpointNotificationActionEntity.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Endpoints/EndpointNotificationActionEntity.cs.md",
      "layer": "infrastructure",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointNotificationActionEntity.cs", "type": "depends_on" }
  ]
}
```
