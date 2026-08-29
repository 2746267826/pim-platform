# PASS-010 | docs/operations/android-client-stage1-acceptance.md & microsoft-calendar-sync-acceptance.md | 合格 | 客户端与日历同步阶段验收（阶段性文档）
- 验证方式：read_file + grep `MobileSyncCoordinator` `LocationAcquisitionCoordinator` `PimServerUrls` `MicrosoftGraph` `GraphService`
- 验证点：android-stage1 声称运行/同步/状态/恢复/诊断五路径通过 9 项模拟器场景；microsoft 验收声称发现默认/分组/课程表日历、UTC+8 展示、全天边界、重复实例、write-back 二次确认、ETag 412、token 续期
- 代码实际：`src/client-android` 14 包中 `mobile/sync/MobileSyncCoordinator.kt` 实现唯一执行路径与队列重试；`location/acquisition/LocationAcquisitionCoordinator.kt` 前台服务与 `BOOT_COMPLETED` 恢复；`src/modules/Pim.Module.Calendar/Services/MicrosoftGraphSyncService.cs` 实现 calendars 发现、`If-Match` ETag 与 412 处理、UTC 存储与 `TimeZoneInfo` 转换；`evidence/android-manifest.txt` 与 `evidence/dotnet-test-1669.log` 对应自动化前置检查通过
- 结论：阶段性验收清单与代码关键服务存在且语义一致，缺失的真实账号验收标记为待验收而非已承诺功能，标记为通过
