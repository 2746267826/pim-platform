# PC Activity Intelligent Classification Design

## Goal

Make the PC activity timeline classify activity intelligently without requiring the user to hand-write a large initial rule set.

The classifier should support two axes:

- Activity category: `编程`, `学习`, `沟通`, `办公`, `娱乐`, `终端`, `文件`, `浏览`, `其他`.
- Project or context tag: `projectGPT`, `PIM`, `ActivityWatch docs`, a repository name, a client name, or another user-defined context.

The first implementation step should fix the current category timeline where all records fall into Other. The second step should add LLM-assisted suggestions and natural-language correction, while keeping user confirmation as the gate before any LLM-produced rule becomes active.

## Current Problems

The current implementation is too app-pattern centric:

- Backend classification uses exact app-name matches against `pc_app_categories`.
- The summary timeline DTO only exposes `appName` and `windowTitle`, not the backend classification result.
- For web pages, `TimelineItem.appName` can be a domain such as `docs.activitywatch.net`, but the frontend tries to match it against app rules such as `msedge` or `chrome`.
- The frontend category timeline performs its own classification instead of displaying a backend-owned classification result.
- Runtime schema initialization does not currently seed the category table, so a local database can have no usable default rules.

These issues mean the system can display every timeline block as Other even when enough source metadata exists to classify it.

## Design Principles

1. Backend owns classification. Frontend components display and aggregate classification results; they do not reimplement category matching.
2. Raw activity remains the source of truth. Classification is derived and can be recomputed.
3. Rules are not the starting burden for the user. Rules are produced from built-ins, heuristics, user corrections, and confirmed LLM suggestions.
4. LLM output never directly changes active classification rules. It creates a draft that the user reviews.
5. URL data sent to an LLM is sanitized. Query strings, fragments, userinfo, credential-like path segments, and token-like parameters are not sent.
6. The classifier should prefer narrow, explainable rules over broad app-wide rules.

## Two-Step Delivery

### Step 1: Local Classifier And Cluster Confirmation

This step must work without an LLM provider.

Scope:

- Add a backend classification service that returns activity category, project tag, color, confidence, source, and explanation.
- Extend timeline DTOs so summary timelines include classification fields.
- Stop the frontend category timeline from matching app names against category rules.
- Replace app-only classification with a rule engine that can match app, domain, title, URL path, file path, and record type.
- Seed a useful builtin rule set.
- Add heuristic classification for common contexts.
- Generate unknown or low-confidence activity clusters.
- Let the user confirm a cluster manually and create an active local rule.
- Support recomputing classification for a date range.

Success after Step 1:

- The category timeline is no longer all Other.
- Browser page records can be categorized by domain and title.
- Code-related records can become programming records and gain project tags from repository or path signals.
- Unknown activity appears as a small set of reviewable clusters instead of many individual events.
- A user confirmation can classify matching historical activity.

### Step 2: LLM Suggestions And Natural-Language Correction

Scope:

- Add an LLM provider interface with an initial no-op implementation and a real provider later.
- Sanitize URLs and context before any LLM call.
- Ask the LLM to suggest category, project tag, explanation, confidence, and draft rules for unknown clusters.
- Add a natural-language correction endpoint where the user can describe what is wrong with a suggestion.
- Ask the LLM to revise the draft rule based on the user feedback.
- Show impact preview before the user accepts a draft.
- Only accepted drafts become active rules.

Success after Step 2:

- The LLM can suggest useful narrow rules for unknown clusters.
- Natural-language feedback such as "This is learning, but only for ActivityWatch docs, not all Edge activity" becomes a revised narrow rule draft.
- LLM failures do not affect local classification.
- No unsanitized query, fragment, userinfo, or token-like URL data is sent to the provider.

## Data Model

### `pc_activity_category_rules`

This table supersedes the current app-only category rule concept.

Fields:

- `id`
- `rule_name`
- `scope`: `activity`, `project`, or `both`
- `category_name`
- `project_tag`
- `color`
- `priority`
- `source`: `builtin`, `user`, `llm_suggested`, or `llm_corrected`
- `status`: `active`, `suggested`, `rejected`, or `archived`
- `conditions_json`
- `confidence`
- `explanation`
- `created_at`
- `updated_at`

Example `conditions_json`:

```json
{
  "all": [
    { "field": "domain", "op": "domainSuffix", "value": "docs.activitywatch.net" },
    { "field": "title", "op": "containsAny", "value": ["REST API", "ActivityWatch"] }
  ]
}
```

Supported fields should include:

- `recordType`
- `appName`
- `appNameNormalized`
- `domain`
- `urlPath`
- `title`
- `windowTitle`
- `filePath`
- `bucketType`

Supported operators should include:

- `equals`
- `contains`
- `containsAny`
- `startsWith`
- `endsWith`
- `domainSuffix`
- `pathPrefix`
- `regex`

`regex` should be allowed only for user-created or confirmed rules, not for unreviewed LLM drafts.

### `pc_activity_classification_suggestions`

Stores unknown or low-confidence clusters and their proposed resolutions.

Fields:

- `id`
- `cluster_key`
- `sample_count`
- `total_duration_seconds`
- `sample_records_json`
- `sanitized_context_json`
- `current_category`
- `suggested_category`
- `suggested_project_tag`
- `suggested_rules_json`
- `user_feedback`
- `llm_response_json`
- `status`: `pending`, `accepted`, `rejected`, or `needs_review`
- `created_at`
- `updated_at`

Suggestions are review artifacts. Accepting a suggestion creates or updates an active rule.

### `pc_activity_classifications`

This cache is optional in the first step but should be reserved for performance and historical recomputation.

Fields:

- `id`
- `record_type`
- `source_event_ids`
- `category_name`
- `category_color`
- `project_tag`
- `confidence`
- `source_rule_id`
- `source`: `rule`, `heuristic`, `inherited`, `llm_confirmed`, or `fallback`
- `explanation`
- `classified_at`
- `classifier_version`

Step 1 can compute classifications at query time and add this cache later if needed.

### Migration From `pc_app_categories`

The existing app-only rules should be migrated instead of discarded:

- Each active `pc_app_categories` row becomes a `pc_activity_category_rules` row.
- `app_pattern` maps to a condition on `appNameNormalized` or `appName`.
- Existing category names and colors are preserved.
- Builtin status maps to `source = builtin`; user-created rows map to `source = user`.
- The old `/api/v1/pc/categories` endpoint can remain as a compatibility facade during Step 1, backed by the new rule table where practical.

The runtime schema initializer should create and seed the new rule table so a fresh local database starts with usable defaults.

## API Design

### Existing Summary And Timeline APIs

`TimelineItem` should add:

- `categoryName`
- `categoryColor`
- `projectTag`
- `classificationConfidence`
- `classificationSource`
- `classificationExplanation`

The category timeline should aggregate these fields directly.

### Rules

`GET /api/v1/pc/classification/rules`

Returns active and suggested rules.

`POST /api/v1/pc/classification/rules`

Creates a manual rule or accepts a draft rule.

`POST /api/v1/pc/classification/recompute`

Recomputes derived classifications for a date range.

### Suggestions

`GET /api/v1/pc/classification/suggestions?date=...`

Returns unknown or low-confidence clusters sorted by total duration, sample count, and recency.

`POST /api/v1/pc/classification/suggestions/{id}/accept`

Accepts a suggestion and creates an active rule.

`POST /api/v1/pc/classification/suggestions/{id}/reject`

Rejects a suggestion.

`POST /api/v1/pc/classification/suggestions/{id}/llm`

Asks the LLM provider to generate a draft rule for a cluster.

`POST /api/v1/pc/classification/suggestions/{id}/correct`

Accepts natural-language user feedback and asks the LLM provider to revise the draft. It returns a new draft and does not activate it.

## Classification Flow

The classifier evaluates records in this order:

1. Active user-confirmed rules.
2. Builtin strong rules.
3. Local heuristics.
4. Cluster or neighbor inheritance.
5. Fallback to Other.

The output always includes an explanation and a confidence score.

### User-Confirmed Rules

Confirmed rules have the highest priority. They are created from manual classification, accepted suggestions, or accepted natural-language corrections.

Example:

```json
{
  "scope": "both",
  "categoryName": "学习",
  "projectTag": "ActivityWatch",
  "conditions": {
    "all": [
      { "field": "domain", "op": "domainSuffix", "value": "docs.activitywatch.net" }
    ]
  }
}
```

### Builtin Strong Rules

Builtin rules should cover common apps and domains:

- IDEs and code tools -> programming.
- Terminals -> terminal.
- Office apps -> office.
- Chat and meeting apps -> communication.
- File managers -> files.
- Music and video entertainment domains -> entertainment.
- Documentation and learning domains -> learning.

Builtin rules should be narrow when possible and easy to override.

### Heuristics

Useful initial heuristics:

- `Code`, `Rider`, or `Visual Studio` with repository or source file signals -> programming.
- Browser records with docs, wiki, api, reference, tutorial, or guide signals -> learning.
- GitHub or GitLab repository pages -> programming with the repository as project tag.
- Localhost, `127.0.0.1`, or known dev ports -> programming.
- Mail, calendar, meeting, or chat title signals -> communication.
- File manager activity inside a known repository path -> files with inherited project tag.
- Short terminal or file-manager events can inherit the project tag from adjacent strong activity.

### Inheritance

Inheritance should help with context, not hide activity type.

Example:

- A two-minute file manager event between two `projectGPT` code events can keep category `文件` and inherit project tag `projectGPT`.
- A short browser OAuth redirect should not become a new project. It can inherit the surrounding project or be ignored for project tagging.

### Fallback

Fallback records receive:

- `categoryName`: `其他`
- `categoryColor`: neutral gray
- low confidence
- source: `fallback`

Fallback records are candidates for clustering.

## Unknown Cluster Generation

Unknown and low-confidence records should be grouped so the user reviews a handful of meaningful clusters.

Cluster keys:

- Web: `domain + title token signature + app`
- IDE: `app + repo/project token`
- Terminal: `app + cwd/title token`
- Local file: `extension + parent path token`
- General app: `normalizedApp + title token signature`

Sorting:

1. Total duration descending.
2. Sample count descending.
3. Most recent activity descending.

The UI should show representative samples, total duration, matched evidence, and a proposed rule if available.

## URL Sanitization

Before LLM requests, build a sanitized context:

- Keep `scheme://host/path`.
- Remove query strings.
- Remove fragments.
- Remove username and password userinfo.
- Redact path segments that look like long opaque tokens.
- Never send parameter values for names such as `token`, `access_token`, `session`, `key`, `code`, `auth`, `password`, `secret`, `credential`, or `jwt`.

The original raw event can remain in the local database. The LLM provider only receives sanitized context.

## LLM Suggestion Contract

The LLM receives a compact cluster summary, not individual raw events.

Example request body:

```json
{
  "taxonomy": ["编程", "学习", "沟通", "办公", "娱乐", "终端", "文件", "浏览", "其他"],
  "existingRules": [],
  "cluster": {
    "app": "msedge",
    "domain": "docs.activitywatch.net",
    "sanitizedUrls": ["https://docs.activitywatch.net/en/latest/api/rest.html"],
    "titles": ["REST API - ActivityWatch"],
    "totalDurationMinutes": 37
  }
}
```

Expected response:

```json
{
  "categoryName": "学习",
  "projectTag": "ActivityWatch",
  "confidence": 0.86,
  "explanation": "The domain and page titles indicate ActivityWatch documentation reading.",
  "rules": [
    {
      "scope": "both",
      "categoryName": "学习",
      "projectTag": "ActivityWatch",
      "conditions": {
        "all": [
          { "field": "domain", "op": "domainSuffix", "value": "docs.activitywatch.net" }
        ]
      }
    }
  ]
}
```

The response must be parsed as structured JSON. Invalid JSON becomes `needs_review` and must not create an active rule.

## Natural-Language Correction

The user can correct a suggestion with natural language.

Example feedback:

> 这个不是浏览，是学习；只把 ActivityWatch 文档算学习，不要把所有 Edge 都改掉。

The correction request includes:

- Current cluster summary.
- Current draft rule.
- Existing active rules.
- User feedback.

The LLM must return:

- Revised rule draft.
- Explanation of how the feedback changed the rule.
- Expected impact summary.
- Confidence.

The system should instruct the LLM to prefer narrower rules, such as domain or title conditions, over broad app rules. The revised draft still requires user confirmation.

## Frontend Interaction

The PC tracker page should stay focused:

- The category timeline displays backend-provided categories and project tags.
- Clicking a block opens classification details: rule source, confidence, explanation, and evidence.
- Other or low-confidence blocks show a correction action.
- The correction panel supports:
  - Direct category and project selection.
  - Accepting an LLM suggestion.
  - Natural-language correction that revises the draft rule.
- Draft changes should be preview-only until the user confirms.
- Confirmation should allow applying to today, a date range, or all history.

## Testing And Verification

Backend tests:

- App-only rules still classify known apps.
- Domain rules classify web-page records even when the browser app is generic.
- Title and URL path rules work with priority ordering.
- User rules override builtin rules.
- Heuristics classify common docs, GitHub, localhost, IDE, and terminal cases.
- URL sanitizer removes query, fragment, userinfo, and token-like path or parameter data.
- Suggestion acceptance creates an active rule.
- Natural-language correction returns a suggested rule and does not activate it.

Frontend tests:

- Category timeline uses `categoryName` from timeline items.
- Timeline does not perform app-rule matching in the component.
- Low-confidence and Other blocks expose correction actions.
- Suggestion drafts render separately from active rules.

Manual verification:

- Open a PC tracker day with browser docs, code editor, terminal, and file manager records.
- Confirm the category timeline contains multiple categories and no longer collapses to Other.
- Confirm accepting a cluster rule changes matching timeline blocks after recompute.
- Confirm LLM suggestions do not activate until accepted.
