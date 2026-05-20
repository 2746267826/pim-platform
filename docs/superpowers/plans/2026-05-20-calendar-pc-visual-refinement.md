# Calendar and PC Tracker Visual Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the calendar timeline/month views and PC tracker analysis views visually polished, responsive, and readable without replacing the existing calendar interaction engine.

**Architecture:** Keep FullCalendar for calendar behavior and wrap it in a stronger visual skin with custom event rendering. Split PC tracker visualization concerns across existing focused components: activity heatmap handles dimension-specific layouts, category timeline handles readable block/tooltip behavior, and keyboard heatmap renders full-width keyboard plus mouse diagrams.

**Tech Stack:** React 19, TypeScript, Vite, Tailwind CSS 4, FullCalendar, @tanstack/react-query.

---

## File Structure

Modify:
- `src/client-web/src/pages/CalendarPage.tsx` - Clean Chinese labels, custom event rendering, timeline/month options.
- `src/client-web/src/index.css` - FullCalendar visual skin and tooltip overflow helpers.
- `src/client-web/src/pages/PcTrackerPage.tsx` - Change category timeline and keyboard/mouse layout from side-by-side to stacked full-width cards.
- `src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx` - Render hour/day/month/year with distinct responsive layouts.
- `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx` - Hide text in narrow blocks and prevent tooltip clipping.
- `src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx` - Render 108-key keyboard plus two-side-button mouse.
- `docs/superpowers/specs/2026-05-20-calendar-pc-visual-refinement-design.md` - Design reference only.

Do not modify API contracts or database code.

---

### Task 1: Calendar Visual Skin and Copy

**Files:**
- Modify: `src/client-web/src/pages/CalendarPage.tsx`
- Modify: `src/client-web/src/index.css`

- [ ] **Step 1: Replace garbled calendar copy**

In `CalendarPage.tsx`, replace the garbled values:

```tsx
const CALENDAR_MODE_OPTIONS: Array<{ value: CalendarMode; label: string }> = [
  { value: 'timeline', label: '时间轴' },
  { value: 'month', label: '月视图' },
];
```

Update `PageHeader` props and navigation labels to clean Chinese:

```tsx
title="日历"
subtitle={mode === 'timeline' ? '按时间轴安排今天的任务和日程' : '按月查看任务和日程分布'}
```

Use button text `上一段`, `今天`, `下一段`, and aria labels `上一段时间范围`, `下一段时间范围`.

- [ ] **Step 2: Add custom event content renderer**

In `CalendarPage.tsx`, import `EventContentArg` from `@fullcalendar/core` and add:

```tsx
function renderCalendarEvent(arg: EventContentArg) {
  const props = arg.event.extendedProps as CalendarEventInput['extendedProps'];
  const isTask = props.type === 'task';
  const raw = props.raw as Partial<TaskResponse & EventResponse>;
  const priority = isTask ? (raw.priority ?? 0) : 0;
  const toneClass = isTask
    ? priority === 1
      ? 'calendar-event--danger'
      : priority === 3
        ? 'calendar-event--quiet'
        : 'calendar-event--warning'
    : 'calendar-event--primary';

  return (
    <div className={`calendar-event-card ${toneClass}`}>
      <span className="calendar-event-dot" />
      <span className="calendar-event-title">{arg.event.title}</span>
      {arg.timeText && <span className="calendar-event-time">{arg.timeText}</span>}
    </div>
  );
}
```

Pass it to FullCalendar:

```tsx
eventContent={renderCalendarEvent}
dayMaxEvents={mode === 'month' ? 3 : undefined}
slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
```

- [ ] **Step 3: Add calendar board CSS**

In `index.css`, replace or extend current `.fc` overrides with:

```css
.calendar-board {
  background:
    radial-gradient(circle at top left, rgba(20, 184, 166, 0.12), transparent 28rem),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.calendar-board .fc {
  --fc-border-color: rgba(148, 163, 184, 0.22);
  --fc-today-bg-color: rgba(37, 99, 235, 0.08);
  --fc-neutral-bg-color: transparent;
  color: var(--pim-text);
}

.calendar-board .fc-scrollgrid {
  border: 0;
}

.calendar-board .fc-theme-standard td,
.calendar-board .fc-theme-standard th {
  border-color: rgba(148, 163, 184, 0.22);
}

.calendar-board .fc-col-header-cell {
  background: rgba(248, 250, 252, 0.9);
  color: #475569;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.calendar-board .fc-timegrid-slot {
  height: 3.25rem;
}

.calendar-board .fc-timegrid-slot-lane {
  background: linear-gradient(90deg, rgba(248, 250, 252, 0.74), rgba(255, 255, 255, 0.92));
}

.calendar-board .fc-timegrid-slot-label {
  color: #94a3b8;
  font-size: 0.7rem;
  font-weight: 600;
}

.calendar-board .fc-daygrid-day {
  background: rgba(255, 255, 255, 0.72);
}

.calendar-board .fc-daygrid-day-frame {
  min-height: 7.5rem;
  padding: 0.35rem;
}

.calendar-board .fc-day-today .fc-daygrid-day-frame,
.calendar-board .fc-timegrid-col.fc-day-today {
  background: linear-gradient(180deg, rgba(219, 234, 254, 0.65), rgba(240, 253, 250, 0.38));
}

.calendar-board .fc-daygrid-day-number {
  margin: 0.35rem;
  border-radius: 999px;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 700;
}

.calendar-board .fc-day-today .fc-daygrid-day-number {
  background: #2563eb;
  color: white;
  padding: 0.2rem 0.45rem;
}

.calendar-board .fc-event {
  border: 0;
  background: transparent;
  box-shadow: none;
}

.calendar-event-card {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 0.35rem;
  border-radius: 0.75rem;
  padding: 0.25rem 0.45rem;
  color: #0f172a;
  font-size: 0.72rem;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.calendar-event-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-event-time {
  flex-shrink: 0;
  color: rgba(15, 23, 42, 0.56);
  font-size: 0.65rem;
  font-weight: 600;
}

.calendar-event-dot {
  height: 0.45rem;
  width: 0.45rem;
  flex-shrink: 0;
  border-radius: 999px;
  background: currentColor;
}

.calendar-event--primary {
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
  color: #1d4ed8;
}

.calendar-event--warning {
  background: linear-gradient(135deg, #fef3c7, #fffbeb);
  color: #b45309;
}

.calendar-event--danger {
  background: linear-gradient(135deg, #fee2e2, #fff1f2);
  color: #dc2626;
}

.calendar-event--quiet {
  background: linear-gradient(135deg, #ccfbf1, #f0fdfa);
  color: #0f766e;
}
```

Add `calendar-board` to the calendar section class.

- [ ] **Step 4: Verify calendar build**

Run:

```powershell
cd src/client-web
npm run build
```

Expected: TypeScript and Vite build succeed.

---

### Task 2: Dimension-Specific Activity Heatmap

**Files:**
- Modify: `src/client-web/src/components/pc-tracker/ActivityHeatmap.tsx`

- [ ] **Step 1: Add date helpers and view selection**

Keep `normalizeCell` and `linearColor`. Add helpers:

```tsx
function parseDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function formatCellLabel(value: string, dimension: string) {
  const date = parseDate(value);
  if (!date) return value;
  if (dimension === 'hour') return `${String(date.getHours()).padStart(2, '0')}:00`;
  if (dimension === 'month') return `${date.getMonth() + 1}/${date.getDate()}`;
  if (dimension === 'year') return `${date.getFullYear()}-${date.getMonth() + 1}`;
  return `${date.getMonth() + 1}/${date.getDate()}`;
}
```

- [ ] **Step 2: Render hour view as full-width 24-hour strip**

Add `renderHourHeatmap(cells, maxKey)` that creates 24 slots and renders a responsive CSS grid:

```tsx
function renderHourHeatmap(cells: SafeHeatmapCell[], maxKey: number) {
  const byHour = new Map<number, SafeHeatmapCell>();
  for (const cell of cells) {
    const date = parseDate(cell.start);
    if (date) byHour.set(date.getHours(), cell);
  }

  return (
    <div className="grid grid-cols-24 gap-1">
      {Array.from({ length: 24 }, (_, hour) => {
        const cell = byHour.get(hour);
        const value = cell?.intensityScore ?? 0;
        return (
          <div key={hour} className="group relative min-h-16 rounded-xl border border-white/70 p-2 shadow-sm transition-transform hover:-translate-y-0.5 hover:ring-2 hover:ring-blue-300" style={{ backgroundColor: linearColor(value, maxKey) }}>
            <div className="text-[11px] font-bold text-slate-700">{String(hour).padStart(2, '0')}</div>
            <div className="mt-3 text-xs font-semibold text-slate-900">{value.toLocaleString('zh-CN')}</div>
            <HeatTooltip label={`${String(hour).padStart(2, '0')}:00`} value={value} />
          </div>
        );
      })}
    </div>
  );
}
```

- [ ] **Step 3: Render month view as calendar-like matrix**

Add `renderMonthHeatmap(cells, maxKey)` that groups by date and uses `grid-cols-7`, with weekday labels. It should not look like the year view.

- [ ] **Step 4: Render day/year fallback grids**

Use `auto-fit` cells for day/year:

```tsx
<div className="grid gap-1.5 [grid-template-columns:repeat(auto-fit,minmax(18px,1fr))]">
```

Use a larger `minmax(30px, 1fr)` for day, and compact `minmax(14px, 1fr)` for year.

- [ ] **Step 5: Verify component compiles**

Run `npm run build`.

---

### Task 3: Category Timeline Tooltip and Narrow Labels

**Files:**
- Modify: `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`

- [ ] **Step 1: Remove clipping from timeline track**

Change the track wrapper from `overflow-hidden` to `overflow-visible`, increase height to allow tooltip space, and place blocks around the center:

```tsx
<div className="relative h-28 overflow-visible rounded-xl border border-slate-200 bg-white px-1">
```

Use blocks with `top-10 h-10`.

- [ ] **Step 2: Hide labels in narrow blocks**

When mapping blocks, compute:

```tsx
const showInlineLabel = widthPct >= 6;
```

Only render the inline `<span>` when `showInlineLabel` is true.

- [ ] **Step 3: Raise tooltip layer and add focus support**

Make each block `tabIndex={0}` and show tooltip for hover and focus:

```tsx
className="group absolute top-10 flex h-10 items-center justify-center rounded-lg px-1 text-[10px] font-medium text-white shadow-sm outline-none ring-offset-2 focus:ring-2 focus:ring-blue-300"
```

Tooltip class:

```tsx
absolute bottom-full left-1/2 z-50 mb-3 hidden min-w-[220px] -translate-x-1/2 whitespace-nowrap rounded-xl bg-slate-950 px-3 py-2 text-left text-[11px] text-white shadow-2xl group-hover:block group-focus:block
```

- [ ] **Step 4: Verify tooltip is not clipped in browser**

Use in-app browser on `/pc-tracker` and hover/focus a timeline block.

---

### Task 4: Full-Width Keyboard and Mouse Heatmap

**Files:**
- Modify: `src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`

- [ ] **Step 1: Change PC page lower layout to stacked full-width cards**

Replace the lower two-column grid in `PcTrackerPage.tsx` with:

```tsx
<div className="space-y-4">
  <AnalysisCard title="分类时间线" subtitle="按 ActivityWatch 时间片聚合分类">
    <CategoryTimeline ... />
  </AnalysisCard>

  <AnalysisCard title="键盘鼠标热力图" subtitle="108 键键盘、鼠标按键与快捷键统计">
    <KeyboardHeatmap keystats={data?.keystats || null} />
  </AnalysisCard>
</div>
```

- [ ] **Step 2: Replace simplified keyboard rows**

In `KeyboardHeatmap.tsx`, replace `KEYBOARD_ROWS` with a layout that separates clusters:

```tsx
type KeySpec = { code: string; label: string; units?: number };
type KeyRow = KeySpec[];
type KeyCluster = { name: string; rows: KeyRow[] };
```

Create clusters for function row, main keys, navigation, arrows, and numpad. Include `Esc`, `F1`-`F12`, alphanumeric rows, `Insert/Home/PageUp`, arrows, and numpad keys. This produces a standard 108-key diagram.

- [ ] **Step 3: Add key aliases**

Add:

```tsx
function aliasesFor(code: string) {
  const map: Record<string, string[]> = {
    Ctrl: ['Ctrl', 'LCtrl', 'RCtrl'],
    Shift: ['Shift', 'LShift', 'RShift'],
    Alt: ['Alt', 'LAlt', 'RAlt'],
    Win: ['Win', 'LWin', 'RWin'],
    Space: ['Space', 'Spacebar'],
    Backspace: ['Backspace', 'Back'],
  };
  return map[code] ?? [code];
}

function countForKey(keyCounts: Map<string, number>, code: string) {
  return aliasesFor(code).reduce((sum, key) => sum + (keyCounts.get(key) ?? 0), 0);
}
```

- [ ] **Step 4: Render mouse diagram**

Add a `MouseHeatmap` section next to the keyboard on wide screens and below on narrow screens. It must display left, right, middle/wheel, side back, and side forward buttons using counts from `keystats`.

- [ ] **Step 5: Preserve shortcuts below diagrams**

Keep shortcut chips below the keyboard/mouse diagrams.

- [ ] **Step 6: Verify build**

Run `npm run build`.

---

### Task 5: Final Verification

**Files:**
- Verify source files from Tasks 1-4.

- [ ] **Step 1: Run lint**

```powershell
cd src/client-web
npm run lint
```

Expected: 0 errors. Existing Fast Refresh warnings are acceptable if unchanged.

- [ ] **Step 2: Run production build**

```powershell
cd src/client-web
npm run build
```

Expected: build succeeds.

- [ ] **Step 3: Browser smoke**

Start dev server:

```powershell
cd src/client-web
npm run dev -- --host 127.0.0.1
```

Use the in-app browser and main account if needed. Verify:

- `/calendar?view=timeline`: one time-grid, custom event cards, no console errors.
- `/calendar?view=month`: one month grid, custom day styling, no console errors.
- Drag a right-side task into timeline and month still opens task drawer with proposed schedule.
- `/pc-tracker`: hour heatmap fills width; month heatmap looks calendar-like; year remains compact.
- Category timeline is full width, narrow blocks hide labels, tooltip is visible.
- Keyboard/mouse heatmap is full width and includes standard keyboard clusters and mouse side buttons.

- [ ] **Step 4: Rebuild Docker API if browser smoke passes**

```powershell
docker compose up -d --build pim-api
```

Expected: API image rebuilds and `project-pim-api-1` becomes healthy.

---

## Self-Review

Spec coverage:
- Calendar visual/copy refinement: Task 1.
- Activity heatmap dimension-specific layouts: Task 2.
- Category timeline tooltip/narrow labels: Task 3.
- Keyboard/mouse full-width 108-key layout: Task 4.
- Verification and Docker rebuild: Task 5.

Placeholder scan:
- No `TBD`, `TODO`, or "implement later" markers.
- All tasks list exact files and concrete verification commands.

Type consistency:
- Uses existing `HeatmapGridResponse`, `KeystatsSummary`, `TimelineItem`, `TaskResponse`, and `EventResponse`.
- Does not change API response contracts.

---

## Execution Result

Completed on 2026-05-20.

Implemented:
- Calendar kept FullCalendar but now uses clean Chinese labels, custom event cards, a softer `calendar-board` skin, and preserved right-side task drag/drop.
- Activity heatmap now renders dimension-specific layouts: 24 responsive hour blocks, day grid, grouped month cards, and compact year matrix.
- Category timeline is full-width, hides inline labels for narrow blocks, and exposes higher z-index hover/focus details.
- Keyboard/mouse heatmap is full-width and always renders a standard keyboard cluster layout plus mouse diagram with left/right/middle/wheel and two side buttons, even when the selected day has no keystats.
- PC tracker lower layout is stacked instead of side-by-side.

Verification:
- `npm run lint`: 0 errors, 4 existing Fast Refresh warnings.
- `npm run build`: succeeded; Vite emitted only the existing large chunk warning.
- In-app browser on Vite dev server: `/calendar?view=timeline`, `/calendar?view=month`, and `/pc-tracker` rendered with 0 console errors.
- Browser drag smoke: dragging a right-side inbox task into timeline opened the task drawer and populated `计划时间`.
- PC browser smoke: hour view exposed 24 hour blocks; month view exposed month cards with weekday labels; year view exposed compact density cells; keyboard and mouse diagrams were visible.
- `docker compose up -d --build pim-api`: rebuilt `project-pim-api:latest`; container reached `healthy`.
- Docker-served UI smoke at `http://127.0.0.1:5858`: calendar board and PC keyboard/mouse visuals rendered with 0 console errors.
- API checks after Docker rebuild: `/health` returned `healthy`; main account login returned success and an access token.
