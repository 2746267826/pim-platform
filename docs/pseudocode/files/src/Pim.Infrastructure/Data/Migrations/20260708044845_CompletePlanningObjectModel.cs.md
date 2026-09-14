# src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `CompletePlanningObjectModel`：扩展 `tasks` 规划字段，并创建规划对象模型相关表（占位、可用窗口、领域项目、习惯、清单、任务本等）与索引/外键。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移管道；Planning / Calendar 任务规划持久化

## 函数级结构化伪代码

### CompletePlanningObjectModel
#### `void Up(MigrationBuilder migrationBuilder)`
- 输入：`migrationBuilder`
- 输出：无
- 副作用：改 `tasks`、建多表、建索引、加 FK
- 步骤：
  1. 给 `tasks` 增加列：`domain_project_id`、`review_outcome`、`source`、`state_reason`、`task_book_id`（字符串列默认 `""`，Guid 可空）
  2. 创建 `ai_planning_placeholders`（用户、标题、起止、原因、来源、状态、确认 id、软删）
  3. 创建 `availability_windows`（用户、标题、起止、kind、source、软删）
  4. 创建 `domain_projects`（用户、名称、描述、状态、软删）
  5. 创建 `habit_routines`（用户、标题、cadence、source、status、`rule_json` 默认 `{}`、软删）
  6. 创建 `task_checklist_items`（`task_id` FK Cascade→`tasks`、标题、`is_done`、`sort_order`、软删）
  7. 创建 `task_books`（可空 `domain_project_id` FK→`domain_projects`、kind、status、软删）
  8. 创建 `habit_occurrences`（`habit_routine_id` FK Cascade→`habit_routines`、起止、status、source、confirmation_id、软删）
  9. 为 tasks 与各新表建查询/唯一索引；`tasks` 外键挂到 `domain_projects` 与 `task_books`
- 分支与异常：迁移失败由 EF 抛出
- 调用：`AddColumn`、`CreateTable`、`CreateIndex`、`AddForeignKey`

#### `void Down(MigrationBuilder migrationBuilder)`
- 输入：`migrationBuilder`
- 输出：无
- 副作用：逆序撤销 Up
- 步骤：
  1. 删除 tasks 上两个 FK
  2. 删除表：占位、可用窗口、习惯实例、任务本、清单项、习惯例程、领域项目
  3. 删除 tasks 相关索引与五列
- 分支与异常：无
- 调用：`DropForeignKey`、`DropTable`、`DropIndex`、`DropColumn`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；分部类 `CompletePlanningObjectModel`
2. `Up`：先 `AddColumn` 扩展 `tasks` 五列
3. 建 `ai_planning_placeholders` 与时间/状态索引
4. 建 `availability_windows` 与 kind/时间索引
5. 建 `domain_projects`（用户+名称唯一、用户+状态索引）
6. 建 `habit_routines`；建 `task_checklist_items`（级联删任务）
7. 建 `task_books` 挂领域项目；建 `habit_occurrences` 挂习惯例程（级联）
8. 为 tasks 建 domain/task_book 索引；`AddForeignKey` 连接 projects 与 books
9. `Down`：先拆 tasks FK，再按依赖删表，最后删 tasks 索引与列
10. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs",
      "label": "CompletePlanningObjectModel",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708044845_CompletePlanningObjectModel.cs", "to": "src/Pim.Core/Planning/PlanningDtos.cs", "type": "depends_on" }
  ]
}
```
