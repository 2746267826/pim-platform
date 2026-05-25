# Stage 2 PC Activity Understanding Design

## Goal

Turn PC records from "what app was open" into "what I was doing, which context it belonged to, and why PIM judged it that way."

This stage focuses on local, non-LLM intelligence:

- Improve category and project-tag quality.
- Give the user a practical correction loop.
- Keep history changes previewable, confirmable, and auditable.
- Keep AI integration out of scope for now, while preserving safe draft-rule boundaries for the future.

## User Model

Raw PC records are like monitoring footage: they are facts and must not be rewritten.

Classification is like attaching labels to that footage: labels can be explained, changed, recomputed, and audited.

The user should not need to understand rule engines or database tables. The product should show:

- What label was attached.
- Why it was attached.
- How confident the system is.
- What will change before a correction is applied.
- Which historical range the correction will affect.

## Scope

In scope:

- Persistent derived classification snapshots.
- Server-owned classification and recomputation.
- Rule management for builtin and user rules.
- Unknown and low-confidence activity suggestions.
- Quick correction from the PC records page.
- A dedicated classification management page.
- Free-text project tags with recently used suggestions.
- Timeline smoothing with a configurable "recommended minimum classification duration."
- Impact preview before applying rules.
- Audit logs for large or historical changes.
- Better builtin rules, heuristics, project-tag inference, and cluster quality.

Out of scope:

- LLM-generated rule drafts.
- Natural-language correction.
- Automatic activation of AI suggestions.
- A heavyweight project management model.
- Replacing raw ActivityWatch or KeyStats facts with derived labels.

## Product Design

### PC Records Page

The PC records page remains the daily working surface.

It should show classification results directly in the timeline and detail views:

- Category.
- Category color.
- Project tag.
- Confidence.
- Source.
- Explanation.

It should add two first-class correction entry points:

- Unknown activity clusters.
- Quick correction from a timeline block or detail row.

Unknown clusters group low-confidence or fallback activity into a small number of reviewable cards. A card should show representative evidence, total duration, sample count, and proposed matching signals.

Quick correction lets the user choose a category, optionally type a project tag, and create a narrow reusable rule from the current activity. Project tags are free text, with suggestions from recently used tags.

Before saving a rule, the page asks the server for an impact preview. The user then chooses an application range:

- Today only.
- A date range.
- All history.

Only after confirmation does the server create or activate the rule, recompute the selected range, and write audit records.

### Classification Management Page

The dedicated classification management page handles heavier governance:

- Browse rules.
- Filter by status, source, category, project tag, and text.
- Inspect rule conditions and explanations.
- Edit user rules.
- Archive or disable user rules.
- Review pending and rejected suggestions.
- Run historical recomputation with preview and confirmation.
- Show recent classification audit events.

Builtin rules should be visible and explainable, but not destructively editable. If the user needs to override a builtin rule, the system should create a higher-priority user rule.

## Architecture

Stage 2 uses three layers.

### Raw Fact Layer

ActivityWatch, browser, AFK, and KeyStats records remain the source of truth.

Raw records are preserved with their original source identifiers and metadata. Derived classification never overwrites them.

### Rule Layer

`pc_activity_category_rules` describes why records should receive labels.

Rules can come from:

- `builtin`
- `user`
- future `llm_suggested` drafts
- future `llm_corrected` drafts

Only active builtin and user-confirmed rules affect classification in this stage. Future AI suggestions remain draft artifacts until explicitly accepted.

Rules should support conditions over:

- Record type.
- App name.
- Normalized app name.
- Domain.
- URL path.
- Title.
- Window title.
- File path.
- Bucket type.

User rules outrank builtin rules. Higher priority wins within the same source group.

### Derived Classification Layer

Add a persistent derived cache, `pc_activity_classifications`.

Each classification snapshot stores:

- Stable record key.
- Record type.
- Source event ids or source identifiers.
- Time range.
- Category name.
- Category color.
- Project tag.
- Confidence.
- Source.
- Source rule id.
- Explanation.
- Classifier version.
- Classified at timestamp.
- Recompute job id or audit id when available.

The stable record key must be deterministic enough to update a classification for the same underlying activity without duplicating snapshots.

Query APIs should prefer stored classifications. If a new record has no classification snapshot, the server may classify it on demand and enqueue or perform a lightweight cache write.

## Classification Flow

The classifier evaluates activity in this order:

1. Active user rules.
2. Strong builtin rules.
3. Local heuristics.
4. Project/context inference.
5. Neighbor context for short ambiguous records.
6. Fallback to `其他`.

The output always includes a category, color, confidence, source, and explanation. Project tag can be null.

Useful local heuristics include:

- IDEs and known code editors -> `编程`.
- Terminals -> `终端`.
- Documentation, API reference, and tutorial pages -> `学习`.
- GitHub or GitLab repository pages -> `编程`, with repository name as project tag when possible.
- Localhost and known development ports -> `编程`.
- Mail, chat, meeting, and calendar signals -> `沟通`.
- File managers -> `文件`, with project tag inherited only when the surrounding context is strong.
- Entertainment domains and apps -> `娱乐`.

Heuristics should prefer narrow, explainable matches. Broad app-level rules are acceptable for known tools but should be easy to override.

## Recommended Minimum Classification Duration

Add a user-adjustable display and suggestion parameter named `recommendedMinimumClassificationDuration`.

Suggested UI presets:

- 1 minute.
- 3 minutes.
- 5 minutes.
- 10 minutes.
- 15 minutes.

Default: 5 minutes.

This parameter controls timeline smoothing and suggestion granularity. It does not delete raw records and does not erase the underlying classification snapshot.

Short blocks below the threshold can be merged into a surrounding stable context when:

- They are between similar higher-confidence blocks.
- They have low confidence.
- They do not have a strong user-rule match.
- They do not have an independent project tag.
- They look like incidental switching rather than a standalone task.

Short blocks should not be merged when:

- A user rule matched them.
- The classification is high-confidence and semantically distinct.
- The project tag is independent.
- The activity is a standalone short operation, such as a brief chat reply.
- The user previously corrected that kind of activity.

The timeline can display a smoothed view by default and retain access to raw classified segments in detail views.

Unknown-cluster generation should also respect this parameter so the user reviews manageable clusters instead of tiny fragments.

## Suggestions And Corrections

Suggestions are review artifacts, not active rules.

The system creates suggestions from fallback or low-confidence activity. It should cluster by meaningful signals:

- Web: domain plus title tokens plus browser app.
- IDE: app plus repository or project token.
- Terminal: app plus cwd or title token where available.
- Local file: extension plus parent path token.
- General app: normalized app plus title tokens.

Suggestions are sorted by:

1. Total duration.
2. Sample count.
3. Recency.

Accepting a suggestion starts the same flow as quick correction:

1. Build or edit a narrow rule.
2. Preview impact.
3. Confirm application range.
4. Save rule.
5. Recompute selected range.
6. Mark suggestion accepted.
7. Write audit records.

Rejecting a suggestion marks the cluster rejected so it does not immediately reappear unchanged.

## Impact Preview And Recompute

Before applying a new or changed rule, the server must support dry-run impact preview.

The preview should include:

- Proposed rule summary.
- Application range.
- Number of affected records or classified segments.
- Total affected duration.
- Current category distribution.
- New category distribution.
- Representative samples.
- Whether the operation requires confirmation.

After confirmation, recomputation should:

- Apply only the selected range.
- Create or update classification snapshots.
- Preserve raw records.
- Mark affected suggestions as accepted, stale, or still pending as appropriate.
- Write audit logs containing actor, action, object, range, result, source, and counts.

Because this stage introduces persistent classification snapshots, `classification/recompute` should evolve from a placeholder into a real service operation.

## API Design

Existing timeline and detail APIs should continue returning classification fields.

Add or complete APIs for:

- `GET /api/v1/pc/classification/rules`
- `POST /api/v1/pc/classification/rules/preview`
- `POST /api/v1/pc/classification/rules`
- `PATCH /api/v1/pc/classification/rules/{id}`
- `POST /api/v1/pc/classification/rules/{id}/archive`
- `GET /api/v1/pc/classification/suggestions`
- `POST /api/v1/pc/classification/suggestions/{id}/preview`
- `POST /api/v1/pc/classification/suggestions/{id}/accept`
- `POST /api/v1/pc/classification/suggestions/{id}/reject`
- `POST /api/v1/pc/classification/recompute/preview`
- `POST /api/v1/pc/classification/recompute`
- `GET /api/v1/pc/classification/audit`
- `GET /api/v1/pc/classification/project-tags/recent`
- `GET/PUT /api/v1/pc/classification/settings`

Settings include `recommendedMinimumClassificationDuration`.

## Error Handling

Invalid rule conditions should return user-facing validation errors before preview or save.

Preview and apply must fail safely:

- If preview fails, no rule is activated.
- If recompute fails after rule creation, the operation is marked failed in audit and can be retried.
- If a rule creates unexpectedly broad impact, the response should require explicit confirmation.
- If a record no longer exists during recompute, the operation should skip it and report skipped counts.

LLM endpoints are not implemented in this stage. Future AI payloads must use sanitized URL and context data only.

## Testing

Backend tests:

- Raw records are not modified by classification.
- Classification snapshots are created and updated deterministically.
- User rules override builtin rules.
- Rule priority ordering works.
- Domain, URL path, title, app, record type, and bucket type conditions work.
- Project tags can be free text.
- Recently used project tags are returned.
- Unknown clusters group low-confidence activity into manageable suggestions.
- Rejected suggestions do not immediately reappear unchanged.
- Impact preview counts match the eventual applied recompute.
- Today, date-range, and all-history ranges are honored.
- Audit logs are written for accepted suggestions, rule changes, and recomputes.
- Recommended minimum duration smooths short ambiguous blocks.
- Strong short blocks are not swallowed by smoothing.
- URL sanitizer continues removing query strings, fragments, userinfo, and token-like path segments.

Frontend tests:

- PC records page renders suggestions and quick correction entry points.
- Rule preview is shown before save.
- The user can choose today, date range, or all history before applying.
- Project tag input accepts free text and recent suggestions.
- Category timeline uses server-provided classification fields.
- Smoothing settings are sent to the server or applied only from server-provided smoothed data.
- Classification management page lists, filters, edits, and archives rules.

Manual verification:

- Open a PC tracker day with code, docs, terminal, file manager, and communication activity.
- Confirm the timeline is readable and not overly fragmented at the 5-minute default.
- Lower the threshold to 1 minute and confirm more detail appears.
- Raise the threshold to 10 minutes and confirm incidental fragments merge without hiding clear short tasks.
- Accept an unknown docs-domain suggestion as `学习` with a project tag.
- Preview the impact for today only, apply it, and confirm the timeline changes.
- Run a date-range recompute from classification management and confirm audit entries show the range and affected counts.

## Completion Definition

Stage 2 is complete when:

- The timeline displays meaningful categories and project tags.
- Classification results are explainable and persisted as derived snapshots.
- Unknown activity becomes a small set of actionable suggestions.
- User corrections create durable rules.
- Historical changes require preview, range selection, confirmation, and audit.
- The recommended minimum classification duration makes the timeline readable without erasing true short tasks.
- Web remains a display, input, and confirmation layer; classification logic stays on the server.
- AI integration remains safely deferred.
