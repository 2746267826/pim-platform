# PC Records App Knowledge Redesign

## Goal

Redesign the PC records experience around daily review and App Knowledge, instead of exposing classification as a standalone rule-management workflow.

The current Route 3 work added useful backend foundations: stable record keys, preview, apply, recompute, audit, activity analysis, classification suggestions, and category-tree APIs. The product experience is still unsatisfying because the UI reads as a pile of modules and the classification surface feels like a separate backend console. This redesign keeps the Route 3 foundation but changes the user-facing model:

```text
PC records discovers unclear context
App Knowledge stores what the system learns
Category tree provides target categories
Backend rules remain an implementation detail
```

## Accepted Direction

- PC records should be a review-first page.
- App Knowledge is the primary place where classification knowledge lives.
- App Knowledge is app-centric.
- Context patterns are more important than simple app default categories.
- Category tree belongs under App Knowledge as a secondary page.
- The standalone Classification Management entry should be removed from the main user navigation.
- Classification suggestions should write back to App Knowledge after preview and confirmation.
- The default suggestion behavior is "smartly broaden after preview": the system recommends a useful scope, shows impact, and the user confirms it.

## Product Model

Users should not think in terms of rule tables, rule JSON, recompute jobs, or low-level classifier internals.

Users should think in terms of:

- This app is usually this kind of activity.
- This domain inside this browser app means something more specific.
- This title pattern points to this project or category.
- When I confirm a suggestion, the system learns it for next time.

Examples:

```text
Code.exe -> Work / Development
msedge.exe + github.com -> Work / Development
msedge.exe + docs.microsoft.com -> Learning / Technical Docs
Code.exe + title contains PIM -> project tag PIM
WeChat.exe + title contains a work contact -> Communication / Work
```

Classification still exists as an interpretation layer over raw PC facts. The difference is that user-facing controls now express knowledge as app, domain, title, category, and project mappings.

## Information Architecture

### Main Navigation

Keep:

- Today
- Calendar
- Quick Notes
- Files
- Tasks
- PC Records
- App Knowledge
- Status
- Settings

Remove from the main navigation:

- Classification Management
- standalone Category Tree

### Routes

Recommended route structure:

```text
/pc-tracker
/app-knowledge-base
/app-knowledge-base/categories
```

Compatibility redirects may remain:

```text
/pc-categories -> /app-knowledge-base/categories
/pc-classification -> /app-knowledge-base or an internal/admin page
```

The exact redirect behavior can be decided during implementation, but the sidebar should no longer present classification management or category tree as separate top-level products.

## PC Records Page

PC records should be review-first. The first screen answers: "What did today look like?"

### Page Order

1. Date, range, and data-quality controls.
2. Daily review summary.
3. Main classified timeline.
4. Context confirmations.
5. Drilldown modules.
6. Keyboard and mouse activity.

### Daily Review Summary

Use a small set of high-signal metrics:

- recorded duration
- effective active duration
- main categories
- context switches
- pending context confirmations
- input activity

Avoid making every metric visually equal. The summary should quickly explain the day, not expose all raw counters.

### Main Timeline

The classified timeline should become the strongest first-screen module. It should show:

- activity blocks by time
- category color
- app, domain, or title summary
- unknown or low-confidence segments
- a way to open detailed records

The timeline is the main review surface. Existing drilldown details can remain, but the page should not force users to scroll past unrelated modules before seeing the day.

### Context Confirmations

Replace the large "classification action queue" feel with context confirmation cards embedded beside or near the review surface.

Each card should read like a knowledge suggestion:

```text
Edge + github.com may be Work / Development.
Recommended scope: github.com/*
Impact: 42 records, 3.2 hours
```

Primary actions:

- Preview and confirm
- Ignore

Secondary or later actions can include:

- adjust scope
- choose a different category
- add project tag

### Drilldown Modules

Keep these as supporting modules below the main review surface:

- activity analysis heatmap
- daily activity ranking
- keyboard and mouse heatmap
- detail dialog

These are useful for investigation, but they should not compete with the first-screen review hierarchy.

### Feedback After Confirmation

After applying a suggestion, the UI should say what the system learned:

```text
Saved to App Knowledge: Edge / github.com -> Work / Development.
Recomputed 42 records.
```

The PC records timeline, activity analysis, suggestions, and App Knowledge data should refresh after confirmation.

## App Knowledge

App Knowledge is app-centric. The page starts with apps, not rules.

### App List

Each app row or card should show:

- display name
- process name
- icon
- default category
- number of known contexts
- recent affected duration
- pending context count

Useful filters:

- search app or process name
- pending contexts
- browser apps
- apps with no default category
- apps with recent high impact

### App Detail

The App detail page or panel should organize knowledge around context patterns:

- default category
- domain patterns
- title patterns
- project tags
- source and confidence
- recent impact

Domain and title patterns are first-class because one app can contain many meanings. Browser apps, chat apps, and IDEs need this more than simple app-level classification.

Recommended fields for each context pattern:

- app id or process name
- pattern type: domain, title, exact URL/path, window title, bucket/source family
- pattern value
- target category id
- optional project tag
- scope summary
- source: user confirmed, system suggested, builtin, imported
- last matched at
- affected record count and duration over recent windows
- enabled state

### Category Tree

Category tree becomes a secondary App Knowledge page:

```text
App Knowledge
  - Apps
  - Category Tree
```

The category tree only maintains category nodes:

- name
- color
- icon
- productivity attribute
- parent
- sort order

It should not be the place where users manage classification rules.

## Smart Broadening Flow

The default correction flow uses smart broadening after preview.

### Flow

```text
PC records finds unclear context
server builds App Knowledge suggestion
UI shows recommended scope and alternatives
user previews impact
user confirms
server saves App Knowledge entry
server creates or updates backend classifier rule
server recomputes affected records
server writes audit trail
UI refreshes review and knowledge surfaces
```

### Example

Observed activity:

```text
app: msedge.exe
domain: github.com
path: /openai/codex
title: pull request - PIM
```

The preview can offer:

- exact: only this path or record cluster
- recommended: github.com under Edge goes to Work / Development
- project-specific: github.com plus title contains PIM gets project tag PIM

The recommended option is selected by default only after impact is shown. The user can adjust scope before confirming.

### Preview Requirements

Preview must show:

- affected record count
- affected duration
- current category distribution
- new category distribution
- affected apps, domains, title samples, and source buckets
- conflicting existing knowledge
- warning when the scope may be too broad

### Apply Requirements

Apply must:

- save the App Knowledge entry
- map that knowledge to the backend classifier mechanism
- recompute affected records
- write audit details
- mark the originating suggestion handled
- refresh PC records and App Knowledge queries

The UI should not describe this as "creating a rule" unless shown in an internal or advanced context.

## Backend And Data Boundaries

Route 3 backend work remains valuable. The following can stay:

- stable record key service
- source identity metadata
- suggestion preview/apply endpoints
- recompute service
- classification audit rows
- activity analysis endpoint
- category tree endpoint
- backend classifier rule storage

The next API layer should add App Knowledge vocabulary above the existing classifier vocabulary.

Possible future endpoints:

```text
GET  /api/v1/pc/app-knowledge/apps
GET  /api/v1/pc/app-knowledge/apps/{id}
POST /api/v1/pc/app-knowledge/apps
PATCH /api/v1/pc/app-knowledge/apps/{id}
GET  /api/v1/pc/app-knowledge/apps/{id}/contexts
POST /api/v1/pc/app-knowledge/suggestions/{id}/preview
POST /api/v1/pc/app-knowledge/suggestions/{id}/apply
GET  /api/v1/pc/categories/tree
```

Existing classification endpoints can remain internally or as compatibility paths, but user-facing frontend code should increasingly express the workflow as App Knowledge.

## Frontend Migration

### Navigation

- Remove `Classification Management` from the sidebar.
- Remove standalone `Category Tree` from the sidebar.
- Keep `App Knowledge`.
- Add an App Knowledge sub-navigation or tabs for Apps and Category Tree.
- Add redirects or compatibility handling for old routes.

### Pages

`PcTrackerPage`:

- reorder modules around daily review
- move main timeline up
- turn classification queue into context confirmation cards
- change copy from classification/rules to App Knowledge learning
- keep drilldown modules below the main surface

`AppKnowledgeBasePage`:

- expand from simple app list into app-centric knowledge center
- add context pattern display and editing
- show pending contexts and recent impact
- link to Category Tree as a secondary view

`CategoryTreePage`:

- reuse under `/app-knowledge-base/categories`
- remove "Classification Management" language
- keep category node editing focused and bounded

`PcClassificationPage`:

- remove from top-level navigation
- optionally keep as an internal compatibility route, or redirect to App Knowledge until an advanced admin route is needed

## Testing Strategy

Frontend tests should cover:

- sidebar no longer shows standalone Classification Management
- sidebar no longer shows standalone Category Tree
- App Knowledge remains visible
- `/app-knowledge-base/categories` renders category tree
- legacy `/pc-categories` route redirects or remains compatible
- PC records page renders review-first ordering
- context confirmations use App Knowledge wording
- preview confirmation cannot apply stale impact results
- successful confirmation refreshes PC records and App Knowledge queries

Backend tests should cover:

- suggestion preview returns App Knowledge-oriented scope and impact
- apply persists knowledge and still uses recompute and audit
- broad scopes return warnings
- conflicts with existing knowledge are visible
- target category validation uses the category tree
- backend rules remain consistent with saved App Knowledge entries

Manual checks should cover:

- first screen reads as a daily review, not a rule queue
- category tree is reachable from App Knowledge
- confirming a suggestion produces "saved to App Knowledge" feedback
- timeline changes after recompute
- browser and mixed-context apps can show domain/title knowledge

## Migration Phases

### Phase 1: Information Architecture And Copy

Focus on frontend structure and wording while reusing most existing APIs.

- sidebar changes
- route changes and redirects
- PC records page reorder
- context confirmation copy
- category tree nested under App Knowledge

### Phase 2: App Knowledge Context Model

Add explicit App Knowledge context patterns.

- domain patterns
- title patterns
- project tags
- impact summaries
- suggestion preview/apply vocabulary

### Phase 3: Deeper Integration

Use App Knowledge outputs in broader PIM surfaces.

- Today summaries
- project statistics
- higher-level review insights
- optional advanced admin tools

## Acceptance Criteria

- PC records first screen is review-first.
- Users can confirm a context and understand that the system learned it.
- Confirmed suggestions write back to App Knowledge, not just a visible rule table.
- App Knowledge is app-centric and supports domain/title context knowledge.
- Category Tree is nested under App Knowledge.
- Classification Management is no longer a standalone main navigation item.
- Existing Route 3 safety properties remain: preview, apply, recompute, audit, stable keys.
- Existing useful modules remain available as drilldown tools.

## Out Of Scope

- Rebuilding the daemon capture layer.
- Replacing stable record identity work.
- LLM-generated classification.
- Today page integration in the first implementation phase.
- Full removal of backend rule storage.
