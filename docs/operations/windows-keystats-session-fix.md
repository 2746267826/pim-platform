# Windows KeyStats Session Fix Notes

Date: 2026-07-12

## Root cause

Local KeyStats API `http://127.0.0.1:18080/api/stats/` returned HTTP 200 with all counters at 0 because a **Session 0** KeyStats process owned the port while user input happened in Session 1.

Observed before fix:

- PID in Session 0 + PID in Session 1
- API reachable, counters never grew
- `PimKeyStats` scheduled task used elevated/highest rights historically

## Local validation

1. Elevated kill of zombie Session 0 instance (`taskkill` via UAC)
2. Start single user-session `KeyStats.exe`
3. Result:
   - one process in Session 1
   - counters non-zero and growing after keyboard/mouse activity
   - example: `keyPresses` 14332 → 14345 within seconds

## Fixes

### keyStats repo (`https://github.com/2746267826/keyStats`)

Commit `5a2f524`:

- refuse start when `SessionId == 0` or `!Environment.UserInteractive`
- recreate `PimKeyStats` with `/rl limited` (interactive user session)

### PIM Windows daemon (`codex/windows-status-center-keystats`)

- `KeyStatsProcessManager` converges to one current-session process
- `KeyStatsHealthProbe` classifies stale-zero / missing / unreachable
- collector skips upload when unhealthy
- heartbeat reports real AW/KeyStats states
- tray + status center primary path (WebView2 shell retained, not primary)

## Operator recovery if all-zero returns

```powershell
# If access denied, run elevated:
taskkill /F /IM KeyStats.exe /T
Start-Process "C:\ProgramLocal\PIM\KeyStats.exe"
Get-Process KeyStats | Select-Object Id, SessionId
Invoke-RestMethod http://127.0.0.1:18080/api/stats/
```

Expect exactly one Session-1 process and non-zero counters after input.
