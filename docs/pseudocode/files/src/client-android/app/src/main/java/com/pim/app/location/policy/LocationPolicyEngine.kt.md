# src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：策略引擎 `LocationPolicyEngine`：根据输入信号给出策略决策。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### LocationPolicyEngine
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L3 声明 `LocationPolicyEngine`
- 分支与异常：无
- 调用：无

### reduce
#### reduce(input: LocationPolicyInput)
- 输入：input: LocationPolicyInput
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `reduce` 参数：input: LocationPolicyInput
  2. 若 (!input.collectionEnabled) 则
  3. 执行：resetScheduleState()
  4. 返回 decision(
  5. 执行：mode = LocationPolicyMode.Off,
  6. 执行：intervalMillis = 0L,
  7. 执行：nowMillis = input.nowMillis,
  8. 执行：reason = "连续采集未开启",
  9. 执行：scheduleLowFrequency = false,
  10. 执行：nextExpectedLocationAtMillis = Long.MAX_VALUE
  11. 执行：val activeSchedule = input.currentScheduleWindow?.takeIf { it.isActiveAt(input.nowMillis) }
  12. 若 (activeSchedule == null) 则
  13. 若 (input.motionSignal.isMoving()) 则
  14. 返回 motionDecision(input.nowMillis, input.motionSignal)
  15. 返回 normalDecision(input.nowMillis, "默认省电档")
  16. 执行：val scheduleKey = ScheduleKey.from(activeSchedule)
  17. 若 (activeScheduleKey != scheduleKey) 则
  18. 执行：activeScheduleKey = scheduleKey
  19. 执行：scheduleAnchorLocation = null
  20. 执行：movementRecoveryActive = false
  21. 若 (movementRecoveryActive) 则
  22. 执行：mode = LocationPolicyMode.MovementRecovery,
  23. 执行：intervalMillis = policy.movementIntervalMillis,
  24. 执行：reason = "日程期间位置变化超过 ${policy.scheduleRecoveryThresholdMeters.toInt()} 米",
  25. 执行：scheduleLowFrequency = false
  26. 执行：mode = LocationPolicyMode.ScheduleLowFrequency,
  27. 执行：intervalMillis = policy.scheduleLowFrequencyIntervalMillis,
  28. 执行：reason = "当前日程包含位置信息，降低定位频率",
  29. 执行：scheduleLowFrequency = true
- 分支与异常：if (!input.collectionEnabled) {；if (activeSchedule == null) {；if (input.motionSignal.isMoving()) {；if (activeScheduleKey != scheduleKey) {；if (movementRecoveryActive) {
- 调用：reduce、resetScheduleState、decision、it.isActiveAt、input.motionSignal.isMoving、motionDecision、normalDecision、ScheduleKey.from、policy.scheduleRecoveryThresholdMeters.toInt

### onAcceptedLocation
#### onAcceptedLocation(location: PolicyLocation)
- 输入：location: PolicyLocation
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `onAcceptedLocation` 参数：location: PolicyLocation
  2. 执行：activeScheduleKey ?: return
  3. 执行：val anchor = scheduleAnchorLocation
  4. 若 (anchor == null) 则
  5. 执行：scheduleAnchorLocation = location
  6. 返回（空）
  7. 执行：val distanceMeters = GeoDistance.metersBetween(anchor, location)
  8. 若 (distanceMeters > policy.scheduleRecoveryThresholdMeters) 则
  9. 执行：movementRecoveryActive = true
- 分支与异常：if (anchor == null) {；if (distanceMeters > policy.scheduleRecoveryThresholdMeters) {
- 调用：onAcceptedLocation、GeoDistance.metersBetween

### normalDecision
#### normalDecision(nowMillis: Long, reason: String)
- 输入：nowMillis: Long, reason: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun normalDecision(nowMillis: Long, reason: String): PolicyDecision =
- 分支与异常：无显著分支
- 调用：normalDecision

### motionDecision
#### motionDecision(nowMillis: Long, motionSignal: MotionSignal)
- 输入：nowMillis: Long, motionSignal: MotionSignal
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun motionDecision(nowMillis: Long, motionSignal: MotionSignal): PolicyDecision =
- 分支与异常：无显著分支
- 调用：motionDecision

### resetScheduleState
#### resetScheduleState(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun resetScheduleState() {
  2. 执行：activeScheduleKey = null
  3. 执行：scheduleAnchorLocation = null
  4. 执行：movementRecoveryActive = false
- 分支与异常：无显著分支
- 调用：resetScheduleState

### from
#### from(window: ScheduleWindow)
- 输入：window: ScheduleWindow
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `from` 参数：window: ScheduleWindow
- 分支与异常：无显著分支
- 调用：from、ScheduleKey

## 近逐行中文伪代码

1. [L3] 定义类 `LocationPolicyEngine`
2. [L4] 执行：private val policy: TrackingPolicy
3. [L6] 执行：private var activeScheduleKey: ScheduleKey? = null
4. [L7] 执行：private var scheduleAnchorLocation: PolicyLocation? = null
5. [L8] 执行：private var movementRecoveryActive: Boolean = false
6. [L10] 函数 `reduce` 参数：input: LocationPolicyInput
7. [L11] 若 (!input.collectionEnabled) 则
8. [L12] 执行：resetScheduleState()
9. [L13] 返回 decision(
10. [L14] 执行：mode = LocationPolicyMode.Off,
11. [L15] 执行：intervalMillis = 0L,
12. [L16] 执行：nowMillis = input.nowMillis,
13. [L17] 执行：reason = "连续采集未开启",
14. [L18] 执行：scheduleLowFrequency = false,
15. [L19] 执行：nextExpectedLocationAtMillis = Long.MAX_VALUE
16. [L23] 执行：val activeSchedule = input.currentScheduleWindow?.takeIf { it.isActiveAt(input.nowMillis) }
17. [L24] 若 (activeSchedule == null) 则
18. [L25] 执行：resetScheduleState()
19. [L26] 若 (input.motionSignal.isMoving()) 则
20. [L27] 返回 motionDecision(input.nowMillis, input.motionSignal)
21. [L29] 返回 normalDecision(input.nowMillis, "默认省电档")
22. [L32] 执行：val scheduleKey = ScheduleKey.from(activeSchedule)
23. [L33] 若 (activeScheduleKey != scheduleKey) 则
24. [L34] 执行：activeScheduleKey = scheduleKey
25. [L35] 执行：scheduleAnchorLocation = null
26. [L36] 执行：movementRecoveryActive = false
27. [L39] 若 (movementRecoveryActive) 则
28. [L40] 返回 decision(
29. [L41] 执行：mode = LocationPolicyMode.MovementRecovery,
30. [L42] 执行：intervalMillis = policy.movementIntervalMillis,
31. [L43] 执行：nowMillis = input.nowMillis,
32. [L44] 执行：reason = "日程期间位置变化超过 ${policy.scheduleRecoveryThresholdMeters.toInt()} 米",
33. [L45] 执行：scheduleLowFrequency = false
34. [L49] 若 (input.motionSignal.isMoving()) 则
35. [L50] 返回 motionDecision(input.nowMillis, input.motionSignal)
36. [L53] 返回 decision(
37. [L54] 执行：mode = LocationPolicyMode.ScheduleLowFrequency,
38. [L55] 执行：intervalMillis = policy.scheduleLowFrequencyIntervalMillis,
39. [L56] 执行：nowMillis = input.nowMillis,
40. [L57] 执行：reason = "当前日程包含位置信息，降低定位频率",
41. [L58] 执行：scheduleLowFrequency = true
42. [L62] 函数 `onAcceptedLocation` 参数：location: PolicyLocation
43. [L63] 执行：activeScheduleKey ?: return
44. [L64] 执行：val anchor = scheduleAnchorLocation
45. [L65] 若 (anchor == null) 则
46. [L66] 执行：scheduleAnchorLocation = location
47. [L67] 返回（空）
48. [L70] 执行：val distanceMeters = GeoDistance.metersBetween(anchor, location)
49. [L71] 若 (distanceMeters > policy.scheduleRecoveryThresholdMeters) 则
50. [L72] 执行：movementRecoveryActive = true
51. [L76] 执行：private fun normalDecision(nowMillis: Long, reason: String): PolicyDecision =
52. [L77] 执行：decision(
53. [L78] 执行：mode = LocationPolicyMode.PowerSavingNormal,
54. [L79] 执行：intervalMillis = policy.normalIntervalMillis,
55. [L80] 执行：nowMillis = nowMillis,
56. [L81] 执行：reason = reason,
57. [L82] 执行：scheduleLowFrequency = false
58. [L85] 执行：private fun motionDecision(nowMillis: Long, motionSignal: MotionSignal): PolicyDecision =
59. [L86] 执行：decision(
60. [L87] 执行：mode = LocationPolicyMode.MotionObservation,
61. [L88] 执行：intervalMillis = policy.movementIntervalMillis,
62. [L89] 执行：nowMillis = nowMillis,
63. [L90] 执行：reason = "检测到运动状态：$motionSignal",
64. [L91] 执行：scheduleLowFrequency = false
65. [L94] 执行：private fun decision(
66. [L95] 执行：mode: LocationPolicyMode,
67. [L96] 执行：intervalMillis: Long,
68. [L97] 执行：nowMillis: Long,
69. [L98] 执行：reason: String,
70. [L99] 执行：scheduleLowFrequency: Boolean,
71. [L100] 执行：nextExpectedLocationAtMillis: Long = nowMillis + intervalMillis
72. [L101] 执行：): PolicyDecision = PolicyDecision(
73. [L102] 执行：mode = mode,
74. [L103] 执行：requestIntervalMillis = intervalMillis,
75. [L104] 执行：nextExpectedLocationAtMillis = nextExpectedLocationAtMillis,
76. [L105] 执行：reason = reason,
77. [L106] 执行：scheduleLowFrequency = scheduleLowFrequency
78. [L109] 执行：private fun resetScheduleState() {
79. [L110] 执行：activeScheduleKey = null
80. [L111] 执行：scheduleAnchorLocation = null
81. [L112] 执行：movementRecoveryActive = false
82. [L115] when 分支匹配
83. [L116] 执行：MotionSignal.Walking,
84. [L117] 执行：MotionSignal.Running,
85. [L118] 执行：MotionSignal.OnBicycle,
86. [L119] 分支臂：MotionSignal.InVehicle -> true
87. [L120] 执行：MotionSignal.Unknown,
88. [L121] 分支臂：MotionSignal.Still -> false
89. [L124] 执行：private data class ScheduleKey(
90. [L125] 执行：val id: String,
91. [L126] 执行：val locationText: String,
92. [L127] 执行：val startsAtMillis: Long,
93. [L128] 执行：val endsAtMillis: Long
94. [L130] 执行：companion object {
95. [L131] 函数 `from` 参数：window: ScheduleWindow
96. [L132] 执行：id = window.id,
97. [L133] 执行：locationText = window.locationText,
98. [L134] 执行：startsAtMillis = window.startsAtMillis,
99. [L135] 执行：endsAtMillis = window.endsAtMillis

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt",
      "label": "LocationPolicyEngine",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyEngine.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
