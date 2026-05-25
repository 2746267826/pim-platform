# Stage 3 Today Control Console Design

## Goal

Make the Web Today page the daily entry point for PIM.

Today should show what matters now: planned work, current PC activity state, system health, data quality, and pending review items. It is not a new business module and it must not become a catch-all dashboard API.

This stage turns Today into a server-backed, extensible, read-only section surface:

- The server provides section data.
- Web chooses layout, titles, ordering, and visual treatment.
- Existing business modules remain the source of truth.
- Future capabilities can add new Today sections without changing the core Today endpoint shape.

## User Decisions

Accepted decisions for this design:

- Do not implement quick notes in Stage 3.
- Do not implement plan-vs-reality deviation analysis in Stage 3.
- Web controls presentation, including titles, order, layout, card size, and empty-state wording.
- The Today API should support future columns or cards through section registration.
- Implement the extensible version now: a lightweight section registry plus per-section loading.

## Scope

In scope:

- A Today section registry endpoint.
- A per-section data endpoint.
- Stable section identifiers and kinds.
- Provider-based server implementation for adding future sections.
- Web Today page loading section registry first, then loading known sections individually.
- Server-side data state for each section, such as normal, empty, warning, critical, or unavailable.
- Links from sections to existing detail pages.
- Section-level error isolation so one failed section does not break the whole Today page.
- Backend tests for registry, section loading, unknown sections, and error handling.
- Frontend tests for API paths, unknown section kinds, and page loading behavior.
- Manual acceptance documentation for Stage 3.

Out of scope:

- Quick notes data model, API, floating entry button, or inbox.
- Plan-vs-reality matching, reports, or deviation scoring.
- Daily review, scheduling recommendations, or AI summaries.
- Server-provided UI titles, layout, display order, column placement, or card sizing.
- Replacing existing Calendar, PC Tracker, or Status APIs.
- Building a formal MCP server.

## Product Model

Today is a section surface.

Each section is a small, typed summary from an existing capability. A section may point to a detail page where the user can act. The section itself is read-only in this stage.

Examples:

- Calendar schedule section points to the calendar page.
- Task section points to the task list or calendar page.
- PC activity section points to the PC tracker page.
- PC quality section points to PC data quality details.
- System health section points to the status page.
- Classification suggestions section points to the PC tracker correction flow.

The user should be able to open `/today` and quickly answer:

- What is planned today?
- Which tasks need attention?
- Is PC data being collected?
- Is the daemon healthy?
- Is there data quality trouble?
- Are there classification suggestions to review?

## API Design

Stage 3 adds a Today API group:

- `GET /api/v1/today/sections?date=YYYY-MM-DD`
- `GET /api/v1/today/sections/{sectionId}?date=YYYY-MM-DD`

Both endpoints require the same authentication boundary as the existing Web APIs.

### Registry Response

The registry endpoint returns metadata for available sections only. It does not return heavyweight section data.

```json
{
  "date": "2026-05-25",
  "pcBusinessDate": "2026-05-25",
  "generatedAt": "2026-05-25T10:00:00Z",
  "sections": [
    {
      "id": "calendar.schedule",
      "kind": "calendar.schedule",
      "status": "available",
      "links": [
        {
          "rel": "self",
          "href": "/api/v1/today/sections/calendar.schedule?date=2026-05-25"
        }
      ]
    }
  ]
}
```

The registry must not include UI titles, layout, priority, column, or display-order fields.

### Section Response

The per-section endpoint returns one section payload.

```json
{
  "id": "calendar.schedule",
  "kind": "calendar.schedule",
  "status": "normal",
  "generatedAt": "2026-05-25T10:00:00Z",
  "data": {
    "events": [],
    "scheduledTasks": []
  },
  "links": [
    {
      "rel": "details",
      "href": "/calendar"
    }
  ]
}
```

The outer contract is stable:

- `id`: stable section id.
- `kind`: stable renderer key for Web.
- `status`: semantic data state.
- `generatedAt`: server generation timestamp.
- `data`: section-specific payload.
- `links`: related API or Web targets.
- `error`: optional safe error summary when unavailable.

The server must not return UI titles, layout, priority, column, or card sizing.

### Section Status

Use a small semantic status set:

- `available`: registry-only status meaning the section can be loaded.
- `normal`: data is available and does not require attention.
- `empty`: no data exists for the selected date.
- `warning`: data is available but needs attention.
- `critical`: data indicates a serious failure.
- `unavailable`: the section failed to load or a dependency is unavailable.

Web maps these states to presentation.

### Section Links

Links use stable relations:

- `self`: section API URL.
- `details`: existing Web detail page.
- `api`: existing source API when useful for debugging or future clients.

Links do not imply layout.

## Initial Sections

Stage 3 includes these sections.

### `calendar.schedule`

Shows today's dated plan:

- Events from the selected date range.
- Tasks scheduled for the selected date.
- Basic time range and object ids.

Source of truth:

- `CalendarService`

Detail link:

- `/calendar`

This section does not create, update, move, or delete calendar objects.

### `calendar.tasks`

Shows task attention data:

- Incomplete tasks.
- Tasks due today.
- Overdue tasks.
- Unscheduled tasks when useful for Today.

Source of truth:

- `CalendarService`

Detail links:

- `/tasks`
- `/calendar`

This section does not implement scheduling or plan-vs-reality comparison.

### `pc.activity`

Shows today's PC activity summary:

- Recorded duration.
- Active input duration.
- Key and click totals.
- Main app ranking.
- Heatmap or compact distribution data.
- Existing category summaries when available.

Source of truth:

- `PcTrackerService`

Detail link:

- `/pc-tracker`

This section reuses existing PC summary data. It does not query or expose raw event detail directly.

### `pc.quality`

Shows PC data quality state:

- Overall quality status.
- Component status.
- Issue count.
- Next-step data from the service.

Source of truth:

- `PcTrackerQualityService`

Detail link:

- `/pc-tracker`

This section explains collection quality but does not repair data.

### `operations.health`

Shows system and daemon health:

- Aggregate system status.
- Windows daemon freshness.
- Background jobs state.
- Database/API status through the existing status model.

Source of truth:

- `ISystemStatusService`

Detail link:

- `/status`

This section must reuse server health interpretation. Web must not recreate daemon freshness rules.

### `pc.classification_suggestions`

Shows classification review pressure:

- Pending suggestion count.
- Representative pending suggestions if cheap to load.
- Status warning when there are pending suggestions.

Source of truth:

- Existing PC classification suggestion flow.

Detail link:

- `/pc-tracker`

This section does not accept or reject suggestions. Existing PC tracker flows handle that.

## Server Architecture

Add a small Today surface, not a new business owner.

Core pieces:

```csharp
public sealed record TodayQuery(DateOnly Date, DateOnly PcBusinessDate);

public interface ITodaySectionProvider
{
    string SectionId { get; }
    string Kind { get; }
    Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct);
}
```

The Today layer contains:

- `TodaySectionRegistry`
- `TodaySectionService`
- `TodayEndpoints`
- shared Today DTOs in `Pim.Core` or a small API-facing namespace

Responsibilities:

- Parse and normalize the requested date.
- Compute the PC business date using the same rule as the Web currently uses.
- List registered sections.
- Load one section by id.
- Convert missing section ids to 404.
- Convert provider failures into safe `unavailable` section responses.

Non-responsibilities:

- Owning calendar business rules.
- Owning PC tracker classification.
- Owning health interpretation.
- Owning Web layout or presentation.

## Provider Boundaries

Provider implementations should stay close to the owning business services.

Calendar-backed providers call `CalendarService`.

PC-backed providers call `PcTrackerService`, `PcTrackerQualityService`, and existing classification suggestion services.

Operations-backed providers call `ISystemStatusService`.

Providers return section DTOs and links. They do not return JSX, CSS, title strings, layout hints, or display order.

If a provider has no data, it should return `empty` with a meaningful data shape, not `null`.

If a provider cannot load because of dependency failure, the Today service should return `unavailable` with a safe error code/message. Sensitive exception details stay in logs.

## Web Design

Web controls presentation.

Today page flow:

1. Query the registry endpoint for the selected date.
2. Filter registry sections to known `kind` values.
3. Load known sections independently.
4. Render each known kind with a dedicated component.
5. Ignore or show a generic placeholder for unknown section kinds.

Web owns:

- Chinese display titles.
- Section ordering.
- Responsive layout.
- Card sizes.
- Loading skeletons.
- Empty-state copy.
- Visual mapping for status.
- Navigation button labels.

The current Today page should be refactored so it no longer combines Calendar, PC, and Status business interpretation from multiple raw APIs. Existing detail pages still use their existing APIs.

Recommended initial renderers:

- `TodayScheduleSection`
- `TodayTasksSection`
- `TodayPcActivitySection`
- `TodayPcQualitySection`
- `TodayHealthSection`
- `TodayClassificationSuggestionsSection`

Unknown sections must not crash the page.

## Error Handling

Registry failure means Today cannot determine available sections. Web should show a page-level error.

Single-section failure should be isolated:

- The section endpoint returns `unavailable`.
- Web shows that card as unavailable.
- Other sections continue loading and rendering.

404 for unknown section ids should use the existing `ApiResponse<T>` error shape.

Invalid dates should return a validation error. The implementation should accept the existing local date format used by Web, `YYYY-MM-DD`.

Provider exceptions should be logged with section id and correlation id. The response must avoid stack traces or sensitive data.

## Extensibility

Future features add Today sections by registering new providers.

Examples:

- `quick_notes.inbox` in Stage 4.
- `plan_reality.delta` in Stage 6.
- `review.daily` in Stage 8.
- `calendar.external_status` in Stage 10.
- `mcp.recent_actions` in Stage 15.

These additions should not require changing:

- Registry response shape.
- Per-section response outer shape.
- Existing section ids.
- Web layout ownership rules.

If sections become numerous or expensive, the chosen per-section loading model already supports independent loading, refresh, and future caching.

## Testing

Backend tests:

- Registry returns the initial Stage 3 section ids and kinds.
- Registry does not return title, layout, order, priority, column, or card-size fields.
- Single-section endpoint returns data for each registered section.
- Unknown section id returns 404.
- Provider exception returns an `unavailable` section without breaking other sections.
- Date parsing uses the requested date and computes PC business date consistently.
- Calendar section uses Calendar service data and does not duplicate business rules.
- PC activity section uses PC summary data.
- PC quality section uses PC quality service data.
- Health section uses `ISystemStatusService`.
- Classification suggestions section reports pending review pressure without applying suggestions.

Frontend tests:

- Today API paths are stable.
- Today page requests registry first.
- Known section kinds map to Web renderers.
- Unknown section kinds do not crash the page.
- Web owns titles and order locally.
- Section unavailable state renders without breaking the page.
- Existing detail-page links are used for navigation.

Manual verification:

- Open `/today` and see schedule, tasks, PC activity, PC quality, system health, and classification suggestion sections.
- Stop or stale the Windows daemon heartbeat and confirm the health section shows attention.
- Use a day with no PC records and confirm the PC activity or quality section shows a clear empty/unavailable state.
- Create pending classification suggestions and confirm Today shows a route into the PC tracker flow.
- Confirm no quick-note UI is implemented in Stage 3.
- Confirm no plan-vs-reality deviation section is implemented in Stage 3.
- Confirm Calendar, PC Tracker, Status, and Classification detail pages still use their existing APIs.

## Completion Definition

Stage 3 is complete when:

- Today has a section registry endpoint.
- Today has a per-section loading endpoint.
- Initial Stage 3 sections can be loaded independently.
- Web renders Today from section data while owning titles, order, and layout.
- One section failure does not break the whole Today page.
- The page surfaces plan, task, PC activity, data quality, daemon/system health, and classification review pressure.
- Quick notes and plan-vs-reality remain deferred.
- The design leaves a clear provider path for future Today sections.
