# PASS-002 | 安装启动 | 通过 | app 安装到模拟器并启动 MainActivity 成功

- 描述：`adb install -r app-debug.apk` 显示 Success，启动 `com.pim.app/.MainActivity` 后 `pidof` 返回 pid 且 `dumpsys window mCurrentFocus` 指向 `com.pim.app.MainActivity`，无闪退。
- 复现：`adb install -r /workspace/pim-platform/src/client-android/app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.pim.app/.MainActivity && adb shell pidof com.pim.app && adb shell dumpsys window | grep mCurrentFocus`
- 预期：安装 Success，进程存活，`mCurrentFocus` 为 `com.pim.app.MainActivity`
- 实际：`Performing Streamed Install Success`，`pid 8185`（本次）/`3588`（首启），`mCurrentFocus=Window{9d6079d u0 com.pim.app/com.pim.app.MainActivity}`，`versionName=0.0.0(local) targetSdk=34`
- 证据：`adb shell pm list packages | grep com.pim.app`、`adb shell dumpsys package com.pim.app | grep versionName`、`adb shell dumpsys window | grep mCurrentFocus`、`adb logcat --pid 8185 | head` 无 crash
