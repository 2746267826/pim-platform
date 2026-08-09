# src/client-web/src/layout/AppLayout.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：`AppLayout`：见源文件职责（AppLayout.tsx）。
- 主要依赖：`src/client-web/src/auth/AuthContext.tsx`、`src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx`、`src/client-web/src/context/CalendarVisibilityContext.tsx`、`src/client-web/src/layout/Sidebar.tsx`、`src/client-web/src/pages/AiSettingsPage.tsx`、`src/client-web/src/pages/AppKnowledgeBasePage.tsx`、`src/client-web/src/pages/CalendarDataManager.tsx`、`src/client-web/src/pages/CalendarPage.tsx`、`src/client-web/src/pages/CategoryTreePage.tsx`、`src/client-web/src/pages/PcDetailQueryPage.tsx`、`src/client-web/src/pages/PcTrackerPage.tsx`、`src/client-web/src/pages/RecycleBinPage.tsx` 等共 17 项
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### SuspenseFallback
#### SuspenseFallback(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `SuspenseFallback`
  2. 返回 JSX/结构
- 分支与异常：无显著分支
- 调用：SuspenseFallback

### AppLayout
#### AppLayout(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `AppLayout`
  2. 赋值 `{ isAuthenticated }` = useAuth()
  3. 赋值 `location` = useLocation()
  4. 执行：const [quickNoteOpen, setQuickNoteOpen] = useState(false);
  5. 若 (!isAuthenticated) 则
  6. 返回 JSX/结构
  7. 赋值 `showCalendarInbox` = location.pathname === '/calendar' || location.pathname.startsWith('/calendar/')
  8. 执行：<CalendarVisibilityProvider>
  9. 执行：<div className="pim-shell h-screen flex overflow-hidden">
  10. 执行：<Sidebar />
  11. 执行：<main className="pim-route-surface flex-1 overflow-auto p-4">
  12. 执行：<Suspense fallback={<SuspenseFallback />}>
  13. 执行：<Route path="/today" element={<TodayPage />} />
  14. 执行：<Route path="/calendar" element={<CalendarPage />} />
  15. 执行：<Route path="/workbench" element={<WorkbenchPage />} />
  16. 执行：<Route path="/sync" element={<Navigate to="/settings/sync" replace />} />
  17. 执行：<Route path="/data-center" element={<DataCenterPage />} />
  18. 执行：<Route path="/confirmations" element={<ConfirmationsPage />} />
  19. 执行：<Route path="/reminders" element={<RemindersPage />} />
  20. 执行：<Route path="/reports" element={<ReportsPage />} />
  21. 执行：<Route path="/habits" element={<HabitsPage />} />
  22. 执行：<Route path="/audit/:objectType/:objectId" element={<AuditTimelinePage />} />
  23. 执行：<Route path="/endpoint-shell" element={<EndpointShellPage />} />
  24. 执行：<Route path="/quick-notes" element={<QuickNotesPage />} />
  25. 执行：<Route path="/files" element={<FilesPage />} />
  26. 执行：<Route path="/timeline" element={<Navigate to="/calendar?view=timeline" replace />} />
  27. 执行：<Route path="/week" element={<Navigate to="/calendar?view=timeline" replace />} />
  28. 执行：<Route path="/month" element={<Navigate to="/calendar?view=month" replace />} />
  29. 执行：<Route path="/tasks" element={<TaskListPage />} />
  30. 执行：<Route path="/pc-tracker" element={<PcTrackerPage />} />
- 分支与异常：if (!isAuthenticated) {
- 调用：AppLayout、useAuth、useLocation、useState、location.pathname.startsWith、setQuickNoteOpen

## 近逐行中文伪代码

1. [L21] 赋值 `QuickNotesPage` = lazy(() => import('../pages/QuickNotesPage'))
2. [L22] 赋值 `FilesPage` = lazy(() => import('../pages/FilesPage'))
3. [L23] 赋值 `MobileRecordsPage` = lazy(() => import('../pages/MobileRecordsPage'))
4. [L24] 赋值 `HistoricalLocationPage` = lazy(() => import('../pages/HistoricalLocationPage'))
5. [L25] 赋值 `QuickNoteFloatingPanel` = lazy(() => import('../components/quick-notes/QuickNoteFloatingPanel'))
6. [L26] 赋值 `WorkbenchPage` = lazy(() => import('../pages/WorkbenchPage'))
7. [L27] 赋值 `SyncPage` = lazy(() => import('../pages/SyncPage'))
8. [L28] 赋值 `DataCenterPage` = lazy(() => import('../pages/DataCenterPage'))
9. [L29] 赋值 `ConfirmationsPage` = lazy(() => import('../pages/ConfirmationsPage'))
10. [L30] 赋值 `RemindersPage` = lazy(() => import('../pages/RemindersPage'))
11. [L31] 赋值 `ReportsPage` = lazy(() => import('../pages/ReportsPage'))
12. [L32] 赋值 `HabitsPage` = lazy(() => import('../pages/HabitsPage'))
13. [L33] 赋值 `AuditTimelinePage` = lazy(() => import('../pages/AuditTimelinePage'))
14. [L34] 赋值 `EndpointShellPage` = lazy(() => import('../pages/EndpointShellPage'))
15. [L36] 定义函数 `SuspenseFallback`
16. [L37] 返回 JSX/结构
17. [L40] 默认导出函数 `AppLayout`
18. [L41] 赋值 `{ isAuthenticated }` = useAuth()
19. [L42] 赋值 `location` = useLocation()
20. [L43] 执行：const [quickNoteOpen, setQuickNoteOpen] = useState(false);
21. [L45] 若 (!isAuthenticated) 则
22. [L46] 返回 JSX/结构
23. [L49] 赋值 `showCalendarInbox` = location.pathname === '/calendar' || location.pathname.startsWith('/calendar/')
24. [L51] 返回 JSX/结构
25. [L52] 执行：<CalendarVisibilityProvider>
26. [L53] 执行：<div className="pim-shell h-screen flex overflow-hidden">
27. [L54] 执行：<Sidebar />
28. [L55] 执行：<main className="pim-route-surface flex-1 overflow-auto p-4">
29. [L56] 执行：<Suspense fallback={<SuspenseFallback />}>
30. [L58] 执行：<Route path="/today" element={<TodayPage />} />
31. [L59] 执行：<Route path="/calendar" element={<CalendarPage />} />
32. [L60] 执行：<Route path="/workbench" element={<WorkbenchPage />} />
33. [L61] 执行：<Route path="/sync" element={<Navigate to="/settings/sync" replace />} />
34. [L62] 执行：<Route path="/data-center" element={<DataCenterPage />} />
35. [L63] 执行：<Route path="/confirmations" element={<ConfirmationsPage />} />
36. [L64] 执行：<Route path="/reminders" element={<RemindersPage />} />
37. [L65] 执行：<Route path="/reports" element={<ReportsPage />} />
38. [L66] 执行：<Route path="/habits" element={<HabitsPage />} />
39. [L67] 执行：<Route path="/audit/:objectType/:objectId" element={<AuditTimelinePage />} />
40. [L68] 执行：<Route path="/endpoint-shell" element={<EndpointShellPage />} />
41. [L69] 执行：<Route path="/quick-notes" element={<QuickNotesPage />} />
42. [L70] 执行：<Route path="/files" element={<FilesPage />} />
43. [L71] 执行：<Route path="/timeline" element={<Navigate to="/calendar?view=timeline" replace />} />
44. [L72] 执行：<Route path="/week" element={<Navigate to="/calendar?view=timeline" replace />} />
45. [L73] 执行：<Route path="/month" element={<Navigate to="/calendar?view=month" replace />} />
46. [L74] 执行：<Route path="/tasks" element={<TaskListPage />} />
47. [L75] 执行：<Route path="/pc-tracker" element={<PcTrackerPage />} />
48. [L76] 执行：<Route path="/mobile-records" element={<MobileRecordsPage />} />
49. [L77] 执行：<Route path="/location-history" element={<HistoricalLocationPage />} />
50. [L78] 执行：<Route path="/status" element={<StatusPage />} />
51. [L79] 执行：<Route path="/settings" element={<SettingsPage />} />
52. [L80] 执行：<Route path="/settings/sync" element={<SyncPage />} />
53. [L81] 执行：<Route path="/settings/ai" element={<AiSettingsPage />} />
54. [L82] 执行：<Route path="/settings/calendar-data" element={<CalendarDataManager />} />
55. [L83] 执行：<Route path="/settings/recycle-bin" element={<RecycleBinPage />} />
56. [L84] 执行：<Route path="/settings/pc-data" element={<PcDetailQueryPage />} />
57. [L85] 执行：<Route path="/app-knowledge-base" element={<AppKnowledgeBasePage />} />
58. [L86] 执行：<Route path="/app-knowledge-base/categories" element={<CategoryTreePage />} />
59. [L87] 执行：<Route path="/pc-categories" element={<Navigate to="/app-knowledge-base/categories" replace />} />
60. [L88] 执行：<Route path="/pc-classification" element={<Navigate to="/app-knowledge-base" replace />} />
61. [L89] 执行：<Route path="*" element={<Navigate to="/today" replace />} />
62. [L90] 执行：</Routes>
63. [L91] 执行：</Suspense>
64. [L93] 执行：{showCalendarInbox && <InboxPanel draggable />}
65. [L94] 执行：<QuickNoteFloatingButton onClick={() => setQuickNoteOpen(true)} />
66. [L95] 执行：{quickNoteOpen && (
67. [L96] 执行：<Suspense fallback={null}>
68. [L97] 执行：<QuickNoteFloatingPanel onClose={() => setQuickNoteOpen(false)} />
69. [L98] 执行：</Suspense>
70. [L101] 执行：</CalendarVisibilityProvider>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/layout/AppLayout.tsx",
      "label": "AppLayout",
      "path": "src/client-web/src/layout/AppLayout.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/layout/AppLayout.tsx.md",
      "layer": "client-web",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/auth/AuthContext.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/context/CalendarVisibilityContext.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/layout/Sidebar.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/AiSettingsPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/AppKnowledgeBasePage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/CalendarDataManager.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/CalendarPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/CategoryTreePage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/PcDetailQueryPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/PcTrackerPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/RecycleBinPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/SettingsPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/StatusPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/TaskListPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/pages/TodayPage.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/layout/AppLayout.tsx",
      "to": "src/client-web/src/panels/InboxPanel.tsx",
      "type": "depends_on"
    }
  ]
}
```
