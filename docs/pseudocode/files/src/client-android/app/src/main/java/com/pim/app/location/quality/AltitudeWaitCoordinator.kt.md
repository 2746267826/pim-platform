# src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：协调器 `AltitudeWaitCoordinator`：编排多步骤同步或上传流程。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### AltitudeWaitCoordinator
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L5 声明 `AltitudeWaitCoordinator`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L5] 定义类 `AltitudeWaitCoordinator`
2. [L6] 执行：private val gate: LocationQualityGate = LocationQualityGate(),
3. [L7] 分支臂：private val nowMillis: () -> Long = { System.currentTimeMillis() },
4. [L8] 分支臂：private val delayMillis: suspend (Long) -> Unit = { delay(it) }
5. [L10] 执行：private var pendingAltitudeFix: PendingAltitudeFix? = null
6. [L12] 挂起函数 `handleFix`
7. [L13] 执行：fix: RawLocationFix,
8. [L14] 分支臂：onAccepted: suspend (QualityAcceptedLocation) -> Unit,
9. [L15] 分支臂：onDropped: suspend (RawLocationFix, String) -> Unit
10. [L17] when 分支匹配
11. [L18] 分支臂：is QualityDecision.AcceptNow -> {
12. [L19] 执行：pendingAltitudeFix = null
13. [L20] 执行：onAccepted(decision.accepted)
14. [L22] 分支臂：is QualityDecision.Drop -> onDropped(decision.fix, decision.reason)
15. [L23] 分支臂：is QualityDecision.WaitForAltitude -> {
16. [L24] 执行：pendingAltitudeFix = decision.pending
17. [L25] 执行：waitThenHandleTimeout(decision.pending, onAccepted, onDropped)
18. [L30] 执行：private suspend fun waitThenHandleTimeout(
19. [L31] 执行：pending: PendingAltitudeFix,
20. [L32] 分支臂：onAccepted: suspend (QualityAcceptedLocation) -> Unit,
21. [L33] 分支臂：onDropped: suspend (RawLocationFix, String) -> Unit
22. [L35] 执行：val remainingMillis = (pending.deadlineMillis - nowMillis()).coerceAtLeast(0L)
23. [L36] 若 (remainingMillis > 0L) 则
24. [L37] 执行：delayMillis(remainingMillis)
25. [L39] 执行：if (pendingAltitudeFix != pending) return
26. [L41] when 分支匹配
27. [L42] 分支臂：is QualityDecision.AcceptNow -> {
28. [L43] 执行：pendingAltitudeFix = null
29. [L44] 执行：onAccepted(decision.accepted)
30. [L46] 分支臂：is QualityDecision.Drop -> onDropped(decision.fix, decision.reason)
31. [L47] 分支臂：is QualityDecision.WaitForAltitude -> waitThenHandleTimeout(
32. [L48] 执行：decision.pending,
33. [L49] 执行：onAccepted,
34. [L50] 执行：onDropped

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt",
      "label": "AltitudeWaitCoordinator",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
