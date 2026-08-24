# PASS-007 | docs/operations/pc-facts-stage1-acceptance.md | 合格 | PC 事实层核心查询与质量报告（除 DOC-014 提示外）
- 验证方式：read_file + grep `MapGet.*pc/quality` `MapGet.*pc/detail` `MapGet.*pc/aw/timeline` `PcTrackerQualityService`
- 验证点：文档 Acceptance Matrix 15 行与 Local Verification、Manual Runtime、Web Checks
- 代码实际：`PcTrackerModule.cs:94` `GET /summary`、`:111` `GET /aw/timeline`、`:128` `GET /aw/heatmap`、`:166` `GET /detail`（含 `view=raw` 分支）、`:193` `GET /quality` 均存在；`PcTrackerQualityService.cs` 对桶缺失、事件缺失、samples 缺口、daemon 心跳做组件级报告；前端 `PcTrackerPage.tsx` 展示 quality 面板与 interpreted/raw 切换
- 结论：除 DOC-014 所述日级兼容路径的表述可进一步明确外，事实层保存、幂等 `SourceEventId`、rawJson、回填、双视图与质量面板均与文档一致，标记为通过
