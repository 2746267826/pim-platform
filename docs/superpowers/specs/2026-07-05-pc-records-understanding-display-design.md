# PC Records Understanding And Display Route 3 Design

## Goal

Route 3 improves PC records by closing the classification feedback loop, clarifying category ownership, and adding better analysis modules to the PC records page.

The work should make user corrections reliable: a correction must be previewable, applied through one backend path, recomputed, audited, and visible in the existing PC records views.

## Accepted Direction

- Keep the current daemon behavior unchanged. It is running well enough for this stage.
- Do not change the Today page in this stage.
- Do not add LLM classification in this stage.
- Do not replace the existing PC records modules shown in the current UI:
  - Category timeline.
  - The "view details" secondary panel.
  - Keyboard and mouse heatmap.
- New UI ideas must be added as new modules, even when they are similar to existing functionality.
- Future UI proposals for this area must show the full PC records page, not isolated new modules only.
- Full "B" work, meaning persisted interpreted activity records as first-class entities, is deferred. This stage adds only the lightweight stability contract needed to protect classification identity.

## Product Model

Raw PC facts remain facts. Classification is an interpretation layered on top of those facts.

The user should see:

- What category an activity has.
- Why the system suggested a change.
- What a rule would affect before it is applied.
- What changed after applying a rule.
- Which existing modules still show the resulting timeline, detail rows, and keyboard/mouse activity.

The user should not need to understand rule JSON, legacy category tables, or ActivityWatch source identifiers.

## Scope

In scope:

- A closed suggestion-to-rule classification flow.
- Rule preview and confirmed apply for suggestions and manual corrections.
- Real recomputation for affected records or ranges.
- Audit records for classification changes.
- Category system cleanup around `pc_categories` as the single final category tree.
- Stable record keys and source identity metadata for classification snapshots.
- A new activity analysis heatmap module.
- A new classification action queue module.
- Full PC records page layout that includes both new modules and preserved existing modules.
- Backend, frontend, and GitHub Actions verification.

Out of scope:

- Daemon reliability redesign.
- Today page changes.
- LLM-generated classification.
- Replacing the existing category timeline.
- Replacing the existing detail secondary panel.
- Replacing the existing keyboard and mouse heatmap.
- Full persisted interpreted activity record storage.

## Classification Closed Loop

The current direct "accept suggestion" path is too risky because it can create rules or snapshots without a full preview, recompute, and audit trail.

All classification changes should use this flow:

```text
suggestion -> rule draft -> preview -> user confirmation -> save rule -> recompute -> audit -> mark suggestion handled -> refresh page
```

### Suggestion Handling

The UI should replace blind acceptance with "Process and preview".

When the user starts from a suggestion, the server generates a rule draft. The draft can target narrow conditions such as:

- App.
- App plus title.
- App plus domain.
- Browser domain.
- Window title pattern.
- ActivityWatch bucket or event family.

The frontend does not assemble the final rule JSON. The backend owns rule draft creation and validation.

### Preview

Preview must show:

- Affected record count.
- Affected duration.
- Current category distribution.
- New category distribution.
- Affected apps, domains, titles, or source buckets.
- Conflicting existing rules.
- Whether the rule scope is unusually broad.

The user can cancel, adjust, or confirm.

### Apply

Applying a rule must:

- Save the validated rule through one backend service.
- Recompute affected classification snapshots.
- Write an audit record.
- Mark the originating suggestion as handled or stale.
- Refresh suggestion and analysis data in the PC records page.

Direct paths that create rules without preview should be removed from the UI or internally redirected into the same preview/apply flow.

## Category System Cleanup

`pc_categories` becomes the single authoritative final category tree.

Other category-like data stays useful, but only as classification hints:

- Legacy app categories can suggest a default category for an app.
- Activity category rules can match evidence and point to a category.
- Builtin knowledge can provide app or domain hints.

The final classification result should point to a category in `pc_categories`.

Example:

```text
condition:
  app = msedge.exe
  domain contains docs.microsoft.com

target:
  Learning / Technical Documentation
```

The user-facing UI should expose category choices as a tree, not as separate legacy systems.

### API Boundary

Use clearer category and classification boundaries:

```text
GET  /api/v1/pc/categories/tree
GET  /api/v1/pc/classification/rules
POST /api/v1/pc/classification/rules/preview
POST /api/v1/pc/classification/rules/apply
PATCH /api/v1/pc/classification/rules/{id}
GET  /api/v1/pc/classification/suggestions
POST /api/v1/pc/classification/suggestions/{id}/preview
POST /api/v1/pc/classification/suggestions/{id}/apply
POST /api/v1/pc/classification/recompute
GET  /api/v1/pc/activity-analysis
```

Existing compatibility endpoints can remain, but they should not gain more responsibilities.

## Lightweight B: Stable Record Identity

Full B, persisted interpreted PC activity records, is important but deferred. This stage still needs stable identity so user corrections do not drift when interpretation logic changes.

Add a lightweight stability contract:

- Record keys are generated by one backend service.
- Source event information is preferred over reconstructed interpretation fields.
- Keys include a version prefix.
- Classification snapshots store source identity metadata where available.
- Recompute uses the same key service as suggestion, preview, apply, and timeline queries.

Recommended key shape:

```text
pc-aw-v1:{bucketId}:{eventId}
```

When event id is unavailable, fallback keys may use source type, start time, end time, app, and title. Fallback keys must be marked as lower stability so future full B migration can handle them carefully.

Classification snapshots should store:

- Record key.
- Key version.
- Source type.
- Bucket id when available.
- Event id when available.
- Start time.
- End time.
- Interpretation version.
- Classification version.

This is not a replacement for full B. It is the minimum foundation required before full B.

## Backend Design

### Services

`ActivitySuggestionService`

Finds low-confidence or unknown activity and produces reviewable suggestions. It does not save rules directly.

`ClassificationRuleDraftService`

Builds server-owned rule drafts from suggestions, quick corrections, or manual user input.

`ClassificationPreviewService`

Runs dry-run classification and returns impact, conflicts, duration, and before/after distributions.

`ActivityClassificationRuleService`

Owns rule validation, creation, update, activation, deactivation, priority, and uniqueness.

`ActivityClassificationRecomputeService`

Performs real recomputation for selected ranges or affected record keys. The current recompute stub should become useful for this stage.

`ActivityClassificationAuditService`

Records classification changes, rule application, affected ranges, affected keys, and suggestion state changes.

`PcActivityRecordKeyService`

Generates stable record keys and exposes source identity metadata for snapshots, suggestions, preview, and recompute.

### Known Issues To Fix

- Frontend `scope: app` must match backend classifier behavior.
- Suggestion acceptance must not bypass preview, apply, recompute, and audit.
- Direct rule creation must not bypass validation and preview.
- `range = all` must either be supported consistently or removed from frontend types.
- User rule and builtin rule precedence must be explicit.
- Rule JSON must be generated and validated by the server for final apply.

### Errors

The backend should return actionable errors when:

- The target category does not exist.
- The rule JSON is invalid.
- The rule name is duplicated.
- The rule conflicts with an existing higher-priority rule.
- The rule scope is too broad for automatic application.
- Source records cannot be located.
- Recompute fails.

The frontend should show these as adjustment states, not as successful processing.

## Frontend Design

The PC records page remains the main working surface.

Keep these existing components and their core behavior:

- `CategoryTimeline`
- `EventTimelineDialog`
- `KeyboardHeatmap`

Add these components:

- `ActivityAnalysisHeatmap`
- `ClassificationActionQueue`
- `ClassificationPreviewDialog`
- `RuleImpactPreviewPanel`

### Full Page Structure

Future UI mockups and implementation reviews should show the complete PC records page:

1. Date, range, and data-quality controls.
2. Overview metrics.
3. New activity analysis heatmap.
4. New classification action queue.
5. Existing category timeline.
6. Existing "view details" secondary panel.
7. Existing keyboard and mouse heatmap.

The new modules can appear above the existing modules or in a two-column layout on wide screens. Narrow screens should stack modules in the same logical order.

### Activity Analysis Heatmap

This is a new module. It does not replace the keyboard and mouse heatmap.

It shows activity by time block:

- Activity intensity.
- Pending classification presence.
- Frequent context switching.
- Classification-change density.
- Selected time-block summary.

Clicking a block should update the selected interval summary and can highlight or scroll the existing category timeline to the same time range.

### Classification Action Queue

This is a new module for pending classification work.

Actions:

- Process and preview.
- Ignore.
- Later.
- Future option: merge into existing rule.

The queue should never directly persist a classification change. It must call preview first, then apply after user confirmation.

### Existing Module Preservation

The existing category timeline, detail secondary panel, and keyboard/mouse heatmap remain stable. They can receive refreshed data after recompute, but their core interaction model should not be replaced in this stage.

## Testing Strategy

### Backend Tests

Cover:

- Stable key generation for the same source event.
- Stable key reuse across interpretation changes.
- Suggestion preview creates a valid rule draft.
- Rule apply saves the rule, recomputes snapshots, audits the change, and marks the suggestion handled.
- Invalid categories fail.
- Duplicate rule names fail.
- Conflicting or too-broad rules return preview warnings or apply errors.
- `scope: app` behavior is consistent between frontend contracts and backend classifier.
- Recompute no longer behaves as a stub.
- Fallback keys are marked as lower stability.

### Frontend Tests

Cover:

- The direct accept path is removed or redirects into preview.
- "Process and preview" calls the preview API.
- Apply success refreshes suggestions, activity analysis, and classification timeline data.
- Preview errors are visible.
- The full PC records page still renders the existing category timeline, detail panel, and keyboard/mouse heatmap.
- The new activity analysis heatmap is separate from the keyboard/mouse heatmap.

### Manual UI Checks

Open the PC records page and verify:

- New modules and existing modules appear on the same full page.
- Pending classification state is visible.
- Activity analysis colors are understandable.
- The existing category timeline is not replaced.
- The existing detail secondary panel remains available.
- The existing keyboard/mouse heatmap remains available.
- Applying a rule updates the visible classification results after recompute.

## Verification And Delivery

Local verification should be run before committing implementation work:

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
git status --short --branch
```

This repository also has GitHub Actions builds. At suitable integration points, use the GitHub workflow as the remote build signal.

For direct `master` work:

```powershell
git status --short --branch
git add <intentional files>
git commit -m "feat: improve pc classification route 3"
git push origin master
gh run watch
```

For branch or PR work:

```powershell
git checkout -b feat/pc-classification-route-3
git add <intentional files>
git commit -m "feat: improve pc classification route 3"
git push -u origin feat/pc-classification-route-3
gh pr create
gh run watch
```

The GitHub Actions result should be treated as the final build confirmation after local checks. If local verification fails, do not push as a successful implementation. If GitHub Actions fails, record the failing workflow, job, and log summary before continuing.

Generated brainstorming mockups under `.superpowers/brainstorm/` are reference material and should not be committed unless the user explicitly asks to preserve them in source control.

## Implementation Order

1. Add and test stable record key service.
2. Normalize classification category and scope contracts.
3. Build rule draft and preview services.
4. Route suggestion apply and manual rule apply through one backend path.
5. Implement real recompute and audit behavior.
6. Add activity analysis API for the new heatmap.
7. Add frontend action queue and preview dialog.
8. Add frontend activity analysis heatmap.
9. Integrate new modules into the full PC records page while preserving existing modules.
10. Run local verification, then use GitHub Actions through `gh` at the integration point.

## Acceptance Criteria

- Users can no longer accidentally accept a classification suggestion without preview.
- A suggestion can be previewed, applied, recomputed, audited, and marked handled.
- Final classification results point to the authoritative category tree.
- Stable record keys protect user corrections across recompute.
- The PC records page shows the new analysis and action modules together with the preserved existing modules.
- The existing category timeline, detail secondary panel, and keyboard/mouse heatmap remain available.
- Local verification commands pass or failures are documented.
- GitHub Actions is used at the appropriate integration point to confirm the remote build.
