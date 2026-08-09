# src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：前台定位采集：权限/Provider 检查、实时与缓存点、质量门、自动/手动入队并触发同步。
- 主要依赖：LocationManager、AltitudeWaitCoordinator、LocationQueueRepository、MobileSyncScheduler、LocationSubmissionPolicy
- 被谁使用：定位 UI / 服务层

## 函数级结构化伪代码

### 数据类 LocationSnapshot / LocationCaptureState
- 快照字段与 UI 状态流

### LocationCaptureRepository
- startCapture：无权限/无 provider 报错；否则注册 listener、seed lastKnown、wait timer
- stopCapture：removeUpdates
- submitCurrentLocationManually：策略允许则 submitSnapshot
- handleLocation：构 snapshot、策略 reason、可自动提交
- submitSnapshot：质量协调器 accept/drop → enqueueAccepted + enqueueNow
- rawJson：组装上传 JSON
- 辅助：enabledProviders、权限、错误文案映射

### formatSubmitStatus / resolveAutoSubmittedState / enqueueThenSchedule
- 状态文案；自动提交成功锁定；入队后调度，捕获非取消异常

## 近逐行中文伪代码

1. Singleton 注入 Context 与队列/调度。
2. StateFlow 暴露捕获状态。
3. 开始：校验权限与 GPS/网络 provider。
4. 监听 onLocationChanged → handle。
5. 每秒刷新 waitDurationMs。
6. 策略决定手动/自动提交。
7. 质量门丢弃或接受后写队列 JSON 并立即同步。
8. 更新 isSubmitting/autoSubmitted/submitStatus。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt",
      "label": "LocationCaptureRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt", "type": "depends_on" }
  ]
}
```
