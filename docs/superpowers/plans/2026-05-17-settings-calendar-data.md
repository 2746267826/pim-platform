# Settings Page — Calendar Data Manager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Settings page with a "管理日程数据" section: filterable event table, detail view, ICS import/export.

**Architecture:** Frontend adds `/settings` and `/settings/calendar-data` routes with a new SettingsPage container and CalendarDataManager page. Backend extends the existing `GET /calendar/events` with search/filter/pagination and tweaks the already-existing ICS import/export endpoints to match frontend needs.

**Tech Stack:** React 18 + TypeScript + Tailwind CSS v4 + @tanstack/react-query; ASP.NET Core 8 minimal API; Ical.Net 5.2.2 (already installed)

**Key simplification:** ICS import/export endpoints and `IcsService` already exist in `Pim.Module.Calendar`. Only minor adjustments needed.

---

### Task 1: Extend Backend Events List API

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/CalendarService.cs`
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`

- [ ] **Step 1: Add `GetEventsPagedAsync` to CalendarService**

Add a new method that supports search, calendarId filter, date range, and pagination. Uses the existing `PagedResult<T>` from `Pim.Core.Common`.

In `CalendarService.cs`, add after `GetEventsAsync`:

```csharp
public async Task<PagedResult<EventResponse>> GetEventsPagedAsync(
    string? search, Guid? calendarId,
    DateTimeOffset? start, DateTimeOffset? end,
    int page = 1, int pageSize = 50,
    CancellationToken ct = default)
{
    if (_currentUser.UserId is null)
        throw new DomainException(01002, "Not authenticated");

    var query = _db.Set<EventEntity>()
        .Where(e => e.Calendar.UserId == _currentUser.UserId.Value);

    if (!string.IsNullOrEmpty(search))
        query = query.Where(e => e.Title.Contains(search));
    if (calendarId.HasValue)
        query = query.Where(e => e.CalendarId == calendarId.Value);
    if (start.HasValue)
        query = query.Where(e => e.DtEnd >= start.Value);
    if (end.HasValue)
        query = query.Where(e => e.DtStart <= end.Value);

    var totalCount = await query.CountAsync(ct);
    var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);

    var items = await query
        .OrderByDescending(e => e.DtStart)
        .Skip((page - 1) * pageSize)
        .Take(pageSize)
        .Select(e => MapEvent(e))
        .ToListAsync(ct);

    return new PagedResult<EventResponse>(items, page, pageSize, totalCount, totalPages);
}
```

- [ ] **Step 2: Update the GET /events endpoint in CalendarModule.cs**

Replace the existing GET `/calendar/events` handler (lines 46-51) with one that accepts optional filter/pagination params:

```csharp
group.MapGet("/events", async (
    [FromQuery] DateTimeOffset? start,
    [FromQuery] DateTimeOffset? end,
    [FromQuery] string? search,
    [FromQuery] Guid? calendarId,
    [FromQuery] int? page,
    [FromQuery] int? pageSize,
    [FromServices] CalendarService svc,
    CancellationToken ct) =>
{
    // If only start/end given (no search/calendarId/page), use old path for backward compat
    if (search is null && calendarId is null && page is null)
    {
        // Old behavior: no pagination, returns List
        var events = await svc.GetEventsAsync(start ?? DateTimeOffset.MinValue, end ?? DateTimeOffset.MaxValue, ct);
        return Results.Ok(ApiResponse<List<EventResponse>>.Ok(events));
    }

    var result = await svc.GetEventsPagedAsync(search, calendarId, start, end, page ?? 1, pageSize ?? 50, ct);
    return Results.Ok(ApiResponse<PagedResult<EventResponse>>.Ok(result));
});
```

- [ ] **Step 3: Build backend to verify compilation**

```bash
dotnet build src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj
```

Expected: 0 errors.

- [ ] **Step 4: Commit**

```bash
git add src/modules/Pim.Module.Calendar/Services/CalendarService.cs src/modules/Pim.Module.Calendar/CalendarModule.cs
git commit -m "feat: extend GET /calendar/events with search, calendar filter, and server-side pagination"
```

---

### Task 2: Tweak Backend ICS Endpoints

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/CalendarModule.cs`

- [ ] **Step 1: Add `ids` param to export-ics and return as file download**

Update the `GET /calendar/export-ics` endpoint to accept optional `ids` param and return as a downloadable file instead of JSON-wrapped string:

```csharp
group.MapGet("/export-ics", async (
    [FromQuery] DateTimeOffset? start,
    [FromQuery] DateTimeOffset? end,
    [FromQuery] string? ids,
    [FromServices] CalendarService svc,
    [FromServices] IcsService icsService,
    CancellationToken ct) =>
{
    var entities = await svc.GetEventEntitiesAsync(
        start ?? DateTimeOffset.MinValue,
        end ?? DateTimeOffset.MaxValue, ct);

    if (!string.IsNullOrEmpty(ids))
    {
        var idSet = ids.Split(',').Select(Guid.Parse).ToHashSet();
        entities = entities.Where(e => idSet.Contains(e.Id)).ToList();
    }

    var icsContent = icsService.ExportEvents(entities);
    return Results.File(
        System.Text.Encoding.UTF8.GetBytes(icsContent),
        "text/calendar",
        "pim-events.ics");
});
```

Note: remove the old `ApiResponse<string>` wrapping — return raw ICS file for browser download.

- [ ] **Step 2: Update import-ics to return `{imported, skipped}` counts**

Update `POST /calendar/import-ics` to return breakdown counts:

```csharp
group.MapPost("/import-ics", async (
    HttpRequest request,
    [FromServices] IcsService icsService,
    [FromServices] CalendarService calendarService,
    CancellationToken ct) =>
{
    if (!request.HasFormContentType)
        return Results.BadRequest(ApiResponse<string>.Error(400, "Expected multipart/form-data"));

    var form = await request.ReadFormAsync(ct);
    var file = form.Files.GetFile("file");
    if (file is null)
        return Results.BadRequest(ApiResponse<string>.Error(400, "No file field"));

    using var reader = new StreamReader(file.OpenReadStream());
    var icsContent = await reader.ReadToEndAsync(ct);
    var parsed = icsService.ImportEvents(icsContent);

    var entities = await calendarService.GetEventEntitiesAsync(
        DateTimeOffset.MinValue, DateTimeOffset.MaxValue, ct);
    var existingKeys = entities.Select(e => (e.Title, e.DtStart)).ToHashSet();

    int imported = 0, skipped = 0;
    foreach (var evt in parsed)
    {
        if (existingKeys.Contains((evt.Title, evt.Start)))
        {
            skipped++;
            continue;
        }

        await calendarService.CreateEventAsync(new CreateEventRequest(
            CalendarId: Guid.Empty, // caller will need to pick a calendar — use user's default
            Title: evt.Title,
            Description: evt.Description,
            Location: evt.Location,
            DtStart: evt.Start,
            DtEnd: evt.End,
            RRule: evt.RRule
        ), ct);

        imported++;
    }

    return Results.Ok(ApiResponse<ImportResult>.Ok(new ImportResult(imported, skipped)));
});
```

Add the DTO in `DTOs/CalendarDtos.cs`:

```csharp
record ImportResult(int Imported, int Skipped);
```

- [ ] **Step 3: Build backend to verify compilation**

```bash
dotnet build src/modules/Pim.Module.Calendar/Pim.Module.Calendar.csproj
```

Expected: 0 errors.

- [ ] **Step 4: Commit**

```bash
git add src/modules/Pim.Module.Calendar/CalendarModule.cs src/modules/Pim.Module.Calendar/DTOs/CalendarDtos.cs
git commit -m "feat: improve ICS endpoints — export as file download with ids filter, import returns imported/skipped counts"
```

---

### Task 3: Add Frontend API Functions

**Files:**
- Modify: `src/client-web/src/api/calendar.ts`
- Modify: `src/client-web/src/types/index.ts`

- [ ] **Step 1: Add types for paged results and import result**

In `src/client-web/src/types/index.ts`, add:

```typescript
interface PagedResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
}

interface ImportResult {
  imported: number;
  skipped: number;
}
```

- [ ] **Step 2: Add API functions**

In `src/client-web/src/api/calendar.ts`, add:

```typescript
import type { ApiResponse, CalendarResponse, EventResponse, TaskResponse, PagedResult, ImportResult } from '../types';

interface GetEventsParams {
  search?: string;
  calendarId?: string;
  start?: string;
  end?: string;
  page?: number;
  pageSize?: number;
}

export async function getEventsPaged(params: GetEventsParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.search) searchParams.set('search', params.search);
  if (params.calendarId) searchParams.set('calendarId', params.calendarId);
  if (params.start) searchParams.set('start', params.start);
  if (params.end) searchParams.set('end', params.end);
  if (params.page) searchParams.set('page', String(params.page));
  if (params.pageSize) searchParams.set('pageSize', String(params.pageSize));

  const qs = searchParams.toString();
  const r = await apiGet<ApiResponse<PagedResult<EventResponse>>>(
    `/calendar/events?${qs}`
  );
  return r.data;
}

export async function exportIcs(ids?: string[], start?: string, end?: string) {
  const params = new URLSearchParams();
  if (ids?.length) params.set('ids', ids.join(','));
  if (start) params.set('start', start);
  if (end) params.set('end', end);

  const resp = await fetch(`/api/v1/calendar/export-ics?${params.toString()}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` }
  });
  if (!resp.ok) throw new Error(`Export failed: ${resp.status}`);
  const blob = await resp.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'pim-events.ics';
  a.click();
  URL.revokeObjectURL(url);
}

export async function importIcs(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  const resp = await fetch('/api/v1/calendar/import-ics', {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('accessToken')}` },
    body: formData
  });
  if (!resp.ok) throw new Error(`Import failed: ${resp.status}`);
  const json = await resp.json() as ApiResponse<ImportResult>;
  return json.data;
}
```

- [ ] **Step 3: Build frontend to verify TypeScript compilation**

```bash
npm --prefix src/client-web run build
```

Expected: 0 errors (some warnings OK).

- [ ] **Step 4: Commit**

```bash
git add src/client-web/src/api/calendar.ts src/client-web/src/types/index.ts
git commit -m "feat: add API functions for paged events, ICS export/import"
```

---

### Task 4: Add SettingsPage Container

**Files:**
- Create: `src/client-web/src/pages/SettingsPage.tsx`

- [ ] **Step 1: Write SettingsPage component**

```tsx
import { useNavigate } from 'react-router-dom';

export default function SettingsPage() {
  const navigate = useNavigate();

  return (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-xl font-bold mb-6">设置</h2>

      <div
        className="bg-white border rounded-lg p-5 hover:border-blue-300 cursor-pointer transition-colors flex items-center justify-between"
        onClick={() => navigate('/settings/calendar-data')}
      >
        <div>
          <h3 className="font-semibold text-base flex items-center gap-2">
            <span>📅</span> 管理日程数据
          </h3>
          <p className="text-sm text-gray-500 mt-1">
            查看、筛选、导入导出全部日程
          </p>
        </div>
        <span className="text-gray-300 text-xl">→</span>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add src/client-web/src/pages/SettingsPage.tsx
git commit -m "feat: add SettingsPage container with calendar data card"
```

---

### Task 5: Add CalendarDataManager Page

**Files:**
- Create: `src/client-web/src/pages/CalendarDataManager.tsx`

This is the largest task. The page has: filters bar, data table with checkboxes, pagination, detail dialog, import/export buttons.

- [ ] **Step 1: Write CalendarDataManager component**

```tsx
import { useState } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getCalendars, getEventsPaged, exportIcs, importIcs } from '../api/calendar';
import type { EventResponse, CalendarResponse } from '../types';

export default function CalendarDataManager() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Filters
  const [search, setSearch] = useState('');
  const [calendarId, setCalendarId] = useState('');
  const [dateRange, setDateRange] = useState('all');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  const [page, setPage] = useState(1);

  // Selection
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Detail dialog
  const [detailEvent, setDetailEvent] = useState<EventResponse | null>(null);

  // Import result
  const [importMsg, setImportMsg] = useState('');

  const { data: calendars } = useQuery({
    queryKey: ['calendars'],
    queryFn: getCalendars
  });

  const dateParams = (() => {
    const now = new Date();
    switch (dateRange) {
      case '7d': return { start: new Date(now.getTime() - 7*86400000).toISOString(), end: now.toISOString() };
      case '30d': return { start: new Date(now.getTime() - 30*86400000).toISOString(), end: now.toISOString() };
      case 'month': return { start: new Date(now.getFullYear(), now.getMonth(), 1).toISOString(), end: now.toISOString() };
      case 'custom': return { start: customStart || undefined, end: customEnd || undefined };
      default: return {};
    }
  })();

  const { data, isLoading } = useQuery({
    queryKey: ['events-paged', search, calendarId, dateRange, customStart, customEnd, page],
    queryFn: () => getEventsPaged({
      search: search || undefined,
      calendarId: calendarId || undefined,
      start: dateParams.start,
      end: dateParams.end,
      page,
      pageSize: 50
    })
  });

  const importMut = useMutation({
    mutationFn: importIcs,
    onSuccess: (result) => {
      setImportMsg(`成功导入 ${result.imported} 条日程${result.skipped > 0 ? `，跳过 ${result.skipped} 条重复` : ''}`);
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
    },
    onError: (err: Error) => setImportMsg(`导入失败: ${err.message}`)
  });

  function toggleSelect(id: string) {
    setSelectedIds(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  function toggleSelectAll() {
    if (!data) return;
    const allIds = data.items.map(e => e.id);
    setSelectedIds(prev => prev.size === allIds.length ? new Set() : new Set(allIds));
  }

  function handleExportSelected() {
    exportIcs(Array.from(selectedIds));
  }

  function handleExportAll() {
    exportIcs(undefined, dateParams.start, dateParams.end);
  }

  function handleImport() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.ics';
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (file) importMut.mutate(file);
    };
    input.click();
  }

  const rruleLabel = (rrule?: string) => {
    if (!rrule) return '—';
    if (rrule.includes('DAILY')) return '每日';
    if (rrule.includes('WEEKLY')) return '每周';
    if (rrule.includes('MONTHLY')) return '每月';
    return '重复';
  };

  return (
    <div className="max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/settings')} className="text-gray-400 hover:text-gray-600">← 返回</button>
          <h2 className="text-xl font-bold">📅 管理日程数据</h2>
        </div>
        <div className="flex gap-2">
          <button onClick={handleImport} disabled={importMut.isPending}
            className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 disabled:opacity-50">
            {importMut.isPending ? '导入中...' : '📥 导入 ICS'}
          </button>
          <button onClick={handleExportSelected} disabled={selectedIds.size === 0}
            className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 disabled:opacity-50">
            📤 导出选中({selectedIds.size})
          </button>
          <button onClick={handleExportAll}
            className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50">
            📤 导出全部
          </button>
        </div>
      </div>

      {/* Import result message */}
      {importMsg && (
        <div className={`mb-3 p-3 rounded text-sm ${importMsg.startsWith('成功') ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'}`}>
          {importMsg}
          <button onClick={() => setImportMsg('')} className="ml-3 underline">关闭</button>
        </div>
      )}

      {/* Filter bar */}
      <div className="flex items-center gap-3 mb-3 bg-white border rounded-lg p-3">
        <input
          type="text" placeholder="搜索标题..."
          value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
          className="border rounded px-3 py-1.5 text-sm w-48"
        />
        <select value={calendarId} onChange={e => { setCalendarId(e.target.value); setPage(1); }}
          className="border rounded px-2 py-1.5 text-sm">
          <option value="">全部日历</option>
          {calendars?.map(cal => (
            <option key={cal.id} value={cal.id}>{cal.name}</option>
          ))}
        </select>
        <select value={dateRange} onChange={e => { setDateRange(e.target.value); setPage(1); }}
          className="border rounded px-2 py-1.5 text-sm">
          <option value="all">全部时间</option>
          <option value="7d">最近 7 天</option>
          <option value="30d">最近 30 天</option>
          <option value="month">本月</option>
          <option value="custom">自定义范围</option>
        </select>
        {dateRange === 'custom' && (
          <>
            <input type="date" value={customStart} onChange={e => setCustomStart(e.target.value)}
              className="border rounded px-2 py-1.5 text-sm" />
            <span className="text-sm text-gray-400">—</span>
            <input type="date" value={customEnd} onChange={e => setCustomEnd(e.target.value)}
              className="border rounded px-2 py-1.5 text-sm" />
          </>
        )}
        <span className="ml-auto text-sm text-gray-500">
          共 {data?.totalCount ?? '—'} 条
        </span>
      </div>

      {/* Table */}
      <div className="bg-white border rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b text-left">
              <th className="p-3 w-8">
                <input type="checkbox"
                  checked={data && selectedIds.size === data.items.length && data.items.length > 0}
                  onChange={toggleSelectAll} />
              </th>
              <th className="p-3">标题</th>
              <th className="p-3 w-20">日历</th>
              <th className="p-3 w-36">开始时间</th>
              <th className="p-3 w-36">结束时间</th>
              <th className="p-3 w-16">重复</th>
              <th className="p-3 w-16">操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={7} className="p-8 text-center text-gray-400">加载中...</td></tr>
            ) : !data || data.items.length === 0 ? (
              <tr><td colSpan={7} className="p-8 text-center text-gray-400">无日程数据</td></tr>
            ) : (
              data.items.map(event => {
                const cal = calendars?.find(c => c.id === event.calendarId);
                return (
                  <tr key={event.id} className="border-b hover:bg-gray-50">
                    <td className="p-3">
                      <input type="checkbox" checked={selectedIds.has(event.id)}
                        onChange={() => toggleSelect(event.id)} />
                    </td>
                    <td className="p-3 font-medium">{event.title}</td>
                    <td className="p-3">
                      {cal && (
                        <span className="inline-flex items-center gap-1 text-xs">
                          <span className="w-2 h-2 rounded-full" style={{ backgroundColor: cal.color }} />
                          {cal.name}
                        </span>
                      )}
                    </td>
                    <td className="p-3 text-gray-600">{new Date(event.dtStart).toLocaleString('zh-CN')}</td>
                    <td className="p-3 text-gray-600">{new Date(event.dtEnd).toLocaleString('zh-CN')}</td>
                    <td className="p-3 text-gray-500 text-xs">{rruleLabel(event.rrule)}</td>
                    <td className="p-3">
                      <button onClick={() => setDetailEvent(event)}
                        className="text-blue-600 hover:underline text-xs">详情</button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-1 mt-3">
          <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page <= 1}
            className="px-3 py-1.5 text-sm border rounded disabled:opacity-30 hover:bg-gray-50">
            上一页
          </button>
          {Array.from({ length: data.totalPages }, (_, i) => i + 1)
            .filter(p => p === 1 || p === data.totalPages || Math.abs(p - page) <= 2)
            .map((p, i, arr) => (
              <span key={p}>
                {i > 0 && arr[i - 1] !== p - 1 && <span className="px-1 text-gray-300">...</span>}
                <button onClick={() => setPage(p)}
                  className={`px-3 py-1.5 text-sm border rounded ${
                    p === page ? 'bg-blue-600 text-white' : 'hover:bg-gray-50'
                  }`}>
                  {p}
                </button>
              </span>
            ))}
          <button onClick={() => setPage(p => Math.min(data.totalPages, p + 1))} disabled={page >= data.totalPages}
            className="px-3 py-1.5 text-sm border rounded disabled:opacity-30 hover:bg-gray-50">
            下一页
          </button>
        </div>
      )}

      {/* Detail Dialog */}
      {detailEvent && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setDetailEvent(null)}>
          <div className="bg-white rounded-lg p-6 max-w-lg w-full mx-4 max-h-[90vh] overflow-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-lg">日程详情</h3>
              <button onClick={() => setDetailEvent(null)} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
            </div>
            <dl className="space-y-3 text-sm">
              <div><dt className="text-gray-400">标题</dt><dd className="font-medium">{detailEvent.title}</dd></div>
              <div><dt className="text-gray-400">日历</dt><dd>{calendars?.find(c => c.id === detailEvent.calendarId)?.name ?? detailEvent.calendarId}</dd></div>
              <div><dt className="text-gray-400">开始时间</dt><dd>{new Date(detailEvent.dtStart).toLocaleString('zh-CN')}</dd></div>
              <div><dt className="text-gray-400">结束时间</dt><dd>{new Date(detailEvent.dtEnd).toLocaleString('zh-CN')}</dd></div>
              {detailEvent.location && <div><dt className="text-gray-400">地点</dt><dd>{detailEvent.location}</dd></div>}
              {detailEvent.description && <div><dt className="text-gray-400">描述</dt><dd className="whitespace-pre-wrap">{detailEvent.description}</dd></div>}
              {detailEvent.rrule && <div><dt className="text-gray-400">重复规则</dt><dd>{rruleLabel(detailEvent.rrule)} ({detailEvent.rrule})</dd></div>}
              <div><dt className="text-gray-400">状态</dt><dd>{detailEvent.status}</dd></div>
            </dl>
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: Build frontend to verify TypeScript compilation**

```bash
npm --prefix src/client-web run build
```

Expected: 0 errors.

- [ ] **Step 3: Commit**

```bash
git add src/client-web/src/pages/CalendarDataManager.tsx
git commit -m "feat: add CalendarDataManager page with filters, table, detail view, ICS import/export"
```

---

### Task 6: Wire Routes and Sidebar

**Files:**
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`

- [ ] **Step 1: Add "设置" nav item to Sidebar**

In `Sidebar.tsx`, add to the `navItems` array (after PC记录):

```tsx
const navItems = [
  { label: '时间轴', path: '/timeline', icon: '⏱' },
  { label: '本周', path: '/week', icon: '📅' },
  { label: '月视图', path: '/month', icon: '📆' },
  { label: '任务', path: '/tasks', icon: '📋' },
  { label: 'PC记录', path: '/pc-tracker', icon: '💻' },
  { label: '设置', path: '/settings', icon: '⚙' },
];
```

- [ ] **Step 2: Add routes to AppLayout**

In `AppLayout.tsx`, add imports and routes:

```tsx
import SettingsPage from '../pages/SettingsPage';
import CalendarDataManager from '../pages/CalendarDataManager';
```

Add routes inside `<Routes>` (before the closing tag):

```tsx
<Route path="/settings" element={<SettingsPage />} />
<Route path="/settings/calendar-data" element={<CalendarDataManager />} />
```

- [ ] **Step 3: Build frontend to verify TypeScript compilation**

```bash
npm --prefix src/client-web run build
```

Expected: 0 errors. wwwroot updated automatically (vite.config.ts outputs to `../Pim.Api/wwwroot`).

- [ ] **Step 4: Commit**

```bash
git add src/client-web/src/layout/Sidebar.tsx src/client-web/src/layout/AppLayout.tsx
git commit -m "feat: add settings route and sidebar entry, wire CalendarDataManager"
```

---

### Task 7: End-to-End Verification

**Files:** None (test manually)

- [ ] **Step 1: Start API server on port 5858**

```bash
dotnet run --project src/Pim.Api/Pim.Api.csproj --urls "http://0.0.0.0:5858"
```

- [ ] **Step 2: Verify ICS export endpoint returns .ics file**

```bash
curl -s -o /dev/null -w "%{http_code}" "http://localhost:5858/api/v1/calendar/export-ics?start=2025-01-01T00:00:00Z&end=2026-12-31T23:59:59Z"
```

Expected: 401 (needs auth — correct) or 200 with file download if authenticated.

- [ ] **Step 3: Verify paged events endpoint**

```bash
curl -s "http://localhost:5858/api/v1/calendar/events?page=1&pageSize=5"
```

Expected: JSON with `items`, `page`, `totalCount`, etc.

- [ ] **Step 4: Open browser at http://localhost:5858**

- Click "设置" in sidebar → should see Settings page with "管理日程数据" card
- Click card → should see event table with filters
- Test search, calendar filter, date range filter
- Test pagination
- Click "详情" → should see detail dialog
- Select events, click "导出选中(N)" → should download .ics file
- Click "导入ICS", select an .ics file → should see success message

- [ ] **Step 5: Commit any fixes if needed**

---

### Task 8: Final Commit

- [ ] **Step 1: Verify full solution builds**

```bash
dotnet build
npm --prefix src/client-web run build
```

- [ ] **Step 2: Final commit if any remaining changes**

```bash
git add -A
git commit -m "feat: complete settings page with calendar data management"
```
