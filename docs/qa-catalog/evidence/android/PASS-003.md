# PASS-003 | 定位 | 通过 | 定位权限授予后定位服务可用（gps 已注入坐标）

- 描述：初始 `dumpsys package` 显示 `ACCESS_FINE/COARSE/BACKGROUND/POST_NOTIFICATIONS` 均为 `granted=false`；执行 `pm grant` + `appops set allow` 后全部 `granted=true`，`dumpsys location` 显示 `Location Setting: true` 且 `adb emu geo fix 121.4737 31.2304` 注入后 `gps provider last location=Location[gps 31.230398,121.473698 hAcc=5.0]`，TTFF 3.599s，97  reports。
- 复现：`adb shell dumpsys package com.pim.app | grep runtime`（初次 false）；`adb shell pm grant com.pim.app android.permission.ACCESS_FINE_LOCATION && pm grant ACCESS_COARSE/ACCESS_BACKGROUND/POST_NOTIFICATIONS && appops set FINE_LOCATION allow && appops set COARSE_LOCATION allow`；`adb emu geo fix 121.4737 31.2304 && adb shell dumpsys location | grep -A2 "last location"`
- 预期：权限 granted，定位服务 enabled，`last location` 非 null 且坐标与注入一致
- 实际：授予后 `ACCESS_FINE_LOCATION: granted=true` 等，`Location Setting: true`，`last location=31.230398,121.473698`，`mStarted=false` 但 `Batching` 已上报 97 次，FLP `fused provider enabled=true`
- 证据：`adb shell dumpsys package com.pim.app | grep -A6 "runtime permissions"`、 `adb shell appops get com.pim.app | grep LOCATION`、`adb shell dumpsys location | grep -E "last location|Location Setting"`、`adb shell logcat -d | grep LocationHistory`
