# PIM Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Web UI as a precise blue/teal productivity console and lightly polish the Windows daemon status UI.

**Architecture:** Add a small shared UI layer for tokens, layout primitives, cards, badges, drawers, and drag states, then rebuild pages on top of those primitives. Keep existing REST APIs and data contracts; use current calendar/task/PC tracker queries. The Windows daemon remains lightweight WPF/WinForms, with better status diagnosis copy and expandable error details.

**Tech Stack:** React 19, TypeScript, Vite, Tailwind CSS 4, @tanstack/react-query, FullCalendar, WPF .NET 8, Windows Forms NotifyIcon.

---

## Scope Decision

This plan intentionally covers both Web UI and Windows daemon UI because they share status language and visual tokens, but implementation should happen in order: Web foundation first, Web product pages second, Windows daemon last. If execution time is limited, stop after Task 6; Tasks 7-8 are polish tracks that do not block the new Web shell.

## File Structure

Create:
- `src/client-web/src/ui/PageHeader.tsx` - Shared page title, subtitle, actions, and filters.
- `src/client-web/src/ui/SegmentedControl.tsx` - Timeline/month and other two-state view switches.
- `src/client-web/src/ui/StatusBadge.tsx` - Consistent status chips for scheduled, due, error, normal, pending.
- `src/client-web/src/ui/MetricCard.tsx` - Compact metric cards used by Today and PC pages.
- `src/client-web/src/ui/EmptyState.tsx` - Unified empty, loading, and error states.
- `src/client-web/src/ui/EditorDrawer.tsx` - Right-side drawer shell for event/task editors.
- `src/client-web/src/components/today/TodayScheduleList.tsx` - Left column of Today page.
- `src/client-web/src/components/today/TodayPcOverview.tsx` - Center PC overview of Today page.
- `src/client-web/src/components/today/TodayTaskColumn.tsx` - Right task column of Today page.
- `src/client-web/src/pages/TodayPage.tsx` - Default `/today` route.
- `src/client-web/src/pages/CalendarPage.tsx` - Single calendar route with timeline/month segmented view and external task drag.
- `src/client-windows/Pim.Client.App/Styles/Theme.xaml` - Lightweight WPF styles and color resources.

Modify:
- `src/client-web/src/index.css` - Design tokens, layout primitives, FullCalendar overrides, drag/drop classes.
- `src/client-web/src/App.tsx` - Redirect `/` to `/today`.
- `src/client-web/src/layout/AppLayout.tsx` - Route table and page-specific right panel behavior.
- `src/client-web/src/layout/Sidebar.tsx` - New navigation order and calmer sidebar visual hierarchy.
- `src/client-web/src/panels/InboxPanel.tsx` - Reusable draggable task cards for calendar pages.
- `src/client-web/src/dialogs/common.tsx` - Keep `Dialog` only for destructive confirms if needed; add shared field classes if retained.
- `src/client-web/src/dialogs/EventEditorDialog.tsx` - Render event editor in `EditorDrawer`.
- `src/client-web/src/dialogs/TaskEditorDialog.tsx` - Render task editor in `EditorDrawer`, accept drag-proposed schedule defaults.
- `src/client-web/src/pages/PcTrackerPage.tsx` - Convert full PC page to overview drilldown layout.
- `src/client-web/src/pages/TaskListPage.tsx` - Align task cards, due-first sorting, scheduled tags.
- `src/client-web/src/auth/LoginPage.tsx` - Light visual unification.
- `src/client-web/src/pages/SettingsPage.tsx` - Light visual unification.
- `src/client-windows/Pim.Client.App/App.xaml` - Load `Styles/Theme.xaml`.
- `src/client-windows/Pim.Client.App/StatusWindow.xaml` - Status card layout.
- `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs` - Diagnostic summaries and expandable raw error text.
- `src/client-windows/Pim.Client.App/TrayIcon.cs` - Clearer tooltip/menu status copy.

---

### Task 1: Web Design Tokens and Core UI Primitives

**Files:**
- Modify: `src/client-web/src/index.css`
- Create: `src/client-web/src/ui/PageHeader.tsx`
- Create: `src/client-web/src/ui/SegmentedControl.tsx`
- Create: `src/client-web/src/ui/StatusBadge.tsx`
- Create: `src/client-web/src/ui/MetricCard.tsx`
- Create: `src/client-web/src/ui/EmptyState.tsx`

- [ ] **Step 1: Add design tokens and base utility classes**

Replace `src/client-web/src/index.css` with tokenized CSS that still imports Tailwind:

```css
@import "tailwindcss";

:root {
  color-scheme: light;
  font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans SC", sans-serif;
  --pim-bg: #f6f8fb;
  --pim-surface: #ffffff;
  --pim-surface-muted: #f8fafc;
  --pim-border: #dfe7f1;
  --pim-border-soft: #e2e8f0;
  --pim-text: #0f172a;
  --pim-text-muted: #64748b;
  --pim-primary: #2563eb;
  --pim-primary-soft: #dbeafe;
  --pim-activity: #14b8a6;
  --pim-activity-soft: #ccfbf1;
  --pim-warning: #f59e0b;
  --pim-warning-soft: #fef3c7;
  --pim-danger: #ef4444;
  --pim-danger-soft: #fee2e2;
  --pim-radius-sm: 8px;
  --pim-radius-md: 12px;
  --pim-radius-lg: 16px;
  --pim-shadow-soft: 0 18px 42px rgba(15, 23, 42, 0.1);
}

html,
body,
#root {
  min-height: 100%;
}

body {
  margin: 0;
  background: var(--pim-bg);
  color: var(--pim-text);
}

button,
input,
select,
textarea {
  font: inherit;
}

.pim-shell {
  min-height: 100vh;
  background: var(--pim-bg);
}

.pim-panel {
  background: var(--pim-surface);
  border: 1px solid var(--pim-border);
  border-radius: var(--pim-radius-lg);
}

.pim-card {
  background: var(--pim-surface);
  border: 1px solid var(--pim-border);
  border-radius: var(--pim-radius-md);
}

.pim-button-primary {
  background: var(--pim-primary);
  color: #ffffff;
  border-radius: 10px;
  transition: background-color 140ms ease, transform 140ms ease;
}

.pim-button-primary:hover {
  background: #1d4ed8;
}

.pim-button-secondary {
  background: #ffffff;
  color: var(--pim-text);
  border: 1px solid var(--pim-border);
  border-radius: 10px;
  transition: border-color 140ms ease, background-color 140ms ease;
}

.pim-button-secondary:hover {
  border-color: #bfdbfe;
  background: #f8fafc;
}

.pim-drop-target {
  border: 2px dashed var(--pim-activity);
  background: rgba(20, 184, 166, 0.08);
}

.pim-drag-card {
  box-shadow: var(--pim-shadow-soft);
  transform: rotate(-1deg);
}

.fc {
  --fc-border-color: var(--pim-border-soft);
  --fc-today-bg-color: rgba(37, 99, 235, 0.08);
  --fc-neutral-bg-color: #f8fafc;
  font-family: inherit;
}

.fc .fc-toolbar {
  display: none;
}

.fc .fc-timegrid-slot,
.fc .fc-scrollgrid,
.fc .fc-col-header-cell,
.fc .fc-daygrid-day {
  border-color: var(--pim-border-soft);
}

.fc .fc-event {
  border-radius: 10px;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: var(--pim-text);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
}
```

- [ ] **Step 2: Create shared `PageHeader`**

Create `src/client-web/src/ui/PageHeader.tsx`:

```tsx
import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  beforeActions?: ReactNode;
  actions?: ReactNode;
}

export default function PageHeader({ title, subtitle, beforeActions, actions }: PageHeaderProps) {
  return (
    <header className="pim-panel px-4 py-3 flex flex-wrap items-center justify-between gap-3">
      <div className="min-w-0">
        <h1 className="text-lg font-semibold text-slate-950 truncate">{title}</h1>
        {subtitle && <p className="text-sm text-slate-500 mt-0.5 truncate">{subtitle}</p>}
      </div>
      <div className="flex items-center gap-2">
        {beforeActions}
        {actions}
      </div>
    </header>
  );
}
```

- [ ] **Step 3: Create shared `SegmentedControl`**

Create `src/client-web/src/ui/SegmentedControl.tsx`:

```tsx
interface SegmentedOption<T extends string> {
  value: T;
  label: string;
}

interface SegmentedControlProps<T extends string> {
  value: T;
  options: SegmentedOption<T>[];
  onChange: (value: T) => void;
  ariaLabel: string;
}

export default function SegmentedControl<T extends string>({
  value,
  options,
  onChange,
  ariaLabel,
}: SegmentedControlProps<T>) {
  return (
    <div className="inline-flex rounded-xl border border-slate-200 bg-slate-100 p-1" role="radiogroup" aria-label={ariaLabel}>
      {options.map(option => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          onClick={() => onChange(option.value)}
          className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
            value === option.value
              ? 'bg-blue-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-white'
          }`}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
```

- [ ] **Step 4: Create shared `StatusBadge`**

Create `src/client-web/src/ui/StatusBadge.tsx`:

```tsx
type StatusTone = 'primary' | 'activity' | 'warning' | 'danger' | 'neutral';

const toneClass: Record<StatusTone, string> = {
  primary: 'bg-blue-100 text-blue-700 border-blue-200',
  activity: 'bg-teal-100 text-teal-700 border-teal-200',
  warning: 'bg-amber-100 text-amber-800 border-amber-200',
  danger: 'bg-red-100 text-red-700 border-red-200',
  neutral: 'bg-slate-100 text-slate-600 border-slate-200',
};

export default function StatusBadge({ children, tone = 'neutral' }: { children: React.ReactNode; tone?: StatusTone }) {
  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium ${toneClass[tone]}`}>
      {children}
    </span>
  );
}
```

- [ ] **Step 5: Create `MetricCard` and `EmptyState`**

Create `src/client-web/src/ui/MetricCard.tsx`:

```tsx
type MetricTone = 'primary' | 'activity' | 'warning' | 'danger' | 'neutral';

const valueClass: Record<MetricTone, string> = {
  primary: 'text-blue-600',
  activity: 'text-teal-600',
  warning: 'text-amber-600',
  danger: 'text-red-600',
  neutral: 'text-slate-950',
};

export default function MetricCard({
  label,
  value,
  helper,
  tone = 'neutral',
}: {
  label: string;
  value: React.ReactNode;
  helper?: React.ReactNode;
  tone?: MetricTone;
}) {
  return (
    <section className="pim-card p-4 min-w-0">
      <p className="text-xs text-slate-500 mb-2 truncate">{label}</p>
      <div className={`text-xl font-semibold ${valueClass[tone]}`}>{value}</div>
      {helper && <p className="text-xs text-slate-400 mt-2 truncate">{helper}</p>}
    </section>
  );
}
```

Create `src/client-web/src/ui/EmptyState.tsx`:

```tsx
export default function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="pim-card p-6 text-center">
      <p className="text-sm font-medium text-slate-700">{title}</p>
      {description && <p className="text-sm text-slate-500 mt-1">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
```

- [ ] **Step 6: Verify build**

Run:

```powershell
npm run build
```

from `src/client-web`.

Expected: TypeScript build and Vite build complete without errors.

- [ ] **Step 7: Commit**

```powershell
git add src/client-web/src/index.css src/client-web/src/ui
git commit -m "feat(web): add visual design primitives"
```

---

### Task 2: Shell Navigation and Page-Specific Right Panel

**Files:**
- Modify: `src/client-web/src/App.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/panels/InboxPanel.tsx`

- [ ] **Step 1: Update app default route**

Modify `src/client-web/src/App.tsx` so `/` redirects to `/today`:

```tsx
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import LoginPage from './auth/LoginPage'
import AppLayout from './layout/AppLayout'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<Navigate to="/today" replace />} />
        <Route path="/*" element={<AppLayout />} />
      </Routes>
    </AuthProvider>
  )
}
```

- [ ] **Step 2: Update `AppLayout` route table**

Modify `src/client-web/src/layout/AppLayout.tsx` to add `/today`, `/calendar`, and legacy redirects:

```tsx
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { CalendarVisibilityProvider } from '../context/CalendarVisibilityContext';
import Sidebar from './Sidebar';
import InboxPanel from '../panels/InboxPanel';
import TodayPage from '../pages/TodayPage';
import CalendarPage from '../pages/CalendarPage';
import TaskListPage from '../pages/TaskListPage';
import PcTrackerPage from '../pages/PcTrackerPage';
import SettingsPage from '../pages/SettingsPage';
import CalendarDataManager from '../pages/CalendarDataManager';
import PcDetailQueryPage from '../pages/PcDetailQueryPage';

export default function AppLayout() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const showCalendarInbox = location.pathname === '/calendar';

  return (
    <CalendarVisibilityProvider>
      <div className="pim-shell h-screen flex overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-auto p-4">
          <Routes>
            <Route path="/today" element={<TodayPage />} />
            <Route path="/calendar" element={<CalendarPage />} />
            <Route path="/timeline" element={<Navigate to="/calendar?view=timeline" replace />} />
            <Route path="/week" element={<Navigate to="/calendar?view=timeline" replace />} />
            <Route path="/month" element={<Navigate to="/calendar?view=month" replace />} />
            <Route path="/tasks" element={<TaskListPage />} />
            <Route path="/pc-tracker" element={<PcTrackerPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/settings/calendar-data" element={<CalendarDataManager />} />
            <Route path="/settings/pc-data" element={<PcDetailQueryPage />} />
            <Route path="*" element={<Navigate to="/today" replace />} />
          </Routes>
        </main>
        {showCalendarInbox && <InboxPanel draggable />}
      </div>
    </CalendarVisibilityProvider>
  );
}
```

- [ ] **Step 3: Update `InboxPanel` props for draggable calendar mode**

Modify `src/client-web/src/panels/InboxPanel.tsx` so the component accepts `draggable?: boolean` and adds `data-task-id` on cards:

```tsx
interface InboxPanelProps {
  draggable?: boolean;
}

export default function InboxPanel({ draggable = false }: InboxPanelProps) {
  // keep existing state and queries
  // update each task card:
  // draggable={draggable}
  // data-task-id={task.id}
  // className={`js-draggable-task ... ${draggable ? 'cursor-grab active:cursor-grabbing' : 'cursor-pointer'}`}
}
```

When implementing this step, preserve the existing editor behavior and menu behavior. The only behavior change is that draggable mode marks task cards as external FullCalendar drag sources.

- [ ] **Step 4: Rebuild `Sidebar` navigation**

Modify `src/client-web/src/layout/Sidebar.tsx`:
- Navigation order: Today `/today`, Calendar `/calendar`, Tasks `/tasks`, PC Records `/pc-tracker`, Settings `/settings`.
- Remove emoji icons from labels.
- Use text labels and simple two-letter/one-letter icon placeholders if no icon package is installed.
- Keep calendar/task book sections but restyle them using blue active state, muted section headers, and color dots.

Use this `navItems` shape:

```tsx
const navItems = [
  { label: '今日', path: '/today', short: '今' },
  { label: '日历', path: '/calendar', short: '历' },
  { label: '任务', path: '/tasks', short: '任' },
  { label: 'PC记录', path: '/pc-tracker', short: 'PC' },
  { label: '设置', path: '/settings', short: '设' },
];
```

- [ ] **Step 5: Verify navigation**

Run:

```powershell
npm run build
```

Expected: build passes. Manual check after dev server starts: `/`, `/today`, `/calendar`, `/timeline`, `/month`, `/tasks`, `/pc-tracker`, `/settings` all route correctly.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/App.tsx src/client-web/src/layout src/client-web/src/panels/InboxPanel.tsx
git commit -m "feat(web): redesign shell navigation"
```

---

### Task 3: Today Workbench

**Files:**
- Create: `src/client-web/src/pages/TodayPage.tsx`
- Create: `src/client-web/src/components/today/TodayScheduleList.tsx`
- Create: `src/client-web/src/components/today/TodayPcOverview.tsx`
- Create: `src/client-web/src/components/today/TodayTaskColumn.tsx`

- [ ] **Step 1: Create Today schedule list**

Create `src/client-web/src/components/today/TodayScheduleList.tsx`:

```tsx
import StatusBadge from '../../ui/StatusBadge';
import type { EventResponse, TaskResponse } from '../../types';

type ScheduledItem =
  | { type: 'event'; id: string; title: string; start: string; end?: string; meta?: string; color?: string }
  | { type: 'task'; id: string; title: string; start: string; end?: string; meta?: string; priority: number };

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

function priorityBorder(priority: number) {
  if (priority === 1) return 'border-l-red-500';
  if (priority === 3) return 'border-l-teal-500';
  return 'border-l-amber-500';
}

export function buildScheduledItems(events: EventResponse[], tasks: TaskResponse[], datePrefix: string): ScheduledItem[] {
  const eventItems: ScheduledItem[] = events.map(event => ({
    type: 'event',
    id: event.id,
    title: event.title,
    start: event.dtStart,
    end: event.dtEnd,
    meta: event.location || event.description || '日程',
  }));

  const taskItems: ScheduledItem[] = tasks
    .filter(task => task.dtStart?.startsWith(datePrefix))
    .map(task => ({
      type: 'task',
      id: task.id,
      title: task.title,
      start: task.dtStart!,
      meta: task.description || '已排程任务',
      priority: task.priority,
    }));

  return [...eventItems, ...taskItems].sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
}

export default function TodayScheduleList({ items, onSelect }: { items: ScheduledItem[]; onSelect?: (item: ScheduledItem) => void }) {
  return (
    <section className="pim-panel p-4 min-w-0">
      <div className="flex items-center justify-between mb-3">
        <h2 className="font-semibold text-slate-900">今日日程</h2>
        <StatusBadge tone="neutral">{items.length} 项</StatusBadge>
      </div>
      <div className="space-y-2">
        {items.length === 0 ? (
          <p className="text-sm text-slate-400 py-6 text-center">今天还没有安排</p>
        ) : items.map(item => (
          <button
            key={`${item.type}-${item.id}`}
            type="button"
            onClick={() => onSelect?.(item)}
            className={`w-full text-left rounded-xl border bg-slate-50 p-3 border-l-4 ${
              item.type === 'task' ? priorityBorder(item.priority) : 'border-l-blue-500'
            } hover:bg-white transition-colors`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-sm font-medium text-slate-900 truncate">{item.title}</p>
                <p className="text-xs text-slate-500 mt-1 truncate">{item.meta}</p>
              </div>
              <div className="text-xs text-slate-500 whitespace-nowrap">{formatTime(item.start)}</div>
            </div>
          </button>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 2: Create Today PC overview**

Create `src/client-web/src/components/today/TodayPcOverview.tsx`:

```tsx
import MetricCard from '../../ui/MetricCard';
import EmptyState from '../../ui/EmptyState';
import type { PcSummaryResponse } from '../../types';

export default function TodayPcOverview({ data, isLoading }: { data?: PcSummaryResponse; isLoading: boolean }) {
  if (isLoading) {
    return <EmptyState title="正在加载 PC 记录" description="正在读取今天的输入和活动摘要" />;
  }

  if (!data || !data.metrics) {
    return <EmptyState title="暂无 PC 记录" description="守护程序上传数据后，这里会显示输入与专注概览" />;
  }

  const totalInput = (data.metrics.totalKeyPresses || 0) + (data.metrics.totalClicks || 0);

  return (
    <section className="pim-panel p-4 min-w-0">
      <div className="flex items-center justify-between gap-3 mb-4">
        <div>
          <h2 className="font-semibold text-slate-900">PC 记录</h2>
          <p className="text-sm text-slate-500">输入 + 专注摘要</p>
        </div>
        <a className="pim-button-secondary px-3 py-2 text-sm" href="/pc-tracker">查看详情</a>
      </div>
      <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 mb-4">
        <MetricCard label="活跃输入时长" value={data.metrics.activeInputDuration} tone="activity" />
        <MetricCard label="输入总量" value={totalInput.toLocaleString('zh-CN')} helper="按键 + 点击" tone="primary" />
        <MetricCard label="最专注应用" value={data.metrics.mostFocusedApp || '-'} tone="neutral" />
        <MetricCard label="切换频率" value={data.metrics.switchFrequency.toFixed(1)} helper="次 / 10 分钟" tone="warning" />
      </div>
      <div className="pim-card p-3">
        <p className="text-sm font-medium text-slate-700 mb-3">今日输入热力</p>
        <div className="grid grid-cols-12 gap-1.5">
          {Array.from({ length: 24 }).map((_, index) => {
            const bucket = data.heatmap.find(item => item.hour === index);
            const intensity = Math.min(1, (bucket?.intensityScore ?? 0) / 100);
            const backgroundColor = intensity > 0.66 ? '#14b8a6' : intensity > 0.33 ? '#5eead4' : intensity > 0 ? '#ccfbf1' : '#e2e8f0';
            return <div key={index} className="aspect-square rounded-md" style={{ backgroundColor }} title={`${index}:00`} />;
          })}
        </div>
      </div>
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-3 mt-4">
        <div className="pim-card p-3">
          <p className="text-sm font-medium text-slate-700 mb-2">活动分类</p>
          <div className="space-y-2">
            {data.categories.slice(0, 4).map(category => (
              <div key={category.categoryName} className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: category.color }} />
                <span className="text-xs text-slate-600 flex-1 truncate">{category.categoryName}</span>
                <span className="text-xs text-slate-400">{category.share.toFixed(0)}%</span>
              </div>
            ))}
          </div>
        </div>
        <div className="pim-card p-3">
          <p className="text-sm font-medium text-slate-700 mb-2">采集状态</p>
          <p className="text-xs text-slate-500">最近数据来自今日 PC 采集。若数据为空，请检查 Windows 守护程序。</p>
        </div>
      </div>
    </section>
  );
}
```

- [ ] **Step 3: Create Today task column**

Create `src/client-web/src/components/today/TodayTaskColumn.tsx`:

```tsx
import StatusBadge from '../../ui/StatusBadge';
import type { TaskResponse } from '../../types';

function dueTime(task: TaskResponse) {
  return task.due ? new Date(task.due).getTime() : Number.POSITIVE_INFINITY;
}

function priorityClass(priority: number) {
  if (priority === 1) return 'border-l-red-500';
  if (priority === 3) return 'border-l-teal-500';
  return 'border-l-amber-500';
}

function dueTone(task: TaskResponse): 'danger' | 'warning' | 'neutral' {
  if (!task.due) return 'neutral';
  const due = new Date(task.due);
  const today = new Date();
  const todayEnd = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
  if (due.getTime() < Date.now()) return 'danger';
  if (due.getTime() < todayEnd.getTime()) return 'warning';
  return 'neutral';
}

export function sortTasksByDue(tasks: TaskResponse[]) {
  return [...tasks].sort((a, b) => {
    const dueDelta = dueTime(a) - dueTime(b);
    if (dueDelta !== 0) return dueDelta;
    return a.title.localeCompare(b.title, 'zh-CN');
  });
}

export default function TodayTaskColumn({ tasks, onSelect }: { tasks: TaskResponse[]; onSelect?: (task: TaskResponse) => void }) {
  const sorted = sortTasksByDue(tasks).filter(task => task.status !== 'COMPLETED');

  return (
    <section className="pim-panel p-4 min-w-0">
      <div className="flex items-center justify-between mb-3">
        <h2 className="font-semibold text-slate-900">任务</h2>
        <StatusBadge tone="neutral">{sorted.length} 项</StatusBadge>
      </div>
      <div className="space-y-2">
        {sorted.map(task => (
          <button
            key={task.id}
            type="button"
            onClick={() => onSelect?.(task)}
            className={`w-full text-left rounded-xl border bg-slate-50 p-3 border-l-4 ${priorityClass(task.priority)} hover:bg-white transition-colors`}
          >
            <p className="text-sm font-medium text-slate-900 truncate">{task.title}</p>
            <div className="flex items-center gap-1.5 flex-wrap mt-2">
              {task.due && <StatusBadge tone={dueTone(task)}>截止 {new Date(task.due).toLocaleDateString('zh-CN')}</StatusBadge>}
              {task.dtStart && <StatusBadge tone="primary">已排程</StatusBadge>}
              {!task.dtStart && <StatusBadge tone="warning">未排程</StatusBadge>}
            </div>
          </button>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: Create `TodayPage`**

Create `src/client-web/src/pages/TodayPage.tsx`:

```tsx
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { getEvents, getTasks } from '../api/calendar';
import { getPcSummary } from '../api/pcTracker';
import PageHeader from '../ui/PageHeader';
import TodayScheduleList, { buildScheduledItems } from '../components/today/TodayScheduleList';
import TodayPcOverview from '../components/today/TodayPcOverview';
import TodayTaskColumn from '../components/today/TodayTaskColumn';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import type { EventResponse, TaskResponse } from '../types';

export default function TodayPage() {
  const today = new Date();
  const dateStr = format(today, 'yyyy-MM-dd');
  const tomorrowStr = format(new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1), 'yyyy-MM-dd');
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();
  const [editingEvent, setEditingEvent] = useState<EventResponse | undefined>();

  const { data: events = [] } = useQuery({
    queryKey: ['events', dateStr, tomorrowStr],
    queryFn: () => getEvents(dateStr, tomorrowStr),
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks(),
  });

  const { data: pcSummary, isLoading: pcLoading } = useQuery({
    queryKey: ['pc-summary', dateStr],
    queryFn: () => getPcSummary(dateStr),
    refetchInterval: 30000,
  });

  const scheduleItems = buildScheduledItems(events, tasks, dateStr);

  return (
    <div className="max-w-[1600px] mx-auto space-y-4">
      <PageHeader
        title="今日工作台"
        subtitle={today.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })}
        actions={<button className="pim-button-primary px-4 py-2 text-sm">新建</button>}
      />
      <div className="grid grid-cols-1 xl:grid-cols-4 gap-4">
        <TodayScheduleList
          items={scheduleItems}
          onSelect={item => {
            if (item.type === 'event') setEditingEvent(events.find(event => event.id === item.id));
            else setEditingTask(tasks.find(task => task.id === item.id));
          }}
        />
        <div className="xl:col-span-2">
          <TodayPcOverview data={pcSummary} isLoading={pcLoading} />
        </div>
        <TodayTaskColumn tasks={tasks} onSelect={setEditingTask} />
      </div>
      <TaskEditorDialog open={Boolean(editingTask)} onClose={() => setEditingTask(undefined)} task={editingTask} />
      <EventEditorDialog open={Boolean(editingEvent)} onClose={() => setEditingEvent(undefined)} event={editingEvent} />
    </div>
  );
}
```

- [ ] **Step 5: Verify build**

Run:

```powershell
npm run build
```

Expected: build passes. Manual check: `/today` renders three columns on desktop and stacks on narrow screens.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/pages/TodayPage.tsx src/client-web/src/components/today
git commit -m "feat(web): add today workbench"
```

---

### Task 4: Editor Drawer

**Files:**
- Create: `src/client-web/src/ui/EditorDrawer.tsx`
- Modify: `src/client-web/src/dialogs/EventEditorDialog.tsx`
- Modify: `src/client-web/src/dialogs/TaskEditorDialog.tsx`

- [ ] **Step 1: Create drawer shell**

Create `src/client-web/src/ui/EditorDrawer.tsx`:

```tsx
import type { ReactNode } from 'react';

export default function EditorDrawer({
  open,
  title,
  subtitle,
  onClose,
  children,
  footer,
}: {
  open: boolean;
  title: string;
  subtitle?: string;
  onClose: () => void;
  children: ReactNode;
  footer: ReactNode;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex justify-end">
      <button aria-label="关闭编辑器" className="absolute inset-0 bg-slate-950/20" onClick={onClose} />
      <aside className="relative h-full w-full max-w-[420px] bg-white shadow-2xl border-l border-slate-200 flex flex-col">
        <header className="px-5 py-4 border-b border-slate-200">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h2 className="text-base font-semibold text-slate-950">{title}</h2>
              {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
            </div>
            <button type="button" onClick={onClose} className="pim-button-secondary px-3 py-1.5 text-sm">关闭</button>
          </div>
        </header>
        <div className="flex-1 overflow-auto px-5 py-4">{children}</div>
        <footer className="px-5 py-4 border-t border-slate-200 flex items-center justify-between gap-3">{footer}</footer>
      </aside>
    </div>
  );
}
```

- [ ] **Step 2: Convert event editor to drawer**

Modify `src/client-web/src/dialogs/EventEditorDialog.tsx`:
- Import `EditorDrawer` instead of `Dialog`.
- Keep existing state, queries, create/update/delete mutations.
- Render fields inside `EditorDrawer`.
- Footer must place Delete on the left and Cancel/Save on the right.

Use this footer JSX:

```tsx
const footer = (
  <>
    <div>
      {event && (
        <button type="button" onClick={handleDelete} disabled={deleteMut.isPending} className="px-3 py-2 text-sm rounded-lg border border-red-200 bg-red-50 text-red-600 disabled:opacity-50">
          删除
        </button>
      )}
    </div>
    <div className="flex gap-2">
      <button type="button" onClick={onClose} className="pim-button-secondary px-4 py-2 text-sm">取消</button>
      <button type="submit" form="event-editor-form" disabled={createMut.isPending || updateMut.isPending} className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50">
        {event ? '保存' : '创建'}
      </button>
    </div>
  </>
);
```

The form element must be:

```tsx
<form id="event-editor-form" onSubmit={handleSubmit} className="space-y-4">
```

- [ ] **Step 3: Convert task editor to drawer**

Modify `src/client-web/src/dialogs/TaskEditorDialog.tsx`:
- Import `EditorDrawer`.
- Add optional prop `defaultDtStart?: string`.
- Initialize `dtStart` from `task?.dtStart || defaultDtStart || ''`.
- Use priority chips instead of a select.
- Keep complete/delete/save behavior.

Update props:

```tsx
interface Props {
  open: boolean;
  onClose: () => void;
  task?: TaskResponse;
  defaultDtStart?: string;
}
```

Use this priority chip group:

```tsx
<div className="flex gap-2">
  {[
    { value: 1, label: '高', className: 'bg-red-50 text-red-600 border-red-200' },
    { value: 0, label: '普通', className: 'bg-amber-50 text-amber-700 border-amber-200' },
    { value: 3, label: '低', className: 'bg-teal-50 text-teal-700 border-teal-200' },
  ].map(item => (
    <button
      key={item.value}
      type="button"
      onClick={() => setPriority(item.value)}
      className={`px-3 py-1.5 text-sm rounded-full border ${priority === item.value ? item.className : 'bg-white text-slate-500 border-slate-200'}`}
    >
      {item.label}
    </button>
  ))}
</div>
```

- [ ] **Step 4: Verify build**

Run:

```powershell
npm run build
```

Expected: build passes. Manual check: opening event/task from Today page uses right drawer.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/ui/EditorDrawer.tsx src/client-web/src/dialogs/EventEditorDialog.tsx src/client-web/src/dialogs/TaskEditorDialog.tsx
git commit -m "feat(web): replace editors with drawer"
```

---

### Task 5: Calendar Page With Timeline/Month Single View and Drag Scheduling

**Files:**
- Create: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/src/panels/InboxPanel.tsx`

- [ ] **Step 1: Create `CalendarPage`**

Create `src/client-web/src/pages/CalendarPage.tsx`:

```tsx
import { useEffect, useMemo, useRef, useState } from 'react';
import FullCalendar from '@fullcalendar/react';
import timeGridPlugin from '@fullcalendar/timegrid';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin, { Draggable } from '@fullcalendar/interaction';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { format, addDays } from 'date-fns';
import { getEvents, getTasks } from '../api/calendar';
import PageHeader from '../ui/PageHeader';
import SegmentedControl from '../ui/SegmentedControl';
import TaskEditorDialog from '../dialogs/TaskEditorDialog';
import EventEditorDialog from '../dialogs/EventEditorDialog';
import { useCalendarVisibility } from '../context/CalendarVisibilityContext';
import type { DateSelectArg, EventClickArg, EventDropArg } from '@fullcalendar/core';
import type { EventResponse, TaskResponse } from '../types';

type CalendarMode = 'timeline' | 'month';

function toDateStr(date: Date) {
  return format(date, 'yyyy-MM-dd');
}

function taskColor(priority: number) {
  if (priority === 1) return '#ef4444';
  if (priority === 3) return '#14b8a6';
  return '#f59e0b';
}

function buildCalendarEvents(events: EventResponse[], tasks: TaskResponse[]) {
  return [
    ...events.map(event => ({
      id: event.id,
      title: event.title,
      start: event.dtStart,
      end: event.dtEnd,
      backgroundColor: '#eff6ff',
      borderColor: '#2563eb',
      textColor: '#0f172a',
      extendedProps: { type: 'event', raw: event },
    })),
    ...tasks.filter(task => task.dtStart).map(task => ({
      id: task.id,
      title: task.title,
      start: task.dtStart!,
      backgroundColor: '#fffbeb',
      borderColor: taskColor(task.priority),
      textColor: '#0f172a',
      extendedProps: { type: 'task', raw: task },
    })),
  ];
}

export default function CalendarPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialMode = searchParams.get('view') === 'month' ? 'month' : 'timeline';
  const [mode, setMode] = useState<CalendarMode>(initialMode);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [eventEditorOpen, setEventEditorOpen] = useState(false);
  const [taskEditorOpen, setTaskEditorOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventResponse | undefined>();
  const [editingTask, setEditingTask] = useState<TaskResponse | undefined>();
  const [defaultTaskStart, setDefaultTaskStart] = useState<string | undefined>();
  const calendarRef = useRef<FullCalendar>(null);
  const externalDragRootRef = useRef<HTMLDivElement>(null);

  const rangeStart = mode === 'month'
    ? toDateStr(new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1))
    : toDateStr(selectedDate);
  const rangeEnd = mode === 'month'
    ? toDateStr(new Date(selectedDate.getFullYear(), selectedDate.getMonth() + 1, 1))
    : toDateStr(addDays(selectedDate, 1));

  const { data: events = [] } = useQuery({
    queryKey: ['events', rangeStart, rangeEnd],
    queryFn: () => getEvents(rangeStart, rangeEnd),
  });

  const { data: tasks = [] } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => getTasks(),
  });

  const { hiddenCalendarIds } = useCalendarVisibility();
  const visibleEvents = hiddenCalendarIds.size > 0 ? events.filter(event => !hiddenCalendarIds.has(event.calendarId)) : events;
  const calendarEvents = useMemo(() => buildCalendarEvents(visibleEvents, tasks), [visibleEvents, tasks]);

  useEffect(() => {
    setSearchParams({ view: mode }, { replace: true });
  }, [mode, setSearchParams]);

  useEffect(() => {
    const root = externalDragRootRef.current;
    if (!root) return;
    const draggable = new Draggable(root, {
      itemSelector: '.js-draggable-task',
      eventData(eventEl) {
        return {
          title: eventEl.getAttribute('data-task-title') || '任务',
          create: false,
        };
      },
    });
    return () => draggable.destroy();
  }, []);

  function handleSelect(arg: DateSelectArg) {
    setEditingEvent(undefined);
    setEventEditorOpen(true);
  }

  function handleEventClick(arg: EventClickArg) {
    const props = arg.event.extendedProps as { type: string; raw: EventResponse | TaskResponse };
    if (props.type === 'task') {
      setEditingTask(props.raw as TaskResponse);
      setTaskEditorOpen(true);
    } else {
      setEditingEvent(props.raw as EventResponse);
      setEventEditorOpen(true);
    }
  }

  function handleExternalDrop(arg: EventDropArg | { draggedEl: HTMLElement; date: Date }) {
    const draggedEl = 'draggedEl' in arg ? arg.draggedEl : undefined;
    const taskId = draggedEl?.getAttribute('data-task-id');
    if (!taskId) return;
    const task = tasks.find(item => item.id === taskId);
    if (!task) return;
    setEditingTask(task);
    setDefaultTaskStart(arg.date.toISOString());
    setTaskEditorOpen(true);
  }

  return (
    <div className="max-w-[1500px] mx-auto space-y-4" ref={externalDragRootRef}>
      <PageHeader
        title="日历"
        subtitle={mode === 'timeline' ? '时间轴排程' : '月视图排程'}
        beforeActions={
          <SegmentedControl
            value={mode}
            ariaLabel="日历视图"
            options={[
              { value: 'timeline', label: '时间轴' },
              { value: 'month', label: '月视图' },
            ]}
            onChange={setMode}
          />
        }
        actions={<button className="pim-button-primary px-4 py-2 text-sm">新建</button>}
      />
      <section className="pim-panel overflow-hidden">
        <FullCalendar
          ref={calendarRef}
          plugins={[timeGridPlugin, dayGridPlugin, interactionPlugin]}
          initialView={mode === 'timeline' ? 'timeGridDay' : 'dayGridMonth'}
          key={mode}
          initialDate={toDateStr(selectedDate)}
          locale="zh-cn"
          height="calc(100vh - 150px)"
          allDaySlot={false}
          slotMinTime="06:00:00"
          slotMaxTime="24:00:00"
          headerToolbar={false}
          selectable
          editable
          droppable
          selectMirror
          events={calendarEvents}
          select={handleSelect}
          eventClick={handleEventClick}
          drop={handleExternalDrop}
          datesSet={arg => setSelectedDate(arg.start)}
        />
      </section>
      <TaskEditorDialog
        open={taskEditorOpen}
        onClose={() => {
          setTaskEditorOpen(false);
          setDefaultTaskStart(undefined);
        }}
        task={editingTask}
        defaultDtStart={defaultTaskStart}
      />
      <EventEditorDialog open={eventEditorOpen} onClose={() => setEventEditorOpen(false)} event={editingEvent} />
    </div>
  );
}
```

- [ ] **Step 2: Fix TypeScript event arg if needed**

If `EventDropArg` does not match FullCalendar external `drop`, replace `handleExternalDrop` signature with:

```tsx
function handleExternalDrop(arg: { draggedEl: HTMLElement; date: Date }) {
  const taskId = arg.draggedEl.getAttribute('data-task-id');
  if (!taskId) return;
  const task = tasks.find(item => item.id === taskId);
  if (!task) return;
  setEditingTask(task);
  setDefaultTaskStart(arg.date.toISOString());
  setTaskEditorOpen(true);
}
```

- [ ] **Step 3: Ensure `InboxPanel` marks task title for drag**

In `src/client-web/src/panels/InboxPanel.tsx`, each draggable task card must include:

```tsx
data-task-id={task.id}
data-task-title={task.title}
```

Expected behavior: drag a task card from the right panel to either timeline or month view. Drop opens `TaskEditorDialog` drawer with `defaultDtStart` set.

- [ ] **Step 4: Verify build**

Run:

```powershell
npm run build
```

Expected: build passes.

- [ ] **Step 5: Manual calendar verification**

Run dev server:

```powershell
npm run dev
```

Open `/calendar?view=timeline`:
- Expected: only timeline view appears.
- Expected: right task inbox appears.
- Expected: dragging a task into a time slot opens task drawer.

Open `/calendar?view=month`:
- Expected: only month view appears.
- Expected: dragging a task into a day opens task drawer with proposed date.

- [ ] **Step 6: Commit**

```powershell
git add src/client-web/src/pages/CalendarPage.tsx src/client-web/src/panels/InboxPanel.tsx
git commit -m "feat(web): add calendar drag scheduling"
```

---

### Task 6: PC Tracker Detail Page Redesign

**Files:**
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Modify: `src/client-web/src/components/pc-tracker/DateDimensionBar.tsx`
- Modify: `src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx`
- Modify: `src/client-web/src/components/pc-tracker/DailyActivityPanel.tsx`
- Modify: `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`
- Modify: `src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx`

- [ ] **Step 1: Reframe `PcTrackerPage` layout**

Modify `src/client-web/src/pages/PcTrackerPage.tsx` so it uses:
- `PageHeader` for title and date/dimension actions.
- Six `MetricCard` items at the top.
- A two-column first analysis row: heatmap large, rank/category side stack.
- A lower row: category timeline and keyboard heatmap.
- Existing settings/detail query link remains available from Settings.

The metric labels must be:

```tsx
const metrics = [
  ['记录时长', data?.metrics?.totalRecordedDuration ?? '-'],
  ['输入时长', data?.metrics?.activeInputDuration ?? '-'],
  ['空闲时长', data?.metrics?.idleDuration ?? '-'],
  ['输入总量', ((data?.metrics?.totalKeyPresses ?? 0) + (data?.metrics?.totalClicks ?? 0)).toLocaleString('zh-CN')],
  ['应用数', data?.metrics?.activeAppCount ?? '-'],
  ['切换频率', data?.metrics ? data.metrics.switchFrequency.toFixed(1) : '-'],
] as const;
```

- [ ] **Step 2: Restyle PC tracker child components**

For each PC tracker child component:
- Remove heavy rounded-xl nested cards when the parent already provides a card.
- Use `border border-slate-200`, `bg-slate-50`, and token colors.
- Preserve existing props and data behavior.

Do not change API functions in this task.

- [ ] **Step 3: Verify PC page build**

Run:

```powershell
npm run build
```

Expected: build passes.

- [ ] **Step 4: Manual PC page verification**

Open `/pc-tracker`.

Expected:
- Page is full-width without right inbox panel.
- Top metrics render even when `data.metrics` is null.
- Heatmap, category timeline, activity panel, and keyboard heatmap still render with existing data.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/PcTrackerPage.tsx src/client-web/src/components/pc-tracker
git commit -m "feat(web): redesign pc tracker page"
```

---

### Task 7: Task List, Login, and Settings Light Unification

**Files:**
- Modify: `src/client-web/src/pages/TaskListPage.tsx`
- Modify: `src/client-web/src/auth/LoginPage.tsx`
- Modify: `src/client-web/src/pages/SettingsPage.tsx`

- [ ] **Step 1: Align task list with due-first sorting**

Modify `TaskListPage.tsx`:
- Use the same due-first sorting as `TodayTaskColumn`.
- Show scheduled tasks with `StatusBadge tone="primary">已排程</StatusBadge>`.
- Show priority only as a colored dot/stripe.
- Keep filters and search.

Expected behavior: due date is the main order, priority is visual only.

- [ ] **Step 2: Restyle login page**

Modify `LoginPage.tsx`:
- Keep login/register logic unchanged.
- Replace gray background and card with tokenized styling.
- Use a concise title: `PIM`.
- Show errors in red soft panel.

Expected behavior: login/register still works; no route changes.

- [ ] **Step 3: Restyle settings page**

Modify `SettingsPage.tsx`:
- Use `PageHeader`.
- Replace emoji cards with tokenized cards and text labels.
- Keep navigation targets unchanged: `/settings/calendar-data` and `/settings/pc-data`.

- [ ] **Step 4: Verify build**

Run:

```powershell
npm run build
```

Expected: build passes.

- [ ] **Step 5: Commit**

```powershell
git add src/client-web/src/pages/TaskListPage.tsx src/client-web/src/auth/LoginPage.tsx src/client-web/src/pages/SettingsPage.tsx
git commit -m "feat(web): unify secondary pages"
```

---

### Task 8: Windows Daemon Lightweight Status UI

**Files:**
- Create: `src/client-windows/Pim.Client.App/Styles/Theme.xaml`
- Modify: `src/client-windows/Pim.Client.App/App.xaml`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml`
- Modify: `src/client-windows/Pim.Client.App/StatusWindow.xaml.cs`
- Modify: `src/client-windows/Pim.Client.App/TrayIcon.cs`

- [ ] **Step 1: Add WPF theme resources**

Create `src/client-windows/Pim.Client.App/Styles/Theme.xaml`:

```xml
<ResourceDictionary xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                    xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Color x:Key="PimPrimaryColor">#2563EB</Color>
    <Color x:Key="PimActivityColor">#14B8A6</Color>
    <Color x:Key="PimWarningColor">#F59E0B</Color>
    <Color x:Key="PimDangerColor">#EF4444</Color>
    <Color x:Key="PimTextColor">#0F172A</Color>
    <Color x:Key="PimMutedTextColor">#64748B</Color>
    <Color x:Key="PimSurfaceColor">#FFFFFF</Color>
    <Color x:Key="PimMutedSurfaceColor">#F8FAFC</Color>
    <Color x:Key="PimBorderColor">#DFE7F1</Color>

    <SolidColorBrush x:Key="PimPrimaryBrush" Color="{StaticResource PimPrimaryColor}" />
    <SolidColorBrush x:Key="PimActivityBrush" Color="{StaticResource PimActivityColor}" />
    <SolidColorBrush x:Key="PimWarningBrush" Color="{StaticResource PimWarningColor}" />
    <SolidColorBrush x:Key="PimDangerBrush" Color="{StaticResource PimDangerColor}" />
    <SolidColorBrush x:Key="PimTextBrush" Color="{StaticResource PimTextColor}" />
    <SolidColorBrush x:Key="PimMutedTextBrush" Color="{StaticResource PimMutedTextColor}" />
    <SolidColorBrush x:Key="PimSurfaceBrush" Color="{StaticResource PimSurfaceColor}" />
    <SolidColorBrush x:Key="PimMutedSurfaceBrush" Color="{StaticResource PimMutedSurfaceColor}" />
    <SolidColorBrush x:Key="PimBorderBrush" Color="{StaticResource PimBorderColor}" />

    <Style x:Key="PimPrimaryButton" TargetType="Button">
        <Setter Property="Background" Value="{StaticResource PimPrimaryBrush}" />
        <Setter Property="Foreground" Value="White" />
        <Setter Property="BorderBrush" Value="{StaticResource PimPrimaryBrush}" />
        <Setter Property="Padding" Value="14,7" />
        <Setter Property="FontSize" Value="12" />
    </Style>

    <Style x:Key="PimSecondaryButton" TargetType="Button">
        <Setter Property="Background" Value="{StaticResource PimSurfaceBrush}" />
        <Setter Property="Foreground" Value="{StaticResource PimTextBrush}" />
        <Setter Property="BorderBrush" Value="{StaticResource PimBorderBrush}" />
        <Setter Property="Padding" Value="14,7" />
        <Setter Property="FontSize" Value="12" />
    </Style>
</ResourceDictionary>
```

- [ ] **Step 2: Load theme in `App.xaml`**

Modify `src/client-windows/Pim.Client.App/App.xaml`:

```xml
<Application x:Class="Pim.Client.App.App"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">
    <Application.Resources>
        <ResourceDictionary>
            <ResourceDictionary.MergedDictionaries>
                <ResourceDictionary Source="Styles/Theme.xaml" />
            </ResourceDictionary.MergedDictionaries>
        </ResourceDictionary>
    </Application.Resources>
</Application>
```

- [ ] **Step 3: Replace status window layout**

Modify `StatusWindow.xaml`:
- Window size: width `560`, height `520`.
- Top area: title, overall state, server URL row.
- Main area: ItemsControl with status rows for Account, KeyStats, ActivityWatch, PIM API, Upload Queue.
- Each status row has name, summary, and an Expander for raw details.
- Footer: View Logs, Manual Sync, Close.

The ItemsControl template must bind `Name`, `Summary`, `ToneBrush`, and `Detail`.

- [ ] **Step 4: Add diagnostic item model**

Modify `StatusWindow.xaml.cs` with this internal record:

```csharp
private sealed record StatusDiagnostic(
    string Name,
    string Summary,
    string Detail,
    System.Windows.Media.Brush ToneBrush);
```

Add a helper:

```csharp
private System.Windows.Media.Brush BrushFor(string tone) => tone switch
{
    "ok" => (System.Windows.Media.Brush)FindResource("PimActivityBrush"),
    "warn" => (System.Windows.Media.Brush)FindResource("PimWarningBrush"),
    "error" => (System.Windows.Media.Brush)FindResource("PimDangerBrush"),
    _ => (System.Windows.Media.Brush)FindResource("PimMutedTextBrush")
};
```

Replace `ProbeAsync` with a method that returns summary and raw detail:

```csharp
private async Task<StatusDiagnostic> ProbeAsync(string name, string url)
{
    try
    {
        using var resp = await Http.GetAsync(url);
        var summary = resp.IsSuccessStatusCode ? "已连接" : $"HTTP {(int)resp.StatusCode} {resp.StatusCode}";
        var detail = $"URL: {url}\nStatus: {(int)resp.StatusCode} {resp.StatusCode}\nTime: {DateTime.Now:yyyy-MM-dd HH:mm:ss}";
        return new StatusDiagnostic(name, summary, detail, BrushFor(resp.IsSuccessStatusCode ? "ok" : "warn"));
    }
    catch (Exception ex)
    {
        var detail = $"URL: {url}\nError: {ex.GetType().Name}\nMessage: {ex.Message}\nTime: {DateTime.Now:yyyy-MM-dd HH:mm:ss}";
        return new StatusDiagnostic(name, "未连接", detail, BrushFor("error"));
    }
}
```

- [ ] **Step 5: Populate status rows**

In `RefreshStatus`, create a list:

```csharp
var diagnostics = new List<StatusDiagnostic>
{
    _authService.IsAuthenticated
        ? new StatusDiagnostic("账号", $"{_authService.CurrentUsername} 已登录", "Token exists in local auth service.", BrushFor("ok"))
        : new StatusDiagnostic("账号", "未登录", "需要登录后才能上传数据到 PIM API.", BrushFor("warn")),
    await ProbeAsync("KeyStats", "http://127.0.0.1:18080/api/stats/"),
    await ProbeAsync("ActivityWatch", "http://127.0.0.1:5600/api/0/buckets/"),
};
```

Append PIM API and upload queue diagnostics after computing base URL and collector states. Bind:

```csharp
StatusItems.ItemsSource = diagnostics;
```

- [ ] **Step 6: Update tray copy**

Modify `TrayIcon.cs`:
- Tooltip: `PIM 守护程序 - 点击查看状态`.
- First menu item: `状态：运行中，点击打开详情` and opens status window instead of disabled static text.
- Keep Login, Sync, Exit actions.

- [ ] **Step 7: Verify Windows build**

Run from repo root:

```powershell
dotnet build src/client-windows/Pim.Client.Windows.slnx
```

Expected: build succeeds.

- [ ] **Step 8: Commit**

```powershell
git add src/client-windows/Pim.Client.App/App.xaml src/client-windows/Pim.Client.App/Styles src/client-windows/Pim.Client.App/StatusWindow.xaml src/client-windows/Pim.Client.App/StatusWindow.xaml.cs src/client-windows/Pim.Client.App/TrayIcon.cs
git commit -m "feat(windows): polish daemon status diagnostics"
```

---

### Task 9: Final Verification and Cleanup

**Files:**
- Verify all modified files.
- No planned source changes unless verification exposes a bug.

- [x] **Step 1: Run Web build**

```powershell
cd src/client-web
npm run build
```

Expected: build succeeds and writes to `src/Pim.Api/wwwroot`.

Result: `npm run build` succeeded again on 2026-05-20 after lint configuration, drawer reset fixes, and duplicate dialog key cleanup. Vite emitted only the existing large chunk warning.

Additional check: `npm run lint` now executes with 0 errors and 4 Fast Refresh warnings after adding `src/client-web/eslint.config.js`.

- [x] **Step 2: Run Windows build**

```powershell
cd C:\Users\a2746\Desktop\0\progectGPT\project
dotnet build src/client-windows/Pim.Client.Windows.slnx
```

Expected: build succeeds.

Result: `dotnet build src/client-windows/Pim.Client.Windows.slnx` succeeded on 2026-05-20 after resolving the WPF `Brush` type ambiguity in `StatusWindow.xaml.cs`.

- [x] **Step 3: Manual route smoke test**

Start dev server:

```powershell
cd src/client-web
npm run dev
```

Open these routes:
- `/today`
- `/calendar?view=timeline`
- `/calendar?view=month`
- `/tasks`
- `/pc-tracker`
- `/settings`
- `/login`

Expected:
- No blank screens.
- No overlapping text at desktop width.
- Calendar route shows only one view at a time.
- `/pc-tracker` has no right inbox panel.
- `/calendar` has right inbox panel.

Result: in-app browser authenticated smoke succeeded on 2026-05-20 after rebuilding and starting the API in Docker. Use `http://127.0.0.1:5858` for the Docker API/UI in this environment; `localhost` can resolve differently. `/today`, `/calendar?view=timeline`, `/calendar?view=month`, `/tasks`, `/pc-tracker`, and `/settings` rendered without returning to login. Calendar timeline showed only `.fc-timegrid`; month view showed only `.fc-daygrid`; calendar routes displayed the right inbox panel, while `/pc-tracker` and `/settings` did not. Console error count was 0 in both Vite-served and Docker-served UI smoke checks.

- [ ] **Step 4: Manual interaction smoke test**

Verify:
- Click a Today schedule item opens the appropriate drawer.
- Click a Today task opens task drawer.
- Drag inbox task to timeline opens task drawer with proposed schedule.
- Drag inbox task to month day opens task drawer with proposed date.
- Task list displays scheduled tags and due-first order.
- Windows status window shows expandable diagnostic details.

Result: authenticated browser interaction smoke succeeded on 2026-05-20. Clicking `新建任务` on Today opened the task drawer. Dragging a right-side inbox task into the timeline opened the task drawer and populated `计划时间` with `2026-05-20T08:30`. Dragging a right-side inbox task into the month grid opened the task drawer and populated `计划时间` with `2026-04-26T00:00`. Task list loaded with filters and task cards, and browser console error count remained 0. A focused subagent also verified that removed parent dialog keys no longer produce duplicate React key conflicts while internal form reset keys remain local. Windows status window interaction was not launched manually in this session; Windows verification is limited to the successful build in Step 2.

- [ ] **Step 5: Commit verification fixes**

If verification required fixes:

```powershell
git add src/client-web src/client-windows/Pim.Client.App
git commit -m "fix: address visual redesign verification issues"
```

If no fixes were needed, do not create an empty commit.

Result: source fixes were applied for final verification findings. Additional lint-enabling, Windows build, and duplicate dialog key fixes were applied on 2026-05-20, but no commit was created in this session.

Additional Docker verification: `docker compose up -d --build pim-api` rebuilt `project-pim-api:latest` with the latest Web assets and restarted `project-pim-api-1`. The container reported `healthy`; `GET http://127.0.0.1:5858/health` returned `{"status":"healthy"}`; login with the main account returned `code: 0`, `message: "success"`, and an access token. Docker publish emitted 3 nullable warnings in existing server code but no build errors.

---

## Self-Review

Spec coverage:
- Web visual system: Task 1.
- Shell navigation and page-specific right panel: Task 2.
- Today workbench 1/2/1 layout: Task 3.
- Right drawer editors: Task 4.
- Calendar timeline/month single view and drag scheduling: Task 5.
- PC tracker overview drilldown: Task 6.
- Task due-first sorting, login/settings polish: Task 7.
- Windows daemon lightweight diagnostic UI: Task 8.
- Verification: Task 9.

Placeholder scan:
- No `TBD`, `TODO`, or "implement later" markers.
- Each task has exact file paths, commands, and expected outcomes.

Type consistency:
- `TaskResponse`, `EventResponse`, and `PcSummaryResponse` match `src/client-web/src/types/index.ts`.
- Calendar/task API calls match `src/client-web/src/api/calendar.ts`.
- PC summary API call matches `src/client-web/src/api/pcTracker.ts`.
