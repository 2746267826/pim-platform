# src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：创建任务执行时段表 `task_execution_segments`，关联 `tasks`，支持规划/确认来源与软删除字段。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移流水线；Calendar/Planning 任务执行段实体与仓储

## 函数级结构化伪代码

### AddTaskExecutionSegments
#### `void Up(MigrationBuilder migrationBuilder)`
- 输入：迁移构建器
- 输出：无
- 副作用：建表、FK Cascade 到 `tasks`、四条索引
- 步骤：
  1. CreateTable `task_execution_segments`：id、task_id、user_id、starts_at/ends_at、status/source（max 40）、planning_reason、confirmation_id、created/updated、deleted_at
  2. PK `PK_task_execution_segments`；FK `FK_task_execution_segments_tasks_task_id` → `tasks.id` Cascade
  3. 索引：confirmation_id、task_id、user_id、复合 `(user_id, task_id, starts_at)`
- 分支与异常：`tasks` 不存在则迁移失败
- 调用：`CreateTable` / `CreateIndex`

#### `void Down(MigrationBuilder migrationBuilder)`
- 输入：迁移构建器
- 输出：无
- 副作用：删除 `task_execution_segments`
- 步骤：1. DropTable
- 分支与异常：依赖未清理则失败
- 调用：`DropTable`

## 近逐行中文伪代码

1. 分部类 `AddTaskExecutionSegments` 继承 `Migration`
2. `Up`：CreateTable 定义列类型与 maxLength
3. 主键 id；外键 task_id → tasks Cascade
4. 创建 confirmation_id / task_id / user_id / 三列复合索引
5. `Down`：DropTable `task_execution_segments`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs",
      "label": "AddTaskExecutionSegments",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707192023_AddTaskExecutionSegments.cs", "to": "tasks", "type": "depends_on" }
  ]
}
```
