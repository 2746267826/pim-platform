# 可观测性：指标、告警、Grafana 与日志聚合

PIM 的可观测性由四部分组成：Prometheus 指标端点、健康检查、告警规则、Grafana 仪表盘与 Loki 日志聚合。默认全部关闭，按需启用，不影响核心功能。

## 1. Prometheus 指标

### 端点

`GET /metrics`（Prometheus 文本格式）。**需要鉴权**，二选一：

- `X-PIM-Ops-Key: <PIM_OPS_KEY 之一>` 请求头；
- `Authorization: Bearer <PIM_OPS_KEY 之一>`（供 Prometheus `bearer_token_file` 使用）；
- 或携带 admin 角色的 JWT。

未配置 `PIM_OPS_KEY` 且非 admin 时返回 401。

### 内置指标

| 指标 | 类型 | 说明 |
|---|---|---|
| `http_requests_received_total` / `http_request_duration_seconds` | Counter/Histogram | prometheus-net 自动采集的 HTTP 指标 |
| `pim_ai_requests_total{module,status}` | Counter | AI 网关请求（status: Completed/Failed/TimedOut/Blocked） |
| `pim_ai_request_duration_seconds{module}` | Histogram | AI 网关耗时 |
| `pim_daemon_heartbeat_freshness_seconds{device,kind}` | Gauge | 距上次守护进程心跳的秒数（30s 刷新） |
| `pim_hangfire_jobs{state}` | Gauge | Hangfire 队列计数（enqueued/processing/scheduled/failed，30s 刷新） |
| `aspnetcore_healthcheck_status{name}` | Gauge | 健康检查导出（1=Healthy, 0.5=Degraded, 0=Unhealthy） |

## 2. 健康检查

| 端点 | 用途 |
|---|---|
| `/health` | 原有存活探针（保持兼容，容器 healthcheck 使用） |
| `/health/live` | Liveness：进程存活即 200 |
| `/health/ready` | Readiness：JSON 明细。`database` 失败 → 503（Unhealthy）；`hangfire`/`minio`/`tika`/`qdrant`/`litellm` 为可选依赖，失败或未配置仅 Degraded（整体仍 200） |

## 3. 告警规则

`deploy/prometheus/alerts.yml`：

| 告警 | 条件 | 级别 |
|---|---|---|
| `PimApiDown` | 抓取失败 2 分钟 | critical |
| `PimDatabaseDown` | database 健康检查 < 1，2 分钟 | critical |
| `PimDaemonHeartbeatStale` | 心跳新鲜度 > 600s，5 分钟 | warning |
| `PimHangfireBacklog` | enqueued > 100，10 分钟 | warning |
| `PimHangfireFailedJobs` | failed > 0，10 分钟 | warning |
| `PimAiErrorRateHigh` | AI 错误率 > 5%，15 分钟 | warning |
| `PimHttp5xxSpike` | 5xx > 0.5/s，5 分钟 | warning |

## 4. 启用可观测性栈（Docker）

开发全家桶与生产编排都内置了 `observability` profile（Prometheus + Grafana + Loki）：

```bash
# 1. 设置运维密钥（.env 或 .env.prod）
PIM_OPS_KEY=<强随机串>

# 2. 把同一密钥写入 Prometheus 令牌文件
echo "<强随机串>" > deploy/prometheus/ops_token

# 3. 启用 Loki 日志聚合（可选但推荐）
LOKI_URL=http://loki:3100   # 写入 .env / .env.prod

# 4. 启动（开发）
docker compose --profile observability up -d
# 生产
docker compose --env-file .env.prod -f docker-compose.prod.yml --profile observability up -d
```

- Grafana：<http://127.0.0.1:3000>，自带 `PIM / PIM Overview` 仪表盘（请求速率、延迟 p95、心跳新鲜度、Hangfire、AI、依赖健康、Loki 日志）。开发环境匿名只读；生产默认 admin/admin，请用 `GF_SECURITY_ADMIN_PASSWORD` 改掉。
- Prometheus：<http://127.0.0.1:9090>，告警规则已加载；接 Alertmanager 时在 Prometheus 侧追加配置即可。

注意：生产 compose 的 Prometheus 使用 `deploy/prometheus/prometheus.prod.yml`（目标 `pim:5000`）；开发使用 `prometheus.yml`（目标 `pim-api:5000`）。

## 5. Loki 日志聚合

设置 `LOKI_URL` 后，Serilog 在原有 JSON 文件与控制台之外，额外推送日志到 Loki（标签 `app=pim-api`）。未设置时完全不启用，无外部依赖。Grafana 仪表盘的 Logs 面板直接查询 `{app="pim-api"}`。

## 6. AI 审计归属修复

自本变更起，`ai_request_logs.user_id` 会记录发起调用的当前用户（此前恒为 NULL）；系统/后台触发的调用仍为 NULL（系统设计）。
