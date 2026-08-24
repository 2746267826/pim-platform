# PASS-045 | Mobile | 通过 | 容差：单设备单天去重后 <=86400*1.05

- 描述：同窗口 `session1-device-A` `2026-09-01~2026-09-30`，`heatmap` 按 `localDate` 聚合后单天最大值 `86400s`（`2026-09-01`），未超过 `90720s`（`86400*1.05`），单天容差通过。
- 复现：
  ```bash
  API=http://127.0.0.1:15733
  TOKEN_A=$(cat /tmp/token_s1a.txt)
  curl -s "$API/api/v1/mobile/analytics/heatmap?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&granularity=hour&force=true" -H "Authorization: Bearer $TOKEN_A" > /tmp/heatmap.json
  python3 -c "import json,collections; buckets=json.load(open('/tmp/heatmap.json'))['data']; bydate=collections.defaultdict(int); [bydate.__setitem__(b['localDate'], bydate[b['localDate']]+b['foregroundSeconds']) for b in buckets]; print(max(bydate.values()), 86400*1.05, max(bydate.values())<=86400*1.05); print(dict(bydate))"
  # 86400 90720 True
  ```
- 预期：单天 `<=90720`。
- 实际：`2026-09-01 86400 <=90720`，其余 `3600~21600`，无超限日。
- 证据：`evidence/api/session1/heatmap.json`、`tolerance_check.txt`（`max per day 86400 limit 90720`）。
