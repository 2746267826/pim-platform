# Settings Page — Manage Calendar Data

## Overview

Add a Settings page to the web frontend. The first settings section is "管理日程数据" (Manage Calendar Data), which provides a full calendar event management table with filtering, detail view, ICS export, and ICS import.

All ICS logic runs server-side — the web frontend only handles display and user interaction.

## Sidebar Navigation

Add a "设置" (Settings) nav item to the sidebar, below existing items and above the username:

```
📅 时间轴
📆 本周
🗓 月视图
✅ 任务
💻 PC记录
━━━━━━━━━━━━
⚙ 设置          ← NEW
━━━━━━━━━━━━
用户: a
[退出登录]
```

Route: `/settings`

## Settings Page

A container page with settings sections as cards. For now, only one section — "管理日程数据".

```
┌──────────────────────────────────────────────┐
│ 设置                                         │
│                                              │
│ ┌────────────────────────────────────────┐   │
│ │ 📅 管理日程数据                         │   │
│ │ 查看、筛选、导入导出全部日程        [→] │   │
│ └────────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

Clicking the card → navigates to `/settings/calendar-data`.

## Calendar Data Manager Page (`/settings/calendar-data`)

### Layout

```
┌──────────────────────────────────────────────┐
│ ← 返回    📅 管理日程数据                     │
│           [📥 导入ICS] [📤 导出选中(0)] [📤 导出全部] │
│                                              │
│ [搜索标题...] [全部日历 ▾] [全部时间 ▾]  共N条 │
│                                              │
│ ┌──┬────────┬──────┬──────────┬──────────┬────┬──────┐
│ │☐ │ 标题   │ 日历 │ 开始时间  │ 结束时间  │重复│ 操作 │
│ ├──┼────────┼──────┼──────────┼──────────┼────┼──────┤
│ │☐ │团队周会│ 默认 │05-19 10:00│05-19 11:00│每周│ 详情 │
│ │☐ │项目评审│ 工作 │05-20 14:00│05-20 15:30│ -- │ 详情 │
│ └──┴────────┴──────┴──────────┴──────────┴────┴──────┘
│                  ← 1 2 3 ... →                 │
└──────────────────────────────────────────────┘
```

### Filters

| Filter | Type | Options |
|--------|------|---------|
| Title search | Text input | Free text, filters client-side |
| Calendar | Dropdown | All calendars + per-calendar (from API) |
| Date range | Dropdown | All time, Last 7 days, Last 30 days, This month, Custom range |
| Count | Text | "共 N 条" display |

Filters are sent as query params to the list API. Calendar dropdown is populated from `GET /calendar/calendars`.

### Table Columns

| Column | Source | Notes |
|--------|--------|-------|
| Checkbox | — | Full table header checkbox for select-all |
| Title | `EventResponse.title` | |
| Calendar | `EventResponse.calendarId` → calendar name | Rendered as colored badge |
| Start | `EventResponse.dtStart` | Formatted `yyyy-MM-dd HH:mm` |
| End | `EventResponse.dtEnd` | Formatted `yyyy-MM-dd HH:mm` |
| Recurring | `EventResponse.rrule` | Display "每日/每周/每月" or "—" |
| Actions | — | "详情" link |

### Interactions

**Detail View**: Click "详情" → opens read-only dialog showing all event fields (title, calendar, start, end, location, description, rrule, status).

**Selection**:
- Header checkbox selects/deselects all visible rows
- Individual checkboxes toggle per row
- "导出选中(N)" button shows selected count, disabled when 0

**ICS Export (selected)**:
- Click "导出选中(N)" → `GET /api/v1/calendar/events/export?ids=...`
- Server generates ICS, returns `text/calendar` content
- Browser triggers file download as `pim-events.ics`

**ICS Export (all)**:
- Click "导出全部" → `GET /api/v1/calendar/events/export` (with current filters)
- Server generates ICS with all matching events
- Same download behavior

**ICS Import**:
- Click "导入ICS" → file picker opens (`.ics` only)
- Upload file via `POST /api/v1/calendar/events/import` (multipart/form-data)
- Server parses ICS, inserts events into database
- Returns `{ imported: N, skipped: M }` counts
- Show result message: "成功导入 N 条日程" (or with skipped count)
- Refresh the event list

### Pagination

Server-side pagination: 50 items per page. Page controls at bottom.

## New API Endpoints

### `GET /api/v1/calendar/events/export`

Query params:
- `ids` (optional): comma-separated event IDs for selected export
- `calendarId` (optional): filter by calendar
- `start` (optional): filter by start date
- `end` (optional): filter by end date

Response: `text/calendar` (ICS file) with `Content-Disposition: attachment; filename="pim-events.ics"`

### `POST /api/v1/calendar/events/import`

Body: `multipart/form-data` with `file` field (.ics file)

Response: `ApiResponse<ImportResult>` where `ImportResult = { imported: int, skipped: int }`

Logic:
- Parse ICS file
- For each VEVENT: map to EventEntity, insert if not duplicate (same title + start time)
- Return counts

### `GET /api/v1/calendar/events` (extend existing)

Add optional query params for filtering:
- `search` (optional): title substring match
- `calendarId` (optional): filter by calendar
- `start` / `end` (optional): date range filter
- `page` / `pageSize` (optional): pagination (default 1/50)

Response: `ApiResponse<PagedResult<EventResponse>>`

## Tech Stack

- **Frontend**: React 18 + TypeScript + Tailwind CSS v4 + @tanstack/react-query
- **Backend**: ASP.NET Core 8 minimal API, existing IModule pattern (Pim.Module.Calendar)
- **ICS library**: Ical.Net (NuGet: Ical.Net 4.x)
- **Database**: PostgreSQL via EF Core (existing `PimDbContext`)

## Files Changed

### Frontend (5 files)

| File | Action |
|------|--------|
| `src/layout/Sidebar.tsx` | Add "设置" nav item, route `/settings` |
| `src/layout/AppLayout.tsx` | Add `/settings` and `/settings/calendar-data` routes |
| `src/pages/SettingsPage.tsx` | New — settings container with cards |
| `src/pages/CalendarDataManager.tsx` | New — full event table + filters + import/export |
| `src/api/calendar.ts` | Add `exportEvents()`, `importEvents()`, extend `getEvents()` |

### Backend (3 files)

| File | Action |
|------|--------|
| `src/modules/Pim.Module.Calendar/CalendarModule.cs` | Add export/import endpoints, extend list endpoint |
| `src/modules/Pim.Module.Calendar/Services/CalendarService.cs` | New — ICS generation and parsing logic |
| `src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj` | Add Ical.Net package reference |

## Out of Scope

- Task ICS import/export (VTODO) — events only for now
- Other settings sections (will be added to SettingsPage later)
- Drag-and-drop import
- Import conflict resolution UI (auto-skip duplicates)
