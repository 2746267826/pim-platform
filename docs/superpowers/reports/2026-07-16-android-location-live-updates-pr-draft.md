# PR Draft: Android Location Live Updates

**Branch:** `codex/android-location-live-updates`  
**Base:** `master`  
**Date:** 2026-07-16

---

## PR Title

```
feat(android): location Live Updates (fluid cloud) for ongoing collection
```

---

## PR Body

### Summary

为 PIM Android **已有**前台持续定位（FGS 通知 `7101`）增加 Android 16 标准 Live Updates（流体云）展示能力，让用户在锁屏/状态栏相关区域快速看到定位是否在跑、多久更新过、精度与策略模式。

**核心设计（方案 B）：**

- `LocationLiveUpdatePresenter`：纯 Kotlin 状态机（Collecting / SuccessHold 30s / Degraded / Paused）
- `LocationNotificationRenderer`：只负责组装 Notification
- `LiveUpdateNotificationCompat`：API 36+ `requestPromotedOngoing` + ProgressStyle，失败静默回退
- **同一条** FGS 通知，不新增第二条定位 ongoing 通知
- 低版本忽略 Live Update API，仍使用更新后的折叠/展开文案

**本 PR 已包含 / 预期包含：**

- Design + implementation plan（`docs/superpowers/specs|plans/2026-07-16-android-location-live-updates*`）
- `compileSdk` 升至 **36**，`targetSdk` 保持 **34**
- CI 安装 `platforms;android-36`（`build-android.yml` 最小改动）
- Presenter / Renderer / Service 薄集成与单元测试（按 plan 落地）

**明确不做：**

- 不修改 `LocationPolicyEngine` 采样间隔、distanceFilter、质量门阈值
- 不改上传协议 / 队列语义 / 服务端 API
- 不引入 OEM 私有流体云 / 双通知
- 不改轨迹地图虚线渲染

### Test plan

#### Verification commands（Windows，工作目录 `src/client-android`）

```powershell
cd src\client-android

# 编译
.\gradlew.bat :app:assembleDebug --no-daemon

# 相关单元测试
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationLiveUpdatePresenterTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationNotificationRendererTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LiveUpdateNotificationCompatTest" --no-daemon
.\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.location.service.ForegroundLocationServiceTest" --no-daemon

# 可选：全量 app 单测
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

#### Manual（API 36 设备 / 模拟器，有则做）

- [ ] 开启连续定位后通知 `7101` 持续展示 Presenter 状态
- [ ] API 36：请求 Live Update 提升；失败时普通 ongoing 仍可用
- [ ] Accepted 后主句约 30s 显示精度摘要，再回落
- [ ] 暂停 / 恢复 / 同步动作仍可用
- [ ] API 低于 36：不崩溃，折叠/展开文案正确

### Notes

| Item | Value |
| --- | --- |
| `compileSdk` | **36** |
| `targetSdk` | **34**（初版不抬 target） |
| `LocationPolicyEngine` | **零 diff / 无行为变更** |
| `dotnet test` | **不需要**（本 PR 仅 Android 客户端展示层 + docs/CI SDK） |
| 通知 ID | 仍为 `7101`，channel `pim_location_collection` |
| Spec | `docs/superpowers/specs/2026-07-16-android-location-live-updates-design.md` |
| Plan | `docs/superpowers/plans/2026-07-16-android-location-live-updates.md` |

### Review focus

1. Presenter 文案优先级与 30s SuccessHold 是否与 design 一致  
2. Live Update API 调用是否严格 SDK ≥ 36 门闩 + try/catch 回退  
3. Service 是否只派发事件、不堆展示逻辑  
4. PolicyEngine / 采样路径无意外改动  

---

## Copy-paste ready (GitHub)

### Title

feat(android): location Live Updates (fluid cloud) for ongoing collection

### Body

## Summary

为 PIM Android 已有前台持续定位（FGS 通知 7101）增加 Android 16 标准 Live Updates（流体云）展示：

- `LocationLiveUpdatePresenter` 纯 Kotlin 状态机（Collecting / SuccessHold 30s / Degraded / Paused）
- `LocationNotificationRenderer` 只组装 Notification
- API 36+ promote + ProgressStyle，失败静默回退；低版本忽略 Live Update API
- 同一条 FGS 通知，不新增第二条定位 ongoing 通知
- `compileSdk` 36 / `targetSdk` 34；CI 安装 `platforms;android-36`
- **不修改** `LocationPolicyEngine` 与采样策略

Spec: `docs/superpowers/specs/2026-07-16-android-location-live-updates-design.md`  
Plan: `docs/superpowers/plans/2026-07-16-android-location-live-updates.md`

## Verification commands (Windows, from `src/client-android`)

    cd src\client-android
    .\gradlew.bat :app:assembleDebug --no-daemon
    .\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationLiveUpdatePresenterTest" --no-daemon
    .\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LocationNotificationRendererTest" --no-daemon
    .\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.notifications.LiveUpdateNotificationCompatTest" --no-daemon
    .\gradlew.bat :app:testDebugUnitTest --tests "com.pim.app.location.service.ForegroundLocationServiceTest" --no-daemon

## Notes

- `compileSdk` 36, `targetSdk` 34
- no `LocationPolicyEngine` change
- no `dotnet test` needed (Android-only surface)
