# PASS-043 | Mobile | 通过 | /usage/events 同 BatchId 幂等，DB 只写1次

- 描述：`POST /api/v1/mobile/usage/events` 同 `deviceId+BatchId` 发2次，DB `mobile_usage_events`/`mobile_sync_batches` 仅落1次，`ItemResults` 第二次返回同一 `accepted` 集合，顺序幂等正常。
- 复现：
  ```bash
  API=http://127.0.0.1:15733
  TOKEN_A=$(cat /tmp/token_s1a.txt)
  cat > /tmp/payload_pass.json <<'JSON'
  {"deviceId":"session1-device-A","clientBatchId":"final-v1-batch-2","sourceWindowStartUtc":"2026-09-20T14:00:00Z","sourceWindowEndUtc":"2026-09-20T15:00:00Z","apps":[{"packageName":"com.final.v1c","displayName":"FinalV1c","versionName":"1.0","versionCode":1,"isSystemApp":false,"categoryName":"test","installerPackageName":"com.android.vending","firstInstallTimeUtc":"2026-01-01T00:00:00Z","lastUpdateTimeUtc":"2026-06-01T00:00:00Z","rawJson":"{}"}],"events":[{"packageName":"com.final.v1c","eventType":"MOVE_TO_FOREGROUND","eventTimestampUtc":"2026-09-20T14:05:00Z","className":"Main","collectedAtUtc":"2026-09-20T14:06:00Z","rawJson":"{}"},{"packageName":"com.final.v1c","eventType":"MOVE_TO_BACKGROUND","eventTimestampUtc":"2026-09-20T14:25:00Z","className":"Main","collectedAtUtc":"2026-09-20T14:26:00Z","rawJson":"{}"}],"fallbackSummaries":[]}
  JSON
  curl -s -X POST $API/api/v1/mobile/usage/events -H "Authorization: Bearer $TOKEN_A" -H "Content-Type: application/json" -d @/tmp/payload_pass.json > /tmp/p1.json
  curl -s -X POST $API/api/v1/mobile/usage/events -H "Authorization: Bearer $TOKEN_A" -H "Content-Type: application/json" -d @/tmp/payload_pass.json > /tmp/p2.json
  PGPASSWORD=62f0a50bb963bb648f8e400399def95a psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT count(*) FROM mobile_usage_events WHERE package_name='com.final.v1c'; SELECT count(*) FROM mobile_sync_batches WHERE batch_id='final-v1-batch-2';"
  # 顺序执行：events 2→2，batches 1，p1与p2 JSON 完全一致（3×accepted）
  ```
- 预期：`count events=2, batches=1, p1==p2`。
- 实际：符合预期，`p1.json`/`p2.json` 均 `acceptedCount=3, ItemResults 3×accepted`，DB `2/1`。
- 证据：`evidence/api/session1/usage_idem_req1.json`、`usage_idem_req2.json`、`evidence/api/session1/db_snapshot.txt`（验证后已清理），`src/modules/Pim.Module.Mobile/Services/MobileUsageIngestService.cs:53-60` `existingBatch` 提前返回 `BuildPersistedResult`。
