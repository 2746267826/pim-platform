# src/client-web/src/types/index.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Web 前端共享类型与 DTO 聚合：API 响应、领域模型、页面状态相关接口与类型别名。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### 导出类型集合
#### 共 147 个 type/interface/class
- 输入：无运行时输入（类型声明）
- 输出：类型符号
- 副作用：无
- 步骤：
  1. 声明 `ApiResponse`（约 L1）
  2. 声明 `AuthResponse`（约 L8）
  3. 声明 `CalendarResponse`（约 L15）
  4. 声明 `EventResponse`（约 L23）
  5. 声明 `TaskResponse`（约 L46）
  6. 声明 `TaskPlanningState`（约 L63）
  7. 声明 `CalendarLayerId`（约 L75）
  8. 声明 `EndpointPlatform`（约 L77）
  9. 声明 `NotificationActionResult`（约 L79）
  10. 声明 `WorkbenchDensityMode`（约 L81）
  11. 声明 `DomainProject`（约 L83）
  12. 声明 `CreateDomainProjectRequest`（约 L90）
  13. 声明 `TaskBook`（约 L96）
  14. 声明 `CreateTaskBookRequest`（约 L104）
  15. 声明 `TaskChecklistItem`（约 L111）
  16. 声明 `AddTaskChecklistItemRequest`（约 L119）
  17. 声明 `HabitRoutine`（约 L124）
  18. 声明 `CreateHabitRequest`（约 L132）
  19. 声明 `ReminderSummary`（约 L141）
  20. 声明 `ReminderDelivery`（约 L159）
  21. 声明 `ReminderActionResponse`（约 L169）
  22. 声明 `CreateReminderRequest`（约 L175）
  23. 声明 `GenerateReportRequest`（约 L188）
  24. 声明 `ReportArtifact`（约 L194）
  25. 声明 `ReportSuggestion`（约 L208）
  26. 声明 `SyncConflict`（约 L217）
  27. 声明 `AuditVersion`（约 L229）
  28. 声明 `AuditTimelineResponse`（约 L243）
  29. 声明 `AuditExportResponse`（约 L247）
  30. 声明 `RestorePreviewResponse`（约 L253）
  31. 声明 `DataCenterObjectRef`（约 L261）
  32. 声明 `DataCenterBatchOperationRequest`（约 L266）
  33. 声明 `DataCenterBatchPreviewResponse`（约 L272）
  34. 声明 `DataCenterBatchExecutionResponse`（约 L280）
  35. 声明 `EndpointStatus`（约 L286）
  36. 声明 `EndpointHeartbeatRequest`（约 L295）
  37. 声明 `EndpointCollectionQuality`（约 L302）
  38. 声明 `EndpointNotificationActionRequest`（约 L310）
  39. 声明 `EndpointNotificationActionResponse`（约 L318）
  40. 声明 `CreateTaskExecutionSegmentRequest`（约 L324）
  41. 声明 `TaskExecutionSegmentResponse`（约 L332）
  42. 声明 `CalendarLayerQueryRequest`（约 L344）
  43. 声明 `CalendarLayerItem`（约 L351）
  44. 声明 `CalendarLayerResponse`（约 L365）
  45. 声明 `DataCenterQueryRequest`（约 L371）
  46. 声明 `DataCenterItem`（约 L380）
  47. 声明 `DataCenterQueryResponse`（约 L391）
  48. 声明 `OutlookSettingsResponse`（约 L398）
  49. 声明 `UpdateOutlookSettingsRequest`（约 L413）
  50. 声明 `OutlookDeviceCodeRequestResponse`（约 L419）
  51. 声明 `OutlookSyncStep`（约 L428）
  52. 声明 `OutlookSyncBatchResponse`（约 L435）
  53. 声明 `OperationRiskLevel`（约 L451）
  54. 声明 `OperationConfirmationStatus`（约 L461）
  55. 声明 `OperationConfirmation`（约 L468）
  56. 声明 `PagedResult`（约 L498）
  57. 声明 `ImportResult`（约 L506）
  58. 声明 `CalendarOperationSample`（约 L511）
  59. 声明 `CalendarDeletePreviewResponse`（约 L520）
  60. 声明 `CalendarOperationResult`（约 L531）
  61. 声明 `CalendarRestoreConflict`（约 L540）
  62. 声明 `CalendarRestorePreviewResponse`（约 L549）
  63. 声明 `CalendarRecycleBinItem`（约 L559）
  64. 声明 `ImportSkippedItem`（约 L572）
  65. 声明 `ImportReport`（约 L579）
  66. 声明 `PimHealthStatus`（约 L586）
  67. 声明 `SystemStatusSummary`（约 L588）
  68. 声明 `StatusComponent`（约 L595）
  69. 声明 `SystemStatusDetail`（约 L605）
  70. 声明 `PcQualityComponent`（约 L611）
  71. 声明 `PcQualityIssue`（约 L619）
  72. 声明 `PcQualityResponse`（约 L627）
  73. 声明 `PcQualityQueryParams`（约 L637）
  74. 声明 `PcSummaryResponse`（约 L644）
  75. 声明 `KeystatsSummary`（约 L654）
  76. 声明 `KeyCountItem`（约 L671）
  77. 声明 `HeatmapBucket`（约 L677）
  78. 声明 `AppRankingItem`（约 L686）
  79. 声明 `TimelineItem`（约 L695）
  80. 声明 `WorkSessionItem`（约 L709）
  81. … 其余 67 个类型见近逐行
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L1] 导出类型 `ApiResponse`
2. [L2] 执行：code: number;
3. [L3] 执行：message: string;
4. [L5] 执行：timestamp: string;
5. [L8] 导出类型 `AuthResponse`
6. [L9] 执行：accessToken: string;
7. [L10] 执行：refreshToken: string;
8. [L11] 执行：expiresAt: string;
9. [L12] 执行：userInfo: { id: string; username: string; displayName: string };
10. [L15] 导出类型 `CalendarResponse`
11. [L16] 执行：id: string;
12. [L17] 执行：name: string;
13. [L18] 执行：color: string;
14. [L19] 执行：kind: string;
15. [L20] 执行：isDefault: boolean;
16. [L23] 导出类型 `EventResponse`
17. [L24] 执行：id: string;
18. [L25] 执行：calendarId: string;
19. [L26] 执行：uid: string;
20. [L27] 执行：title: string;
21. [L28] 执行：description?: string;
22. [L29] 执行：location?: string;
23. [L30] 执行：dtStart: string;
24. [L31] 执行：dtEnd: string;
25. [L32] 执行：rrule?: string;
26. [L33] 执行：status: string;
27. [L34] 执行：source: string;
28. [L35] 执行：originalEventId?: string;
29. [L36] 执行：isAllDay?: boolean;
30. [L37] 执行：timeZoneId?: string;
31. [L38] 执行：sourceTimeZoneId?: string;
32. [L39] 执行：sourceUid?: string;
33. [L40] 执行：externalMetadataJson?: string;
34. [L41] 执行：recurrenceId?: string;
35. [L42] 执行：exDatesJson?: string;
36. [L43] 执行：recurrenceMetadataJson?: string;
37. [L46] 导出类型 `TaskResponse`
38. [L47] 执行：id: string;
39. [L48] 执行：calendarId?: string;
40. [L49] 执行：title: string;
41. [L50] 执行：description?: string;
42. [L51] 执行：priority: number;
43. [L52] 执行：estimatedDuration?: string;
44. [L53] 执行：minimumSegment?: string;
45. [L54] 执行：dtStart?: string;
46. [L55] 执行：due?: string;
47. [L56] 执行：plannedEnd?: string;
48. [L57] 执行：status: string;
49. [L58] 执行：isInbox: boolean;
50. [L59] 执行：sortOrder?: number;
51. [L60] 执行：subTasks?: TaskResponse[];
52. [L63] 导出类型 `TaskPlanningState`
53. [L64] 执行：| 'Inbox'
54. [L65] 执行：| 'ToPlan'
55. [L66] 执行：| 'Planned'
56. [L67] 执行：| 'InProgress'
57. [L68] 执行：| 'Waiting'
58. [L69] 执行：| 'Blocked'
59. [L70] 执行：| 'Deferred'
60. [L71] 执行：| 'Paused'
61. [L72] 执行：| 'Completed'
62. [L73] 执行：| 'Cancelled';
63. [L75] 导出类型 `CalendarLayerId`
64. [L77] 导出类型 `EndpointPlatform`
65. [L79] 导出类型 `NotificationActionResult`
66. [L81] 导出类型 `WorkbenchDensityMode`
67. [L83] 导出类型 `DomainProject`
68. [L84] 执行：id: string;
69. [L85] 执行：name: string;
70. [L86] 执行：description?: string | null;
71. [L87] 执行：status: string;
72. [L90] 导出类型 `CreateDomainProjectRequest`
73. [L91] 执行：name: string;
74. [L92] 执行：description?: string | null;
75. [L93] 执行：status?: string | null;
76. [L96] 导出类型 `TaskBook`
77. [L97] 执行：id: string;
78. [L98] 执行：domainProjectId?: string | null;
79. [L99] 执行：name: string;
80. [L100] 执行：kind: string;
81. [L101] 执行：status: string;
82. [L104] 导出类型 `CreateTaskBookRequest`
83. [L105] 执行：domainProjectId?: string | null;
84. [L106] 执行：name: string;
85. [L107] 执行：kind?: string | null;
86. [L108] 执行：status?: string | null;
87. [L111] 导出类型 `TaskChecklistItem`
88. [L112] 执行：id: string;
89. [L113] 执行：taskId: string;
90. [L114] 执行：title: string;
91. [L115] 执行：isDone: boolean;
92. [L116] 执行：sortOrder: number;
93. [L119] 导出类型 `AddTaskChecklistItemRequest`
94. [L120] 执行：title: string;
95. [L121] 执行：sortOrder?: number | null;
96. [L124] 导出类型 `HabitRoutine`
97. [L125] 执行：id: string;
98. [L126] 执行：title: string;
99. [L127] 执行：cadence: 'Daily' | 'Weekly' | 'Monthly' | string;
100. [L128] 执行：source: string;
101. [L129] 执行：status: string;
102. [L132] 导出类型 `CreateHabitRequest`
103. [L133] 执行：title: string;
104. [L134] 执行：description?: string | null;
105. [L135] 执行：cadence?: string | null;
106. [L136] 执行：source?: string | null;
107. [L137] 执行：status?: string | null;
108. [L138] 执行：ruleJson?: string | null;
109. [L141] 导出类型 `ReminderSummary`
110. [L142] 执行：id: string;
111. [L143] 执行：relatedObjectType?: string;
112. [L144] 执行：relatedObjectId?: string;
113. [L145] 执行：title: string;
114. [L146] 执行：body?: string;
115. [L147] 执行：triggerReason?: string;
116. [L148] 执行：riskLevel: OperationRiskLevel;
117. [L149] 执行：channels: string[];
118. [L150] 执行：doNotDisturbStart?: string | null;
119. [L151] 执行：doNotDisturbEnd?: string | null;
120. [L152] 执行：scheduledAt?: string;
121. [L153] 执行：status: string;
122. [L154] 执行：escalationPolicy?: string | null;
123. [L155] 执行：deliveryHistory?: ReminderDelivery[];
124. [L156] 执行：responseHistory?: ReminderDelivery[];
125. [L159] 导出类型 `ReminderDelivery`
126. [L160] 执行：id: string;
127. [L161] 执行：reminderId: string;
128. [L162] 执行：channel: string;
129. [L163] 执行：status: string;
130. [L164] 执行：payloadJson: string;
131. [L165] 执行：createdAt: string;
132. [L166] 执行：respondedAt?: string | null;
133. [L169] 导出类型 `ReminderActionResponse`
134. [L170] 执行：kind: string;
135. [L171] 执行：status: string;
136. [L172] 执行：detailUrl?: string | null;
137. [L175] 导出类型 `CreateReminderRequest`
138. [L176] 执行：relatedObjectType: string;
139. [L177] 执行：relatedObjectId: string;
140. [L178] 执行：title: string;
141. [L179] 执行：body?: string;
142. [L180] 执行：triggerReason?: string;
143. [L181] 执行：riskLevel?: OperationRiskLevel;
144. [L182] 执行：channels?: string[];
145. [L183] 执行：doNotDisturbStart?: string | null;
146. [L184] 执行：doNotDisturbEnd?: string | null;
147. [L185] 执行：scheduledAt: string;
148. [L188] 导出类型 `GenerateReportRequest`
149. [L189] 执行：kind: 'Daily' | 'Weekly' | 'Monthly' | 'Project' | string;
150. [L190] 执行：date: string;
151. [L191] 执行：projectId?: string | null;
152. [L194] 导出类型 `ReportArtifact`
153. [L195] 执行：id: string;
154. [L196] 执行：kind: string;
155. [L197] 执行：title?: string;
156. [L198] 执行：projectId?: string | null;
157. [L199] 执行：riskLevel: OperationRiskLevel;
158. [L200] 执行：contentMarkdown?: string;
159. [L201] 执行：metricsJson?: string;
160. [L202] 执行：generatedAt: string;
161. [L203] 执行：status?: string;
162. [L204] 执行：suggestions?: ReportSuggestion[];
163. [L205] 执行：confirmationId?: string | null;
164. [L208] 导出类型 `ReportSuggestion`
165. [L209] 执行：id: string;
166. [L210] 执行：reportId: string;
167. [L211] 执行：action: string;
168. [L212] 执行：summary: string;
169. [L213] 执行：status: string;
170. [L214] 执行：confirmationId?: string | null;
171. [L217] 导出类型 `SyncConflict`
172. [L218] 执行：id: string;
173. [L219] 执行：provider: string;
174. [L220] 执行：objectType: string;
175. [L221] 执行：objectId: string;
176. [L222] 执行：graphEventId?: string | null;
177. [L223] 执行：conflictKind?: string;
178. [L224] 执行：changedFields: string[];
179. [L225] 执行：status: string;
180. [L226] 执行：resolvedConfirmationId?: string | null;
181. [L229] 导出类型 `AuditVersion`
182. [L230] 执行：id: string;
183. [L231] 执行：objectType: string;
184. [L232] 执行：objectId: string;
185. [L233] 执行：confirmationId?: string | null;
186. [L234] 执行：source?: string;
187. [L235] 执行：actor?: string;
188. [L236] 执行：beforeJson: string;
189. [L237] 执行：afterJson: string;
190. [L238] 执行：changedFields: string[];
191. [L239] 执行：changedFieldsJson?: string;
192. [L240] 执行：createdAt: string;
193. [L243] 导出类型 `AuditTimelineResponse`
194. [L244] 执行：items: AuditVersion[];
195. [L247] 导出类型 `AuditExportResponse`
196. [L248] 执行：fileName: string;
197. [L249] 执行：contentType: string;
198. [L250] 执行：content: string;
199. [L253] 导出类型 `RestorePreviewResponse`
200. [L254] 执行：objectType: string;
201. [L255] 执行：objectId: string;
202. [L256] 执行：summary: string;
203. [L257] 执行：requiresConfirmation: boolean;
204. [L258] 执行：changedFields: string[];
205. [L261] 导出类型 `DataCenterObjectRef`
206. [L262] 执行：objectType: string;
207. [L263] 执行：objectId: string;
208. [L266] 导出类型 `DataCenterBatchOperationRequest`
209. [L267] 执行：action: string;
210. [L268] 执行：objects: DataCenterObjectRef[];
211. [L269] 执行：reason?: string | null;
212. [L272] 导出类型 `DataCenterBatchPreviewResponse`
213. [L273] 执行：riskLevel: OperationRiskLevel;
214. [L274] 执行：requiresStrictConfirmation: boolean;
215. [L275] 执行：summary: string;
216. [L276] 执行：affectedObjectTypes: string[];
217. [L277] 执行：affectedCount: number;
218. [L280] 导出类型 `DataCenterBatchExecutionResponse`
219. [L281] 执行：confirmationId: string;
220. [L282] 执行：status: string;
221. [L283] 执行：affectedCount: number;
222. [L286] 导出类型 `EndpointStatus`
223. [L287] 执行：deviceId: string;
224. [L288] 执行：platform: EndpointPlatform;
225. [L289] 执行：uploadStatus: PimHealthStatus | string;
226. [L290] 执行：collectionCacheCount: number;
227. [L291] 执行：onlineOnlyBlockedCount: number;
228. [L292] 执行：lastHeartbeatAt?: string | null;
229. [L295] 导出类型 `EndpointHeartbeatRequest`
230. [L296] 执行：platform: EndpointPlatform;
231. [L297] 执行：appVersion?: string | null;
232. [L298] 执行：uploadStatus?: string | null;
233. [L299] 执行：collectionCacheCount?: number;
234. [L302] 导出类型 `EndpointCollectionQuality`
235. [L303] 执行：deviceId: string;
236. [L304] 执行：platform: EndpointPlatform;
237. [L305] 执行：uploadStatus: PimHealthStatus | string;
238. [L306] 执行：issueCount: number;
239. [L307] 执行：checkedAt: string;
240. [L310] 导出类型 `EndpointNotificationActionRequest`
241. [L311] 执行：action: string;
242. [L312] 执行：riskLevel: OperationRiskLevel;
243. [L313] 执行：confirmationId?: string | null;
244. [L314] 执行：relatedObjectType?: string | null;
245. [L315] 执行：relatedObjectId?: string | null;
246. [L318] 导出类型 `EndpointNotificationActionResponse`
247. [L319] 执行：result: NotificationActionResult;
248. [L320] 执行：detailUrl?: string | null;
249. [L321] 执行：message?: string | null;
250. [L324] 导出类型 `CreateTaskExecutionSegmentRequest`
251. [L325] 执行：startsAt: string;
252. [L326] 执行：endsAt: string;
253. [L327] 执行：status: string;
254. [L328] 执行：source: string;
255. [L329] 执行：planningReason?: string | null;
256. [L332] 导出类型 `TaskExecutionSegmentResponse`
257. [L333] 执行：id: string;
258. [L334] 执行：taskId: string;
259. [L335] 执行：taskTitle: string;
260. [L336] 执行：startsAt: string;
261. [L337] 执行：endsAt: string;
262. [L338] 执行：status: string;
263. [L339] 执行：source: string;
264. [L340] 执行：planningReason?: string | null;
265. [L341] 执行：confirmationId?: string | null;
266. [L344] 导出类型 `CalendarLayerQueryRequest`
267. [L345] 执行：start: string;
268. [L346] 执行：end: string;
269. [L347] 执行：layers?: Array<CalendarLayerId | string>;
270. [L348] 执行：outlookOnly?: boolean;
271. [L351] 导出类型 `CalendarLayerItem`
272. [L352] 执行：id: string;
273. [L353] 执行：layer: CalendarLayerId | string;
274. [L354] 执行：objectType: string;
275. [L355] 执行：objectId: string;
276. [L356] 执行：title: string;
277. [L357] 执行：startsAt: string;
278. [L358] 执行：endsAt: string;
279. [L359] 执行：source: string;
280. [L360] 执行：status: string;
281. [L361] 执行：color: string;
282. [L362] 执行：requiresConfirmation: boolean;
283. [L365] 导出类型 `CalendarLayerResponse`
284. [L366] 执行：start: string;
285. [L367] 执行：end: string;
286. [L368] 执行：items: CalendarLayerItem[];
287. [L371] 导出类型 `DataCenterQueryRequest`
288. [L372] 执行：search?: string | null;
289. [L373] 执行：objectType?: string | null;
290. [L374] 执行：source?: string | null;
291. [L375] 执行：pendingOnly: boolean;
292. [L376] 执行：page?: number;
293. [L377] 执行：pageSize?: number;
294. [L380] 导出类型 `DataCenterItem`
295. [L381] 执行：objectType: string;
296. [L382] 执行：objectId: string;
297. [L383] 执行：title: string;
298. [L384] 执行：source: string;
299. [L385] 执行：status: string;
300. [L386] 执行：startsAt?: string | null;
301. [L387] 执行：endsAt?: string | null;
302. [L388] 执行：summary: string;
303. [L391] 导出类型 `DataCenterQueryResponse`
304. [L392] 执行：items: DataCenterItem[];
305. [L393] 执行：page: number;
306. [L394] 执行：pageSize: number;
307. [L395] 执行：totalCount: number;
308. [L398] 导出类型 `OutlookSettingsResponse`
309. [L399] 执行：provider: string;
310. [L400] 执行：tenantId: string;
311. [L401] 执行：clientId?: string | null;
312. [L402] 执行：scopes: string;
313. [L403] 执行：status: string;
314. [L404] 执行：tokenHealth: string;
315. [L405] 执行：deltaLink?: string | null;
316. [L406] 执行：syncWindowDays?: number | null;
317. [L407] 执行：writebackDefault?: string | null;
318. [L408] 执行：conflictPolicy?: string | null;
319. [L409] 执行：lastSyncedAt?: string | null;
320. [L410] 执行：lastError?: string | null;
321. [L413] 导出类型 `UpdateOutlookSettingsRequest`
322. [L414] 执行：tenantId: string;
323. [L415] 执行：clientId?: string | null;
324. [L416] 执行：scopes: string;
325. [L419] 导出类型 `OutlookDeviceCodeRequestResponse`
326. [L420] 执行：endpoint: string;
327. [L421] 执行：verificationUri: string;
328. [L422] 执行：userCode: string;
329. [L423] 执行：expiresAt: string;
330. [L424] 执行：message: string;
331. [L425] 执行：deviceCode?: string | null;
332. [L428] 导出类型 `OutlookSyncStep`
333. [L429] 执行：name: string;
334. [L430] 执行：status: string;
335. [L431] 执行：detail: string;
336. [L432] 执行：at: string;
337. [L435] 导出类型 `OutlookSyncBatchResponse`
338. [L436] 执行：id: string;
339. [L437] 执行：provider: string;
340. [L438] 执行：status: string;
341. [L439] 执行：readCount: number;
342. [L440] 执行：createdCount: number;
343. [L441] 执行：updatedCount: number;
344. [L442] 执行：conflictCount: number;
345. [L443] 执行：confirmationCount: number;
346. [L444] 执行：failureCount: number;
347. [L445] 执行：steps: OutlookSyncStep[];
348. [L446] 执行：errorSummary?: string | null;
349. [L447] 执行：startedAt: string;
350. [L448] 执行：finishedAt?: string | null;
351. … 其余约 677 条有效逻辑行同序压缩（源文件共 1312 行）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/types/index.ts",
      "label": "ApiResponse",
      "path": "src/client-web/src/types/index.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/types/index.ts.md",
      "layer": "client-web",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
