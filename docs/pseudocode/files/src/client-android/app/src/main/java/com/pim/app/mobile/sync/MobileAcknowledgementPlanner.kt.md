# src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：运行时组件 `MobileAcknowledgementItem`：移动端采集/同步链路中的策略或上报单元。
- 主要依赖：`src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### MobileAcknowledgementItem
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L8 声明 `MobileAcknowledgementItem`
- 分支与异常：无
- 调用：无

### MobileAcknowledgementPlan
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L13 声明 `MobileAcknowledgementPlan`
- 分支与异常：无
- 调用：无

### MobileTypedAcknowledgementPlan
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L20 声明 `MobileTypedAcknowledgementPlan`
- 分支与异常：无
- 调用：无

### MobileAcknowledgementPlanner
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L28 声明 `MobileAcknowledgementPlanner`
- 分支与异常：无
- 调用：无

### ambiguous
#### ambiguous(sentItems: Set<MobileAcknowledgementItem>)
- 输入：sentItems: Set<MobileAcknowledgementItem>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun ambiguous(sentItems: Set<MobileAcknowledgementItem>) = typedPlan(
- 分支与异常：无显著分支
- 调用：ambiguous、typedPlan

### explicitCountsMatch
#### explicitCountsMatch(response: MobileIngestResponse)
- 输入：response: MobileIngestResponse
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun explicitCountsMatch(response: MobileIngestResponse): Boolean {
  2. 执行：val accepted = response.itemResults.count { it.outcome == "accepted" }
  3. 执行：val skipped = response.itemResults.count { it.outcome == "skipped" }
  4. 执行：val rejected = response.itemResults.count { it.outcome == "rejected" }
  5. 执行：val failed = response.itemResults.count { it.outcome == "failed" }
  6. 返回 response.acceptedCount == accepted &&
  7. 执行：response.skippedCount == skipped &&
  8. 执行：response.rejectedCount == rejected &&
  9. 执行：response.failedCount == failed &&
  10. 执行：accepted + skipped + rejected + failed == response.itemResults.size
- 分支与异常：无显著分支
- 调用：explicitCountsMatch

### formatItemError
#### formatItemError(result: MobileIngestItemResult, fallback: String)
- 输入：result: MobileIngestItemResult, fallback: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 执行：private fun formatItemError(result: MobileIngestItemResult, fallback: String): String {
  2. 执行：val hasCode = result.code.isNotBlank()
  3. 执行：val hasMessage = result.message.isNotBlank()
  4. 返回 when {
  5. 分支臂：hasCode && hasMessage -> "${result.code}: ${result.message}"
  6. 分支臂：hasCode -> result.code
  7. 分支臂：hasMessage -> result.message
  8. when 默认 else
- 分支与异常：else -> fallback
- 调用：formatItemError、result.code.isNotBlank、result.message.isNotBlank

## 近逐行中文伪代码

1. [L8] 定义类 `MobileAcknowledgementItem`
2. [L9] 执行：val entityType: String,
3. [L10] 执行：val clientItemKey: String
4. [L13] 定义类 `MobileAcknowledgementPlan`
5. [L14] 执行：val confirmedKeys: Set<String>,
6. [L15] 执行：val retryKeys: Set<String>,
7. [L16] 执行：val deadLetterKeys: Set<String>,
8. [L17] 执行：val failureCode: String? = null
9. [L20] 定义类 `MobileTypedAcknowledgementPlan`
10. [L21] 执行：val confirmedItems: Set<MobileAcknowledgementItem> = emptySet(),
11. [L22] 执行：val retryItems: Set<MobileAcknowledgementItem> = emptySet(),
12. [L23] 执行：val deadLetterItems: Set<MobileAcknowledgementItem> = emptySet(),
13. [L24] 执行：val failureCode: String? = null,
14. [L25] 执行：val itemErrors: Map<MobileAcknowledgementItem, String> = emptyMap()
15. [L28] 单例 object `MobileAcknowledgementPlanner`
16. [L29] 执行：private const val LegacyEntityType = "usage-event"
17. [L30] 执行：private val KnownEntityTypes = setOf("app-metadata", "usage-event", "usage-summary")
18. [L33] 执行：fun plan(
19. [L34] 执行：sentKeys: Set<String>,
20. [L35] 执行：response: MobileIngestResponse
21. [L36] 执行：): MobileAcknowledgementPlan {
22. [L37] 执行：val typedPlan = planTyped(
23. [L38] 执行：sentItems = sentKeys.mapTo(linkedSetOf()) {
24. [L39] 执行：MobileAcknowledgementItem(LegacyEntityType, it)
25. [L41] 执行：response = response
26. [L43] 返回 MobileAcknowledgementPlan(
27. [L44] 执行：confirmedKeys = typedPlan.confirmedItems.mapTo(linkedSetOf()) { it.clientItemKey },
28. [L45] 执行：retryKeys = typedPlan.retryItems.mapTo(linkedSetOf()) { it.clientItemKey },
29. [L46] 执行：deadLetterKeys = typedPlan.deadLetterItems.mapTo(linkedSetOf()) { it.clientItemKey },
30. [L47] 执行：failureCode = typedPlan.failureCode
31. [L51] 执行：fun planTyped(
32. [L52] 执行：sentItems: Set<MobileAcknowledgementItem>,
33. [L53] 执行：response: MobileIngestResponse
34. [L54] 执行：): MobileTypedAcknowledgementPlan {
35. [L55] 若 (sentItems.any { it.entityType !in KnownEntityTypes || it.clientItemKey.isBlank() }) 则
36. [L56] 返回 ambiguous(sentItems)
37. [L59] 若 (response.itemResults.isEmpty()) 则
38. [L60] 执行：val aggregateIsComplete = response.acceptedCount >= 0 &&
39. [L61] 执行：response.skippedCount >= 0 &&
40. [L62] 执行：response.acceptedCount + response.skippedCount == sentItems.size &&
41. [L63] 执行：response.rejectedCount == 0 &&
42. [L64] 执行：response.failedCount == 0
43. [L66] 返回 if (aggregateIsComplete) {
44. [L67] 执行：typedPlan(confirmedItems = sentItems)
45. [L69] 执行：ambiguous(sentItems)
46. [L73] 若 (!explicitCountsMatch(response)) 则
47. [L74] 返回 ambiguous(sentItems)
48. [L77] 分支臂：val typedResults = response.itemResults.map { result ->
49. [L78] 执行：MobileAcknowledgementItem(result.entityType, result.clientItemKey) to result
50. [L80] 分支臂：val hasUnexpectedResult = typedResults.any { (item, _) ->
51. [L81] 执行：item.entityType !in KnownEntityTypes ||
52. [L82] 执行：item.clientItemKey.isBlank() ||
53. [L83] 执行：item !in sentItems
54. [L85] 若 (hasUnexpectedResult) 则
55. [L86] 返回 ambiguous(sentItems)
56. [L89] 执行：val resultsByItem = typedResults.groupBy(
57. [L90] 分支臂：keySelector = { (item, _) -> item },
58. [L91] 分支臂：valueTransform = { (_, result) -> result }
59. [L93] 执行：val confirmed = linkedSetOf<MobileAcknowledgementItem>()
60. [L94] 执行：val retry = linkedSetOf<MobileAcknowledgementItem>()
61. [L95] 执行：val deadLetter = linkedSetOf<MobileAcknowledgementItem>()
62. [L96] 执行：val itemErrors = linkedMapOf<MobileAcknowledgementItem, String>()
63. [L97] 赋值 `hasAmbiguity` = false
64. [L99] 循环 for (item in sentItems)
65. [L100] 执行：val itemResults = resultsByItem[item]
66. [L101] 若 (itemResults?.size != 1) 则
67. [L102] 执行：retry += item
68. [L103] 执行：itemErrors[item] = "server-ack-ambiguous"
69. [L104] 执行：hasAmbiguity = true
70. [L108] 执行：val result = itemResults.single()
71. [L109] when 分支匹配
72. [L110] 分支臂："accepted", "skipped" -> confirmed += item
73. [L111] 分支臂："rejected" -> {
74. [L112] 执行：deadLetter += item
75. [L113] 执行：itemErrors[item] = formatItemError(result, "server-rejected")
76. [L115] 分支臂："failed" -> {
77. [L116] 执行：retry += item
78. [L117] 执行：itemErrors[item] = formatItemError(result, "server-retry")
79. [L119] when 默认 else
80. [L120] 执行：retry += item
81. [L121] 执行：itemErrors[item] = "server-ack-ambiguous"
82. [L122] 执行：hasAmbiguity = true
83. [L127] 返回 typedPlan(
84. [L128] 执行：confirmedItems = confirmed,
85. [L129] 执行：retryItems = retry,
86. [L130] 执行：deadLetterItems = deadLetter,
87. [L131] 执行：failureCode = if (hasAmbiguity) "server-ack-ambiguous" else null,
88. [L132] 执行：itemErrors = itemErrors
89. [L136] 执行：private fun ambiguous(sentItems: Set<MobileAcknowledgementItem>) = typedPlan(
90. [L137] 执行：retryItems = sentItems,
91. [L138] 执行：failureCode = "server-ack-ambiguous",
92. [L139] 执行：itemErrors = sentItems.associateWith { "server-ack-ambiguous" }
93. [L142] 执行：private fun explicitCountsMatch(response: MobileIngestResponse): Boolean {
94. [L143] 执行：val accepted = response.itemResults.count { it.outcome == "accepted" }
95. [L144] 执行：val skipped = response.itemResults.count { it.outcome == "skipped" }
96. [L145] 执行：val rejected = response.itemResults.count { it.outcome == "rejected" }
97. [L146] 执行：val failed = response.itemResults.count { it.outcome == "failed" }
98. [L147] 返回 response.acceptedCount == accepted &&
99. [L148] 执行：response.skippedCount == skipped &&
100. [L149] 执行：response.rejectedCount == rejected &&
101. [L150] 执行：response.failedCount == failed &&
102. [L151] 执行：accepted + skipped + rejected + failed == response.itemResults.size
103. [L154] 执行：private fun typedPlan(
104. [L155] 执行：confirmedItems: Set<MobileAcknowledgementItem> = emptySet(),
105. [L156] 执行：retryItems: Set<MobileAcknowledgementItem> = emptySet(),
106. [L157] 执行：deadLetterItems: Set<MobileAcknowledgementItem> = emptySet(),
107. [L158] 执行：failureCode: String? = null,
108. [L159] 执行：itemErrors: Map<MobileAcknowledgementItem, String> = emptyMap()
109. [L160] 执行：) = MobileTypedAcknowledgementPlan(
110. [L161] 执行：confirmedItems = confirmedItems,
111. [L162] 执行：retryItems = retryItems,
112. [L163] 执行：deadLetterItems = deadLetterItems,
113. [L164] 执行：failureCode = failureCode,
114. [L165] 执行：itemErrors = itemErrors
115. [L168] 执行：private fun formatItemError(result: MobileIngestItemResult, fallback: String): String {
116. [L169] 执行：val hasCode = result.code.isNotBlank()
117. [L170] 执行：val hasMessage = result.message.isNotBlank()
118. [L171] 返回 when {
119. [L172] 分支臂：hasCode && hasMessage -> "${result.code}: ${result.message}"
120. [L173] 分支臂：hasCode -> result.code
121. [L174] 分支臂：hasMessage -> result.message
122. [L175] when 默认 else
123. [L180] 挂起函数 `applyAcknowledgementPlan`
124. [L181] 执行：dao: MobileDataDao,
125. [L182] 执行：plan: MobileTypedAcknowledgementPlan
126. [L184] 若 (plan.confirmedItems.isNotEmpty()) 则
127. [L185] 执行：val eventIds = plan.confirmedItems
128. [L186] 执行：.filter { it.entityType == "usage-event" }
129. [L187] 执行：.mapNotNull { it.clientItemKey.toLongOrNull() }
130. [L188] 若 (eventIds.isNotEmpty()) dao.deleteUsageEventByIds(eventIds) 则
131. [L190] 执行：val summaryIds = plan.confirmedItems
132. [L191] 执行：.filter { it.entityType == "usage-summary" }
133. [L192] 执行：.mapNotNull { it.clientItemKey.toLongOrNull() }
134. [L193] 若 (summaryIds.isNotEmpty()) dao.deleteUsageSummaryByIds(summaryIds) 则
135. [L195] 执行：val pkgNames = plan.confirmedItems
136. [L196] 执行：.filter { it.entityType == "app-metadata" }
137. [L197] 执行：.map { it.clientItemKey.substringBeforeLast("@") }
138. [L198] 若 (pkgNames.isNotEmpty()) dao.deleteAppMetadataByPackageNames(pkgNames) 则
139. [L201] 分支臂：plan.deadLetterItems.groupBy { it.entityType }.forEach { (entityType, items) ->
140. [L202] 分支臂：items.groupBy { plan.itemErrors[it] ?: "server-rejected" }.forEach { (error, errorItems) ->
141. [L203] when 分支匹配
142. [L204] 分支臂："usage-event" -> {
143. [L205] 执行：val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
144. [L206] 若 (ids.isNotEmpty()) dao.updateUsageEventSyncStatus(ids, MobileSyncStatus.REJECTED, error) 则
145. [L208] 分支臂："usage-summary" -> {
146. [L209] 执行：val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
147. [L210] 若 (ids.isNotEmpty()) dao.updateUsageSummarySyncStatus(ids, MobileSyncStatus.REJECTED, error) 则
148. [L212] 分支臂："app-metadata" -> {
149. [L213] 执行：val names = errorItems.map { it.clientItemKey.substringBeforeLast("@") }
150. [L214] 若 (names.isNotEmpty()) dao.updateAppMetadataSyncStatus(names, MobileSyncStatus.REJECTED, error) 则
151. [L220] 分支臂：plan.retryItems.groupBy { it.entityType }.forEach { (entityType, items) ->
152. [L221] 分支臂：items.groupBy { plan.itemErrors[it] ?: "server-retry" }.forEach { (error, errorItems) ->
153. [L222] when 分支匹配
154. [L223] 分支臂："usage-event" -> {
155. [L224] 执行：val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
156. [L225] 若 (ids.isNotEmpty()) dao.updateUsageEventSyncStatus(ids, MobileSyncStatus.PENDING, error) 则
157. [L227] 分支臂："usage-summary" -> {
158. [L228] 执行：val ids = errorItems.mapNotNull { it.clientItemKey.toLongOrNull() }
159. [L229] 若 (ids.isNotEmpty()) dao.updateUsageSummarySyncStatus(ids, MobileSyncStatus.PENDING, error) 则
160. [L231] 分支臂："app-metadata" -> {
161. [L232] 执行：val names = errorItems.map { it.clientItemKey.substringBeforeLast("@") }
162. [L233] 若 (names.isNotEmpty()) dao.updateAppMetadataSyncStatus(names, MobileSyncStatus.PENDING, error) 则
163. [L240] 挂起函数 `processUsageAcknowledgements`
164. [L241] 执行：dao: MobileDataDao,
165. [L242] 执行：sentItems: Set<MobileAcknowledgementItem>,
166. [L243] 执行：response: MobileIngestResponse
167. [L245] 执行：val plan = MobileAcknowledgementPlanner.planTyped(sentItems, response)
168. [L246] 执行：applyAcknowledgementPlan(dao, plan)

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt",
      "label": "MobileAcknowledgementItem",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileAcknowledgementPlanner.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "type": "depends_on"
    }
  ]
}
```
