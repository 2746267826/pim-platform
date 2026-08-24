# PASS-009 | docs/ops-readonly-api.md | 合格 | 运维只读 API 核心契约（鉴权/日志/DB/限流/错误码）
- 验证方式：read_file 全文 + grep `OpsKeyMiddleware` `OpsLogsService` `OpsDbService` `SqlAstValidator` `OpsRateLimiter` `OpsIpHelper` + 交叉对比 `docker-compose.prod.yml` `.env.prod.example`
- 验证点：文档 12 章（鉴权头大小写不敏感 trim、多值轮换 FixedTimeEquals、IP 取 RemoteIpAddress + KnownProxies、Docker/systemd/tar.gz 落位、files/tail/query 参数与 206 截断、DB 显式列名与 * 拒绝、tables/describe、并发 2 + 5MB/10s 截断、错误码表、pim_ro 授权脚本、审计、SSH 移除、端到端 curl）
- 代码实际：`OpsKeyValidator.cs:15` trim + FixedTimeEquals；`OpsKeyMiddleware.cs:36-40` 40101/50301；`OpsIpHelper.cs:8` 仅 RemoteIpAddress；`OpsLogsService.cs:46-78` 白名单 regex 与 500 上限与 206；`OpsDbService.cs:127` 校验与 5MB/10s；`OpsRateLimiter.cs:9` 并发 2；`OpsDbService.cs` libpg_query + 正则兜底
- 结论：除 DOC-009/012 提及的跨文档旧阈值残留外，本文档本身的承诺与代码实现逐项一致，标记为通过
