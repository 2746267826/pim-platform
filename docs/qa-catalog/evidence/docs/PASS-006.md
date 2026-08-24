# PASS-006 | docs/input/APIS/* | 合格 | 外部采集上游接口文档
- 验证方式：read_file `KeyStats接口文档.md` `swagger.json` `默认端口：5600.txt` + grep `KEYSTATS_BASE_URL` `AW_BASE_URL` `ActivityWatch`
- 验证点：KeyStats 文档声称 `http://127.0.0.1:18080/api/stats/` GET 返回 `keyPresses/leftClicks/.../mouseDistance/scrollDistance/peakKPS/appStats`；ActivityWatch swagger 声称 `/api/0/buckets/` GET；端口 5600
- 代码实际：`src/client-windows/Pim.Client.Core/Services/KeyStatsLocalStatsClient.cs:9` `Environment.GetEnvironmentVariable("KEYSTATS_BASE_URL") ?? "http://127.0.0.1:18080"` 与 `GetFromJsonAsync<StatsDto>("/api/stats/")` 解析 `keyPresses/mouseDistance/...` 字段名与文档一致（大小写按 JsonPropertyName 精确匹配）；`AwCollectorService.cs:20` `AW_BASE_URL ?? "http://127.0.0.1:5600"` 与 `StatusWindow.xaml.cs:82` 探测 `/api/0/buckets/` 一致
- 结论：外部上游接口的端口、路径、字段与文档一致，`FormattedMouseDistance` 等格式化字段仅展示用，未被采集链依赖，不构成不一致，标记为通过
