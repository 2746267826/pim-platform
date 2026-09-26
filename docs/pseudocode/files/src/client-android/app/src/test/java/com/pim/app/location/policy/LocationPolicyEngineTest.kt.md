# src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt

## 元信息
- 语言：Kotlin / JUnit
- 程序集或包：client-android tests
- 职责：验证 LocationPolicyEngine 模式切换：Off/Normal/日程低频/移动恢复/运动观测。
- 主要依赖：LocationPolicyEngine、TrackingSettings、ScheduleWindow、MotionSignal
- 被谁使用：测试运行器

## 函数级结构化伪代码

### LocationPolicyEngineTest
#### offBecomesNormalWhenCollectionStarts
- 采集开启 → PowerSavingNormal，间隔 = normalInterval

#### collectionDisabledHasNoNextExpectedLocation
- 采集关闭 → Off，nextExpected = Long.MAX_VALUE

#### currentScheduleWithLocationEntersLowFrequency
- 有当前日程窗口 → ScheduleLowFrequency

#### scheduleEndsReturnsToNormal
- 先进入日程再窗口 null → 回到 PowerSavingNormal

#### movementOverOneHundredMetersRecoversFromScheduleLowFrequency
- 日程中两次 accepted location 位移超阈值 → MovementRecovery

#### sameScheduleIdWithChangedWindowResetsRecoveryState
- 同 id 但地点/时间变更 → 恢复状态重置为 ScheduleLowFrequency

#### motionSignalShortensInterval
- Walking 信号 → MotionObservation，间隔 = movementInterval

## 近逐行中文伪代码

1. 默认 TrackingSettings 转 policy；固定 now 与带地点的 schedule。
2. 开启采集应从 Off 进入省电正常模式。
3. 关闭采集无下次定位期望。
4. 当前有日程地点进入低频。
5. 日程结束后回正常。
6. 日程内大幅移动触发 MovementRecovery。
7. 同 id 窗口变更重置 recovery。
8. 步行运动信号缩短间隔。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt",
      "label": "LocationPolicyEngineTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/location/policy/LocationPolicyEngineTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettings.kt",
      "type": "depends_on"
    }
  ]
}
```
