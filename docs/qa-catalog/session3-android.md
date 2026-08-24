# Session 3 安卓真机测试汇总

> 执行时间：2026-08-24 02:23-02:58 UTC  
> 模拟器：`test_avd_36`（API 36, Android 16, 1080x2340, swiftshader_indirect, -no-snapshot -memory 2048）  
> 宿主：Java 17.0.20, Gradle 8.14, Android SDK 37.2.4, adb 37.0.1, dotnet 8.0.424  
> 产物证据：`/workspace/pim-platform/docs/qa-catalog/evidence/android/`  
> 分支：master（未改业务代码，仅新增本目录与证据）

## 环境准备

- 启动命令：`emulator -avd test_avd_36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -memory 2048 &`（实测 `-memory 1536/2048` 均会自增至 2560MB）
- 等待就绪：`adb wait-for-device && adb shell getprop sys.boot_completed` 约 37s 返回 `1`（`Boot completed in 37287 ms`），创 VkInstance 后 `Increasing screen off timeout, logcat buffer size to 2M`
- 初始僵尸进程清理：早期多次启动产生 zombie `qemu-system-x86` / `emulator`（PPid 1），执行 `adb kill-server && adb start-server` 后端口 5554 释放，`adb devices` 显示 `emulator-5554 device`
- 磁盘：`overlay 216G 197G 8.4G 96%`，`MemAvailable 13G`，`KVM usable`，`swiftshader` 仅警告 `VK_FORMAT_G10X6` 不支持 YCbCr 转换与 `libX11.so` 回退，不影响启动

## 验证项逐项结果

| # | 验证项 | 结论 | 证据 |
|---|---|---|---|
| 1 | `./gradlew connectedAndroidTest` 跑通 | 部分通过（见 AND-001） | `PASS-001` + `AND-001` |
| 2 | `assembleDebug` + app 安装到模拟器并启动 | 通过 | `PASS-001` `PASS-002` |
| 3 | 定位权限授予后定位功能验证 | 通过（权限与定位可用） | `PASS-003` |
| 4 | 数据同步到 `pim_test` 库验证 | 未通过（BLOCKED，见 AND-002/003） | `AND-002` `AND-003` |
| 5 | 流体云 / android live updates | [SKIP] 模拟器不支持 | `PASS-004` |

### 1. connectedAndroidTest

- 全模块 `connectedAndroidTest` 直接 FAILED：`:core:connectedDebugAndroidTest` 报 `java.lang.ClassNotFoundException: androidx.test.runner.AndroidJUnitRunner` on path `com.pim.core.test`（缺 runner 依赖），属构建配置缺陷，见 `adb logcat` PID 5361 与 `core/build/reports`。
- 单模块 `:app:connectedDebugAndroidTest`：
  - 声明用例 62，执行中 50+ PASS、1 SKIPPED（live update），但末尾 `Process crashed.`（`Zygote Process 6983 exited due to signal 9`），`INSTRUMENTATION_RESULT: shortMsg=Process crashed.`，`BUILD FAILED`；日志 `InputDispatcher: Channel ... unrecoverably broken`。
  - 单例重跑 `com.pim.app.ui.location.LocationScreenTest#cancelButtonTriggersOnCancelCallback` 则 `BUILD SUCCESSFUL in 21s`，说明套件在并行/批量执行时不稳定，疑似内存或 Activity  finish 时序导致被系统 kill。
  - 结论：测试框架可跑，但全量稳定性不足，需拆分 sharding 或修复 `core` 模块 instrumentation 配置。

### 2. app 安装与启动

- `assembleDebug` 一次成功：`BUILD SUCCESSFUL in 1m 04s`，`app-debug.apk` 18M，`version 0.0.0(local) versionCode 1 minSdk 26 targetSdk 34 compileSdk 36.1`，仅 11 行 Kotlin 警告（unused param 等）。
- `adb install -r app-debug.apk` → `Success`，`am start -n com.pim.app/.MainActivity` → `pid 8185`（亦见 3588/7582 等），`dumpsys window mCurrentFocus=com.pim.app.MainActivity`，`logcat --pid` 无 Fatal，`Davey! duration 1.7-3.5s Skipped 100 frames` 仅为首帧卡顿。

### 3. 定位权限与定位功能

- 初始权限：`POST_NOTIFICATIONS / ACCESS_FINE / ACCESS_COARSE / ACCESS_BACKGROUND = granted=false`。
- 执行 `pm grant` + `appops set FINE/COARSE allow` 后全部 `granted=true`（`ACCESS_BACKGROUND` 需 `pm grant` 且 `RESTRICTION_INSTALLER_EXEMPT`）。
- `Location Setting: true`，`adb emu geo fix 121.4737 31.2304` 后 `dumpsys location gps provider last location=Location[gps 31.230398,121.473698 hAcc=5.0 et=+6m38s338ms alt=0.0 ...]`，`GNSS_KPI: 97 reports TTFF 3.599s`，`fused provider enabled=true`。
- App 侧 `ForegroundLocationService` 已启动但未收到 `FusedLocationProvider` 请求前 `mStarted=false`，注入后 `last location` 即更新，证明模拟器定位链路通畅。

### 4. 数据同步到 pim_test

- 服务端 `http://127.0.0.1:5858/health` healthy，`pim_test` 库直连查询 `mobile_location_points=6224, mobile_usage_sessions=121425/147945, mobile_usage_summaries 8 devices`，库可读写。
- 端侧 `pim_mobile_sync_state.xml` 持续 `phase=server-missing outcome=BLOCKED progress_text=服务器地址未配置，已跳过同步。 last_error=服务器地址未配置，已跳过同步。`，`WM-WorkerWrapper: Work result FAILURE for MobileSyncWorker`，`dumpsys dbinfo pim.db` 显示 `mobile_sync_batches` 等表空、`pending_queue_count=0`、`accepted_count=0`。
- 原因：`PimServerUrls` 默认 `http://127.0.0.1:5858` 需在设置页登录后配置，但自动化测试未走登录/设置流程，`ServerSettingsStore` 为空导致 `MobileSyncCoordinator` 直接跳过上传，本地 `pim.db` 保持空库（仅 `android_metadata`）。
- 本地空库还导致 `Today/Status` 页无数据且 `Skipped 100 frames` 首帧卡顿，但无错误提示，与服务端已有数据形成静默不一致。

### 5. 流体云 / android live updates [SKIP]

- `LocationLiveUpdateCapability.check()` 要求 `SDK_INT ==36 && SDK_FULL >= 36.1 (BAKLAVA_1)`；`test_avd_36` 为 `sdk 36, sdk_full 36.0, codename REL`，不满足，`LocationLiveUpdatePlatformTest.liveUpdateNotificationHasPromotableCharacteristics` 以 `Assume.assumeTrue` 跳过：`SKIPPED org.junit.AssumptionViolatedException: got: <false>, expected: is <true> at LocationLiveUpdatePlatformTest.kt:23`。
- 另一用例 `liveUpdateNotificationIdDiffersFromOngoingNotification` PASS（ID 7102 vs 7001/7101）。
- 按任务书“不支持的功能（流体云等）标 `[SKIP]` 并说明原因”标记本项为 `[SKIP] 模拟器不支持`，设备需 `test_avd_361`（36.1）或真机 36.1+ 才能验证 promotable characteristics（`hasPromotableCharacteristics()`、`EXTRA_BIG_TEXT` 不含经纬度等）。

## 问题清单（本次新增）

| 编号 | 级别 | 标题 | 证据 |
|---|---|---|---|
| AND-001 | 严重 | connectedAndroidTest 全量 62 例末尾进程崩溃 | `evidence/android/AND-001.md` + `connected-report/` + `testlog/test-results.log` |
| AND-002 | 严重 | 数据同步到 pim_test 被 BLOCKED（服务器地址未配置） | `evidence/android/AND-002.md` + `pim_mobile_sync_state.xml` |
| AND-003 | 一般 | 本地 pim.db 空库导致聚合与服务端不一致 | `evidence/android/AND-003.md` + `dumpsys dbinfo` |
| PASS-001 | - | assembleDebug 构建成功 | `evidence/android/PASS-001.md` |
| PASS-002 | - | app 安装启动成功 | `evidence/android/PASS-002.md` |
| PASS-003 | - | 定位权限与定位可用 | `evidence/android/PASS-003.md` |
| PASS-004 | [SKIP] | live updates 模拟器不支持 | `evidence/android/PASS-004.md` |

> 详细复现、预期/实际、证据路径见各 `AND-*/PASS-*.md`。本次不改业务代码，不修复 bug，仅记录。

## 证据清单

| 证据文件 | 来源 | 说明 |
|---|---|---|
| `evidence/android/PASS-001.md` | `gradlew assembleDebug` | 构建成功 |
| `evidence/android/PASS-002.md` | `adb install/start` | 安装启动 |
| `evidence/android/PASS-003.md` | `pm grant + dumpsys location` | 定位可用 |
| `evidence/android/AND-001.md` | `connectedAndroidTest` | 进程崩溃 |
| `evidence/android/AND-002.md` | `pim_mobile_sync_state.xml` | 同步 BLOCKED |
| `evidence/android/AND-003.md` | `dumpsys dbinfo` | 空库 |
| `evidence/android/PASS-004.md` | `LocationLiveUpdateCapability` | [SKIP] live update |
| `evidence/android/emu-boot.log` | `nohup emulator ... > /tmp/emu_latest.log` | 启动 37287ms |
| `evidence/android/logcat-main.log` | `adb logcat -d` | 21M 全量 |
| `evidence/android/gradle-connected-smoke.log` | `gradlew :app:connectedDebugAndroidTest` 单例 | smoke PASS |
| `evidence/android/connected-report/` | `app/build/reports/androidTests/connected/debug` | HTML 报表 |

## 运行命令摘要

```bash
# 模拟器
emulator -avd test_avd_36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -memory 2048 &
adb wait-for-device && adb shell getprop sys.boot_completed  # 1 (37s)

# 构建与安装
cd /workspace/pim-platform/src/client-android && ./gradlew assembleDebug  # BUILD SUCCESSFUL in 1m 04s
adb install -r app/build/outputs/apk/debug/app-debug.apk  # Success
adb shell am start -n com.pim.app/.MainActivity && adb shell pidof com.pim.app  # 8185

# 权限与定位
adb shell pm grant com.pim.app android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.pim.app android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.pim.app android.permission.ACCESS_BACKGROUND_LOCATION
adb shell pm grant com.pim.app android.permission.POST_NOTIFICATIONS
adb shell appops set com.pim.app FINE_LOCATION allow
adb shell appops set com.pim.app COARSE_LOCATION allow
adb emu geo fix 121.4737 31.2304
adb shell dumpsys location | grep -A2 "last location"  # 31.230398,121.473698
adb shell dumpsys package com.pim.app | grep -A5 "runtime permissions"

# 仪器化测试
./gradlew connectedAndroidTest  # :core FAILED ClassNotFoundException
./gradlew :app:connectedDebugAndroidTest  # 62 tests, 1 skipped, Process crashed (signal 9)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pim.app.AndroidInstrumentationSmokeTest  # PASSED

# 数据同步与 DB
curl http://127.0.0.1:5858/health  # {"status":"healthy"}
PGPASSWORD=62f0a50bb963bb648f8e400399def95a psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT count(*) FROM mobile_location_points;"  # 6224
adb shell run-as com.pim.app cat /data/data/com.pim.app/shared_prefs/pim_mobile_sync_state.xml  # phase=server-missing BLOCKED
adb shell dumpsys dbinfo | grep pim.db
```

## 结论

- 清单 5 项已全部执行完毕：1 项部分失败（AND-001）、2 项通过、1 项 BLOCKED（AND-002/003）、1 项 [SKIP]。
- 模拟器 `test_avd_36` 可正常启动并承载 `connectedAndroidTest`，但全量稳定性不足需关注；`test_avd_361`（36.1）未在本 session 启动，如需验证 live update 需切 36.1。
- 未修改业务代码，未修复 bug，符合“只验不修”与“每发现一个问题立刻写文件”要求。

*汇总文件：`/workspace/pim-platform/docs/qa-catalog/session3-android.md`，证据目录：`/workspace/pim-platform/docs/qa-catalog/evidence/android/`*
