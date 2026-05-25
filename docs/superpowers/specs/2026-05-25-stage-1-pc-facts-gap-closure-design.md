# PIM 第 1 阶段 PC 记录事实层查缺补漏设计

## 目的

本轮工作的目标是把第 1 阶段“PC 记录事实层”收口成可验收、可长期使用的能力包。

这不是重做 PC Tracker。现有代码已经实现了 ActivityWatch 原始事件保存、bucket 元数据保存、`source_event_id` 幂等、KeyStats 分钟快照、浏览器页面解释时间线、ActivityWatch 回填入口、混合详情查询和一批单元测试。本轮只做查缺补漏：把事实层是否可信变成服务端可判断、Web 可看见、测试可守住、文档可验收。

## 已有基础

当前仓库已经有这些第 1 阶段基础：

- `pc_aw_buckets` 保存 ActivityWatch bucket 元数据。
- `pc_aw_events` 保存 ActivityWatch window、afk、web 原始事件，并保留 `bucket_id`、`source_event_id`、`data_json`。
- `pc_keystats_samples` 保存 KeyStats 每分钟完整快照和 raw JSON。
- `/api/v1/pc/aw/upload-complete` 按设备、bucket、source event id 幂等写入。
- `/api/v1/pc/keystats/samples` 按设备和分钟 upsert。
- `/api/v1/pc/detail` 返回 window、afk、web-page、web、input-minute 等混合详情记录。
- Windows daemon 已能发现 ActivityWatch bucket，排除 `aw-watcher-input`，上传 window、afk、web bucket。
- Windows daemon 已有 ActivityWatch 最近 14 天回填入口。
- 浏览器页面解释时间线能用 web 页面记录解释浏览器窗口，并保留 raw view。
- 第 0 阶段已提供 daemon heartbeat 和系统状态页基础。

这些实现是本轮设计的基础，不应被重写。

## 非目标

本轮不做以下事情：

- 不重写 ActivityWatch 或 KeyStats 采集器主流程。
- 不进入第 2 阶段智能分类、LLM 规则建议或自然语言纠错。
- 不做复杂图表和大规模 PC Tracker UI 改版。
- 不伪造历史 KeyStats 分钟数据。
- 不把 Web 变成事实判断来源。
- 不做正式 MCP server。
- 不把浏览器页面 bucket 缺失视为事实层失败，因为 window 事件仍可作为回退事实。

## 验收矩阵

新增一份第 1 阶段验收矩阵，逐条映射 `docs/plan.md` 中的要求。

矩阵记录：

- 路线图要求。
- 当前实现证据，包含文件、服务、API 或测试。
- 当前状态：已满足、部分满足、缺失、暂不适用。
- 缺口说明。
- 本轮是否处理。
- 后续阶段备注。

矩阵应覆盖这些重点：

- ActivityWatch bucket 元数据。
- ActivityWatch window / afk / web 原始事件。
- ActivityWatch 原始 event id。
- ActivityWatch `data_json`。
- KeyStats 每日兼容数据。
- KeyStats 每分钟快照。
- KeyStats 分钟 delta。
- 采集缺口检测。
- ActivityWatch 历史回填。
- 原始数据查询。
- 解释时间线查询。
- 浏览器页面和窗口不重复计时。
- 数据质量状态展示。
- daemon 最近上传、最近错误、队列数量、数据源可用性。

这份矩阵可以写入设计文档或独立验收文档。实现阶段优先把它落到 `docs/operations/pc-facts-stage1-acceptance.md`。

## 架构

本轮新增一层“质量观察层”，不改变事实来源。

数据流保持：

```text
Windows daemon -> 服务端上传 API -> 原始事实表 -> 查询/解释服务 -> Web 展示
```

新增质量流：

```text
原始事实表 + daemon heartbeat -> PcTrackerQualityService -> /api/v1/pc/quality -> Web 质量状态
```

服务端仍然是事实判断来源。Web 只展示服务端返回的质量状态、问题和下一步建议。

## 服务端质量摘要

新增 `PcTrackerQualityService`，读取现有表和第 0 阶段 daemon heartbeat。

它应该检查：

- `pc_aw_buckets`：window、afk、web bucket 是否被发现，最近 `seen_at` 和 `last_updated_source` 是否过旧。
- `pc_aw_events`：指定日期范围内是否有 window、afk、web 原始事件，记录是否有 `source_event_id` 和 `data_json`。
- `pc_keystats_samples`：按设备检查最近样本时间、样本间隔、超过 2 分钟缺口、counter reset 或负 delta。
- daemon heartbeat：最近心跳、最近上传时间、最近错误、上传队列数量、ActivityWatch 和 KeyStats 可用性。
- 解释时间线：是否能从原始记录生成非重复的解释记录。

新增 API：

```text
GET /api/v1/pc/quality?date=yyyy-MM-dd
GET /api/v1/pc/quality?dateFrom=yyyy-MM-dd&dateTo=yyyy-MM-dd
```

返回结构应包含：

- `overallStatus`：`Healthy`、`Warning`、`Critical` 或 `Unknown`。
- `components`：AW buckets、AW events、KeyStats samples、daemon upload、interpreted timeline。
- `issues`：结构化问题列表。
- `nextSteps`：用户或下一轮 agent 能执行的建议。
- `checkedAt`：服务端检查时间。

问题列表至少包含：

- 缺少 window bucket。
- 缺少 afk bucket。
- 缺少 web bucket。
- ActivityWatch bucket 很久未 seen。
- 查询范围内没有 window 事件。
- 查询范围内没有 KeyStats 样本。
- KeyStats 样本间隔超过 2 分钟。
- KeyStats counter reset。
- AW 原始记录缺少 `source_event_id`。
- AW 原始记录缺少或无法解析 `data_json`。
- daemon 长时间无心跳。
- daemon 报告最近上传错误。

## 状态规则

质量状态按“事实是否可信”判断，不按“页面有没有数据”判断。

`Healthy`：

- daemon 最近有心跳。
- AW window 和 afk bucket 可见。
- KeyStats 最近有分钟样本。
- 查询范围内没有明显长缺口。
- 原始记录完整性没有明显问题。

`Warning`：

- 浏览器页面 bucket 缺失，但 window 事件可用。
- KeyStats 有超过 2 分钟的采样缺口。
- AW 某类非关键事件当天为空，但 daemon 在线。
- 有少量 legacy AW 记录缺少 `source_event_id`。
- daemon 报告某个非阻塞数据源异常。

`Critical`：

- daemon 很久没有心跳。
- AW window bucket 缺失。
- KeyStats 长时间没有样本。
- 上传持续失败。
- 大量原始记录缺少 `source_event_id` 或 `data_json`。

`Unknown`：

- 新环境尚未采样。
- 查询范围没有足够信息判断。
- 服务端无法读取相关表或 heartbeat。

## Web 展示

Web 做最小展示，不做 PC Tracker 大改版。

PC 记录页顶部显示“今日 PC 数据质量”摘要：

- 状态标签。
- 简短说明。
- 关键问题数量。
- 下一步建议入口。

状态页新增或扩展 PC 采集质量组件：

- ActivityWatch bucket 状态。
- ActivityWatch 原始事件状态。
- KeyStats 样本状态。
- daemon 上传状态。
- 解释时间线状态。

PC 详细数据页空状态区分：

- 真的没有活动记录。
- 采集源不可用。
- 查询范围没有分钟样本。
- 新环境仍在等待采样。

Web 只消费 `/api/v1/pc/quality` 和现有状态 API，不在 TypeScript 中复制质量判断规则。

## 错误处理和边界

浏览器页面 bucket 缺失只产生 warning，因为 window 事件仍是有效事实来源。

KeyStats 历史分钟数据不能回填。历史缺口只能标记，不能伪造。

KeyStats reset 或负 delta 不混入正常趋势。详情记录可以保留 reset 标记，质量摘要必须暴露 reset issue。

legacy AW 记录如果没有 `source_event_id`，继续允许查询，但质量摘要标记 raw completeness 不完整。

raw view 必须继续能查看 window、web、afk 原始事件。解释时间线不能替代原始事件。

服务端无法判断某项质量时返回 `Unknown`，并提供 next step，而不是把未知当作健康。

## 测试策略

后端测试：

- 缺少 window bucket 产生 critical。
- 缺少 web bucket 产生 warning。
- KeyStats 样本间隔超过 2 分钟产生 warning。
- KeyStats counter reset 产生 warning 或 critical，取决于范围和数量。
- daemon 长时间无心跳产生 critical。
- legacy AW 记录缺少 `source_event_id` 被标记为完整性问题。
- raw `data_json` 缺失或无效被标记为完整性问题。
- `web-page` 解释记录不会和浏览器 window 重复计时。
- raw view 仍能返回原始 web 事件。

前端测试或构建验证：

- PC 记录页能渲染质量摘要的 healthy、warning、critical、unknown 状态。
- 状态页能显示 PC 采集质量组件。
- 详情页空状态能根据质量 API 显示不同原因。
- `npm --prefix src/client-web run build` 通过。

全仓验证：

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
```

## 手动验收文档

新增 `docs/operations/pc-facts-stage1-acceptance.md`。

文档应包含：

- 启动 API、Web、Windows daemon。
- 检查 `/health`。
- 检查 `/api/v1/pc/quality`。
- 检查 `/api/v1/pc/detail`。
- 等待两分钟确认 KeyStats 样本连续。
- 触发 ActivityWatch 最近 14 天回填。
- 检查 `pc_aw_buckets`、`pc_aw_events`、`pc_keystats_samples`。
- 在 Web 查看 PC 记录页、状态页、详细数据页。
- 常见失败处理：ActivityWatch 未启动、浏览器插件未安装、KeyStats 不可用、daemon 未登录、上传失败。

## 完成定义

本轮完成后应满足：

- 第 1 阶段要求有验收矩阵，不再只靠印象判断完成度。
- 服务端能生成 PC 事实层质量摘要。
- Web 能看见 PC 数据质量和下一步建议。
- 采集缺口、reset、raw 完整性问题能被看见。
- 重复上传不重复事实的现有能力继续被测试保护。
- 浏览器页面和窗口不会重复计时的现有能力继续被测试保护。
- 手动验收文档足够让下一轮对新环境和现有环境做检查。

## 后续预留

第 2 阶段 PC 记录理解层可以复用质量摘要，避免在低质量事实上生成分类建议。

Today 页面可以后续引用同一质量 API，把 PC 数据异常放到每日入口。

未来 Android 多设备活动也可以复用相同质量组件模式：原始来源、样本连续性、daemon 心跳、解释记录完整性。

未来 MCP 查询工具可以返回质量摘要，让 AI 知道某个日期的事实层是否可信。
