# src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：活动识别运动信号：映射 DetectedActivity、注册 ActivityTransition、StateFlow 状态。
- 主要依赖：GMS ActivityRecognition、MotionSignal
- 被谁使用：ForegroundLocationService

## 函数级结构化伪代码

### MotionSignalMapper / MotionTransitionPlanner / MotionSignalStatus
- 活动类型映射；ENTER/EXIT 过渡请求；不可用状态工厂

### MotionSignalRepository
- status StateFlow；权限检查；request/remove updates；BroadcastReceiver 更新信号

## 近逐行中文伪代码

1. 映射 STILL/WALKING 等。
2. 规划 transition 列表。
3. 有权限则注册 GMS 更新。
4. ENTER 设信号，EXIT 回 Unknown。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt",
      "label": "MotionSignalRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/location/motion/MotionSignalRepository.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt",
      "type": "depends_on"
    }
  ]
}
`
