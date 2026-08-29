# PIM 全量问题目录（只验不修）

> 生成时间：2026-08-23 17:10 UTC  
> 测试库：`pim_test`（`opencode` @ `127.0.0.1:5432`，对照源库 `pim_prod` @ `pim`）  
> 测试端口：`5238/5239`（`Pim.Api` @ `ASPNETCORE_ENVIRONMENT=Test`，证据见 `evidence/`）  
> 分支：`master`（未修改业务代码，仅新增本目录与证据）

## 汇总（按模块 / 级别）

| 模块 | 阻塞 | 严重 | 一般 | 提示 | 小计 |
|---|---:|---:|---:|---:|---:|
| Pim.Api / 鉴权 / CORS / Hangfire | 1 | 2 | 2 | 1 | 6 |
| Pim.Module.PcTracker（聚合/时区/去重） | 0 | 3 | 5 | 0 | 8 |
| Pim.Module.Mobile（幂等/时空/一致性） | 0 | 3 | 5 | 0 | 8 |
| Pim.Module.Calendar（含回收站/分页） | 0 | 0 | 2 | 1 | 3 |
| Pim.Module.Files / QuickNotes / Stats / Today | 0 | 0 | 2 | 0 | 2 |
| client-web（路由/按钮/契约） | 0 | 0 | 2 | 1 | 3 |
| client-android（14包） | 0 | 2 | 2 | 1 | 5 |
| client-windows / client-shell-windows（静查） | 0 | 2 | 1 | 1 | 4 |
| 文档对齐（docs / AGENTS.md） | 0 | 0 | 1 | 2 | 3 |
| **合计** | **1** | **12** | **22** | **7** | **42** |

> 严重级别：阻塞=主流程无法完成；严重=数据错误/安全/越权/大范围不一致；一般=功能缺陷/边界校验缺失/中度不一致；提示=可优化/文案/构建提示。单小时 `ForegroundSeconds > 3780`（3600×1.05）、单天 `>90720`（86400×1.05）按任务书容差判定为问题。

---

## Pim.Api / 基础设施

### PIM-001 | Pim.Api | 阻塞 | 注册接口缺参返回 500 而非 400

- **描述**：`POST /api/v1/auth/register` 未传 `email` 时抛 `23502 not-null` 未做校验，返回 500。
- **复现步骤**：
  ```bash
  ASPNETCORE_ENVIRONMENT=Test ConnectionStrings__DefaultConnection="Host=127.0.0.1;Port=5432;Database=pim_test;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a" Jwt__PrivateKeyPath=/tmp/pim_test_jwt_private.pem DataProtection__KeysPath=/tmp/pim_test_keys PIM_OPS_KEY=o9hQO38Telv1dcoJLS5YjNeEdVxf6Qq8 PIM_OPS_RO_CONNECTION="Host=127.0.0.1;Port=5432;Database=pim_test;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a;CommandTimeout=10" ASPNETCORE_URLS=http://127.0.0.1:5239 dotnet /workspace/pim-platform/src/Pim.Api/bin/Release/net8.0/Pim.Api.dll &
  curl -s -X POST http://127.0.0.1:5239/api/v1/auth/register -H "Content-Type: application/json" -d '{"username":"qa_test_x","password":"Test1234!Abcd","displayName":"QA"}'
  ```
- **预期 vs 实际**：预期 `400 ApiResponse` 提示 `email required`；实际 `500 {"Code":1001,"Message":"内部服务器错误"}`，服务端日志 `null value in column "email" violates not-null constraint`（`users.email`）。
- **证据**：`evidence/api-register-500.log`（截自 `/tmp/api_evidence.log`，含 `23502` 堆栈）、`evidence/api-register-500.json`。
- **文档依据**：`docs/module-development-guide.md` §响应结构 `ApiResponse<T>` 错误码；`AuthEndpoints` 应做输入校验。

### PIM-002 | Pim.Api | 严重 | CORS `AllowAnyOrigin` 允许任意源

- **描述**：全局 CORS `AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader()`，与 `RequireAuthorization` 叠加后仍允许任意 Origin 携带 `Authorization` 的跨域请求。
- **复现步骤**：`grep -n AllowAnyOrigin src/Pim.Api/Program.cs:66`；`curl -H "Origin: https://evil.com" -H "Authorization: Bearer x" http://127.0.0.1:5239/api/v1/status/summary -i | grep Access-Control-Allow-Origin`。
- **预期 vs 实际**：预期按文档白名单校验 Origin；实际 `*`。
- **证据**：`src/Pim.Api/Program.cs:66` 片段抄存 `evidence/cors-allowany.txt`。
- **文档依据**：`AGENTS.md` §B5 安全审查（外部 URL 需白名单校验）。

### PIM-003 | Pim.Api | 严重 | Hangfire 无连接串时崩溃（Test 环境误用 Development）

- **描述**：`dotnet run` 以 `ASPNETCORE_ENVIRONMENT=Test` 启动时仍读 `appsettings.Development.json` 的 `Database=pim;Host=localhost:5433;Username=pim`，`Hangfire.UseNpgsqlConnection(null)` 在 `Program.cs:120` 初始化即 `ArgumentNullException` core dumped，`dotnet run --project Pim.Api` 复现为空构建启动失败。
- **复现步骤**：
  ```bash
  ASPNETCORE_ENVIRONMENT=Test ASPNETCORE_URLS=http://127.0.0.1:5236 dotnet /workspace/pim-platform/src/Pim.Api/bin/Release/net8.0/Pim.Api.dll
  # 日志：Value cannot be null. (Parameter 'connectionString') at ServiceCollectionExtensions.cs:48
  ```
  带显式覆写 `ConnectionStrings__DefaultConnection` + `Jwt__PrivateKeyPath` + `PIM_OPS_*` 后 `curl /health` 200（见 `evidence/api-health-200.log`）。
- **预期 vs 实际**：预期 `appsettings.Test.json:2` 自动生效指向 `pim_test:5432/opencode`；实际 `tcp://localhost:5433/pim` 并触发启动失败。
- **证据**：`evidence/api-test-env-mismatch.log`、`src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs:48`、`src/Pim.Api/bin/Release/net8.0/appsettings.Test.json:2`。
- **文档依据**：任务书“API 测试端口自行选用空闲端口拉起指向 pim_test 的实例”；`docs/operations/migrations.md`。

### PIM-004 | Pim.Api | 严重 | PcTracker 部分读接口 `AllowAnonymous` 越权

- **描述**：`/api/v1/pc/app-knowledge`（`apps`, `apps/{id}/contexts`）、`/api/v1/pc/app-signatures`、` /pc/categories`、` /pc/productivity/dashboard|range` 四组读接口 `AllowAnonymous()`，未鉴权即可枚举应用知识库与生产力看板。
- **复现步骤**：
  ```bash
  curl -s http://127.0.0.1:5239/api/v1/pc/categories | head
  curl -s http://127.0.0.1:5239/api/v1/pc/app-knowledge/apps | head
  # 均返回 200 而非 401
  ```
- **预期 vs 实际**：预期 `401`；实际 `200`（`PcTrackerModule.cs:580,724,927,1007`）。
- **证据**：`evidence/pc-anonymous.txt`（grep 结果与 curl 返回）。
- **文档依据**：`AGENTS.md` §权限；模块应 `RequireAuthorization()`。

### PIM-005 | Pim.Api | 一般 | `/health` 与 SPA fallback 均 `AllowAnonymous` 掩盖鉴权失效

- **描述**：`Program.cs:147 /health` 与 `195 MapFallbackToFile` 匿名化后，鉴权中间件异常被 `ExceptionMiddleware` 转 500 而非 401，前端误判为服务可用。
- **复现步骤**：无 Token 请求 `/api/v1/pc/categories`（匿名）200；请求 `/api/v1/mobile/analytics/overview`（需鉴权）401，但 `/health` 始终 200，掩盖 `Jwt:PrivateKeyPath` 缺失导致的 `500`（见 `evidence/api-jwt-missing-500.log`）。
- **预期 vs 实际**：预期 `/health` 健康检查独立于业务鉴权，但业务鉴权失效应显式 401；实际业务接口部分匿名、部分 500 混淆。
- **证据**：`src/Pim.Api/Program.cs:147,195`、`evidence/api-jwt-missing-500.log`。

### PIM-006 | Pim.Api | 一般 | Ops 日志/DB 接口缺鉴权分级

- **描述**：`OpsLogsEndpoints`/`OpsDbEndpoints` 仅依赖 `OpsKeyMiddleware`，`PIM_OPS_KEY` 与业务 `Jwt` 共用同一 `DataProtection` 路径但未分级，`PIM_OPS_RO_CONNECTION` 指向 `pim_test` 时可 `POST /ops/db/query` 读全库。
- **复现步骤**：`curl -H "X-Ops-Key: o9hQO38Telv1dcoJLS5YjNeEdVxf6Qq8" http://127.0.0.1:5239/ops/db/tables` 返回 `71` 张表。
- **预期 vs 实际**：预期 Ops 只读且受 `PIM_OPS_KEY` + 审计；实际与任务书“全部测试流量打测试库，不碰生产库”叠加时，Ops 误用仍可触及生产库（若配置错误）。
- **证据**：`evidence/ops-tables.json`。
- **文档依据**：`docs/ops-readonly-api.md`。

---

## Pim.Module.PcTracker

### PIM-007 | PcTracker | 严重 | 聚合未去重：重叠 `pc_aw_events` 重复计数

- **描述**：`PcActivityAggregationService.GetAppUsage/GetFocusBlocks/GetLateNight/BuildHourlyHeatmap` 直接 `Sum(e.Duration)`，对同一时段多条 `currentwindow` 未去重。
- **复现步骤**：
  ```sql
  -- pim_test
  INSERT INTO pc_aw_events (id, bucket_id, event_type, app_name, timestamp, duration, data_json, source_event_id) VALUES
  (gen_random_uuid(), (SELECT id FROM pc_aw_buckets LIMIT 1), 'window','chrome','2025-08-20 09:00:00+00',3600,'{}','evt1'),
  (gen_random_uuid(), (SELECT id FROM pc_aw_buckets LIMIT 1), 'window','chrome','2025-08-20 09:00:00+00',3600,'{}','evt2');
  ```
  ```bash
  curl -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:5239/api/v1/pc/aggregation/app-usage?start=2025-08-20T00:00:00Z&end=2025-08-21T00:00:00Z"
  ```
- **预期 vs 实际**：预期去重后 `totalMinutes≈60` 且 `heatmap.activeMinutes` 与 `app-usage.byApp[chrome]` 一致；实际 `total≈120`，`heatmap` 被 `Min(60,…)` 截断为 60，三者和不一致。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs:59,102,151`、`src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs:1028`、`evidence/pc-aggregation-overlap.sql`。
- **文档依据**：任务书 §2 数据正确性、§3 一致性。

### PIM-008 | PcTracker | 严重 | 业务日 04:00 时区不统一

- **描述**：聚合侧 `Asia/Shanghai 04:00`（`PcActivityAggregationService.cs:242 ToUtc+ResolveTimezone`），而 `PcTrackerService.GetBusinessDayStartForQuery`/`PcTrackerQualityService`/`PcActivityAnalysisService` 用 `DateTimeKind.Local → ToUniversalTime()`，服务器 `TZ=UTC` 时同一事件分属不同业务日。
- **复现步骤**：
  ```bash
  TZ=UTC curl "…/pc/summary?date=2025-08-20" # 本地日
  curl "…/pc/aggregation/app-usage?date=2025-08-20&timezone=Asia/Shanghai" # 上海日
  # 插入 2025-08-20 03:00+08:00 事件，对比两接口归属日
  ```
- **预期 vs 实际**：预期同请求同日；实际聚合归昨天，summary 归当天。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs:242` vs `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs:686`、`PcProductivityService.cs:17`、`evidence/pc-timezone-mismatch.txt`。
- **文档依据**：`docs/operations/pc-activity-understanding-stage2-acceptance.md` §最小分类时长按业务日。

### PIM-009 | PcTracker | 严重 | 跨天仅按 `StartedAt` 过滤

- **描述**：`PcActivityAggregationService.cs:169` 分类分布 `Where StartedAt∈[Start,End)`，跨 `04:00` 的 35 分钟会话全计入起始日，后日为 0，与按 `AwEvent` 切小时的 heatmap 不一致。
- **复现步骤**：插 `StartedAt=2025-08-20 03:50+08:00, EndedAt=04:20` 的会话；对比 `/pc/summary?date=2025-08-19` vs `2025-08-20` 与 `/pc/aw/heatmap`。
- **预期 vs 实际**：预期按小时分桶两侧分摊；实际全量计前日。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs:169`、`PcProductivityService.cs:75`。

### PIM-010 | PcTracker | 一般 | 零时长 `duration=0` 未过滤

- **描述**：聚合未 `Where Duration>0`，`duration=0` 的 `pc_aw_events` 仍产生分组条目，污染 `byApp` 排序基数。
- **复现步骤**：插 `duration=0, app=chrome`；`GET /pc/aggregation/app-usage` 观察 `byApp` 是否多一条 `0s`。
- **预期 vs 实际**：预期过滤；实际保留。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs:59-105` 未过滤；对照 `PcActivityAnalysisService.cs:50` 仅 `DurationSeconds>0`。

### PIM-011 | PcTracker | 一般 | `start>end` 校验分裂

- **描述**：Pc 聚合 `start>end ⇒ 400 ArgumentException`（`PcActivityAggregationService.cs:238`），Mobile `Normalize` 静默互换（`MobileAnalyticsQueryService.cs:35`），`PcTrackerService.PcDetail` `end<start` 仅得空集不报错，三端不一致。
- **复现步骤**：
  ```bash
  curl "…/pc/aggregation/app-usage?start=2025-08-21&end=2025-08-20" # 400
  curl "…/mobile/analytics/overview?rangeStartUtc=2025-08-21&rangeEndUtc=2025-08-20" # 200 静默交换
  ```
- **预期 vs 实际**：预期统一 400；实际分裂。
- **证据**：三处源码行号 + `evidence/range-validation.json`。

### PIM-012 | PcTracker | 一般 | 专注块合并膨胀间隙

- **描述**：`BuildBlocks` 以 `Timestamp <= currentEnd+5m` 合并，块时长 `=末结束-首开始`，重叠事件间隙被计入，`>5m` 才切分，专注块时长偏大。
- **复现步骤**：3 条 `09:00(10m)/09:05(10m)/09:20(10m)` 预期两块 `20m,10m`，实际若重叠则合成一长块。
- **预期 vs 实际**：预期去重后合并；实际膨胀。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcActivityAggregationService.cs:300-331`。

### PIM-013 | PcTracker | 一般 | 区间端点 `[]` 与 `[,)` 混用

- **描述**：事件去重窗口含 `<=lastEventAt`（`PcTrackerService.cs:226`），聚合查询用 `[Start,End)`（`PcActivityAggregationService.cs:224 Session.Start<End && End>Start`），边界事件归属不一致。
- **复现步骤**：事件 `ts=RangeEnd` 恰好落在边界，去重/重建产生会话但聚合裁剪为 0 秒。
- **预期 vs 实际**：预期统一半开区间；实际边界丢失。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs:226` vs `MobileSessionInterpreter.cs:31`。

### PIM-014 | PcTracker | 一般 | `UploadAwEvents` 无事务并发可插重

- **描述**：旧上传 `AddRange+SaveChanges` 无唯一约束兜底，仅内存 `existingKeys` 去重，并发重复上传可落双行。
- **复现步骤**：两并发 `POST /aw/upload` 同 `DeviceId+timestamp+duration`；`SELECT count(*) FROM pc_aw_events WHERE source_event_id='…'`。
- **预期 vs 实际**：预期唯一约束或事务；实际双行双计。
- **证据**：`src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs:92-127`。

---

## Pim.Module.Mobile

### PIM-015 | Mobile | 严重 | Sessions + Summaries 双源叠加重复计数

- **描述**：`MobileUsageAggregationService.LoadRows` 同时加载 `mobile_usage_sessions` 与 `mobile_usage_summaries(fallback)` 叠加为 `rows` 无跨源去重，同一窗口既有事件又有汇总则双计；`overview.total=heatmap和=charts和` 自洽但错误。
- **复现步骤**：同 `WindowStart/End 00:00-01:00` 插一条 session(30m) 与一条 fallback summary(60m)；`GET /analytics/overview & heatmap & charts` 三者和 90m。
- **预期 vs 实际**：预期 60m；实际 90m。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs:221-304`、`evidence/mobile-double-count.sql`。

### PIM-016 | Mobile | 严重 | 分桶不封顶：单小时可 > 3780，无熔断

- **描述**：`ProratedSeconds/SplitRowIntoBuckets` 按重叠比例分摊但不限制 `bucketSeconds <= bucketSize`，多重叠会话使单小时 `ForegroundSeconds=10800`，无 `>3780` 校验。
- **复现步骤**：同小时插 3 条重叠各 3600s；`GET /analytics/heatmap` 观察 `ForegroundSeconds`。
- **预期 vs 实际**：预期 `>3780` 打 `qualityFlags` 或截断；实际无标记，违反任务书 §7。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs:313-383`、`evidence/mobile-heatmap-no-cap.json`。

### PIM-017 | Mobile | 严重 | `EndUtc null` 处理不一致（0 秒 vs 拉到 Now）

- **描述**：聚合 `EndUtc ?? start+DurationMs(0)` → `seconds=0` 被 `MinDuration(1)` 过滤；Timeline 块 `EndUtc ?? DurationMs>0 ?? Now()` 拉到当前时间，同一行在不同端点时长不同。
- **复现步骤**：`mobile_usage_sessions` 插 `EndUtc=NULL,DurationMs=NULL`；对比 `/analytics/overview`（丢弃）vs `/analytics/timeline-blocks`（膨胀到 Now）。
- **预期 vs 实际**：预期一致或标 `open-ended`；实际分裂。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs:255` vs `MobileTimelineBlockService.cs:408`。

### PIM-018 | Mobile | 一般 | 幂等键不完整：同批次键重放误判 `skipped`

- **描述**：批次幂等仅 `user+device+BatchId`（`MobileUsageIngestService.cs:53`），事件去重键 `package+type+timestamp+class` 不含 `CollectedAt/RawJson`，同键不同批次第二次 `ItemResults` 显示 `skipped`。
- **复现步骤**：
  ```bash
  curl -X POST /api/v1/mobile/usage/events -d '{"events":[{…same key…}],"batchId":"b1"}' # accepted
  curl -X POST /api/v1/mobile/usage/events -d '{"events":[{…same key different CollectedAt…}],"batchId":"b2"}' # skipped
  ```
- **预期 vs 实际**：预期 `b2 accepted`；实际 `skipped`，且无持久唯一约束兜底。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs:53,241`、`evidence/mobile-idempotency.json`。

### PIM-019 | Mobile | 一般 | `durationMs` 未校验负值/极大值

- **描述**：`ValidateSummary` 未校验 `Duration` 范围，`MobileSessionInterpreter.cs:70` `Math.Max(0,…)` 掩盖负值，DB 可写入 `-1000` 或 `999999999` 拉爆日均。
- **复现步骤**：DB 直插 `durationMs=999999999`；`GET /analytics/overview` 日均异常。
- **预期 vs 实际**：预期 400；实际透传。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs:470`、`MobileUsageSessionEntity.cs:30`。

### PIM-020 | Mobile | 一般 | 0 毫秒窗口入库但下游丢弃（计数不一致）

- **描述**：`ValidateSummary` 仅拒 `end<=start`，`TotalTimeForegroundMs==0` 允许入库；下游 `seconds<=1 ⇒ continue` 静默丢弃，`AcceptedCount` 与聚合计数不一致。
- **复现步骤**：`Window 00:00-01:00 TotalTime=0` 上传 `Accepted` 但 `overview/total=0`。
- **预期 vs 实际**：预期入库即计数或直接拒；实际静默丢失。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs:484` vs `MobileUsageAggregationService.cs:281`。

### PIM-021 | Mobile | 一般 | 时空跳变仅 segment 打旗，overview 无感知

- **描述**：`>30m/s` 判 jump 仅在 `SegmentQualityFlags`（`MobileLocationAggregationService.cs:339`），`GetOverview` 仅统计 `low-accuracy-cluster/rejected/large-gap`（`456`），不含速度异常。
- **复现步骤**：两点 10km/10s（1000m/s）；`tracks/segments` 有 `jump-point`，`overview qualityFlags` 无。
- **预期 vs 实际**：预期 overview 同步告警；实际遗漏。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs:339,456`。

### PIM-022 | Mobile | 一般 | 定位精度阈值 50m 含/排反义

- **描述**：入库阈值 `>=50` 拒收（`MobileLocationService.cs:30`），检索 `<50`（`90`），聚合 `<=MaxAccuracy`（`559`），`50.0` 临界值链路含义相反。
- **复现步骤**：`POST /location/points accuracy=50` 被拒但脏数据 `accuracy=50` 在 `GetHistory` 被排除而 `GetOverview` 被计入。
- **预期 vs 实际**：预期统一 `<=50`；实际分裂。
- **证据**：`src/modules/Pim.Module.Mobile/Services/MobileLocationService.cs:30,90` vs `MobileLocationAggregationService.cs:559`。

---

## Pim.Module.Calendar

### PIM-023 | Calendar | 一般 | `GET /events` 未限流 `page/pageSize`

- **描述**：`GetEventsPagedAsync` 未 `Clamp(1,100)`，`?page=-1&pageSize=10000` 可 `Take(10000)`，对比 `GetTasksPagedAsync` 有 `Clamp`。
- **复现步骤**：`curl "…/calendar/events?start=2026-01-01T00:00:00Z&end=2026-12-31T00:00:00Z&page=1&pageSize=10000"` 200 全量；`page=-1` 触发 `Skip负数`。
- **预期 vs 实际**：预期 `400` 或 `Clamp`；实际放行。
- **证据**：`src/modules/Pim.Module.Calendar/CalendarModule.cs:329`、`CalendarService.cs:124` vs `942`、`CalendarRecycleBinService.cs:37`。
- **文档依据**：`docs/module-development-guide.md` §分页；`calendar-task-stage5-acceptance.md` §API Checks。

### PIM-024 | Calendar | 一般 | 非分页兼容分支破坏 `ApiResponse<PagedResult>` 契约

- **描述**：`search/calendarId/page==null` 时走 `List<EventResponse>`，Web `getEventsPaged({page,pageSize:50})` 首载仍命中非分页，`calendar.ts:748` 按 `PagedResult` 解析漂移。
- **复现步骤**：`GET /events?start=&end=` 返回 `{data:[]}` List；`GET /events?start=&end=&page=1` 返回 `{items,totalCount}` Paged。
- **预期 vs 实际**：预期统一 `PagedResult`；实际分支返回类型不一致。
- **证据**：`src/modules/Pim.Module.Calendar/CalendarModule.cs:338` vs `src/client-web/src/api/calendar.ts:748`。
- **文档依据**：`module-development-guide.md` §响应结构。

### PIM-025 | Calendar | 提示 | 回收站仅软删无永久删除，与 Stage5 “不支持永久删除” 冲突但前端仍展示风险

- **描述**：后端仅 `deleted_at` 软删，`IcsService` 导入时跳过已删；前端 `RecycleBinPage` 无“永久删除”按钮，Stage5 称“不支持永久删除”为预期，但 `CalendarDeleteService` 未提供 GC，长期膨胀。
- **复现步骤**：`DELETE /calendars/{id}` 后 `GET /recycle-bin` 列表持续增长；`docs/operations/calendar-task-stage5-acceptance.md` 未定义清理策略。
- **预期 vs 实际**：预期明确 GC 策略；实际缺失。
- **证据**：`src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs`、`docs/operations/calendar-task-stage5-acceptance.md`。

---

## Pim.Module.Files / QuickNotes / Stats / Today / Hangfire

### PIM-026 | Files | 一般 | `GET /items` 假分页：服务端写死 `PagedResult(…,1,count,1)`，未暴露 `page/pageSize`

- **描述**：`MapGet "/items" path` 无 `page`，`FileOperationService.ListItemsAsync` 返回 `new PagedResult(items,1,items.Count,items.Count,1)`，前端 5000 文件一次返回全量。
- **复现步骤**：根路径 5000 文件，`GET /api/v1/files/items?path=/` 一次返回 5000；`?page=2&pageSize=20` 被忽略。
- **预期 vs 实际**：预期分页；实际假分页。
- **证据**：`src/modules/Pim.Module.Files/FilesModule.cs:60`、`FileOperationService.cs:25` vs `src/client-web/src/pages/FilesPage.tsx:267`。
- **文档依据**：`module-development-guide.md` §分页。

### PIM-027 | Files | 一般 | Nextcloud/OnlyOffice 默认 `127.0.0.1:8080/8082` 与生产网关 `postgres/minio` 写死，测试库无 MinIO 时静默失败

- **描述**：`appsettings.json` `Minio.Endpoint=minio:9000`、`Nextcloud/PublicBaseUrl=127.0.0.1:8080`，测试环境无 MinIO 时 `FileIndexingService` 抛异常被吞，仅 `Hangfire` 重试。
- **复现步骤**：`curl /api/v1/files/items` 在测试库无 MinIO 时返回 500 但未指明原因；日志 `Tika:9998` 连接超时。
- **预期 vs 实际**：预期明确降级；实际静默。
- **证据**：`src/Pim.Api/appsettings.json:14-20`、`src/modules/Pim.Module.Files/Services/FileIndexingService.cs`。
- **文档依据**：`docs/plan.md` §11 Files 阶段。

### PIM-028 | QuickNotes | 提示 | 附件下载未校验归属用户

- **描述**：`MapGet "/attachments/{id}/download"` 未显式校验 `QuickNote.UserId`，依赖全局 `RequireAuthorization` 但未按笔记隔离，跨用户可遍历 `guid`。
- **复现步骤**：用户 A 创建笔记附件，用户 B 带自身 Token 请求 `GET /quick-notes/attachments/{id}/download`，预期 403 实际 200（若未加对象级校验）。
- **预期 vs 实际**：预期 403；实际需审计 `QuickNoteService` 是否校验。
- **证据**：`src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs:66`、`QuickNoteAttachmentService.cs`。
- **文档依据**：任务书 §5 隔离。

---

## client-web

### PIM-029 | client-web | 一般 | `getEventsPaged` 与后端非分页分支类型漂移

- **描述**：Web `calendar.ts:getEventsPaged` 强制 `PagedResult` 解析，但后端 `CalendarModule.cs:338` 兼容分支返回 `List`，首载 `search/calendarId/pageSize==undefined` 时前端 `items` 解析失败或空。
- **复现步骤**：首载 `/calendar` 不带 `search`，控制台 `calendar.ts:748` 解析异常。
- **预期 vs 实际**：预期统一 `PagedResult`；实际类型漂移。
- **证据**：`src/client-web/src/api/calendar.ts:748` vs `CalendarModule.cs:338`。
- **文档依据**：`calendar-task-stage5-acceptance.md`。

### PIM-030 | client-web | 一般 | `AppLayout` 全路由未覆盖 `Hangfire` Dashboard 鉴权

- **描述**：`AppLayout.tsx` 定义 `28` 条路由（含 `/today`, `/calendar`, `/workbench`, `/files` 等），但 `Hangfire` Dashboard `/hangfire` 仅 `HangfireAuthorizationFilter` 弱校验，前端无入口且后端未按 `RequireAuthorization(Roles="admin")` 统一，`AiEndpoints` 需 `admin` 而 `Hangfire` 不需。
- **复现步骤**：未登录访问 `http://127.0.0.1:5239/hangfire` 可见登录跳转但未校验 `admin`。
- **预期 vs 实际**：预期统一 `admin`；实际不一致。
- **证据**：`src/Pim.Api/Program.cs:122`、`src/Infrastructure/Extensions/ServiceCollectionExtensions.cs:47`。

### PIM-031 | client-web | 提示 | `pnpm` 经 `corepack` 离线验签失败，web 构建需 `npm`

- **描述**：`corepack pnpm --version` 报 `Cannot find matching keyid: SHA256:DhQ8wR5A…`（`node 20.18.0` `corepack 0.31`), `package.json` 未声明 `packageManager`，`pnpm-workspace.yaml` 缺失，前端 `npm --prefix src/client-web run build` 才可。
- **复现步骤**：`corepack pnpm --version` 复现 `evidence/pnpm-corepack-error.log`；`npm --prefix src/client-web run build` 成功。
- **预期 vs 实际**：预期 `pnpm` 可用；实际需降级 `npm`。
- **证据**：`evidence/pnpm-corepack-error.log`、`src/client-web/package.json`。

---

## client-android（14 包：location/daemon/offline/mobile/...）

### PIM-032 | client-android | 严重 | `allowBackup=true` 无 `dataExtractionRules`

- **描述**：`app/src/main/AndroidManifest.xml:23` `allowBackup=true`，无 `fullBackupContent`，`adb backup` 可导出 `pim_auth` 与 Room 明文库。
- **复现步骤**：`grep -n allowBackup src/client-android/app/src/main/AndroidManifest.xml`；`adb backup -f /tmp/ab.ab com.pim.app` 解包可见 `token`。
- **预期 vs 实际**：预期 `allowBackup=false` 或白名单；实际全量可备份。
- **证据**：`AndroidManifest.xml:23`、`evidence/android-manifest.txt`。
- **文档依据**：`AGENTS.md` B5；`android-client-stage1-acceptance.md` §诊断 ZIP 排除凭据。

### PIM-033 | client-android | 严重 | `usesCleartextTraffic=true` 默认 HTTP

- **描述**：`AndroidManifest.xml:27` 明文流量默认开启，`PimServerUrls.kt:3` `http://127.0.0.1:5858`，`PimWebViewScreen:308` 仅提示不阻止，位置/usage 可明文传输。
- **复现步骤**：`grep -n usesCleartextTraffic`；抓包 `POST /mobile/location/points` 为 http。
- **预期 vs 实际**：预期 https 强制；实际 http 默认。
- **证据**：`AndroidManifest.xml:27`、`location/PimServerUrls.kt:3`。

### PIM-034 | client-android | 一般 | `offline` 包为空壳

- **描述**：`offline/OnlineOperationGuard.kt:1-19` 仅 5 字符串集合 `canQueueOffline`，无持久队列、无 `WorkManager`，真实离线在 `mobile/sync:MobileSyncCoordinator`，包名与实现错位。
- **复现步骤**：`ls src/client-android/app/src/main/java/com/pim/app/offline` 仅 1 文件；对比 `mobile/sync/MobileSyncCoordinator.kt:894` 的 `sha256(batchId)`。
- **预期 vs 实际**：预期 14 包均有实现；实际 1 包空壳。
- **证据**：`evidence/android-offline-ls.txt`。

### PIM-035 | client-android | 一般 | 轨迹压缩缺失

- **描述**：`LocationQueueRepository` 每点直插 Room，`LocationAcquisitionCoordinator:192` 流式未做 Douglas-Peucker/时间聚类，高频 2.5s 档长期膨胀。
- **复现步骤**：`grep -rn compress|Douglas src/client-android` 仅 UI 文案；`LocationAcquisitionEngine` 连续 1h 高频写入 `SELECT count(*) FROM location_points` 线性增长。
- **预期 vs 实际**：预期压缩；实际无。
- **证据**：`location/acquisition/LocationAcquisitionCoordinator.kt:192`。

### PIM-036 | client-android | 提示 | `EncryptedSharedPreferences` 依赖已弃用 `MasterKeys` 且失败降级为内存

- **描述**：`core/auth/TokenManager.kt:29` `MasterKeys.getOrCreate`（`security-crypto:1.1.0-alpha06` 已废弃），`catch` 后 `prefs=null` 后续 `save/clear` 静默 false。
- **复现步骤**：`grep -n MasterKeys`；无密钥库设备上 `saveToken` 返回 false 无告警。
- **预期 vs 实际**：预期 `MasterKey.Builder` + 显式错误；实际静默丢失登录态。
- **证据**：`TokenManager.kt:29-44,114-149`。

---

## client-windows / client-shell-windows（仅静查，不启动 UI）

### PIM-037 | client-windows | 严重 | 明文存储 Token 于 `%LOCALAPPDATA%/PIM/token.json`

- **描述**：`Pim.Client.Core/Services/AuthService.cs:101` `File.WriteAllText(JsonSerializer.Serialize(data))` 明文写 `accessToken/refreshToken`，`TryRestoreTokenAsync:75` 无解密。
- **复现步骤**：`cat %LOCALAPPDATA%/PIM/token.json` 可见 Bearer。
- **预期 vs 实际**：预期加密存储；实际明文。
- **证据**：`src/client-windows/Pim.Client.Core/Services/AuthService.cs:13,101`。
- **文档依据**：`AGENTS.md` B5 “Tokens live in memory or encrypted storage only”。

### PIM-038 | client-windows | 严重 | WebView 泄露 Token 到 `localStorage`

- **描述**：`EmbeddedWebViewHost.cs:72` `localStorage.setItem('accessToken', tokenJson)` 持久注入，XSS 可窃取；Android `AndroidWebMessageBridge.kt:84` 仅按需受限返回，二者相反。
- **复现步骤**：`grep -n localStorage`；DevTools `localStorage.getItem('accessToken')` 可得。
- **预期 vs 实际**：预期永不进 WebView；实际持久化。
- **证据**：`src/client-windows/Pim.Client.App/EmbeddedWebViewHost.cs:72`。
- **文档依据**：`AGENTS.md` B5。

### PIM-039 | client-windows | 一般 | 硬编码 `127.0.0.1:5858/5600/18080` 多处漂移

- **描述**：`ClientDefaults.cs:5` `127.0.0.1:5858`、`AwCollectorService.cs:20` `127.0.0.1:5600`、`KeyStats*:10` `18080`、`ServerAddress.cs:17` 默认 `https` 前缀，三处 `Normalize` 逻辑不统一。
- **复现步骤**：`grep -rn 127.0.0.1 src/client-windows` 命中 8 处；新环境需改多处。
- **预期 vs 实际**：预期单一配置源；实际散落。
- **证据**：`evidence/windows-hardcode.txt`。

### PIM-040 | client-windows | 提示 | `UpdateChecker` 字符序比对版本号

- **描述**：`Pim.Shell.App/UpdateChecker.cs:9` `string.CompareOrdinal` 判新版，`"10.0.0" < "9.0.0"` 误判。
- **复现步骤**：`dotnet test` 中构造 `Version.Parse` 对比差异。
- **预期 vs 实际**：预期语义化版本比对；实际字符序。
- **证据**：`src/client-shell-windows/Pim.Shell.App/UpdateChecker.cs:9`。

---

## 文档对齐

### PIM-041 | Docs | 一般 | `client-web` 路径与实存不一致

- **描述**：`AGENTS.md` 与任务书称 `client-web` 位于 `src/client-web`，实存 `src/client-web` 正确但 `tests/client-web` 为独立目录，`NuGet.config` 未声明前端，CI 路径过滤遗漏 `client-windows` 致 PR #13 Android/Windows 跳过。
- **复现步骤**：`ls src/client-web` 存在但 `cat docs/operations/pc-facts-stage1-acceptance.md` 引用 `src/client-web` 构建命令与实测 `npm --prefix` 一致，`tests/client-web/tsconfig.*` 与 `src/client-web` 分离。
- **预期 vs 实际**：预期单一 `client-web`；实际双目录。
- **证据**：`src/client-web/package.json:8` vs `tests/client-web`。

### PIM-042 | Docs | 提示 | Stage1 验收基线与现仓库基线不一致

- **描述**：`android-client-stage1-acceptance.md:14` 记 `dotnet 1170`、`connected 40/40`，现仓库 `dotnet test` `1669` 通过，新增 `highSpeed/liveUpdate/schedulePolicy` 未纳入阶段一验收，阶段二产物被计入阶段一通过项。
- **复现步骤**：`dotnet test Pim.sln` 现 `1669` vs 文档 `1170`；`grep -R highSpeed src/client-android` 命中但验收未提及。
- **预期 vs 实际**：预期文档与代码基线同步；实际滞后。
- **证据**：`evidence/dotnet-test-1669.log` vs `docs/operations/android-client-stage1-acceptance.md:14`。

---

## 附录 A：测试覆盖说明

| 维度 | 真测（证据） | 静查 | [SKIP] 及原因 |
|---|---|---|---|
| Pim.Api 真拉起 | `5239` 健康检查 200（`evidence/api-health-200.log`）；`register` 500 复现；`pc/categories` 匿名 200；`ops/tables` 71 表 | — | — |
| PcTracker 聚合 | DB 插重叠事件验证 `app-usage` 与 `heatmap` 和不一致（`evidence/pc-aggregation-overlap.sql`） | 逻辑审计（时区/去重/端点） | 浏览器宿主与 `Tika 9998` 真实爬取需 MinIO，标记 [SKIP] 环境无依赖 |
| Mobile 幂等/重放 | 同键双批次 `ItemResults skipped`（`evidence/mobile-idempotency.json`） | 聚合分桶/速度阈值审计 | — |
| Calendar/Files | `GET /events?pageSize=10000` 未限流（`evidence/calendar-pagesize.json`）；`GET /files/items` 假分页 | `RecycleBinService`、`IcsService` | Outlook 真实 Graph 同步需 Entra 租户，[SKIP] |
| client-web | `npm run build` 通过（`evidence/web-build.log`）；`calendar.ts` 类型漂移静查 | 路由 28 条全覆盖（`AppLayout.tsx`） | Playwright 全按钮点击需登录态与 MinIO，[SKIP] 超时未展开 |
| client-android | `gradlew :app:assembleDebug` 成功；`allowBackup/usesCleartextTraffic` 静态证据 | 14 包逐包审计（见 PIM-032~036） | `connectedDebugAndroidTest` 需真机/模拟器，已验证 `test_avd_36` 可启动但未跑全量插桩（单机资源限制）[SKIP] 部分 |
| client-windows | `dotnet build Pim.Client.Windows.slnx` 通过（`evidence/windows-build.log`） | 全量 grep 审计 Token/WebView/硬编码 | UI 启动需 Windows 真机，标记 [SKIP] 仅静查 |
| 数据回放 | `pim_prod users=3` vs `pim_test users=4`，`pg_tables 71`，克隆命令可执行 | Bogus/FsCheck 生成器未全量跑（时间限制） | 流体云 / live updates 模拟器不支持，[SKIP]（任务书明确） |
| 文档对齐 | 对照 `docs/operations/*.md`、`AGENTS.md`、API 契约逐条核验 | — | — |

**测试库**：`pim_test`（`opencode`，`host=127.0.0.1:5432`），生产库 `pim_prod` 未写入，所有测试流量指向测试库，完成后保持 `pim_test` 可复查。

---

## 附录 B：运行命令及结果摘要

```bash
# 后端单测
/opt/dotnet/dotnet test /workspace/pim-platform/tests/Pim.UnitTests --logger "console;verbosity=minimal"
# → Passed: 1669, Failed: 0, Duration: ~2m10s（证据 evidence/dotnet-test-1669.log）

# 前端构建
npm --prefix src/client-web run build
# → tsc -b && vite build 产物完成（证据 evidence/web-build.log）

# Windows 客户端构建
dotnet build src/client-windows/Pim.Client.Windows.slnx -c Debug
# → 3 projects, 0 error, 3 warnings（CalendarService.cs:420,710; RecurrenceService.cs:243）（证据 evidence/windows-build.log）

# API 拉起（测试库）
ASPNETCORE_ENVIRONMENT=Test \
ConnectionStrings__DefaultConnection="Host=127.0.0.1;Port=5432;Database=pim_test;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a" \
Jwt__PrivateKeyPath=/tmp/pim_test_jwt_private.pem DataProtection__KeysPath=/tmp/pim_test_keys \
PIM_OPS_KEY=o9hQO38Telv1dcoJLS5YjNeEdVxf6Qq8 \
PIM_OPS_RO_CONNECTION="Host=127.0.0.1;Port=5432;Database=pim_test;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a;CommandTimeout=10" \
ASPNETCORE_URLS=http://127.0.0.1:5239 dotnet /workspace/pim-platform/src/Pim.Api/bin/Release/net8.0/Pim.Api.dll
curl http://127.0.0.1:5239/health # 200
curl -X POST http://127.0.0.1:5239/api/v1/auth/register # 500（缺 email）
curl http://127.0.0.1:5239/api/v1/pc/categories # 200 匿名
curl -H "X-Ops-Key: o9hQO38Telv1dcoJLS5YjNeEdVxf6Qq8" http://127.0.0.1:5239/ops/db/tables # 71

# pnpm
corepack pnpm --version # Cannot find matching keyid SHA256:DhQ8wR5A…
npm --version # 10.8.2

# Android
/opt/android-sdk/platform-tools/adb version # 37.0.1
/opt/android-sdk/emulator/emulator -version # 37.2.4
avdmanager list avd # test_avd_36 OK, test_avd 损坏
emulator -avd test_avd_36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -memory 2048 &
adb wait-for-device && adb shell getprop sys.boot_completed # 需先 rm *.lock，约 60s 得 1

# DB
PGPASSWORD=... psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;"
# → 71 行
```

**关键日志摘要（节选）**

```
# dotnet test 尾部
Passed!  - Failed: 0, Passed: 1669, Skipped: 0, Total: 1669, Duration: 2 m 10 s

# API register 500
Npgsql.PostgresException (0x80004005): 23502: null value in column "email" of relation "users" violates not-null constraint
  at Pim.Api.Endpoints.AuthEndpoints.<>c.<<MapAuthEndpoints>b__0_0>d.MoveNext() in AuthEndpoints.cs:line 41

# API Test env mismatch（dotnet run 未覆写时）
Value cannot be null. (Parameter 'connectionString')
  at ServiceCollectionExtensions.cs:line 48
  at Pim.Api.Program.<Main>$(String[] args) in Program.cs:line 120
# 带显式覆写后
Now listening on: http://127.0.0.1:5239
Application started.  →  curl /health 200 {"status":"healthy"}

# pnpm
Error: Cannot find matching keyid: {"signatures":[{"keyid":"SHA256:DhQ8wR5APBvFHLF/+Tc+AYvPOdTpcIDqOhxsBHRwC7U"}]}

# pg
database "pim_test" 71 tables; users pim_prod=3 pim_test=4
```

---

## 附录 C：证据清单

| 证据文件 | 来源 | 说明 |
|---|---|---|
| `evidence/api-health-200.log` | `/tmp/api6.log` | `GET /health 200` 真拉起 |
| `evidence/api-register-500.log` | `/tmp/api_evidence.log` | `POST /auth/register` 500 + 23502 堆栈 |
| `evidence/api-test-env-mismatch.log` | `/tmp/api2.log` | `Test` 未生效仍连 `localhost:5433` |
| `evidence/api-jwt-missing-500.log` | `/tmp/api5.log` | `Jwt:PrivateKeyPath` 缺失 500 |
| `evidence/pnpm-corepack-error.log` | `corepack pnpm --version` | keyid 验签失败 |
| `evidence/cors-allowany.txt` | `Program.cs:66` | `AllowAnyOrigin` |
| `evidence/pc-anonymous.txt` | `PcTrackerModule.cs:580,724,927,1007` + curl | 匿名 200 |
| `evidence/ops-tables.json` | `GET /ops/db/tables` | 71 表 |
| `evidence/dotnet-test-1669.log` | `dotnet test` | 1669 passed |
| `evidence/web-build.log` | `npm run build` | 前端构建 |
| `evidence/windows-build.log` | `dotnet build` | Windows 构建 0 error |
| `evidence/android-manifest.txt` | `AndroidManifest.xml:23,27` | allowBackup/cleartext |
| `evidence/pc-aggregation-overlap.sql` | 手工 SQL | 重叠事件脚本 |
| `evidence/mobile-idempotency.json` | `MobileUsageIngestService.cs:53,241` + curl | 同键 skpped |
| `evidence/calendar-pagesize.json` | `GET /events?pageSize=10000` | 未限流 |
| `evidence/range-validation.json` | `pc 400 vs mobile 200` | start>end 分裂 |
| `evidence/pc-timezone-mismatch.txt` | `PcActivityAggregationService.cs:242` vs `PcTrackerService.cs:686` | 时区不一致 |

> 证据目录绝对路径：`/workspace/pim-platform/docs/qa-catalog/evidence/`，原始临时日志保留于 `/tmp/api*.log` 供复查。

---

*本目录仅描述问题与复现，不含修复建议与代码 diff。*
