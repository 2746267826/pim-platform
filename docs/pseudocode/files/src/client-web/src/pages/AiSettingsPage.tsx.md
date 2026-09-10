# src/client-web/src/pages/AiSettingsPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `AiSettingsPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/ai.ts`、`src/client-web/src/components/ai/AiRequestDetailPanel.tsx`、`src/client-web/src/components/ai/AiRequestLogTable.tsx`、`src/client-web/src/components/ai/AiStatusPanel.tsx`、`src/client-web/src/components/ai/AiUsageOverview.tsx`、`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### asError
#### asError(error: unknown)
- 输入：error: unknown
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `asError`
  2. 返回 error instanceof Error ? error : null
- 分支与异常：无显著分支
- 调用：asError

### AiSettingsPage
#### AiSettingsPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `AiSettingsPage`
  2. 执行：const [selectedId, setSelectedId] = useState<string | null>(null);
  3. 赋值 `statusQuery` = useQuery({
  4. 执行：queryKey: ['ai-status'],
  5. 执行：queryFn: getAiStatus,
  6. 执行：refetchInterval: 60_000,
  7. 赋值 `usageQuery` = useQuery({
  8. 执行：queryKey: ['ai-usage-summary'],
  9. 执行：queryFn: getAiUsageSummary,
  10. 赋值 `requestsQuery` = useQuery({
  11. 执行：queryKey: ['ai-requests', requestFilters],
  12. 执行：queryFn: () => getAiRequests(requestFilters),
  13. 执行：refetchInterval: 30_000,
  14. 注册 `useEffect` 副作用
  15. 若 (!selectedId && requestsQuery.data?.items.length) 则
  16. 更新状态 setSelectedId(requestsQuery.data.items[0].id)
  17. 赋值 `detailQuery` = useQuery({
  18. 执行：queryKey: ['ai-request-detail', selectedId],
  19. 执行：queryFn: () => getAiRequestDetail(selectedId as string),
  20. 执行：enabled: !!selectedId,
  21. 返回 JSX/结构
  22. 执行：<div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
  23. 执行：<PageHeader title="AI 设置" subtitle="LiteLLM 状态、用量、请求日志与详情" />
  24. 执行：<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
  25. 执行：<AiStatusPanel
  26. 执行：status={statusQuery.data}
  27. 执行：isLoading={statusQuery.isLoading}
  28. 执行：error={asError(statusQuery.error)}
  29. 执行：<AiUsageOverview
  30. 执行：summary={usageQuery.data}
- 分支与异常：if (!selectedId && requestsQuery.data?.items.length) {
- 调用：AiSettingsPage、useQuery、getAiRequests、useEffect、setSelectedId、getAiRequestDetail、minmax、_minmax、asError

## 近逐行中文伪代码

1. [L4] 执行：getAiRequestDetail,
2. [L5] 执行：getAiRequests,
3. [L6] 执行：getAiStatus,
4. [L7] 执行：getAiUsageSummary,
5. [L15] 赋值 `requestFilters` = { page: 1, pageSize: 50 }
6. [L17] 定义函数 `asError`
7. [L18] 返回 error instanceof Error ? error : null
8. [L21] 默认导出函数 `AiSettingsPage`
9. [L22] 执行：const [selectedId, setSelectedId] = useState<string | null>(null);
10. [L24] 赋值 `statusQuery` = useQuery({
11. [L25] 执行：queryKey: ['ai-status'],
12. [L26] 执行：queryFn: getAiStatus,
13. [L27] 执行：refetchInterval: 60_000,
14. [L30] 赋值 `usageQuery` = useQuery({
15. [L31] 执行：queryKey: ['ai-usage-summary'],
16. [L32] 执行：queryFn: getAiUsageSummary,
17. [L33] 执行：refetchInterval: 60_000,
18. [L36] 赋值 `requestsQuery` = useQuery({
19. [L37] 执行：queryKey: ['ai-requests', requestFilters],
20. [L38] 执行：queryFn: () => getAiRequests(requestFilters),
21. [L39] 执行：refetchInterval: 30_000,
22. [L42] 注册 `useEffect` 副作用
23. [L43] 若 (!selectedId && requestsQuery.data?.items.length) 则
24. [L44] 更新状态 setSelectedId(requestsQuery.data.items[0].id)
25. [L48] 赋值 `detailQuery` = useQuery({
26. [L49] 执行：queryKey: ['ai-request-detail', selectedId],
27. [L50] 执行：queryFn: () => getAiRequestDetail(selectedId as string),
28. [L51] 执行：enabled: !!selectedId,
29. [L54] 返回 JSX/结构
30. [L55] 执行：<div className="mx-auto w-full max-w-[1500px] space-y-4 pb-8">
31. [L56] 执行：<PageHeader title="AI 设置" subtitle="LiteLLM 状态、用量、请求日志与详情" />
32. [L58] 执行：<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
33. [L59] 执行：<AiStatusPanel
34. [L60] 执行：status={statusQuery.data}
35. [L61] 执行：isLoading={statusQuery.isLoading}
36. [L62] 执行：error={asError(statusQuery.error)}
37. [L64] 执行：<AiUsageOverview
38. [L65] 执行：summary={usageQuery.data}
39. [L66] 执行：isLoading={usageQuery.isLoading || usageQuery.isFetching}
40. [L67] 执行：error={asError(usageQuery.error)}
41. [L71] 执行：<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.85fr)]">
42. [L72] 执行：<AiRequestLogTable
43. [L73] 执行：data={requestsQuery.data}
44. [L74] 执行：selectedId={selectedId}
45. [L75] 执行：isLoading={requestsQuery.isLoading}
46. [L76] 执行：error={asError(requestsQuery.error)}
47. [L77] 执行：onSelect={setSelectedId}
48. [L79] 执行：<AiRequestDetailPanel
49. [L80] 执行：detail={detailQuery.data}
50. [L81] 执行：isLoading={detailQuery.isLoading}
51. [L82] 执行：error={asError(detailQuery.error)}

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/AiSettingsPage.tsx",
      "label": "AiSettingsPage",
      "path": "src/client-web/src/pages/AiSettingsPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/AiSettingsPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/AiSettingsPage.tsx",
      "to": "src/client-web/src/api/ai.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AiSettingsPage.tsx",
      "to": "src/client-web/src/components/ai/AiRequestDetailPanel.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AiSettingsPage.tsx",
      "to": "src/client-web/src/components/ai/AiRequestLogTable.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AiSettingsPage.tsx",
      "to": "src/client-web/src/components/ai/AiStatusPanel.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AiSettingsPage.tsx",
      "to": "src/client-web/src/components/ai/AiUsageOverview.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/AiSettingsPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
