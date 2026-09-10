# src/modules/Pim.Module.Calendar/Entities/EventEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历事件持久化实体，映射表 `events`，支持 iCal/Outlook 元数据、软删除与关联日历。
- 主要依赖：`ISoftDeletable`、`CalendarEntity`、DataAnnotations/Schema
- 被谁使用：`CalendarService`、`PlanningModelService`、Outlook/ICS 同步、EF 迁移与 `PimDbContext`

## 函数级结构化伪代码

### EventEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段读写
- 副作用：无
- 步骤：
  1. 表名 `events`；实现 `ISoftDeletable`
  2. 主键 `Id` 默认 `NewGuid`
  3. 归属 `CalendarId`；导航 `Calendar`
  4. iCal 核心：`Uid`/`Title`/`Description`/`Location`/`DtStart`/`DtEnd`/`DtStamp`/`RRule`/`Status`/`Organizer`
  5. 来源：`Source` 默认 `manual`；`OutlookEventId`/`OutlookChangeKey`/`OutlookEtag`/`SourceUid`/`SourceIcsComponent`
  6. 时区：`IsAllDay`、`TimeZoneId`、`SourceTimeZoneId`
  7. 规划：`SchedulePlanId`
  8. jsonb：`ExternalMetadataJson` 默认 `{}`；`ExDatesJson` 默认 `[]`；`RecurrenceMetadataJson` 默认 `{}`；`RecurrenceId`
  9. 软删审计：`DeletedByOperationId`/`DeletedByOperationKind`/`DeletedAt`
  10. 时间戳：`CreatedAt`/`UpdatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema、`Pim.Core.Data`
2. 命名空间 `Pim.Module.Calendar.Entities`
3. `[Table("events")]` 类实现 `ISoftDeletable`
4. 声明主键、日历外键、UID、标题、描述、地点
5. 起止时间、时间戳、RRule、状态默认 CONFIRMED、组织者
6. 来源与 Outlook 标识、排程计划 ID、全天与时区字段
7. 源 UID、变更键、ETag、ICS 组件原文
8. 外部元数据与例外日期/重复元数据 jsonb
9. 删除操作追溯字段与 created/updated/deleted 时间
10. FK 导航到 `CalendarEntity`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs",
      "label": "EventEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/EventEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "to": "src/Pim.Core/Data", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "to": "src/modules/Pim.Module.Calendar/Entities/CalendarEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/PlanningModelService.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" }
  ]
}
```
