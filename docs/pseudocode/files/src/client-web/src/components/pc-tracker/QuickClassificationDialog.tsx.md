# src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `QuickClassificationDialog`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### getClusterParts
#### getClusterParts(clusterKey: string)
- 输入：clusterKey: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getClusterParts`
  2. 赋值 `separator` = clusterKey.indexOf(':')
  3. 执行：if (separator < 0) return { prefix: '', value: clusterKey.trim() };
  4. 返回 JSX/结构
  5. 执行：prefix: clusterKey.slice(0, separator).trim().toLowerCase(),
  6. 执行：value: clusterKey.slice(separator + 1).trim(),
- 分支与异常：if (separator < 0) return { prefix: '', value: clusterKey.trim() };
- 调用：getClusterParts、clusterKey.indexOf、clusterKey.trim、clusterKey.slice、trim、toLowerCase

### buildConditionsJson
#### buildConditionsJson(clusterKey: string)
- 输入：clusterKey: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `buildConditionsJson`
  2. 赋值 `{ prefix, value }` = getClusterParts(clusterKey)
  3. 若 (!value || (prefix !== 'web' && prefix !== 'app')) 则
  4. 返回 null
  5. 赋值 `condition` = prefix === 'web'
  6. 执行：? { field: 'domain', op: 'domainSuffix', value }
  7. 执行：: { field: 'appNameNormalized', op: 'equals', value };
  8. 返回 JSON.stringify({ all: [condition] })
- 分支与异常：if (!value || (prefix !== 'web' && prefix !== 'app')) {
- 调用：buildConditionsJson、getClusterParts、JSON.stringify

### formatMinutes
#### formatMinutes(seconds: number)
- 输入：seconds: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatMinutes`
  2. 赋值 `minutes` = Math.round((seconds / 60) * 10) / 10
  3. 返回 `${minutes.toLocaleString('zh-CN')} 分钟`
- 分支与异常：无显著分支
- 调用：formatMinutes、Math.round、minutes.toLocaleString

### compactEntries
#### compactEntries(entries: Record<string, number>)
- 输入：entries: Record<string, number>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `compactEntries`
  2. 返回 Object.entries(entries)
  3. 执行：.sort((a, b) => b[1] - a[1])
  4. 执行：.slice(0, 4)
  5. 执行：.map(([name, count]) => `${name || '未分类'} ${count}`)
  6. 执行：.join('、');
- 分支与异常：无显著分支
- 调用：compactEntries、Object.entries、sort、slice、map、join

## 近逐行中文伪代码

1. [L3] 执行：ActivityClassificationApplyRange,
2. [L4] 执行：ActivityClassificationPreview,
3. [L5] 执行：ActivityClassificationSuggestion,
4. [L6] 执行：SaveActivityClassificationRuleRequest,
5. [L9] 执行：const categoryColors: Record<string, string> = {
6. [L10] 执行：编程: '#6B5EE4',
7. [L11] 执行：终端: '#E05A7A',
8. [L12] 执行：沟通: '#F5935A',
9. [L13] 执行：办公: '#F59E0B',
10. [L14] 执行：文件: '#3B82F6',
11. [L15] 执行：浏览: '#0EA8A0',
12. [L16] 执行：学习: '#2563EB',
13. [L17] 执行：娱乐: '#EC4899',
14. [L18] 执行：其他: '#64748b',
15. [L21] 赋值 `categoryOptions` = Object.keys(categoryColors)
16. [L23] 定义类型 `Props`
17. [L24] 执行：suggestion: ActivityClassificationSuggestion | null;
18. [L25] 执行：date: string;
19. [L26] 执行：recentProjectTags: string[];
20. [L27] 执行：preview?: ActivityClassificationPreview | null;
21. [L28] 执行：isPreviewing: boolean;
22. [L29] 执行：isApplying: boolean;
23. [L30] 执行：onClose: () => void;
24. [L31] 执行：onDraftChange: () => void;
25. [L32] 执行：onPreview: (rule: SaveActivityClassificationRuleRequest, range: ActivityClassificationApplyRange) => void;
26. [L33] 执行：onApply: (rule: SaveActivityClassificationRuleRequest, range: ActivityClassificationApplyRange) => void;
27. [L36] 定义函数 `getClusterParts`
28. [L37] 赋值 `separator` = clusterKey.indexOf(':')
29. [L38] 执行：if (separator < 0) return { prefix: '', value: clusterKey.trim() };
30. [L39] 返回 JSX/结构
31. [L40] 执行：prefix: clusterKey.slice(0, separator).trim().toLowerCase(),
32. [L41] 执行：value: clusterKey.slice(separator + 1).trim(),
33. [L45] 定义函数 `buildConditionsJson`
34. [L46] 赋值 `{ prefix, value }` = getClusterParts(clusterKey)
35. [L47] 若 (!value || (prefix !== 'web' && prefix !== 'app')) 则
36. [L48] 返回 null
37. [L51] 赋值 `condition` = prefix === 'web'
38. [L52] 执行：? { field: 'domain', op: 'domainSuffix', value }
39. [L53] 执行：: { field: 'appNameNormalized', op: 'equals', value };
40. [L55] 返回 JSON.stringify({ all: [condition] })
41. [L58] 定义函数 `formatMinutes`
42. [L59] 赋值 `minutes` = Math.round((seconds / 60) * 10) / 10
43. [L60] 返回 `${minutes.toLocaleString('zh-CN')} 分钟`
44. [L63] 定义函数 `compactEntries`
45. [L64] 返回 Object.entries(entries)
46. [L65] 执行：.sort((a, b) => b[1] - a[1])
47. [L66] 执行：.slice(0, 4)
48. [L67] 执行：.map(([name, count]) => `${name || '未分类'} ${count}`)
49. [L68] 执行：.join('、');
50. [L71] 默认导出函数 `QuickClassificationDialog`
51. [L72] 执行：suggestion,
52. [L74] 执行：recentProjectTags,
53. [L76] 执行：isPreviewing,
54. [L77] 执行：isApplying,
55. [L79] 执行：onDraftChange,
56. [L80] 执行：onPreview,
57. [L83] 赋值 `titleId` = useId()
58. [L84] 赋值 `projectTagListId` = useId()
59. [L85] 执行：const [categoryName, setCategoryName] = useState('其他');
60. [L86] 执行：const [projectTag, setProjectTag] = useState('');
61. [L87] 执行：const [rangeMode, setRangeMode] = useState<ActivityClassificationApplyRange['mode']>('today');
62. [L88] 执行：const [dateFrom, setDateFrom] = useState(date);
63. [L89] 执行：const [dateTo, setDateTo] = useState(date);
64. [L91] 注册 `useEffect` 副作用
65. [L92] 执行：if (!suggestion) return;
66. [L94] 更新状态 setCategoryName(suggestion.suggestedCategory || suggestion.currentCategory || '其他')
67. [L95] 更新状态 setProjectTag(suggestion.suggestedProjectTag || '')
68. [L96] 更新状态 setRangeMode('today')
69. [L97] 更新状态 setDateFrom(date)
70. [L98] 更新状态 setDateTo(date)
71. [L99] 执行：onDraftChange();
72. [L102] Hook `useMemo` 绑定 `rule`
73. [L103] 执行：if (!suggestion) return null;
74. [L105] 赋值 `trimmedCategory` = categoryName.trim() || '其他'
75. [L106] 赋值 `trimmedProjectTag` = projectTag.trim()
76. [L107] 赋值 `clusterValue` = getClusterParts(suggestion.clusterKey).value
77. [L108] 赋值 `conditionsJson` = buildConditionsJson(suggestion.clusterKey)
78. [L109] 执行：if (!conditionsJson) return null;
79. [L111] 返回 JSX/结构
80. [L112] 执行：ruleName: `用户纠错: ${suggestion.clusterKey} ${new Date().toISOString()}`,
81. [L113] 执行：scope: 'both',
82. [L114] 执行：categoryName: trimmedCategory,
83. [L115] 执行：projectTag: trimmedProjectTag || null,
84. [L116] 执行：color: categoryColors[trimmedCategory] || categoryColors['其他'],
85. [L117] 执行：priority: 900,
86. [L118] 执行：conditionsJson,
87. [L119] 执行：confidence: 0.95,
88. [L120] 执行：explanation: `用户快捷纠错，来源建议 ${suggestion.id}，匹配 ${clusterValue}。`,
89. [L124] 执行：if (!suggestion) return null;
90. [L126] 执行：const range: ActivityClassificationApplyRange = {
91. [L127] 执行：mode: rangeMode,
92. [L128] 执行：dateFrom,
93. [L131] 赋值 `canSubmit` = Boolean(rule) && categoryName.trim().length > 0
94. [L132] 赋值 `isRangeDisabled` = rangeMode !== 'range'
95. [L134] 返回 JSX/结构
96. [L135] 执行：<div className="fixed inset-0 z-50 flex items-center justify-center px-3 py-6">
97. [L136] 执行：<div className="absolute inset-0 bg-slate-950/25" onClick={onClose} />
98. [L138] 执行：role="dialog"
99. [L139] 执行：aria-modal="true"
100. [L140] 执行：aria-labelledby={titleId}
101. [L141] 执行：className="relative flex max-h-full w-full max-w-[640px] flex-col overflow-hidden rounded-2xl border border-sl
102. [L143] 执行：<header className="border-b border-slate-200 px-5 py-4">
103. [L144] 执行：<div className="flex items-start justify-between gap-3">
104. [L145] 执行：<div className="min-w-0">
105. [L146] 执行：<h2 id={titleId} className="text-base font-semibold text-slate-950">快捷纠错</h2>
106. [L147] 执行：<p className="mt-1 truncate text-sm text-slate-500">{suggestion.clusterKey}</p>
107. [L149] 执行：<button type="button" onClick={onClose} className="pim-button-secondary h-9 shrink-0 px-3 text-sm">
108. [L151] 执行：</button>
109. [L153] 执行：</header>
110. [L155] 执行：<div className="min-h-0 flex-1 space-y-4 overflow-auto px-5 py-4">
111. [L156] 执行：<div className="grid gap-3 sm:grid-cols-2">
112. [L157] 执行：<label className="min-w-0 text-sm">
113. [L158] 执行：<span className="mb-1 block text-xs font-medium text-slate-500">分类</span>
114. [L160] 执行：value={categoryName}
115. [L161] 执行：onChange={e => {
116. [L162] 更新状态 setCategoryName(e.target.value)
117. [L163] 执行：onDraftChange();
118. [L165] 执行：className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none fo
119. [L167] 执行：{categoryOptions.map(category => (
120. [L168] 执行：<option key={category} value={category}>{category}</option>
121. [L170] 执行：</select>
122. [L173] 执行：<label className="min-w-0 text-sm">
123. [L174] 执行：<span className="mb-1 block text-xs font-medium text-slate-500">项目标签</span>
124. [L176] 执行：value={projectTag}
125. [L177] 执行：onChange={e => {
126. [L178] 更新状态 setProjectTag(e.target.value)
127. [L179] 执行：onDraftChange();
128. [L181] 执行：list={projectTagListId}
129. [L182] 执行：placeholder="可留空"
130. [L183] 执行：className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none pl
131. [L185] 执行：<datalist id={projectTagListId}>
132. [L186] 执行：{recentProjectTags.map(tag => (
133. [L187] 执行：<option key={tag} value={tag} />
134. [L189] 执行：</datalist>
135. [L193] 执行：<div className="space-y-3">
136. [L194] 执行：<div className="grid grid-cols-2 gap-2">
137. [L195] 执行：{(['today', 'range'] as const).map(mode => (
138. [L197] 执行：key={mode}
139. [L198] 执行：type="button"
140. [L199] 执行：onClick={() => {
141. [L200] 更新状态 setRangeMode(mode)
142. [L201] 执行：onDraftChange();
143. [L203] 执行：className={`h-10 rounded-lg border px-2 text-sm font-medium transition-colors ${
144. [L204] 执行：rangeMode === mode
145. [L205] 执行：? 'border-blue-600 bg-blue-50 text-blue-700'
146. [L206] 执行：: 'border-slate-200 bg-white text-slate-600 hover:border-blue-200'
147. [L209] 执行：{mode === 'today' ? '今天' : '范围'}
148. [L210] 执行：</button>
149. [L214] 执行：<div className="grid gap-3 sm:grid-cols-2">
150. [L215] 执行：<label className="min-w-0 text-sm">
151. [L216] 执行：<span className="mb-1 block text-xs font-medium text-slate-500">开始日期</span>
152. [L218] 执行：type="date"
153. [L219] 执行：value={dateFrom}
154. [L220] 执行：disabled={isRangeDisabled}
155. [L221] 执行：onChange={e => {
156. [L222] 更新状态 setDateFrom(e.target.value)
157. [L223] 执行：onDraftChange();
158. [L225] 执行：className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none di
159. [L228] 执行：<label className="min-w-0 text-sm">
160. [L229] 执行：<span className="mb-1 block text-xs font-medium text-slate-500">结束日期</span>
161. [L231] 执行：type="date"
162. [L232] 执行：value={dateTo}
163. [L233] 执行：disabled={isRangeDisabled}
164. [L234] 执行：onChange={e => {
165. [L235] 更新状态 setDateTo(e.target.value)
166. [L236] 执行：onDraftChange();
167. [L238] 执行：className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none di
168. [L244] 执行：{rule ? (
169. [L245] 执行：<div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-xs text-slate-600">
170. [L246] 执行：<div className="break-all font-medium text-slate-800">{rule.conditionsJson}</div>
171. [L247] 执行：<div className="mt-2 grid gap-1 sm:grid-cols-3">
172. [L248] 执行：<span>优先级 {rule.priority}</span>
173. [L249] 执行：<span>置信度 {rule.confidence}</span>
174. [L250] 执行：<span>范围 {rule.scope}</span>
175. [L254] 执行：<div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-3 text-sm text-amber-900">
176. [L255] 执行：暂不支持这个建议类型，无法自动生成安全的纠错规则。
177. [L259] 执行：{preview && (
178. [L260] 执行：<div className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-3">
179. [L261] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
180. [L262] 执行：<h3 className="text-sm font-semibold text-blue-950">预览结果</h3>
181. [L263] 执行：{preview.requiresConfirmation && (
182. [L264] 执行：<span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-8
183. [L269] 执行：<p className="mt-2 text-sm text-blue-900">
184. [L270] 执行：将影响 {preview.affectedRecordCount.toLocaleString('zh-CN')} 条记录，合计 {formatMinutes(preview.affectedDurationSecond
185. [L272] 执行：{preview.summary && <p className="mt-1 break-words text-xs text-blue-700">{preview.summary}</p>}
186. [L273] 执行：<div className="mt-2 space-y-1 text-xs text-blue-800">
187. [L274] 执行：<p className="break-words">当前：{compactEntries(preview.currentCategoryCounts) || '无'}</p>
188. [L275] 执行：<p className="break-words">应用后：{compactEntries(preview.newCategoryCounts) || '无'}</p>
189. [L281] 执行：<footer className="flex flex-col gap-2 border-t border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:just
190. [L282] 执行：<p className="text-xs text-slate-500">先预览影响范围，再应用纠错规则。</p>
191. [L283] 执行：<div className="grid grid-cols-2 gap-2 sm:w-[220px]">
192. [L285] 执行：type="button"
193. [L286] 执行：onClick={() => {
194. [L287] 执行：if (rule) onPreview(rule, range);
195. [L289] 执行：disabled={!canSubmit || isPreviewing || isApplying}
196. [L290] 执行：className="pim-button-secondary h-10 px-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
197. [L292] 执行：{isPreviewing ? '预览中' : '预览'}
198. [L293] 执行：</button>
199. [L295] 执行：type="button"
200. [L296] 执行：onClick={() => {
201. [L297] 执行：if (rule) onApply(rule, range);
202. [L299] 执行：disabled={!preview || !canSubmit || isPreviewing || isApplying}
203. [L300] 执行：className="pim-button-primary h-10 px-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-60"
204. [L302] 执行：{isApplying ? '应用中' : '应用'}
205. [L303] 执行：</button>
206. [L305] 执行：</footer>
207. [L306] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx",
      "label": "QuickClassificationDialog",
      "path": "src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/QuickClassificationDialog.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
