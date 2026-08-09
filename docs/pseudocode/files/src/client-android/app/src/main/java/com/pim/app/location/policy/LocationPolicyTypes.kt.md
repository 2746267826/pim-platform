# src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：`LocationPolicyMode`：见源文件职责（LocationPolicyTypes.kt）。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### LocationPolicyMode
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L3 声明 `LocationPolicyMode`
- 分支与异常：无
- 调用：无

### TrackingPolicy
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L12 声明 `TrackingPolicy`
- 分支与异常：无
- 调用：无

### PolicyDecision
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L21 声明 `PolicyDecision`
- 分支与异常：无
- 调用：无

### ScheduleWindow
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L29 声明 `ScheduleWindow`
- 分支与异常：无
- 调用：无

### MotionSignal
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L40 声明 `MotionSignal`
- 分支与异常：无
- 调用：无

### PolicyLocation
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L49 声明 `PolicyLocation`
- 分支与异常：无
- 调用：无

### LocationPolicyInput
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L55 声明 `LocationPolicyInput`
- 分支与异常：无
- 调用：无

### isActiveAt
#### isActiveAt(nowMillis: Long)
- 输入：nowMillis: Long
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `isActiveAt` 参数：nowMillis: Long
- 分支与异常：无显著分支
- 调用：isActiveAt

## 近逐行中文伪代码

1. [L3] 枚举 `LocationPolicyMode`
2. [L5] 执行：PowerSavingNormal,
3. [L6] 执行：ScheduleLowFrequency,
4. [L7] 执行：MotionObservation,
5. [L8] 执行：MovementRecovery,
6. [L9] 执行：SyncFallback
7. [L12] 定义类 `TrackingPolicy`
8. [L13] 执行：val normalIntervalMillis: Long = 3 * 60 * 1000L,
9. [L14] 执行：val scheduleLowFrequencyIntervalMillis: Long = 15 * 60 * 1000L,
10. [L15] 执行：val movementIntervalMillis: Long = 60 * 1000L,
11. [L16] 执行：val scheduleRecoveryThresholdMeters: Double = 100.0,
12. [L17] 执行：val altitudeWaitTimeoutMillis: Long = 15 * 1000L,
13. [L18] 执行：val maxUploadAccuracyMetersExclusive: Float = 50f
14. [L21] 定义类 `PolicyDecision`
15. [L22] 执行：val mode: LocationPolicyMode,
16. [L23] 执行：val requestIntervalMillis: Long,
17. [L24] 执行：val nextExpectedLocationAtMillis: Long,
18. [L25] 执行：val reason: String,
19. [L26] 执行：val scheduleLowFrequency: Boolean
20. [L29] 定义类 `ScheduleWindow`
21. [L30] 执行：val id: String,
22. [L31] 执行：val title: String,
23. [L32] 执行：val locationText: String,
24. [L33] 执行：val startsAtMillis: Long,
25. [L34] 执行：val endsAtMillis: Long
26. [L36] 函数 `isActiveAt` 参数：nowMillis: Long
27. [L37] 执行：nowMillis in startsAtMillis until endsAtMillis && locationText.isNotBlank()
28. [L40] 枚举 `MotionSignal`
29. [L45] 执行：OnBicycle,
30. [L46] 执行：InVehicle
31. [L49] 定义类 `PolicyLocation`
32. [L50] 执行：val latitude: Double,
33. [L51] 执行：val longitude: Double,
34. [L52] 执行：val recordedAtMillis: Long
35. [L55] 定义类 `LocationPolicyInput`
36. [L56] 执行：val nowMillis: Long,
37. [L57] 执行：val collectionEnabled: Boolean,
38. [L58] 执行：val currentScheduleWindow: ScheduleWindow? = null,
39. [L59] 执行：val motionSignal: MotionSignal = MotionSignal.Unknown

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt",
      "label": "LocationPolicyMode",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
