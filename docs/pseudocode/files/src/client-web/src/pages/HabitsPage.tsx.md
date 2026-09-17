# src/client-web/src/pages/HabitsPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `HabitsPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/components/schedule/HabitRoutineEditor.tsx`、`src/client-web/src/ui/PageHeader.tsx`、`src/client-web/src/ui/SegmentedControl.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### HabitsPage
#### HabitsPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `HabitsPage`
  2. 执行：const [tab, setTab] = useState<HabitTab>('active');
  3. 执行：const [cadence, setCadence] = useState('all');
  4. 执行：const [source, setSource] = useState('all');
  5. 赋值 `{ data: habits = [] }` = useQuery({
  6. 执行：queryKey: ['habits'],
  7. 执行：queryFn: getHabits,
  8. 赋值 `filteredHabits` = habits.filter(habit => {
  9. 赋值 `cadenceMatches` = cadence === 'all' || habit.cadence.toLowerCase() === cadence
  10. 赋值 `sourceMatches` = source === 'all' || habit.source.toLowerCase() === source
  11. 赋值 `archiveMatches` = tab === 'archive'
  12. 执行：? habit.status.toLowerCase() === 'archived'
  13. 执行：: tab === 'active'
  14. 执行：? habit.status.toLowerCase() !== 'archived'
  15. 返回 cadenceMatches && sourceMatches && archiveMatches
  16. 返回 JSX/结构
  17. 执行：<div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
  18. 执行：<PageHeader
  19. 执行：title="习惯中心"
  20. 执行：subtitle="管理习惯规则、完成历史、复盘指标与投射到日历的时间块。"
  21. 执行：actions={<SegmentedControl value={tab} options={habitTabs} onChange={setTab} ariaLabel="习惯视图" />}
  22. 执行：<HabitRoutineEditor />
  23. 执行：<section className="pim-panel p-4">
  24. 执行：<div className="grid grid-cols-1 gap-3 md:grid-cols-2">
  25. 执行：<span className="text-xs font-semibold text-slate-500">频率</span>
  26. 执行：<select value={cadence} onChange={event => setCadence(event.target.value)} className="mt-1 w-full rounded-lg b
  27. 执行：<option value="all">全部频率</option>
  28. 执行：<option value="daily">每天</option>
  29. 执行：<option value="weekly">每周</option>
  30. 执行：<option value="monthly">每月</option>
- 分支与异常：无显著分支
- 调用：HabitsPage、useState、useQuery、habits.filter、habit.cadence.toLowerCase、habit.source.toLowerCase、habit.status.toLowerCase、setCadence、setSource、filteredHabits.map

## 近逐行中文伪代码

1. [L8] 定义类型 `HabitTab`
2. [L10] 执行：const habitTabs: Array<{ value: HabitTab; label: string }> = [
3. [L11] 执行：{ value: 'active', label: '执行中' },
4. [L12] 执行：{ value: 'planning', label: '规划' },
5. [L13] 执行：{ value: 'archive', label: '归档' },
6. [L16] 默认导出函数 `HabitsPage`
7. [L17] 执行：const [tab, setTab] = useState<HabitTab>('active');
8. [L18] 执行：const [cadence, setCadence] = useState('all');
9. [L19] 执行：const [source, setSource] = useState('all');
10. [L20] 赋值 `{ data: habits = [] }` = useQuery({
11. [L21] 执行：queryKey: ['habits'],
12. [L22] 执行：queryFn: getHabits,
13. [L25] 赋值 `filteredHabits` = habits.filter(habit => {
14. [L26] 赋值 `cadenceMatches` = cadence === 'all' || habit.cadence.toLowerCase() === cadence
15. [L27] 赋值 `sourceMatches` = source === 'all' || habit.source.toLowerCase() === source
16. [L28] 赋值 `archiveMatches` = tab === 'archive'
17. [L29] 执行：? habit.status.toLowerCase() === 'archived'
18. [L30] 执行：: tab === 'active'
19. [L31] 执行：? habit.status.toLowerCase() !== 'archived'
20. [L33] 返回 cadenceMatches && sourceMatches && archiveMatches
21. [L36] 返回 JSX/结构
22. [L37] 执行：<div className="mx-auto w-full max-w-[1200px] space-y-4 pb-8">
23. [L38] 执行：<PageHeader
24. [L39] 执行：title="习惯中心"
25. [L40] 执行：subtitle="管理习惯规则、完成历史、复盘指标与投射到日历的时间块。"
26. [L41] 执行：actions={<SegmentedControl value={tab} options={habitTabs} onChange={setTab} ariaLabel="习惯视图" />}
27. [L44] 执行：<HabitRoutineEditor />
28. [L46] 执行：<section className="pim-panel p-4">
29. [L47] 执行：<div className="grid grid-cols-1 gap-3 md:grid-cols-2">
30. [L49] 执行：<span className="text-xs font-semibold text-slate-500">频率</span>
31. [L50] 执行：<select value={cadence} onChange={event => setCadence(event.target.value)} className="mt-1 w-full rounded-lg b
32. [L51] 执行：<option value="all">全部频率</option>
33. [L52] 执行：<option value="daily">每天</option>
34. [L53] 执行：<option value="weekly">每周</option>
35. [L54] 执行：<option value="monthly">每月</option>
36. [L55] 执行：</select>
37. [L58] 执行：<span className="text-xs font-semibold text-slate-500">来源</span>
38. [L59] 执行：<select value={source} onChange={event => setSource(event.target.value)} className="mt-1 w-full rounded-lg bor
39. [L60] 执行：<option value="all">全部来源</option>
40. [L61] 执行：<option value="manual">手动</option>
41. [L62] 执行：<option value="template">模板</option>
42. [L63] 执行：<option value="ai">智能</option>
43. [L64] 执行：</select>
44. [L67] 执行：</section>
45. [L69] 执行：<section className="pim-panel p-4">
46. [L70] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
47. [L71] 执行：<h2 className="text-sm font-semibold text-slate-950">习惯规则</h2>
48. [L72] 执行：<span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
49. [L73] 执行：{cadence} / {source}
50. [L76] 执行：<div className="mt-4 grid gap-2">
51. [L77] 执行：{filteredHabits.map(habit => (
52. [L78] 执行：<article key={habit.id} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
53. [L79] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
54. [L80] 执行：<h3 className="text-sm font-semibold text-slate-900">{habit.title}</h3>
55. [L81] 执行：<span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-500">
56. [L82] 执行：{habit.cadence} · {habit.status}
57. [L85] 执行：<p className="mt-2 text-xs text-slate-500">规则变更会进入确认中心，避免误改长期习惯事实。</p>
58. [L86] 执行：</article>
59. [L88] 执行：{filteredHabits.length === 0 && (
60. [L89] 执行：<p className="rounded-lg border border-dashed border-slate-200 px-3 py-10 text-center text-sm text-slate-500">
61. [L90] 执行：当前筛选下没有习惯记录。
62. [L94] 执行：</section>
63. [L96] 执行：<div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
64. [L97] 执行：<section className="pim-panel p-4">
65. [L98] 执行：<h2 className="text-sm font-semibold text-slate-950">完成历史</h2>
66. [L99] 执行：<p className="mt-3 text-sm text-slate-500">
67. [L100] 执行：完成记录、漏打卡、连续天数与复盘指标会在这里汇总。
68. [L102] 执行：</section>
69. [L104] 执行：<section className="pim-panel p-4">
70. [L105] 执行：<h2 className="text-sm font-semibold text-slate-950">投射到日历</h2>
71. [L106] 执行：<p className="mt-3 text-sm text-slate-500">
72. [L107] 执行：习惯规则会生成日历图层，可请求生成任务或检查项，并对规则变更发起确认。
73. [L109] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/HabitsPage.tsx",
      "label": "HabitsPage",
      "path": "src/client-web/src/pages/HabitsPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/HabitsPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/HabitsPage.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/HabitsPage.tsx",
      "to": "src/client-web/src/components/schedule/HabitRoutineEditor.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/HabitsPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/pages/HabitsPage.tsx",
      "to": "src/client-web/src/ui/SegmentedControl.tsx",
      "type": "depends_on"
    }
  ]
}
```
