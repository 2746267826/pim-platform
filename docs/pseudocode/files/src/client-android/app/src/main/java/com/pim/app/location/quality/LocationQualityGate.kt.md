# src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：运行时组件 `RawLocationFix`：移动端采集/同步链路中的策略或上报单元。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### RawLocationFix
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L3 声明 `RawLocationFix`
- 分支与异常：无
- 调用：无

### QualityAcceptedLocation
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L17 声明 `QualityAcceptedLocation`
- 分支与异常：无
- 调用：无

### PendingAltitudeFix
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L24 声明 `PendingAltitudeFix`
- 分支与异常：无
- 调用：无

### QualityDecision
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L29 声明 `QualityDecision`
- 分支与异常：无
- 调用：无

### AcceptNow
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L30 声明 `AcceptNow`
- 分支与异常：无
- 调用：无

### WaitForAltitude
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L31 声明 `WaitForAltitude`
- 分支与异常：无
- 调用：无

### Drop
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L32 声明 `Drop`
- 分支与异常：无
- 调用：无

### LocationQualityGate
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L35 声明 `LocationQualityGate`
- 分支与异常：无
- 调用：无

### evaluate
#### evaluate(fix: RawLocationFix, nowMillis: Long = fix.recordedAtMillis)
- 输入：fix: RawLocationFix, nowMillis: Long = fix.recordedAtMillis
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `evaluate` 参数：fix: RawLocationFix, nowMillis: Long = fix.recordedAtMillis
  2. 执行：val accuracy = fix.horizontalAccuracyMeters
  3. 执行：?: return QualityDecision.Drop(fix, "missing-horizontal-accuracy")
  4. 若 (!accuracy.isFinite() || accuracy >= maxAccuracyMetersExclusive) 则
  5. 返回 QualityDecision.Drop(fix, "horizontal-accuracy-too-low")
  6. 执行：val altitude = fix.altitudeMeters
  7. 返回 if (altitude != null) {
  8. 执行：QualityDecision.AcceptNow(
  9. 执行：QualityAcceptedLocation(
  10. 执行：fix = fix,
  11. 执行：altitudeMeters = altitude,
  12. 执行：acceptedAtMillis = nowMillis,
  13. 执行：qualityFlags = emptySet()
  14. 执行：QualityDecision.WaitForAltitude(
  15. 执行：PendingAltitudeFix(
  16. 执行：deadlineMillis = fix.recordedAtMillis + altitudeWaitTimeoutMillis
- 分支与异常：if (!accuracy.isFinite() || accuracy >= maxAccuracyMetersExclusive) {
- 调用：evaluate、QualityDecision.Drop、accuracy.isFinite、QualityDecision.AcceptNow、QualityAcceptedLocation、emptySet、QualityDecision.WaitForAltitude、PendingAltitudeFix

### timeoutDecision
#### timeoutDecision(pending: PendingAltitudeFix, nowMillis: Long)
- 输入：pending: PendingAltitudeFix, nowMillis: Long
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `timeoutDecision` 参数：pending: PendingAltitudeFix, nowMillis: Long
  2. 若 (nowMillis < pending.deadlineMillis) 则
  3. 返回 QualityDecision.WaitForAltitude(pending)
  4. 返回 QualityDecision.AcceptNow(
  5. 执行：QualityAcceptedLocation(
  6. 执行：fix = pending.fix,
  7. 执行：altitudeMeters = null,
  8. 执行：acceptedAtMillis = nowMillis,
  9. 执行：qualityFlags = setOf("altitude-missing-timeout")
- 分支与异常：if (nowMillis < pending.deadlineMillis) {
- 调用：timeoutDecision、QualityDecision.WaitForAltitude、QualityDecision.AcceptNow、QualityAcceptedLocation、setOf

## 近逐行中文伪代码

1. [L3] 定义类 `RawLocationFix`
2. [L4] 执行：val latitude: Double,
3. [L5] 执行：val longitude: Double,
4. [L6] 执行：val horizontalAccuracyMeters: Float?,
5. [L7] 执行：val altitudeMeters: Double?,
6. [L8] 执行：val provider: String,
7. [L9] 执行：val recordedAtMillis: Long,
8. [L10] 执行：val policyMode: String,
9. [L11] 执行：val scheduleLowFrequency: Boolean,
10. [L12] 执行：val motionSignal: String,
11. [L13] 执行：val speedMetersPerSecond: Float? = null,
12. [L14] 执行：val bearingDegrees: Float? = null
13. [L17] 定义类 `QualityAcceptedLocation`
14. [L18] 执行：val fix: RawLocationFix,
15. [L19] 执行：val altitudeMeters: Double?,
16. [L20] 执行：val acceptedAtMillis: Long,
17. [L21] 执行：val qualityFlags: Set<String>
18. [L24] 定义类 `PendingAltitudeFix`
19. [L25] 执行：val fix: RawLocationFix,
20. [L26] 执行：val deadlineMillis: Long
21. [L29] 执行：sealed class QualityDecision {
22. [L30] 定义类 `AcceptNow`
23. [L31] 定义类 `WaitForAltitude`
24. [L32] 定义类 `Drop`
25. [L35] 定义类 `LocationQualityGate`
26. [L36] 执行：private val maxAccuracyMetersExclusive: Float = 50f,
27. [L37] 执行：private val altitudeWaitTimeoutMillis: Long = 15_000L
28. [L39] 函数 `evaluate` 参数：fix: RawLocationFix, nowMillis: Long = fix.recordedAtMillis
29. [L40] 执行：val accuracy = fix.horizontalAccuracyMeters
30. [L41] 执行：?: return QualityDecision.Drop(fix, "missing-horizontal-accuracy")
31. [L43] 若 (!accuracy.isFinite() || accuracy >= maxAccuracyMetersExclusive) 则
32. [L44] 返回 QualityDecision.Drop(fix, "horizontal-accuracy-too-low")
33. [L47] 执行：val altitude = fix.altitudeMeters
34. [L48] 返回 if (altitude != null) {
35. [L49] 执行：QualityDecision.AcceptNow(
36. [L50] 执行：QualityAcceptedLocation(
37. [L51] 执行：fix = fix,
38. [L52] 执行：altitudeMeters = altitude,
39. [L53] 执行：acceptedAtMillis = nowMillis,
40. [L54] 执行：qualityFlags = emptySet()
41. [L58] 执行：QualityDecision.WaitForAltitude(
42. [L59] 执行：PendingAltitudeFix(
43. [L60] 执行：fix = fix,
44. [L61] 执行：deadlineMillis = fix.recordedAtMillis + altitudeWaitTimeoutMillis
45. [L67] 函数 `timeoutDecision` 参数：pending: PendingAltitudeFix, nowMillis: Long
46. [L68] 若 (nowMillis < pending.deadlineMillis) 则
47. [L69] 返回 QualityDecision.WaitForAltitude(pending)
48. [L72] 返回 QualityDecision.AcceptNow(
49. [L73] 执行：QualityAcceptedLocation(
50. [L74] 执行：fix = pending.fix,
51. [L75] 执行：altitudeMeters = null,
52. [L76] 执行：acceptedAtMillis = nowMillis,
53. [L77] 执行：qualityFlags = setOf("altitude-missing-timeout")

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt",
      "label": "RawLocationFix",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
