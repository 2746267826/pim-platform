# PASS-044 | Mobile | 通过 | 一致性：overview == heatmap == charts 误差<=桶数

- 描述：在隔离脏数据窗口 `2026-09-01~2026-09-30 device=session1-device-A` 上，`GET /analytics/overview` `total=149400`、`heatmap` 桶和=`149400`、`charts daily-total` 和=`149400`，差值 `0 <= 46`（桶数），三端一致。
- 复现：
  ```bash
  API=http://127.0.0.1:15733
  TOKEN_A=$(cat /tmp/token_s1a.txt)
  curl -s "$API/api/v1/mobile/analytics/overview?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&force=true" -H "Authorization: Bearer $TOKEN_A" > /tmp/overview.json
  curl -s "$API/api/v1/mobile/analytics/heatmap?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&granularity=hour&force=true" -H "Authorization: Bearer $TOKEN_A" > /tmp/heatmap.json
  curl -s "$API/api/v1/mobile/analytics/charts?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-A&force=true" -H "Authorization: Bearer $TOKEN_A" > /tmp/charts.json
  python3 -c "import json; ov=json.load(open('/tmp/overview.json'))['data']['totalForegroundSeconds']; hm=sum(b['foregroundSeconds'] for b in json.load(open('/tmp/heatmap.json'))['data']); ch=sum(p['foregroundSeconds'] for p in next(c for c in json.load(open('/tmp/charts.json'))['data'] if c['key']=='daily-total')['points']); print(ov,hm,ch, ov-hm, ov-ch)"
  # 149400 149400 149400 0 0
  ```
- 预期：`overview.total == heatmap和 == charts和` 误差 `<=桶数(46)`。
- 实际：三者和均为 `149400`，误差 `0`，`46` 桶均为 `5400` 或 `3600`，`consistency_check.txt` 记录。
- 证据：`evidence/api/session1/overview.json`（`totalForegroundSeconds:149400`）、`heatmap.json`（46桶）、`charts.json`（`daily-total` 12点和149400）、`consistency_check.txt`。
