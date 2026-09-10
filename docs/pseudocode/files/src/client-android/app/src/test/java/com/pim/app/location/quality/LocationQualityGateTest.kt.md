# src/client-android/app/src/test/java/com/pim/app/location/quality/LocationQualityGateTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.location.quality
- 职责：单元测试 `LocationQualityGate`：水平精度门槛（\<50m）、缺海拔等待/超时接受。
- 主要依赖：`LocationQualityGate`、`QualityDecision`、`RawLocationFix`、JUnit
- 被谁使用：测试运行器

## 函数级结构化伪代码

### LocationQualityGateTest
#### 类字段 gate
- 输入：无
- 输出：maxAccuracy 50f exclusive、altitudeWait 15s 的 gate
- 副作用：无
- 步骤：1. 固定阈值构造
- 分支与异常：无
- 调用：`LocationQualityGate` 构造

#### missingHorizontalAccuracyIsDropped
- 输入：无
- 输出：断言
- 副作用：无
- 步骤：evaluate 精度 null → Drop，reason `missing-horizontal-accuracy`
- 分支与异常：无
- 调用：`gate.evaluate`、`fix`

#### accuracyBelowFiftyMetersIsAccepted
- 步骤：49.9m + 海拔 → AcceptNow；保留海拔与 acceptedAt；无 timeout flag
- 调用：`gate.evaluate`

#### accuracyAtFiftyMetersIsDropped / accuracyAboveFiftyMetersIsDropped / nonFiniteAccuracyIsDropped
- 步骤：50.0 / 80.0 / NaN → Drop，`horizontal-accuracy-too-low`
- 调用：`gate.evaluate`

#### missingAltitudeWaitsUntilDeadline
- 步骤：有精度无海拔 → WaitForAltitude，deadline=recorded+15s；timeoutDecision 在 deadline 前仍 Wait
- 调用：`evaluate`、`timeoutDecision`

#### missingAltitudeTimeoutAcceptsNullAltitudeWithQualityFlag
- 步骤：到 deadline → AcceptNow，海拔 null，flags 含 `altitude-missing-timeout`
- 调用：`evaluate`、`timeoutDecision`

#### fix(helper)
- 输入：精度、海拔、时间
- 输出：固定上海坐标的 `RawLocationFix`
- 步骤：填充 policyMode/motionSignal 等默认
- 调用：`RawLocationFix` 构造

## 近逐行中文伪代码

1. [L9-14] 构造 gate（50m 排他、15s 海拔等待）
2. [L15-24] 缺水平精度 → Drop
3. [L26-39] \<50m 接受
4. [L41-65] =50 / \>50 / NaN 丢弃
5. [L67-80] 缺海拔等待
6. [L82-98] 海拔超时接受并打 flag
7. [L100-114] `fix` 测试夹具

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/quality/LocationQualityGateTest.kt",
      "label": "LocationQualityGateTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/quality/LocationQualityGateTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/quality/LocationQualityGateTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/location/quality/LocationQualityGateTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt",
      "type": "tests"
    }
  ]
}
```
