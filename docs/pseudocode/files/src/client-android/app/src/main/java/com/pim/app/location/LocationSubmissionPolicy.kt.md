# src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：根据水平精度与是否已自动提交，决定手动/自动提交策略。
- 主要依赖：无
- 被谁使用：LocationCaptureRepository

## 函数级结构化伪代码

### LocationSubmissionDecision
- canSubmitManually、shouldAutoSubmit、statusLabel、reason

### LocationSubmissionPolicy.decide
- 无精度 → 不可提交
- <=10m → 可手动；若未自动过则可自动
- <50m → 仅手动
- 否则拒绝 >=50m

### decideLocationSubmission
- 转发 object decide

## 近逐行中文伪代码

1. 精度阈值 10m/50m 三档。
2. autoAlreadySubmitted 抑制重复自动提交。
3. 返回决策结构供 UI/采集层使用。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt",
      "label": "LocationSubmissionPolicy",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
