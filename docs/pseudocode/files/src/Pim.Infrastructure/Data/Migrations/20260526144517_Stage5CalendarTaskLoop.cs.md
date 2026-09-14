# src/Pim.Infrastructure/Data/Migrations/20260526144517_Stage5CalendarTaskLoop.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `Stage5CalendarTaskLoop`——为 tasks/events/calendars 增加软删操作溯源、任务 planned_end、事件循环/ICS 相关列及查询索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移流水线 / `dotnet ef database update`

## 函数级结构化伪代码

### Stage5CalendarTaskLoop : Migration
#### void Up(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：ALTER 表、CREATE INDEX
- 步骤：
  1. **tasks**：加 `deleted_by_operation_id`(uuid 可空)、`deleted_by_operation_kind`(varchar64 可空)、`planned_end`(timestamptz 可空)
  2. **events**：加删除操作溯源两列；`exdates_json` jsonb 默认 `[]`；`external_metadata_json` jsonb 默认 `{}`；`is_all_day` bool 默认 false；`recurrence_id` varchar255 可空；`recurrence_metadata_json` jsonb 默认 `{}`；`source_ics_component` text 可空；`source_time_zone_id` varchar100 可空；`source_uid` varchar255 可空；`time_zone_id` varchar100 可空
  3. **calendars**：加删除操作溯源两列
  4. 索引：
     - tasks: `deleted_by_operation_id`；`(user_id, deleted_at)`；`(user_id, dtstart, planned_end)`
     - events: `(deleted_at, dtstart)`；`deleted_by_operation_id`；`source_uid`
     - calendars: `deleted_by_operation_id`；`(user_id, deleted_at)`
- 分支与异常：迁移失败由 EF 事务回滚
- 调用：`migrationBuilder.AddColumn` / `CreateIndex`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：DROP INDEX、DROP COLUMN
- 步骤：
  1. 按 Up 的逆序删除全部新建索引
  2. 删除 tasks/events/calendars 上 Up 中新增的全部列
- 分支与异常：同 EF 迁移
- 调用：`DropIndex` / `DropColumn`

## 近逐行中文伪代码

1. 命名空间 `Pim.Infrastructure.Data.Migrations`；`#nullable disable`
2. partial 类 `Stage5CalendarTaskLoop` 继承 `Migration`
3. `Up`：tasks 加 deleted_by_operation_id/kind、planned_end
4. events 加 deleted 溯源、exdates/external_metadata/is_all_day/recurrence_*/source_*/time_zone_id
5. calendars 加 deleted 溯源
6. 创建 8 个索引（tasks 3 + events 3 + calendars 2）
7. `Down`：先 Drop 全部索引
8. 再 Drop tasks 三列、events 十一列、calendars 两列

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260526144517_Stage5CalendarTaskLoop.cs",
      "label": "Stage5CalendarTaskLoop",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260526144517_Stage5CalendarTaskLoop.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260526144517_Stage5CalendarTaskLoop.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260526144517_Stage5CalendarTaskLoop.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260526144517_Stage5CalendarTaskLoop.cs", "type": "depends_on" }
  ]
}
```
