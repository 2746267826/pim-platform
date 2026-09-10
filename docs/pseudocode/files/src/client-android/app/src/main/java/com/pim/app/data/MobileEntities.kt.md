# src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：实体定义 `MobileSyncStatus`：Room/本地持久化模型。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### MobileSyncStatus
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L12 声明 `MobileSyncStatus`
- 分支与异常：无
- 调用：无

### MobileUsageEventEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L27 声明 `MobileUsageEventEntity`
- 分支与异常：无
- 调用：无

### MobileUsageSummaryEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L52 声明 `MobileUsageSummaryEntity`
- 分支与异常：无
- 调用：无

### MobileAppMetadataEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L76 声明 `MobileAppMetadataEntity`
- 分支与异常：无
- 调用：无

### MobileLocationPointEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L103 声明 `MobileLocationPointEntity`
- 分支与异常：无
- 调用：无

### MobileLocationDroppedDiagnosticEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L154 声明 `MobileLocationDroppedDiagnosticEntity`
- 分支与异常：无
- 调用：无

### MobileLocationPolicyTransitionEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L185 声明 `MobileLocationPolicyTransitionEntity`
- 分支与异常：无
- 调用：无

### MobileSyncBatchEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L215 声明 `MobileSyncBatchEntity`
- 分支与异常：无
- 调用：无

### MobileLogEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L237 声明 `MobileLogEntity`
- 分支与异常：无
- 调用：无

### MobileDeviceProfileEntity
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L257 声明 `MobileDeviceProfileEntity`
- 分支与异常：无
- 调用：无

### fromAccepted
#### fromAccepted(accepted: QualityAcceptedLocation, rawJson: String)
- 输入：accepted: QualityAcceptedLocation, rawJson: String
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `fromAccepted` 参数：accepted: QualityAcceptedLocation, rawJson: String
  2. 返回 MobileLocationPointEntity(
  3. 执行：latitude = accepted.fix.latitude,
  4. 执行：longitude = accepted.fix.longitude,
  5. 执行：altitudeMeters = accepted.altitudeMeters,
  6. 执行：accuracyMeters = accepted.fix.horizontalAccuracyMeters,
  7. 执行：speedMetersPerSecond = accepted.fix.speedMetersPerSecond,
  8. 执行：bearingDegrees = accepted.fix.bearingDegrees,
  9. 执行：provider = accepted.fix.provider,
  10. 执行：recordedAtUtc = accepted.fix.recordedAtMillis,
  11. 执行：source = "auto",
  12. 执行：collectedAtUtc = accepted.acceptedAtMillis,
  13. 执行：rawJson = rawJson,
  14. 执行：submittedAtUtc = accepted.acceptedAtMillis,
  15. 执行：policyMode = accepted.fix.policyMode,
  16. 执行：scheduleLowFrequency = accepted.fix.scheduleLowFrequency,
  17. 执行：motionState = accepted.fix.motionSignal,
  18. 执行：qualityFlags = accepted.qualityFlags.toJsonArrayString()
- 分支与异常：无显著分支
- 调用：fromAccepted、MobileLocationPointEntity、accepted.qualityFlags.toJsonArrayString

## 近逐行中文伪代码

1. [L12] 单例 object `MobileSyncStatus`
2. [L13] 执行：const val PENDING = "pending"
3. [L14] 执行：const val SYNCING = "syncing"
4. [L15] 执行：const val SYNCED = "synced"
5. [L16] 执行：const val FAILED = "failed"
6. [L17] 执行：const val REJECTED = "rejected"
7. [L20] 注解 @Entity
8. [L21] 执行：tableName = "mobile_usage_events",
9. [L22] 执行：indices = [
10. [L23] 执行：Index(value = ["package_name", "event_time_utc"]),
11. [L24] 执行：Index(value = ["sync_status"])
12. [L27] 定义类 `MobileUsageEventEntity`
13. [L28] 注解 @PrimaryKey
14. [L29] 注解 @ColumnInfo
15. [L30] 注解 @ColumnInfo
16. [L31] 注解 @ColumnInfo
17. [L32] 注解 @ColumnInfo
18. [L33] 注解 @ColumnInfo
19. [L34] 注解 @ColumnInfo
20. [L35] 注解 @ColumnInfo
21. [L36] 注解 @ColumnInfo
22. [L37] 注解 @ColumnInfo
23. [L38] 注解 @ColumnInfo
24. [L39] 注解 @ColumnInfo
25. [L40] 注解 @ColumnInfo
26. [L41] 注解 @ColumnInfo
27. [L42] 注解 @ColumnInfo
28. [L45] 注解 @Entity
29. [L46] 执行：tableName = "mobile_usage_summaries",
30. [L47] 执行：indices = [
31. [L48] 执行：Index(value = ["package_name", "window_start_utc", "window_end_utc"]),
32. [L49] 执行：Index(value = ["sync_status"])
33. [L52] 定义类 `MobileUsageSummaryEntity`
34. [L53] 注解 @PrimaryKey
35. [L54] 注解 @ColumnInfo
36. [L55] 注解 @ColumnInfo
37. [L56] 注解 @ColumnInfo
38. [L57] 注解 @ColumnInfo
39. [L58] 注解 @ColumnInfo
40. [L59] 注解 @ColumnInfo
41. [L60] 注解 @ColumnInfo
42. [L61] 注解 @ColumnInfo
43. [L62] 注解 @ColumnInfo
44. [L63] 注解 @ColumnInfo
45. [L64] 注解 @ColumnInfo
46. [L65] 注解 @ColumnInfo
47. [L66] 注解 @ColumnInfo
48. [L67] 注解 @ColumnInfo
49. [L68] 注解 @ColumnInfo
50. [L69] 注解 @ColumnInfo
51. [L72] 注解 @Entity
52. [L73] 执行：tableName = "mobile_app_metadata",
53. [L74] 执行：indices = [Index(value = ["sync_status"])]
54. [L76] 定义类 `MobileAppMetadataEntity`
55. [L77] 注解 @PrimaryKey
56. [L78] 注解 @ColumnInfo
57. [L79] 执行：val packageName: String,
58. [L80] 注解 @ColumnInfo
59. [L81] 注解 @ColumnInfo
60. [L82] 注解 @ColumnInfo
61. [L83] 注解 @ColumnInfo
62. [L84] 注解 @ColumnInfo
63. [L85] 注解 @ColumnInfo
64. [L86] 注解 @ColumnInfo
65. [L87] 注解 @ColumnInfo
66. [L88] 注解 @ColumnInfo
67. [L89] 注解 @ColumnInfo
68. [L90] 注解 @ColumnInfo
69. [L91] 注解 @ColumnInfo
70. [L92] 注解 @ColumnInfo
71. [L93] 注解 @ColumnInfo
72. [L96] 注解 @Entity
73. [L97] 执行：tableName = "mobile_location_points",
74. [L98] 执行：indices = [
75. [L99] 执行：Index(value = ["recorded_at_utc"]),
76. [L100] 执行：Index(value = ["sync_status"])
77. [L103] 定义类 `MobileLocationPointEntity`
78. [L104] 注解 @PrimaryKey
79. [L105] 注解 @ColumnInfo
80. [L106] 注解 @ColumnInfo
81. [L107] 注解 @ColumnInfo
82. [L108] 注解 @ColumnInfo
83. [L109] 注解 @ColumnInfo
84. [L110] 注解 @ColumnInfo
85. [L111] 注解 @ColumnInfo
86. [L112] 注解 @ColumnInfo
87. [L113] 注解 @ColumnInfo
88. [L114] 注解 @ColumnInfo
89. [L115] 注解 @ColumnInfo
90. [L116] 注解 @ColumnInfo
91. [L117] 注解 @ColumnInfo
92. [L118] 注解 @ColumnInfo
93. [L119] 注解 @ColumnInfo
94. [L120] 注解 @ColumnInfo
95. [L121] 注解 @ColumnInfo
96. [L122] 注解 @ColumnInfo
97. [L123] 注解 @ColumnInfo
98. [L124] 注解 @ColumnInfo
99. [L126] 执行：companion object {
100. [L127] 函数 `fromAccepted` 参数：accepted: QualityAcceptedLocation, rawJson: String
101. [L128] 返回 MobileLocationPointEntity(
102. [L129] 执行：latitude = accepted.fix.latitude,
103. [L130] 执行：longitude = accepted.fix.longitude,
104. [L131] 执行：altitudeMeters = accepted.altitudeMeters,
105. [L132] 执行：accuracyMeters = accepted.fix.horizontalAccuracyMeters,
106. [L133] 执行：speedMetersPerSecond = accepted.fix.speedMetersPerSecond,
107. [L134] 执行：bearingDegrees = accepted.fix.bearingDegrees,
108. [L135] 执行：provider = accepted.fix.provider,
109. [L136] 执行：recordedAtUtc = accepted.fix.recordedAtMillis,
110. [L137] 执行：source = "auto",
111. [L138] 执行：collectedAtUtc = accepted.acceptedAtMillis,
112. [L139] 执行：rawJson = rawJson,
113. [L140] 执行：submittedAtUtc = accepted.acceptedAtMillis,
114. [L141] 执行：policyMode = accepted.fix.policyMode,
115. [L142] 执行：scheduleLowFrequency = accepted.fix.scheduleLowFrequency,
116. [L143] 执行：motionState = accepted.fix.motionSignal,
117. [L144] 执行：qualityFlags = accepted.qualityFlags.toJsonArrayString()
118. [L150] 注解 @Entity
119. [L151] 执行：tableName = "mobile_location_dropped_diagnostics",
120. [L152] 执行：indices = [Index(value = ["recorded_at_utc"])]
121. [L154] 定义类 `MobileLocationDroppedDiagnosticEntity`
122. [L155] 注解 @PrimaryKey
123. [L156] 注解 @ColumnInfo
124. [L157] 注解 @ColumnInfo
125. [L158] 注解 @ColumnInfo
126. [L159] 注解 @ColumnInfo
127. [L160] 注解 @ColumnInfo
128. [L161] 注解 @ColumnInfo
129. [L163] 执行：companion object {
130. [L164] 执行：fun fromDropped(
131. [L165] 执行：fix: RawLocationFix,
132. [L166] 执行：reason: String,
133. [L167] 执行：createdAtUtc: Long = System.currentTimeMillis()
134. [L168] 执行：): MobileLocationDroppedDiagnosticEntity {
135. [L169] 返回 MobileLocationDroppedDiagnosticEntity(
136. [L170] 执行：recordedAtUtc = fix.recordedAtMillis,
137. [L171] 执行：provider = fix.provider,
138. [L172] 执行：accuracyMeters = fix.horizontalAccuracyMeters,
139. [L173] 执行：policyMode = fix.policyMode,
140. [L174] 执行：reason = reason,
141. [L175] 执行：createdAtUtc = createdAtUtc
142. [L181] 注解 @Entity
143. [L182] 执行：tableName = "mobile_location_policy_transitions",
144. [L183] 执行：indices = [Index(value = ["occurred_at_utc"])]
145. [L185] 定义类 `MobileLocationPolicyTransitionEntity`
146. [L186] 注解 @PrimaryKey
147. [L187] 注解 @ColumnInfo
148. [L188] 注解 @ColumnInfo
149. [L189] 注解 @ColumnInfo
150. [L190] 注解 @ColumnInfo
151. [L192] 执行：companion object {
152. [L193] 执行：fun fromDecision(
153. [L194] 执行：fromMode: LocationPolicyMode?,
154. [L195] 执行：decision: PolicyDecision,
155. [L196] 执行：occurredAtUtc: Long
156. [L197] 执行：): MobileLocationPolicyTransitionEntity {
157. [L198] 返回 MobileLocationPolicyTransitionEntity(
158. [L199] 执行：fromMode = fromMode?.name,
159. [L200] 执行：toMode = decision.mode.name,
160. [L201] 执行：reason = decision.reason,
161. [L202] 执行：occurredAtUtc = occurredAtUtc
162. [L208] 注解 @Entity
163. [L209] 执行：tableName = "mobile_sync_batches",
164. [L210] 执行：indices = [
165. [L211] 执行：Index(value = ["batch_id"], unique = true),
166. [L212] 执行：Index(value = ["sync_status"])
167. [L215] 定义类 `MobileSyncBatchEntity`
168. [L216] 注解 @PrimaryKey
169. [L217] 注解 @ColumnInfo
170. [L218] 注解 @ColumnInfo
171. [L219] 注解 @ColumnInfo
172. [L220] 注解 @ColumnInfo
173. [L221] 注解 @ColumnInfo
174. [L222] 注解 @ColumnInfo
175. [L223] 注解 @ColumnInfo
176. [L224] 注解 @ColumnInfo
177. [L225] 注解 @ColumnInfo
178. [L226] 注解 @ColumnInfo
179. [L227] 注解 @ColumnInfo
180. [L230] 注解 @Entity
181. [L231] 执行：tableName = "mobile_logs",
182. [L232] 执行：indices = [
183. [L233] 执行：Index(value = ["occurred_at_utc"]),
184. [L234] 执行：Index(value = ["sync_status"])
185. [L237] 定义类 `MobileLogEntity`
186. [L238] 注解 @PrimaryKey
187. [L239] 注解 @ColumnInfo
188. [L240] 注解 @ColumnInfo
189. [L241] 注解 @ColumnInfo
190. [L242] 注解 @ColumnInfo
191. [L243] 注解 @ColumnInfo
192. [L244] 注解 @ColumnInfo
193. [L245] 注解 @ColumnInfo
194. [L246] 注解 @ColumnInfo
195. [L247] 注解 @ColumnInfo
196. [L248] 注解 @ColumnInfo
197. [L249] 注解 @ColumnInfo
198. [L250] 注解 @ColumnInfo
199. [L253] 注解 @Entity
200. [L254] 执行：tableName = "mobile_device_profile",
201. [L255] 执行：indices = [Index(value = ["sync_status"])]
202. [L257] 定义类 `MobileDeviceProfileEntity`
203. [L258] 注解 @PrimaryKey
204. [L259] 注解 @ColumnInfo
205. [L260] 执行：val profileId: String = "default",
206. [L261] 注解 @ColumnInfo
207. [L262] 注解 @ColumnInfo
208. [L263] 注解 @ColumnInfo
209. [L264] 注解 @ColumnInfo
210. [L265] 注解 @ColumnInfo
211. [L266] 注解 @ColumnInfo
212. [L267] 注解 @ColumnInfo
213. [L268] 注解 @ColumnInfo
214. [L269] 注解 @ColumnInfo
215. [L270] 注解 @ColumnInfo
216. [L271] 注解 @ColumnInfo
217. [L272] 注解 @ColumnInfo
218. [L273] 注解 @ColumnInfo
219. [L274] 注解 @ColumnInfo
220. [L275] 注解 @ColumnInfo
221. [L278] 执行：private fun Set<String>.toJsonArrayString(): String {
222. [L279] 返回 sorted().joinToString(prefix = "[", postfix = "]") { flag ->
223. [L280] 执行："\"${flag.replace("\\", "\\\\").replace("\"", "\\\"")}\""

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt",
      "label": "MobileSyncStatus",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt.md",
      "layer": "client-android",
      "kind": "entity"
    }
  ],
  "edges": []
}
```
