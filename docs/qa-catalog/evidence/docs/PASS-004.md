# PASS-004 | docs/operations/pc-activity-understanding-stage2-acceptance.md | 合格 | 本地规则分类闭环
- 验证方式：read_file + grep `MapGet.*classification` `MapPost.*recompute` `MapPost.*preview` `ActivityClassificationRuleService` + curl 幂等路径检查
- 验证点：文档 Scope 列 14 项（持久化快照、持久分类优先、保护已纠正、规则 preview/apply、建议队列、快捷纠错、专用管理页、可调最小分类时长、AI 延后）
- 代码实际：`PcTrackerModule.cs:255-541` 存在 `GET /classification/rules`、`GET /classification/suggestions`、`POST /classification/rules/preview`、`POST /classification/rules/apply`、`POST /classification/suggestions/{id}/preview|apply`、`POST /classification/recompute`、`PUT /classification/settings`；`PcTrackerService.cs:429` 处理 `recommendedMinimumDuration` 仅影响平滑与分组，未删原始 `pc_aw_events/pc_keystats_samples`
- 结论：承诺的端点与服务均存在，时长配置不删原始事实符合描述，标记为通过
