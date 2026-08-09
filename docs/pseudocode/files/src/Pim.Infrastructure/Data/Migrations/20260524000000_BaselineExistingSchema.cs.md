# src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF Core 基线迁移 `BaselineExistingSchema`：一次性创建既有核心/日历/PC 追踪相关表、外键与索引（约 18 表）。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`、`Npgsql.EntityFrameworkCore.PostgreSQL.Metadata`
- 被谁使用：EF 迁移历史起点；后续 `AddAiGateway` 等迁移叠在其上

## 函数级结构化伪代码

### BaselineExistingSchema
#### protected override void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：创建基线 schema
- 步骤：
  1. 独立表（无 FK）：
     - `calendars`：用户日历（name/color/kind/is_default/软删）
     - `outlook_connections`：加密 token 与订阅/同步时间
     - `pc_activity_category_rules`：PC 活动分类规则（conditions jsonb、priority）
     - `pc_activity_classification_suggestions`：聚类建议与 LLM 响应
     - `pc_app_categories`：应用模式→分类
     - `pc_aw_buckets`：ActivityWatch bucket 元数据
     - `pc_aw_events`：AW 事件（app/window/afk/data_json）
     - `pc_keystats_daily`：按日键鼠统计
     - `pc_keystats_samples`：分钟级采样（key_counts/app_stats/raw jsonb）
     - `pending_confirmations`：待确认操作
     - `scheduling_feedback`：排程选项反馈
     - `users`：账号（username/email 等，软删）
  2. 依赖 calendars：
     - `events`：日历事件（FK calendar cascade；含 outlook_event_id）
     - `tasks`：任务（FK calendar 可选；自引用 parent_task）
  3. 依赖 pc_keystats_daily：
     - `pc_keystats_app_breakdown`、`pc_keystats_key_counts`（cascade）
  4. 依赖 users：
     - `login_attempts`、`refresh_tokens`（tokens cascade）
  5. 批量 CreateIndex：用户/uid/设备时间唯一约束、规则名唯一、AW 源事件唯一等
- 分支与异常：无（DDL）
- 调用：`CreateTable`、`CreateIndex`、`ForeignKey`

#### protected override void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：按依赖逆序 Drop 全部基线表
- 步骤：先子表（events/tasks/keystats 子表/login/refresh）再父表
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. `#nullable disable`；namespace Migrations；partial `BaselineExistingSchema : Migration`
2. Up 依次 CreateTable 上述 18 张表
3. events.calendar_id → calendars cascade；tasks 可选 calendar + parent 自引用
4. keystats 子表 daily_snapshot_id cascade；login_attempts/refresh_tokens → users
5. 大量 IX_/ux_ 索引（设备+时间唯一、规则名、pending cluster 等）
6. Down：DropTable 全表回滚基线

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs",
      "label": "BaselineExistingSchema",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260524000000_BaselineExistingSchema.cs", "type": "depends_on" }
  ]
}
```
