# src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt

## 元信息
- 语言：Kotlin (JUnit)
- 程序集或包：client-android app test
- 职责：验证 DetectedActivity → MotionSignal 映射、不可用状态文案、Transition 规划 ENTER/EXIT 覆盖。
- 主要依赖：MotionSignalMapper、MotionSignalStatus、MotionTransitionPlanner、GMS ActivityTransition/DetectedActivity
- 被谁使用：单元测试运行器

## 函数级结构化伪代码

### MotionSignalMapperTest
#### mapsDetectedActivitiesToPolicySignals
- 步骤：对 STILL/WALKING/RUNNING/ON_BICYCLE/IN_VEHICLE/UNKNOWN 断言 fromDetectedActivity 等于对应 MotionSignal

#### unavailableMotionKeepsPolicyAtUnknownWithStatusIssue
- 步骤：MotionSignalStatus.unavailable(权限消息)
- 断言 signal=Unknown、issueCode=activity-recognition-unavailable、message=缺少活动识别权限、无 U+FFFD

#### buildsEnterAndExitTransitionsForPolicyMotionSignals
- 步骤：transitions() 共 10 条；五种活动类型各含 ENTER 与 EXIT

## 近逐行中文伪代码

1. 类 MotionSignalMapperTest。
2. 测试一：六种 DetectedActivity 映射。
3. 测试二：权限不可用状态字段与无乱码。
4. 测试三：Transition 规划 5 活动 × 2 过渡 = 10。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt",
      "label": "MotionSignalMapperTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/motion/MotionSignalMapperTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/MotionSignal.kt", "type": "depends_on" }
  ]
}
```
