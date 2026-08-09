# src/client-web/src/pages/FilesPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `FilesPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/files.ts`、`src/client-web/src/types`、`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### errorMessage
#### errorMessage(error: unknown)
- 输入：error: unknown
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `errorMessage`
  2. 返回 error instanceof Error ? error.message : '操作失败，请稍后重试。'
- 分支与异常：无显著分支
- 调用：errorMessage

### normalizeFolderPath
#### normalizeFolderPath(path: string)
- 输入：path: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `normalizeFolderPath`
  2. 赋值 `trimmed` = path.trim()
  3. 执行：if (!trimmed || trimmed === '/') return '/';
  4. 赋值 `withSlash` = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  5. 返回 withSlash.replace(/\/+$/, '') || '/'
- 分支与异常：if (!trimmed || trimmed === '/') return '/';
- 调用：normalizeFolderPath、path.trim、trimmed.startsWith、withSlash.replace

### joinPath
#### joinPath(folder: string, name: string)
- 输入：folder: string, name: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `joinPath`
  2. 赋值 `base` = normalizeFolderPath(folder)
  3. 返回 base === '/' ? `/${name}` : `${base}/${name}`
- 分支与异常：无显著分支
- 调用：joinPath、normalizeFolderPath

### parentPath
#### parentPath(path: string)
- 输入：path: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `parentPath`
  2. 赋值 `normalized` = normalizeFolderPath(path)
  3. 执行：if (normalized === '/') return '/';
  4. 赋值 `index` = normalized.lastIndexOf('/')
  5. 返回 index <= 0 ? '/' : normalized.slice(0, index)
- 分支与异常：if (normalized === '/') return '/';
- 调用：parentPath、normalizeFolderPath、normalized.lastIndexOf、normalized.slice

### breadcrumb
#### breadcrumb(path: string)
- 输入：path: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `breadcrumb`
  2. 赋值 `normalized` = normalizeFolderPath(path)
  3. 执行：if (normalized === '/') return [{ label: '根目录', path: '/' }];
  4. 赋值 `parts` = normalized.split('/').filter(Boolean)
  5. 返回 [
  6. 执行：{ label: '根目录', path: '/' },
  7. 执行：...parts.map((part, index) => ({
  8. 执行：label: part,
  9. 执行：path: `/${parts.slice(0, index + 1).join('/')}`,
- 分支与异常：if (normalized === '/') return [{ label: '根目录', path: '/' }];
- 调用：breadcrumb、normalizeFolderPath、normalized.split、filter、parts.map、parts.slice、join

### formatDateTime
#### formatDateTime(value: string | null | undefined)
- 输入：value: string | null | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatDateTime`
  2. 执行：if (!value) return '-';
  3. 赋值 `date` = new Date(value)
  4. 执行：if (Number.isNaN(date.getTime())) return value;
  5. 返回 date.toLocaleString('zh-CN', {
  6. 执行：month: '2-digit',
  7. 执行：day: '2-digit',
  8. 执行：hour: '2-digit',
  9. 执行：minute: '2-digit',
- 分支与异常：if (!value) return '-';；if (Number.isNaN(date.getTime())) return value;
- 调用：formatDateTime、Date、Number.isNaN、date.getTime、date.toLocaleString

### formatBytes
#### formatBytes(value: number | null | undefined)
- 输入：value: number | null | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatBytes`
  2. 执行：if (value == null) return '-';
  3. 执行：if (value < 1024) return `${value} B`;
  4. 赋值 `units` = ['KB', 'MB', 'GB', 'TB']
  5. 赋值 `size` = value / 1024
  6. 赋值 `unit` = 0
  7. 当 (size >= 1024 && unit < units.length - 1) 循环
  8. 执行：size /= 1024;
  9. 执行：unit += 1;
  10. 返回 `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unit]}`
- 分支与异常：if (value == null) return '-';；if (value < 1024) return `${value} B`;
- 调用：formatBytes、size.toFixed

### isOoxmlFile
#### isOoxmlFile(item: FileItem | null)
- 输入：item: FileItem | null
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `isOoxmlFile`
  2. 执行：if (!item) return false;
  3. 赋值 `name` = item.name.toLowerCase()
  4. 赋值 `mime` = item.mimeType?.toLowerCase() ?? ''
  5. 返回 JSX/结构
  6. 执行：mime.includes('officedocument')
  7. 执行：|| name.endsWith('.docx')
  8. 执行：|| name.endsWith('.xlsx')
  9. 执行：|| name.endsWith('.pptx')
- 分支与异常：if (!item) return false;
- 调用：isOoxmlFile、item.name.toLowerCase、toLowerCase、mime.includes、name.endsWith

### statusTone
#### statusTone(status: string | null | undefined)
- 输入：status: string | null | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `statusTone`
  2. 赋值 `normalized` = (status ?? '').toLowerCase()
  3. 若 (['connected', 'completed', 'indexed', 'accepted', 'success'].includes(normalized)) 则
  4. 返回 'border-emerald-200 bg-emerald-50 text-emerald-700'
  5. 若 (['error', 'failed', 'deleted', 'dismissed'].includes(normalized)) 则
  6. 返回 'border-red-200 bg-red-50 text-red-700'
  7. 若 (['pending', 'queued', 'running', 'processing'].includes(normalized)) 则
  8. 返回 'border-blue-200 bg-blue-50 text-blue-700'
  9. 返回 'border-slate-200 bg-slate-50 text-slate-600'
- 分支与异常：if (['connected', 'completed', 'indexed', 'accepted', 'success'].includes(normalized)) {；if (['error', 'failed', 'deleted', 'dismissed'].includes(normalized)) {；if (['pending', 'queued', 'running', 'processing'].includes(normalized)) {
- 调用：statusTone、toLowerCase、includes

### statusLabel
#### statusLabel(status: string | null | undefined)
- 输入：status: string | null | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `statusLabel`
  2. 执行：if (!status) return '-';
  3. 执行：const labels: Record<string, string> = {
  4. 执行：accepted: '已采纳',
  5. 执行：completed: '已完成',
  6. 执行：connected: '已连接',
  7. 执行：current: '当前',
  8. 执行：deleted: '已删除',
  9. 执行：dismissed: '已忽略',
  10. 执行：error: '错误',
  11. 执行：failed: '失败',
  12. 执行：indexed: '已索引',
  13. 执行：pending: '待处理',
  14. 执行：processing: '处理中',
  15. 执行：queued: '排队中',
  16. 执行：running: '运行中',
  17. 执行：success: '成功',
  18. 返回 labels[status.toLowerCase()] ?? status
- 分支与异常：if (!status) return '-';
- 调用：statusLabel、status.toLowerCase

### itemTypeLabel
#### itemTypeLabel(type: FileItem['itemType'])
- 输入：type: FileItem['itemType']
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `itemTypeLabel`
  2. 返回 type === 'folder' ? '文件夹' : '文件'
- 分支与异常：无显著分支
- 调用：itemTypeLabel

### sourceLabel
#### sourceLabel(source: string | null | undefined)
- 输入：source: string | null | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `sourceLabel`
  2. 执行：if (!source) return '-';
  3. 执行：const labels: Record<string, string> = {
  4. 执行：local: '本地',
  5. 执行：nextcloud: 'Nextcloud',
  6. 执行：remote: '远端',
  7. 返回 labels[source.toLowerCase()] ?? source
- 分支与异常：if (!source) return '-';
- 调用：sourceLabel、source.toLowerCase

### suggestionTypeLabel
#### suggestionTypeLabel(type: string | null | undefined)
- 输入：type: string | null | undefined
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `suggestionTypeLabel`
  2. 执行：if (!type) return '-';
  3. 执行：const labels: Record<string, string> = {
  4. 执行：classification: '分类',
  5. 执行：move: '移动',
  6. 执行：rename: '重命名',
  7. 执行：tag: '标签',
  8. 返回 labels[type.toLowerCase()] ?? type
- 分支与异常：if (!type) return '-';
- 调用：suggestionTypeLabel、type.toLowerCase

### saveBlob
#### saveBlob(blob: Blob, filename: string)
- 输入：blob: Blob, filename: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `saveBlob`
  2. 赋值 `url` = URL.createObjectURL(blob)
  3. 赋值 `anchor` = document.createElement('a')
  4. 执行：anchor.href = url;
  5. 执行：anchor.download = filename;
  6. 执行：anchor.click();
  7. 执行：URL.revokeObjectURL(url);
- 分支与异常：无显著分支
- 调用：saveBlob、URL.createObjectURL、document.createElement、anchor.click、URL.revokeObjectURL

### Section
#### Section({ title, children, actions }: { title: string; children: ReactNode; actions?: ReactNode })
- 输入：{ title, children, actions }: { title: string; children: ReactNode; actions?: ReactNode }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `Section`
  2. 返回 JSX/结构
  3. 执行：<section className="min-w-0 rounded-lg border border-slate-200 bg-white">
  4. 执行：<div className="flex min-h-[44px] items-center justify-between gap-3 border-b border-slate-200 px-3 py-2">
  5. 执行：<h2 className="truncate text-sm font-semibold text-slate-900">{title}</h2>
  6. 执行：{actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
  7. 执行：{children}
  8. 执行：</section>
- 分支与异常：无显著分支
- 调用：Section

### StatusBadge
#### StatusBadge({ label }: { label: string | null | undefined })
- 输入：{ label }: { label: string | null | undefined }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `StatusBadge`
  2. 返回 JSX/结构
  3. 执行：<span className={`inline-flex max-w-full items-center rounded-full border px-2 py-0.5 text-xs font-medium ${st
  4. 执行：<span className="truncate">{statusLabel(label)}</span>
- 分支与异常：无显著分支
- 调用：StatusBadge、statusTone、statusLabel

### itemIcon
#### itemIcon(item: FileItem)
- 输入：item: FileItem
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `itemIcon`
  2. 执行：if (item.itemType === 'folder') return '夹';
  3. 赋值 `ext` = item.name.includes('.') ? item.name.split('.').pop()?.toUpperCase() : null
  4. 返回 ext?.slice(0, 4) || '文'
- 分支与异常：if (item.itemType === 'folder') return '夹';
- 调用：itemIcon、item.name.includes、item.name.split、pop、toUpperCase、slice

### FilesPage
#### FilesPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `FilesPage`
  2. 赋值 `queryClient` = useQueryClient()
  3. Hook `useRef` 绑定 `fileInputRef`
  4. 执行：const [activeProviderId, setActiveProviderId] = useState<string | null>(null);
  5. 执行：const [currentPath, setCurrentPath] = useState('/');
  6. 执行：const [selectedId, setSelectedId] = useState<string | null>(null);
  7. 执行：const [searchText, setSearchText] = useState('');
  8. 执行：const [submittedSearch, setSubmittedSearch] = useState('');
  9. 执行：const [searchMode, setSearchMode] = useState<FileSearchMode>('hybrid');
  10. 执行：const [sortKey, setSortKey] = useState<SortKey>('name');
  11. 执行：const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  12. 执行：const [providerMessage, setProviderMessage] = useState<string | null>(null);
  13. 执行：const [error, setError] = useState<string | null>(null);
  14. 执行：const [bindForm, setBindForm] = useState({
  15. 执行：baseUrl: '',
  16. 执行：internalBaseUrl: '',
  17. 执行：username: '',
  18. 执行：appPassword: '',
  19. 赋值 `providersQuery` = useQuery({
  20. 执行：queryKey: ['files', 'providers'],
  21. 执行：queryFn: getFileProviders,
  22. 赋值 `providers` = providersQuery.data ?? emptyProviders
  23. Hook `useMemo` 绑定 `activeProvider`
  24. 执行：() => providers.find(provider => provider.id === activeProviderId) ?? providers[0] ?? null,
  25. 执行：[activeProviderId, providers],
  26. 注册 `useEffect` 副作用
  27. 若 (providers.length === 0) 则
  28. 更新状态 setActiveProviderId(null)
  29. 返回（空）
  30. 若 (!activeProviderId || !providers.some(provider => provider.id === activeProviderId)) 则
- 分支与异常：if (providers.length === 0) {；if (!activeProviderId || !providers.some(provider => provider.id === activeProviderId)) {；if (a.itemType !== b.itemType) return a.itemType === 'folder' ? -1 : 1;；if (sortKey === 'size') {；if (id) {；if (!confirmed) return null;；if (itemId) {；if (!activeProvider) throw new Error('请先选择文件来源，再恢复回收站项目。');；if (!bindForm.baseUrl.trim() || !bindForm.username.trim() || !bindForm.appPassword.trim()) {；if (sortKey === nextKey) {
- 调用：FilesPage、useQueryClient、useState、useQuery、useMemo、providers.find、useEffect、setActiveProviderId、providers.some、getFileItems、breadcrumb、map、filter、searchFiles、sort

## 近逐行中文伪代码

1. [L2] 定义类型 `ChangeEvent`
2. [L3] 定义类型 `FormEvent`
3. [L4] 定义类型 `ReactNode`
4. [L5] 执行：useEffect,
5. [L8] 执行：useState,
6. [L12] 执行：acceptFileSuggestion,
7. [L13] 执行：bindNextcloudProvider,
8. [L14] 执行：deleteFile,
9. [L15] 执行：dismissFileSuggestion,
10. [L16] 执行：downloadFileBlob,
11. [L17] 执行：downloadFileVersionBlob,
12. [L18] 执行：getFileItem,
13. [L19] 执行：getFileItems,
14. [L20] 执行：getFileOpenLink,
15. [L21] 执行：getFileProviders,
16. [L22] 执行：getFileSuggestions,
17. [L23] 执行：getFileTrash,
18. [L24] 执行：getFileVersions,
19. [L25] 执行：indexFile,
20. [L26] 执行：moveFile,
21. [L27] 执行：renameFile,
22. [L28] 执行：restoreFileTrash,
23. [L29] 执行：restoreFileVersion,
24. [L30] 执行：restoreFileVersionPreview,
25. [L31] 执行：searchFiles,
26. [L32] 执行：syncFileProvider,
27. [L33] 执行：testFileProvider,
28. [L34] 执行：uploadFile,
29. [L37] 执行：FileItem,
30. [L38] 执行：FileOpenLinkMode,
31. [L39] 执行：FileProvider,
32. [L40] 执行：FileSearchMode,
33. [L41] 执行：FileSuggestion,
34. [L42] 执行：FileTrashItem,
35. [L43] 执行：FileVersion,
36. [L47] 定义类型 `SortKey`
37. [L48] 定义类型 `SortDirection`
38. [L50] 执行：const emptyProviders: FileProvider[] = [];
39. [L51] 执行：const emptyItems: FileItem[] = [];
40. [L52] 执行：const emptySuggestions: FileSuggestion[] = [];
41. [L53] 执行：const emptyTrash: FileTrashItem[] = [];
42. [L54] 执行：const emptyVersions: FileVersion[] = [];
43. [L56] 定义函数 `errorMessage`
44. [L57] 返回 error instanceof Error ? error.message : '操作失败，请稍后重试。'
45. [L60] 定义函数 `normalizeFolderPath`
46. [L61] 赋值 `trimmed` = path.trim()
47. [L62] 执行：if (!trimmed || trimmed === '/') return '/';
48. [L63] 赋值 `withSlash` = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
49. [L64] 返回 withSlash.replace(/\/+$/, '') || '/'
50. [L67] 定义函数 `joinPath`
51. [L68] 赋值 `base` = normalizeFolderPath(folder)
52. [L69] 返回 base === '/' ? `/${name}` : `${base}/${name}`
53. [L72] 定义函数 `parentPath`
54. [L73] 赋值 `normalized` = normalizeFolderPath(path)
55. [L74] 执行：if (normalized === '/') return '/';
56. [L75] 赋值 `index` = normalized.lastIndexOf('/')
57. [L76] 返回 index <= 0 ? '/' : normalized.slice(0, index)
58. [L79] 定义函数 `breadcrumb`
59. [L80] 赋值 `normalized` = normalizeFolderPath(path)
60. [L81] 执行：if (normalized === '/') return [{ label: '根目录', path: '/' }];
61. [L83] 赋值 `parts` = normalized.split('/').filter(Boolean)
62. [L84] 返回 [
63. [L85] 执行：{ label: '根目录', path: '/' },
64. [L86] 执行：...parts.map((part, index) => ({
65. [L87] 执行：label: part,
66. [L88] 执行：path: `/${parts.slice(0, index + 1).join('/')}`,
67. [L93] 定义函数 `formatDateTime`
68. [L94] 执行：if (!value) return '-';
69. [L95] 赋值 `date` = new Date(value)
70. [L96] 执行：if (Number.isNaN(date.getTime())) return value;
71. [L97] 返回 date.toLocaleString('zh-CN', {
72. [L98] 执行：month: '2-digit',
73. [L99] 执行：day: '2-digit',
74. [L100] 执行：hour: '2-digit',
75. [L101] 执行：minute: '2-digit',
76. [L105] 定义函数 `formatBytes`
77. [L106] 执行：if (value == null) return '-';
78. [L107] 执行：if (value < 1024) return `${value} B`;
79. [L108] 赋值 `units` = ['KB', 'MB', 'GB', 'TB']
80. [L109] 赋值 `size` = value / 1024
81. [L110] 赋值 `unit` = 0
82. [L111] 当 (size >= 1024 && unit < units.length - 1) 循环
83. [L112] 执行：size /= 1024;
84. [L113] 执行：unit += 1;
85. [L115] 返回 `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unit]}`
86. [L118] 定义函数 `isOoxmlFile`
87. [L119] 执行：if (!item) return false;
88. [L120] 赋值 `name` = item.name.toLowerCase()
89. [L121] 赋值 `mime` = item.mimeType?.toLowerCase() ?? ''
90. [L122] 返回 JSX/结构
91. [L123] 执行：mime.includes('officedocument')
92. [L124] 执行：|| name.endsWith('.docx')
93. [L125] 执行：|| name.endsWith('.xlsx')
94. [L126] 执行：|| name.endsWith('.pptx')
95. [L130] 定义函数 `statusTone`
96. [L131] 赋值 `normalized` = (status ?? '').toLowerCase()
97. [L132] 若 (['connected', 'completed', 'indexed', 'accepted', 'success'].includes(normalized)) 则
98. [L133] 返回 'border-emerald-200 bg-emerald-50 text-emerald-700'
99. [L135] 若 (['error', 'failed', 'deleted', 'dismissed'].includes(normalized)) 则
100. [L136] 返回 'border-red-200 bg-red-50 text-red-700'
101. [L138] 若 (['pending', 'queued', 'running', 'processing'].includes(normalized)) 则
102. [L139] 返回 'border-blue-200 bg-blue-50 text-blue-700'
103. [L141] 返回 'border-slate-200 bg-slate-50 text-slate-600'
104. [L144] 定义函数 `statusLabel`
105. [L145] 执行：if (!status) return '-';
106. [L146] 执行：const labels: Record<string, string> = {
107. [L147] 执行：accepted: '已采纳',
108. [L148] 执行：completed: '已完成',
109. [L149] 执行：connected: '已连接',
110. [L150] 执行：current: '当前',
111. [L151] 执行：deleted: '已删除',
112. [L152] 执行：dismissed: '已忽略',
113. [L153] 执行：error: '错误',
114. [L154] 执行：failed: '失败',
115. [L155] 执行：indexed: '已索引',
116. [L156] 执行：pending: '待处理',
117. [L157] 执行：processing: '处理中',
118. [L158] 执行：queued: '排队中',
119. [L159] 执行：running: '运行中',
120. [L160] 执行：success: '成功',
121. [L162] 返回 labels[status.toLowerCase()] ?? status
122. [L165] 定义函数 `itemTypeLabel`
123. [L166] 返回 type === 'folder' ? '文件夹' : '文件'
124. [L169] 定义函数 `sourceLabel`
125. [L170] 执行：if (!source) return '-';
126. [L171] 执行：const labels: Record<string, string> = {
127. [L172] 执行：local: '本地',
128. [L173] 执行：nextcloud: 'Nextcloud',
129. [L174] 执行：remote: '远端',
130. [L176] 返回 labels[source.toLowerCase()] ?? source
131. [L179] 定义函数 `suggestionTypeLabel`
132. [L180] 执行：if (!type) return '-';
133. [L181] 执行：const labels: Record<string, string> = {
134. [L182] 执行：classification: '分类',
135. [L183] 执行：move: '移动',
136. [L184] 执行：rename: '重命名',
137. [L185] 执行：tag: '标签',
138. [L187] 返回 labels[type.toLowerCase()] ?? type
139. [L190] 定义函数 `saveBlob`
140. [L191] 赋值 `url` = URL.createObjectURL(blob)
141. [L192] 赋值 `anchor` = document.createElement('a')
142. [L193] 执行：anchor.href = url;
143. [L194] 执行：anchor.download = filename;
144. [L195] 执行：anchor.click();
145. [L196] 执行：URL.revokeObjectURL(url);
146. [L199] 定义函数 `Section`
147. [L200] 返回 JSX/结构
148. [L201] 执行：<section className="min-w-0 rounded-lg border border-slate-200 bg-white">
149. [L202] 执行：<div className="flex min-h-[44px] items-center justify-between gap-3 border-b border-slate-200 px-3 py-2">
150. [L203] 执行：<h2 className="truncate text-sm font-semibold text-slate-900">{title}</h2>
151. [L204] 执行：{actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
152. [L206] 执行：{children}
153. [L207] 执行：</section>
154. [L211] 定义函数 `StatusBadge`
155. [L212] 返回 JSX/结构
156. [L213] 执行：<span className={`inline-flex max-w-full items-center rounded-full border px-2 py-0.5 text-xs font-medium ${st
157. [L214] 执行：<span className="truncate">{statusLabel(label)}</span>
158. [L219] 定义函数 `itemIcon`
159. [L220] 执行：if (item.itemType === 'folder') return '夹';
160. [L221] 赋值 `ext` = item.name.includes('.') ? item.name.split('.').pop()?.toUpperCase() : null
161. [L222] 返回 ext?.slice(0, 4) || '文'
162. [L225] 默认导出函数 `FilesPage`
163. [L226] 赋值 `queryClient` = useQueryClient()
164. [L227] Hook `useRef` 绑定 `fileInputRef`
165. [L228] 执行：const [activeProviderId, setActiveProviderId] = useState<string | null>(null);
166. [L229] 执行：const [currentPath, setCurrentPath] = useState('/');
167. [L230] 执行：const [selectedId, setSelectedId] = useState<string | null>(null);
168. [L231] 执行：const [searchText, setSearchText] = useState('');
169. [L232] 执行：const [submittedSearch, setSubmittedSearch] = useState('');
170. [L233] 执行：const [searchMode, setSearchMode] = useState<FileSearchMode>('hybrid');
171. [L234] 执行：const [sortKey, setSortKey] = useState<SortKey>('name');
172. [L235] 执行：const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
173. [L236] 执行：const [providerMessage, setProviderMessage] = useState<string | null>(null);
174. [L237] 执行：const [error, setError] = useState<string | null>(null);
175. [L238] 执行：const [bindForm, setBindForm] = useState({
176. [L239] 执行：baseUrl: '',
177. [L240] 执行：internalBaseUrl: '',
178. [L241] 执行：username: '',
179. [L242] 执行：appPassword: '',
180. [L245] 赋值 `providersQuery` = useQuery({
181. [L246] 执行：queryKey: ['files', 'providers'],
182. [L247] 执行：queryFn: getFileProviders,
183. [L250] 赋值 `providers` = providersQuery.data ?? emptyProviders
184. [L251] Hook `useMemo` 绑定 `activeProvider`
185. [L252] 执行：() => providers.find(provider => provider.id === activeProviderId) ?? providers[0] ?? null,
186. [L253] 执行：[activeProviderId, providers],
187. [L256] 注册 `useEffect` 副作用
188. [L257] 若 (providers.length === 0) 则
189. [L258] 更新状态 setActiveProviderId(null)
190. [L259] 返回（空）
191. [L262] 若 (!activeProviderId || !providers.some(provider => provider.id === activeProviderId)) 则
192. [L263] 更新状态 setActiveProviderId(providers[0].id)
193. [L267] 赋值 `itemsQuery` = useQuery({
194. [L268] 执行：queryKey: ['files', 'items', currentPath],
195. [L269] 执行：queryFn: () => getFileItems(currentPath),
196. [L270] 执行：enabled: providers.length > 0,
197. [L273] 赋值 `listItems` = itemsQuery.data?.result.items ?? emptyItems
198. [L274] Hook `useMemo` 绑定 `folderTreeRows`
199. [L275] 赋值 `trail` = breadcrumb(currentPath).map((crumb, index) => ({
200. [L276] 执行：id: `path:${crumb.path}`,
201. [L277] 执行：label: crumb.label,
202. [L278] 执行：path: crumb.path,
203. [L279] 执行：depth: index,
204. [L280] 执行：current: crumb.path === currentPath,
205. [L281] 执行：item: null as FileItem | null,
206. [L284] 赋值 `children` = listItems
207. [L285] 执行：.filter(item => item.itemType === 'folder')
208. [L286] 执行：.map(folder => ({
209. [L287] 执行：id: `folder:${folder.id}`,
210. [L288] 执行：label: folder.name,
211. [L289] 执行：path: folder.path,
212. [L290] 执行：depth: trail.length,
213. [L291] 执行：current: false,
214. [L292] 执行：item: folder,
215. [L295] 返回 [...trail, ...children]
216. [L298] 赋值 `searchQuery` = useQuery({
217. [L299] 执行：queryKey: ['files', 'search', submittedSearch, searchMode],
218. [L300] 执行：queryFn: () => searchFiles(submittedSearch, searchMode),
219. [L301] 执行：enabled: submittedSearch.length > 0,
220. [L304] 赋值 `visibleItems` = submittedSearch ? (searchQuery.data?.items ?? emptyItems) : listItems
221. [L305] 赋值 `semanticHits` = submittedSearch ? (searchQuery.data?.chunks ?? []) : []
222. [L307] Hook `useMemo` 绑定 `sortedItems`
223. [L308] 赋值 `multiplier` = sortDirection === 'asc' ? 1 : -1
224. [L309] 返回 [...visibleItems].sort((a, b) => {
225. [L310] 执行：if (a.itemType !== b.itemType) return a.itemType === 'folder' ? -1 : 1;
226. [L312] 若 (sortKey === 'size') 则
227. [L313] 返回 JSX/结构
228. [L316] 赋值 `left` = sortKey === 'modifiedAt' ? a.modifiedAt : a.name
229. [L317] 赋值 `right` = sortKey === 'modifiedAt' ? b.modifiedAt : b.name
230. [L318] 返回 left.localeCompare(right, 'zh-CN') * multiplier
231. [L322] 赋值 `detailQuery` = useQuery({
232. [L323] 执行：queryKey: ['files', 'item', selectedId],
233. [L324] 执行：queryFn: () => getFileItem(selectedId as string),
234. [L325] 执行：enabled: Boolean(selectedId),
235. [L328] 赋值 `selectedFromList` = visibleItems.find(item => item.id === selectedId) ?? null
236. [L329] 赋值 `selected` = detailQuery.data ?? selectedFromList
237. [L331] 赋值 `versionsQuery` = useQuery({
238. [L332] 执行：queryKey: ['files', 'versions', selectedId],
239. [L333] 执行：queryFn: () => getFileVersions(selectedId as string),
240. [L334] 执行：enabled: Boolean(selectedId),
241. [L337] 赋值 `versions` = versionsQuery.data ?? emptyVersions
242. [L339] 赋值 `suggestionsQuery` = useQuery({
243. [L340] 执行：queryKey: ['files', 'suggestions'],
244. [L341] 执行：queryFn: getFileSuggestions,
245. [L342] 执行：enabled: providers.length > 0,
246. [L345] 赋值 `suggestions` = suggestionsQuery.data ?? emptySuggestions
247. [L346] 赋值 `selectedSuggestions` = selected
248. [L347] 执行：? suggestions.filter(suggestion => suggestion.fileItemId === selected.id)
249. [L348] 执行：: emptySuggestions;
250. [L350] 赋值 `trashQuery` = useQuery({
251. [L351] 执行：queryKey: ['files', 'trash'],
252. [L352] 执行：queryFn: getFileTrash,
253. [L353] 执行：enabled: providers.length > 0,
254. [L356] 赋值 `trashItems` = trashQuery.data ?? emptyTrash
255. [L358] 定义函数 `invalidateFiles`
256. [L359] 执行：void queryClient.invalidateQueries({ queryKey: ['files'] });
257. [L360] 若 (id) 则
258. [L361] 执行：void queryClient.invalidateQueries({ queryKey: ['files', 'item', id] });
259. [L362] 执行：void queryClient.invalidateQueries({ queryKey: ['files', 'versions', id] });
260. [L366] 赋值 `bindMutation` = useMutation({
261. [L367] 执行：mutationFn: () => bindNextcloudProvider({
262. [L368] 执行：baseUrl: bindForm.baseUrl.trim(),
263. [L369] 执行：internalBaseUrl: bindForm.internalBaseUrl.trim() || null,
264. [L370] 执行：username: bindForm.username.trim(),
265. [L371] 执行：appPassword: bindForm.appPassword,
266. [L373] 执行：onSuccess: provider => {
267. [L374] 更新状态 setActiveProviderId(provider.id)
268. [L375] 更新状态 setBindForm(current => ({ ...current, appPassword: '' }))
269. [L376] 更新状态 setProviderMessage('Nextcloud 文件来源已保存。')
270. [L377] 更新状态 setError(null)
271. [L378] 执行：invalidateFiles();
272. [L380] 执行：onError: error => setError(errorMessage(error)),
273. [L383] 赋值 `testMutation` = useMutation({
274. [L384] 执行：mutationFn: (providerId: string) => testFileProvider(providerId),
275. [L385] 执行：onSuccess: result => {
276. [L386] 更新状态 setProviderMessage(result.success ? '连接测试通过。' : result.errorMessage || '连接测试失败。')
277. [L387] 更新状态 setError(null)
278. [L389] 执行：onError: error => setError(errorMessage(error)),
279. [L392] 赋值 `syncMutation` = useMutation({
280. [L393] 执行：mutationFn: (providerId: string) => syncFileProvider(providerId),
281. [L394] 执行：onSuccess: () => {
282. [L395] 更新状态 setProviderMessage('同步完成。')
283. [L396] 更新状态 setSubmittedSearch('')
284. [L397] 更新状态 setSearchText('')
285. [L398] 更新状态 setError(null)
286. [L399] 执行：invalidateFiles();
287. [L401] 执行：onError: error => setError(errorMessage(error)),
288. [L404] 赋值 `uploadMutation` = useMutation({
289. [L405] 执行：mutationFn: ({ providerId, path, file }: { providerId: string; path: string; file: File }) => uploadFile(provi
290. [L406] 执行：onSuccess: item => {
291. [L407] 更新状态 setSelectedId(item.id)
292. [L408] 更新状态 setError(null)
293. [L409] 执行：invalidateFiles(item.id);
294. [L411] 执行：onError: error => setError(errorMessage(error)),
295. [L414] 赋值 `moveMutation` = useMutation({
296. [L415] 执行：mutationFn: ({ id, destinationPath }: { id: string; destinationPath: string }) => moveFile(id, { destinationPa
297. [L416] 执行：onSuccess: item => {
298. [L417] 更新状态 setSelectedId(item.id)
299. [L418] 更新状态 setError(null)
300. [L419] 执行：invalidateFiles(item.id);
301. [L421] 执行：onError: error => setError(errorMessage(error)),
302. [L424] 赋值 `renameMutation` = useMutation({
303. [L425] 执行：mutationFn: ({ id, name }: { id: string; name: string }) => renameFile(id, { name }),
304. [L426] 执行：onSuccess: item => {
305. [L427] 更新状态 setSelectedId(item.id)
306. [L428] 更新状态 setError(null)
307. [L429] 执行：invalidateFiles(item.id);
308. [L431] 执行：onError: error => setError(errorMessage(error)),
309. [L434] 赋值 `deleteMutation` = useMutation({
310. [L435] 执行：mutationFn: (id: string) => deleteFile(id),
311. [L436] 执行：onSuccess: () => {
312. [L437] 更新状态 setSelectedId(null)
313. [L438] 更新状态 setError(null)
314. [L439] 执行：invalidateFiles();
315. [L441] 执行：onError: error => setError(errorMessage(error)),
316. [L444] 赋值 `indexMutation` = useMutation({
317. [L445] 执行：mutationFn: (id: string) => indexFile(id),
318. [L446] 执行：onSuccess: job => {
319. [L447] 更新状态 setProviderMessage(`索引任务${statusLabel(job.status)}。`)
320. [L448] 更新状态 setError(null)
321. [L449] 执行：invalidateFiles(job.fileItemId);
322. [L451] 执行：onError: error => setError(errorMessage(error)),
323. [L454] 赋值 `openLinkMutation` = useMutation({
324. [L455] 执行：mutationFn: ({ id, mode }: { id: string; mode: FileOpenLinkMode }) => getFileOpenLink(id, mode),
325. [L456] 执行：onSuccess: link => {
326. [L457] 执行：window.open(link.url, '_blank', 'noopener,noreferrer');
327. [L458] 更新状态 setError(null)
328. [L460] 执行：onError: error => setError(errorMessage(error)),
329. [L463] 赋值 `downloadMutation` = useMutation({
330. [L464] 执行：mutationFn: async (item: FileItem) => ({
331. [L465] 执行：filename: item.name,
332. [L466] 执行：blob: await downloadFileBlob(item.id),
333. [L468] 执行：onSuccess: ({ blob, filename }) => {
334. [L469] 执行：saveBlob(blob, filename);
335. [L470] 更新状态 setError(null)
336. [L472] 执行：onError: error => setError(errorMessage(error)),
337. [L475] 赋值 `versionDownloadMutation` = useMutation({
338. [L476] 执行：mutationFn: async ({ item, version }: { item: FileItem; version: FileVersion }) => ({
339. [L477] 执行：filename: `${version.modifiedAt.slice(0, 10)}-${item.name}`,
340. [L478] 执行：blob: await downloadFileVersionBlob(item.id, version.id),
341. [L480] 执行：onSuccess: ({ blob, filename }) => {
342. [L481] 执行：saveBlob(blob, filename);
343. [L482] 更新状态 setError(null)
344. [L484] 执行：onError: error => setError(errorMessage(error)),
345. [L487] 赋值 `restoreVersionMutation` = useMutation({
346. [L488] 执行：mutationFn: async ({ item, version }: { item: FileItem; version: FileVersion }) => {
347. [L489] 等待 `restoreFileVersionPreview(item.id, version.id)` 赋给 `preview`
348. [L490] 赋值 `confirmed` = window.confirm(`${preview.summary}\n${preview.currentVersionLabel} -> ${preview.restoreVersionLabel}
349. [L491] 执行：if (!confirmed) return null;
350. [L492] 等待异步：restoreFileVersion(item.id, version.id)
351. … 其余约 429 条有效逻辑行同序压缩（源文件共 1173 行）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/FilesPage.tsx",
      "label": "FilesPage",
      "path": "src/client-web/src/pages/FilesPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/FilesPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/FilesPage.tsx",
      "to": "src/client-web/src/api/files.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/FilesPage.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/FilesPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
