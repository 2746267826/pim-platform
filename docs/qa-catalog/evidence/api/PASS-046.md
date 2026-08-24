# PASS-046 | Mobile | 通过 | 隔离：user A 的Token 查 user B 数据为空

- 描述：`RequireAuthorization` + `MobileUserContext.RequireUserId` 按 `userId` 过滤，`user A`（`s1userA`）带 `session1-device-B`（归属 `s1userB`）查询 `overview/heatmap/timeline-blocks/location/history/devices` 均返回空，`user B` 查自身返回 `9000s`。
- 复现：
  ```bash
  API=http://127.0.0.1:15733
  TOKEN_A=$(cat /tmp/token_s1a.txt) # s1userA e1c1b0c4-...
  TOKEN_B=$(cat /tmp/token_s1b.txt) # s1userB 3f22a020-...
  curl -s "$API/api/v1/mobile/analytics/overview?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-B&force=true" -H "Authorization: Bearer $TOKEN_A"
  # {"code":0,"data":{"totalForegroundSeconds":0,"appCount":0,...}}
  curl -s "$API/api/v1/mobile/analytics/overview?rangeStartUtc=2026-09-01T00:00:00Z&rangeEndUtc=2026-09-30T00:00:00Z&deviceId=session1-device-B&force=true" -H "Authorization: Bearer $TOKEN_B"
  # {"code":0,"data":{"totalForegroundSeconds":9000,"appCount":5,...}}
  curl -s "$API/api/v1/mobile/devices" -H "Authorization: Bearer $TOKEN_A" # 仅 session1-device-A
  curl -s "$API/api/v1/mobile/devices" -H "Authorization: Bearer $TOKEN_B" # 仅 session1-device-B
  PGPASSWORD=62f0a50bb963bb648f8e400399def95a psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT device_id, user_id FROM mobile_devices WHERE device_id IN ('session1-device-A','session1-device-B');"
  ```
- 预期：`A查B` 返回 `total 0 / 空列表`（404或空），无越权泄露。
- 实际：`A查B overview 0`、`timeline-blocks 0`、`location/history 0`、`devices` 仅自身1条，符合预期。
- 证据：`evidence/api/session1/isolation_A_query_B.json`（`total 0`）、`isolation_B_query_B.json`（`total 9000`）、`src/modules/Pim.Module.Mobile/Services/MobileUserContext.cs`、`MobileUsageAggregationService.cs:220` `Where(userId==currentUser)`。
