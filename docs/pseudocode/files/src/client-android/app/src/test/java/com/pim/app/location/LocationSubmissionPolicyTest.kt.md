# src/client-android/app/src/test/java/com/pim/app/location/LocationSubmissionPolicyTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.location
- 职责：验证 LocationSubmissionPolicy 人工/自动提交精度阈值（50m 边界）。
- 主要依赖：JUnit、LocationSubmissionPolicy
- 被谁使用：测试运行器

## 函数级结构化伪代码

### LocationSubmissionPolicyTest
#### manualSubmissionRejectsFiftyMeterAccuracy
- decide(50f, autoAlreadySubmitted=false)
- 断言 canSubmitManually=false、shouldAutoSubmit=false

#### manualSubmissionAcceptsAccuracyBelowFiftyMeters
- decide(49.9f, autoAlreadySubmitted=false)
- 断言 canSubmitManually=true、shouldAutoSubmit=false

## 近逐行中文伪代码

1. 定义测试类 LocationSubmissionPolicyTest。
2. 精度 50m：拒绝手动提交，不自动提交。
3. 精度 49.9m：允许手动提交，仍不自动提交。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/LocationSubmissionPolicyTest.kt",
      "label": "LocationSubmissionPolicyTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/LocationSubmissionPolicyTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/LocationSubmissionPolicyTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/LocationSubmissionPolicyTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt", "type": "tests" }
  ]
}
```
