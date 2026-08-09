# src/client-web/src/pages/HistoricalLocationPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `HistoricalLocationPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/mobile.ts`、`src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx`、`src/client-web/src/components/mobile/mobileFormatting.ts`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### errorMessage
#### errorMessage(error: unknown)
- 输入：error: unknown
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `errorMessage`
  2. 执行：if (error instanceof Error && error.message) return error.message;
  3. 返回 error ? '历史位置加载失败，请稍后刷新。' : null
- 分支与异常：if (error instanceof Error && error.message) return error.message;
- 调用：errorMessage

### HistoricalLocationPage
#### HistoricalLocationPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `HistoricalLocationPage`
  2. Hook `useMemo` 绑定 `defaultRange`
  3. 执行：const [rangeShortcut, setRangeShortcut] = useState<MobileRangeShortcut>('7d');
  4. 执行：const [rangeStartDate, setRangeStartDate] = useState(defaultRange.startDate);
  5. 执行：const [rangeEndDate, setRangeEndDate] = useState(defaultRange.endDate);
  6. 执行：const [selectedDeviceId, setSelectedDeviceId] = useState('');
  7. 执行：const [maxAccuracyMeters, setMaxAccuracyMeters] = useState(50);
  8. 执行：const [includeRejected, setIncludeRejected] = useState(false);
  9. 执行：const [selectedSegmentId, setSelectedSegmentId] = useState<string | null>(null);
  10. 执行：const [selectedPointId, setSelectedPointId] = useState<string | null>(null);
  11. 赋值 `deviceId` = selectedDeviceId || undefined
  12. Hook `useMemo` 绑定 `utcRange`
  13. 执行：() => toMobileAnalyticsUtcRange({ startDate: rangeStartDate, endDate: rangeEndDate }),
  14. 执行：[rangeStartDate, rangeEndDate],
  15. 使用 `useMemo` 缓存计算结果
  16. 执行：...utcRange,
  17. 执行：deviceId,
  18. 执行：maxAccuracyMeters,
  19. 执行：includeRejected,
  20. 执行：[utcRange, deviceId, maxAccuracyMeters, includeRejected],
  21. 赋值 `devicesQuery` = useQuery({
  22. 执行：queryKey: ['mobile-devices'],
  23. 执行：queryFn: getMobileDevices,
  24. 执行：staleTime: 60000,
  25. 赋值 `overviewQuery` = useQuery({
  26. 执行：queryKey: ['mobile-location-analytics-overview', locationQuery],
  27. 执行：queryFn: () => getMobileLocationAnalyticsOverview(locationQuery),
  28. 执行：refetchInterval: 30000,
  29. 赋值 `tracksQuery` = useQuery({
  30. 执行：queryKey: ['mobile-location-analytics-tracks', locationQuery],
- 分支与异常：无显著分支
- 调用：HistoricalLocationPage、useMemo、buildMobileAnalyticsDateRange、useState、toMobileAnalyticsUtcRange、useQuery、getMobileLocationAnalyticsOverview、getMobileLocationAnalyticsTracks、tracks.flatMap、segments.some、getMobileLocationAnalyticsSegmentPoints、Boolean、points.some、applyShortcut、setRangeShortcut

## 近逐行中文伪代码

1. [L4] 执行：getMobileDevices,
2. [L5] 执行：getMobileLocationAnalyticsOverview,
3. [L6] 执行：getMobileLocationAnalyticsSegmentPoints,
4. [L7] 执行：getMobileLocationAnalyticsTracks,
5. [L8] 定义类型 `MobileLocationAnalyticsParams`
6. [L12] 执行：buildMobileAnalyticsDateRange,
7. [L13] 执行：toMobileAnalyticsUtcRange,
8. [L14] 定义类型 `MobileRangeShortcut`
9. [L16] 执行：export { formatAccuracyLabel } from '../components/mobile/locationFormatting';
10. [L18] 定义函数 `errorMessage`
11. [L19] 执行：if (error instanceof Error && error.message) return error.message;
12. [L20] 返回 error ? '历史位置加载失败，请稍后刷新。' : null
13. [L23] 默认导出函数 `HistoricalLocationPage`
14. [L24] Hook `useMemo` 绑定 `defaultRange`
15. [L25] 执行：const [rangeShortcut, setRangeShortcut] = useState<MobileRangeShortcut>('7d');
16. [L26] 执行：const [rangeStartDate, setRangeStartDate] = useState(defaultRange.startDate);
17. [L27] 执行：const [rangeEndDate, setRangeEndDate] = useState(defaultRange.endDate);
18. [L28] 执行：const [selectedDeviceId, setSelectedDeviceId] = useState('');
19. [L29] 执行：const [maxAccuracyMeters, setMaxAccuracyMeters] = useState(50);
20. [L30] 执行：const [includeRejected, setIncludeRejected] = useState(false);
21. [L31] 执行：const [selectedSegmentId, setSelectedSegmentId] = useState<string | null>(null);
22. [L32] 执行：const [selectedPointId, setSelectedPointId] = useState<string | null>(null);
23. [L33] 赋值 `deviceId` = selectedDeviceId || undefined
24. [L35] Hook `useMemo` 绑定 `utcRange`
25. [L36] 执行：() => toMobileAnalyticsUtcRange({ startDate: rangeStartDate, endDate: rangeEndDate }),
26. [L37] 执行：[rangeStartDate, rangeEndDate],
27. [L40] 使用 `useMemo` 缓存计算结果
28. [L42] 执行：...utcRange,
29. [L43] 执行：deviceId,
30. [L44] 执行：maxAccuracyMeters,
31. [L45] 执行：includeRejected,
32. [L47] 执行：[utcRange, deviceId, maxAccuracyMeters, includeRejected],
33. [L50] 赋值 `devicesQuery` = useQuery({
34. [L51] 执行：queryKey: ['mobile-devices'],
35. [L52] 执行：queryFn: getMobileDevices,
36. [L53] 执行：staleTime: 60000,
37. [L56] 赋值 `overviewQuery` = useQuery({
38. [L57] 执行：queryKey: ['mobile-location-analytics-overview', locationQuery],
39. [L58] 执行：queryFn: () => getMobileLocationAnalyticsOverview(locationQuery),
40. [L59] 执行：refetchInterval: 30000,
41. [L62] 赋值 `tracksQuery` = useQuery({
42. [L63] 执行：queryKey: ['mobile-location-analytics-tracks', locationQuery],
43. [L64] 执行：queryFn: () => getMobileLocationAnalyticsTracks(locationQuery),
44. [L65] 执行：refetchInterval: 30000,
45. [L68] Hook `useMemo` 绑定 `tracks`
46. [L69] Hook `useMemo` 绑定 `segments`
47. [L70] 赋值 `effectiveSelectedSegmentId` = selectedSegmentId && segments.some(segment => segment.id === selectedSegmentId)
48. [L71] 执行：? selectedSegmentId
49. [L72] 执行：: segments[0]?.id ?? null;
50. [L74] 赋值 `pointsQuery` = useQuery({
51. [L75] 执行：queryKey: ['mobile-location-analytics-segment-points', effectiveSelectedSegmentId, locationQuery],
52. [L76] 执行：queryFn: () => getMobileLocationAnalyticsSegmentPoints(effectiveSelectedSegmentId!, {
53. [L77] 执行：...locationQuery,
54. [L78] 执行：pageSize: 100,
55. [L80] 执行：enabled: Boolean(effectiveSelectedSegmentId),
56. [L81] 执行：refetchInterval: 30000,
57. [L84] Hook `useMemo` 绑定 `points`
58. [L85] 赋值 `effectiveSelectedPointId` = selectedPointId && points.some(point => point.id === selectedPointId)
59. [L86] 执行：? selectedPointId
60. [L87] 执行：: points[0]?.id ?? null;
61. [L89] 定义函数 `applyShortcut`
62. [L90] 赋值 `nextRange` = buildMobileAnalyticsDateRange(shortcut)
63. [L91] 更新状态 setRangeShortcut(nextRange.shortcut)
64. [L92] 更新状态 setRangeStartDate(nextRange.startDate)
65. [L93] 更新状态 setRangeEndDate(nextRange.endDate)
66. [L94] 更新状态 setSelectedSegmentId(null)
67. [L95] 更新状态 setSelectedPointId(null)
68. [L98] 定义函数 `applyCustomRange`
69. [L99] 更新状态 setRangeShortcut('custom')
70. [L100] 更新状态 setRangeStartDate(range.startDate)
71. [L101] 更新状态 setRangeEndDate(range.endDate)
72. [L102] 更新状态 setSelectedSegmentId(null)
73. [L103] 更新状态 setSelectedPointId(null)
74. [L106] 定义函数 `refresh`
75. [L107] 执行：void Promise.all([
76. [L108] 执行：devicesQuery.refetch(),
77. [L109] 执行：overviewQuery.refetch(),
78. [L110] 执行：tracksQuery.refetch(),
79. [L111] 执行：pointsQuery.refetch(),
80. [L115] 定义函数 `updateMaxAccuracy`
81. [L116] 更新状态 setMaxAccuracyMeters(Math.max(1, Math.round(value)))
82. [L117] 更新状态 setSelectedSegmentId(null)
83. [L118] 更新状态 setSelectedPointId(null)
84. [L121] 定义函数 `updateDevice`
85. [L122] 更新状态 setSelectedDeviceId(value)
86. [L123] 更新状态 setSelectedSegmentId(null)
87. [L124] 更新状态 setSelectedPointId(null)
88. [L127] 定义函数 `updateIncludeRejected`
89. [L128] 更新状态 setIncludeRejected(value)
90. [L129] 更新状态 setSelectedSegmentId(null)
91. [L130] 更新状态 setSelectedPointId(null)
92. [L133] 返回 JSX/结构
93. [L134] 执行：<HistoricalLocationDashboard
94. [L135] 执行：rangeShortcut={rangeShortcut}
95. [L136] 执行：rangeStartDate={rangeStartDate}
96. [L137] 执行：rangeEndDate={rangeEndDate}
97. [L138] 执行：selectedDeviceId={selectedDeviceId}
98. [L139] 执行：devices={devicesQuery.data ?? []}
99. [L140] 执行：maxAccuracyMeters={maxAccuracyMeters}
100. [L141] 执行：includeRejected={includeRejected}
101. [L142] 执行：overview={overviewQuery.data}
102. [L143] 执行：tracks={tracks}
103. [L144] 执行：selectedSegmentId={effectiveSelectedSegmentId}
104. [L145] 执行：selectedPointId={effectiveSelectedPointId}
105. [L146] 执行：points={points}
106. [L147] 执行：isLoading={devicesQuery.isLoading || overviewQuery.isLoading || tracksQuery.isLoading}
107. [L148] 执行：isFetching={devicesQuery.isFetching || overviewQuery.isFetching || tracksQuery.isFetching || pointsQuery.isFet
108. [L149] 执行：errorMessage={errorMessage(devicesQuery.error) ?? errorMessage(overviewQuery.error) ?? errorMessage(tracksQuer
109. [L150] 执行：onShortcutChange={applyShortcut}
110. [L151] 执行：onCustomRangeChange={applyCustomRange}
111. [L152] 执行：onDeviceChange={updateDevice}
112. [L153] 执行：onMaxAccuracyChange={updateMaxAccuracy}
113. [L154] 执行：onIncludeRejectedChange={updateIncludeRejected}
114. [L155] 执行：onRefresh={refresh}
115. [L156] 执行：onSelectSegment={setSelectedSegmentId}
116. [L157] 执行：onSelectPoint={setSelectedPointId}

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/HistoricalLocationPage.tsx",
      "label": "HistoricalLocationPage",
      "path": "src/client-web/src/pages/HistoricalLocationPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/HistoricalLocationPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/HistoricalLocationPage.tsx",
      "to": "src/client-web/src/api/mobile.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/HistoricalLocationPage.tsx",
      "to": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/HistoricalLocationPage.tsx",
      "to": "src/client-web/src/components/mobile/mobileFormatting.ts",
      "type": "depends_on"
    }
  ]
}
```
