# src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `AddReportArtifacts`：创建报告产物表 `report_artifacts` 与建议表 `report_suggestions` 及索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移管道；报告/建议与确认流持久化

## 函数级结构化伪代码

### AddReportArtifacts
#### `void Up(MigrationBuilder migrationBuilder)`
- 输入：`migrationBuilder`
- 输出：无
- 副作用：建两表与索引
- 步骤：
  1. 创建 `report_artifacts`：`id` PK；`user_id`；`kind`；可空 `project_id`；`risk_level`（默认 `L0AutomaticArtifact`）；`inputs_json`/`metrics_json` jsonb；`content_markdown`；`generated_at`（默认 `now()`）；`status`（默认 `Active`）；时间戳与软删 `deleted_at`
  2. 创建 `report_suggestions`：`id` PK；`report_id` FK Cascade→`report_artifacts`；`user_id`；`action`；`summary`；`changed_fields_json`（默认 `[]`）；`payload_json`；`status`（默认 `Open`）；可空 `confirmation_id`；时间戳
  3. 索引：artifacts 按 `(user_id, kind, generated_at)`、`(user_id, project_id)`；suggestions 按 `confirmation_id`、`report_id`、`(user_id, status)`
- 分支与异常：迁移失败由 EF 抛出
- 调用：`CreateTable`、`CreateIndex`、`ForeignKey`

#### `void Down(MigrationBuilder migrationBuilder)`
- 输入：`migrationBuilder`
- 输出：无
- 副作用：先删 suggestions 再删 artifacts
- 步骤：
  1. `DropTable report_suggestions`
  2. `DropTable report_artifacts`
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；分部类 `AddReportArtifacts`
2. `Up`：建 `report_artifacts`（含风险级别默认 L0、jsonb 输入/指标）
3. 建 `report_suggestions`，FK 级联删除报告
4. 为报告与建议建用户/类型/项目/状态相关索引
5. `Down`：先删建议表再删报告表
6. （文件结束）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.cs",
      "label": "AddReportArtifacts",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708070711_AddReportArtifacts.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "depends_on" }
  ]
}
```
