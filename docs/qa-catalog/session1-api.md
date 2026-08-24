# Session 1：后端API全量回放测试 汇总

> 生成时间：2026-08-24 03:00 UTC  
> 测试库：`pim_test`（`opencode` @ `127.0.0.1:5432`，源库 `pim_prod` @ `pim` `Host=pim_prod_2026_home`）  
> API：`Pim.Api` @ `ASPNETCORE_ENVIRONMENT=Test` `http://127.0.0.1:15733` `/health 200`（见 `evidence/api/session1/health.json`），旧实例 `http://127.0.0.1:5860` 仍存活  
> 分支：`master`（未改业务代码，仅新增 `docs/qa-catalog/evidence/api/PIM-043~046` 与 `PASS-043~046` 及本汇总）  
> 脏数据：`≥20重叠会话（40行）+10跨天+3跨上海04:00+5 null EndUtc+5零窗口`，均落在 `session1-device-A/s1userA (e1c1b0c4-...)`，对照 `session1-device-B/s1userB (3f22a020-...)` 5条隔离样本，`pc_aw_events 27行` 含 `20重叠+3跨天+5零时长`，`mobile_location_points 3 baseline`（见 `evidence/api/session1/db_snapshot.txt`）

## 7 项逐项结论

| # | 验证项 | 结论 | 证据 |
|---|---|---|---|
| 1 | 幂等：`/usage/events` 同批发2次，查库只写1次 | **通过**（顺序幂等正常；并发同BatchId触发一次 `23505` 但经重试返回同一幂等结果，DB `1`） | `PASS-043.md`、`usage_idem_req1/2.json`、`concurrent_error.log`、`PIM-046.md`（并发瑕疵单列） |
| 2 | 幂等：`/location/points` 同批发2次，查库只写1次 | **问题** | `PIM-043.md` |
| 3 | 一致性：`overview.total == heatmap桶和 == charts桶和` 误差≤桶数 | **通过** `149400==149400==149400 误差0≤46` | `PASS-044.md`、`consistency_check.txt`、`overview/heatmap/charts.json` |
| 4 | 容差：单设备单小时去重后 `<=3600*1.05 (=3780)` | **问题** `20/46` 桶 `5400>3780` | `PIM-044.md`、`tolerance_check.txt` |
| 5 | 容差：单设备单天去重后 `<=86400*1.05 (=90720)` | **通过** `max 86400≤90720` | `PASS-045.md` |
| 6 | 时序：所有session `start<end` | **问题** `5 null EndUtc +5零窗口`，全局 `284` 行 `start>=end` | `PIM-045.md`、`temporal_check.txt` |
| 7 | 隔离：user A token 查 user B 数据 预期404/空 | **通过** `A查B overview 0/timeline 0/history 0/devices仅自身`，`B查B 9000` | `PASS-046.md`、`isolation_A/B_query_B.json` |

> 4/7 通过，3/7 检出问题（`PIM-043/044/045`），1个并发瑕疵 `PIM-046` 为补充发现。

## 新发现问题（按文件）

| ID | 级别 | 标题 | 关联验证 |
|---|---|---|---|
| `PIM-043` | 严重 | `/location/points` 无幂等，同请求重复落库 | #2 |
| `PIM-044` | 严重 | 聚合未去重：重叠会话使单小时 `5400>3780` | #4 |
| `PIM-045` | 严重 | 时序异常：`null EndUtc` 与 `0毫秒` 窗口违反 `start<end` | #6 |
| `PIM-046` | 一般 | 并发同 `BatchId` 触发 `mobile_app_catalog` `23505` 需重试才收敛，`AcceptedCount` 与 `ItemResults` 不一致 | #1 延伸 |

## 通过项（按文件）

| ID | 验证 |
|---|---|
| `PASS-043` | #1 `/usage/events` 幂等 |
| `PASS-044` | #3 一致性 |
| `PASS-045` | #5 单天容差 |
| `PASS-046` | #7 隔离 |

## 全部 curl 命令 + 返回（节选）

```bash
# 健康
curl http://127.0.0.1:15733/health
# {"status":"healthy","timestamp":"2026-08-24T02:29:05.9950182+00:00"}

# #1 幂等 usage/events
curl -s -X POST http://127.0.0.1:15733/api/v1/mobile/usage/events -H "Authorization: Bearer $TOKEN_A" -H "Content-Type: application/json" -d @/tmp/payload_pass.json
# {"code":0,"data":{"batchId":"final-v1-batch-2","acceptedCount":3,"skippedCount":0,"itemResults":[{"clientItemKey":"com.final.v1c@1","outcome":"accepted"}, ...]}, ...}
# 第二次同参数返回完全一致，DB: mobile_usage_events 2, mobile_sync_batches 1

# #2 位置 points（问题）
curl -s -X POST http://127.0.0.1:15733/api/v1/mobile/location/points -H "Authorization: Bearer $TOKEN_A" -H "Content-Type: application/json" -d '{"deviceId":"session1-device-A","recordedAtUtc":"2026-09-21T11:00:00Z","latitude":39.9,"longitude":116.4,"horizontalAccuracyMeters":10,"provider":"gps","sourceKind":"manual","rawJson":"{}"}'
# 第一次 {"data":{"id":"adb79f61-0cef-4f1a-b812-c720e9563dcd",...}}
# 第二次 {"data":{"id":"db6b77f4-242f-402e-af68-6cc58ebd244f",...}} 新id
PGPASSWORD=62f0a50bb963bb648f8e400399def95a psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT count(*) FROM mobile_location_points WHERE recorded_at_utc='2026-09-21T11:00:00Z';"
# 2 -> 并发后 4

# #3 一致性
curl -s "http://127.0.0.1:15733/api/v1/mobile/analytics/overview?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&force=true" -H "Authorization: Bearer $TOKEN_A"
# {"data":{"totalForegroundSeconds":149400,...}}
curl -s "http://127.0.0.1:15733/api/v1/mobile/analytics/heatmap?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&granularity=hour&force=true" -H "Authorization: Bearer $TOKEN_A"
# 46桶和 149400
curl -s "http://127.0.0.1:15733/api/v1/mobile/analytics/charts?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&force=true" -H "Authorization: Bearer $TOKEN_A"
# daily-total和 149400

# #4/5 容差
python3 -c "import json; buckets=json.load(open('/tmp/heatmap.json'))['data']; print(max(b['foregroundSeconds'] for b in buckets))"
# 5400 >3780
python3 -c "import json,collections; buckets=json.load(open('/tmp/heatmap.json'))['data']; d=collections.defaultdict(int); [d.__setitem__(b['localDate'], d[b['localDate']]+b['foregroundSeconds']) for b in buckets]; print(max(d.values()))"
# 86400

# #6 时序
PGPASSWORD=62f0a50bb963bb648f8e400399def95a psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT count(*) FROM mobile_usage_sessions WHERE end_utc IS NULL; SELECT count(*) FROM mobile_usage_sessions WHERE start_utc>=end_utc AND end_utc IS NOT NULL;"
# 5 ; 284

# #7 隔离
curl -s "http://127.0.0.1:15733/api/v1/mobile/analytics/overview?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-B&force=true" -H "Authorization: Bearer $TOKEN_A" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['totalForegroundSeconds'])"
# 0
curl -s "http://127.0.0.1:15733/api/v1/mobile/analytics/overview?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-B&force=true" -H "Authorization: Bearer $TOKEN_B" | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['totalForegroundSeconds'])"
# 9000
```

> 完整返回见 `evidence/api/session1/`：`overview.json`（`total 149400`）、`heatmap.json`（`46桶 5400×20`）、`charts.json`、`isolation_A_query_B.json`（`0`）、`isolation_B_query_B.json`（`9000`）、`usage_idem_req*.json`、`concurrent_error.log`（`23505`）、`consistency_check.txt`、`tolerance_check.txt`、`temporal_check.txt`、`db_snapshot.txt`、`health.json`。

## 环境

- PG：`127.0.0.1:5432` 源库 `pim_prod (pim/pim_prod_2026_home)` 71表，测试库 `pim_test (opencode/62f0a50bb963bb648f8e400399def95a)` 71表，`pg_dump` 版本不匹配未重做全量克隆，沿用存量 `pim_test`（含 `mobile_usage_sessions 121k/121424, pc_aw_events 217k`）仅注入脏数据，未改业务代码。
- API 拉起：`15733`（`ASPNETCORE_ENVIRONMENT=Test`，显式 `ConnectionStrings__DefaultConnection`/`Jwt__PrivateKeyPath=/tmp/pim_test_jwt_private.pem`/`DataProtection__KeysPath=/tmp/pim_test_keys`/`PIM_OPS_KEY`/`PIM_OPS_RO_CONNECTION`），日志 `/tmp/api_session1_15733.log`；`5860` 旧实例并存。
- 账号：`s1userA/e1c1b0c4-...` `session1-device-A`、`s1userB/3f22a020-...` `session1-device-B`（`POST /auth/register` 新建，`devices/register` 注册）。
- 脏数据 SQL：`/tmp/inject_dirty.py` + `inject_dirty2.py`，满足 `≥20重叠/10跨天/5 null/5零窗口`。

## 不做的事（遵守）

- 未改业务代码
- 未做 Web E2E
- 未做安卓测试
- 未修复任何 bug（仅读与注入脏数据）

## 证据清单（新增）

| 文件 | 说明 |
|---|---|
| `evidence/api/PIM-043.md` | 位置幂等问题 |
| `evidence/api/PIM-044.md` | 小时容差问题 |
| `evidence/api/PIM-045.md` | 时序问题 |
| `evidence/api/PIM-046.md` | 并发 `23505` 瑕疵 |
| `evidence/api/PASS-043.md` | usage幂等通过 |
| `evidence/api/PASS-044.md` | 一致性通过 |
| `evidence/api/PASS-045.md` | 单天容差通过 |
| `evidence/api/PASS-046.md` | 隔离通过 |
| `evidence/api/session1/health.json` | `/health 200` |
| `evidence/api/session1/overview/heatmap/charts.json` | 三端一致 149400 |
| `evidence/api/session1/consistency_check.txt` | `diff 0 <=46` |
| `evidence/api/session1/tolerance_check.txt` | `20 violations 5400>3780` |
| `evidence/api/session1/temporal_check.txt` | `5 null+5 zero` |
| `evidence/api/session1/db_snapshot.txt` | `sessions 64, null 5, zero 5` |
| `evidence/api/session1/usage_idem_req*.json` | 幂等返回 |
| `evidence/api/session1/concurrent_error.log` | `23505` 堆栈 |
| `evidence/api/session1/isolation_*.json` | `0 vs 9000` |

*本汇总仅描述问题与复现，不含修复建议与代码 diff。*
