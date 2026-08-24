# PASS-004 | 流体云 | 跳过 | android live updates 模拟器不支持 [SKIP]

- 描述：`LocationLiveUpdateCapability.check()` 在 `test_avd_36` 上返回 false，`LocationLiveUpdatePlatformTest.liveUpdateNotificationHasPromotableCharacteristics` 以 `Assume.assumeTrue` 跳过（`SKIPPED`），符合任务书“流体云/android live updates → 标 [SKIP] 模拟器不支持”。
- 复现：`adb shell getprop ro.build.version.sdk`（36）`ro.build.version.sdk_full`（36.0）；`LocationLiveUpdateCapability.supportsLiveUpdates` 要求 `majorSdk==36 && SDK_FULL >= BAKLAVA_1 (36.1)`；跑 `LocationLiveUpdatePlatformTest` 观察 `SKIPPED`
- 预期：模拟器上 live update 不可用，测试标记 skipped
- 实际：`SKIPPED org.junit.AssumptionViolatedException: got: <false>, expected: is <true>` 于 `LocationLiveUpdatePlatformTest.kt:23`，`liveUpdateNotificationIdDiffersFromOngoingNotification` 仍 PASS（ID 7102 vs 7101）
- 证据：`app/build/outputs/androidTest-results/connected/debug/test_avd_36(AVD) - 16/testlog/test-results.log`（`SKIPPED`）、`app/src/main/java/com/pim/app/location/liveupdate/LocationLiveUpdateCapability.kt:14`（`majorSdk <36 return false; SDK_FULL >= BAKLAVA_1`）、`adb shell getprop ro.build.version.sdk_full`（36.0）
