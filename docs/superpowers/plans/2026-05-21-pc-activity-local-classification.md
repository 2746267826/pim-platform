# PC Activity Local Classification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first delivery step for intelligent PC activity classification: backend-owned local classification, multi-field rules, seeded defaults, unknown clusters, manual confirmation, and a category timeline that no longer collapses to Other.

**Architecture:** Add a focused classification layer inside `Pim.Module.PcTracker` that evaluates activity records with active rules, builtins, heuristics, and fallback. The existing raw ActivityWatch and KeyStats storage remains the source of truth; classification is derived at query time in this step. The frontend timeline consumes classification fields returned by the API instead of reclassifying app names.

**Tech Stack:** C# / ASP.NET Core Minimal API / EF Core / PostgreSQL / xUnit, React / TypeScript / React Query / Tailwind CSS.

---

## File Structure

Backend files:

- Create `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`: classification DTOs, rule DTOs, suggestion DTOs, and request DTOs.
- Create `src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs`: persisted multi-field rules.
- Create `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs`: persisted review clusters.
- Modify `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`: EF indexes for the new entities.
- Create `src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs`: URL sanitizer shared by local suggestions and future LLM calls.
- Create `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs`: parses `conditions_json` and evaluates rules.
- Create `src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs`: local classifier with user rules, builtins, heuristics, inheritance helpers, and fallback.
- Create `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`: creates unknown clusters and accepts/rejects suggestions.
- Modify `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`: accept the classifier and classify web-page/window detail records.
- Modify `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`: load classification rules, return classified detail/timeline records, and expose rules/suggestions methods.
- Modify `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`: create and seed new rule/suggestion tables and migrate old app categories.
- Modify `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`: add classification fields to `TimelineItem` and `PcDetailRecord`.
- Modify `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`: register new services and map classification endpoints.

Frontend files:

- Modify `src/client-web/src/types/index.ts`: add classification fields and rule/suggestion types.
- Modify `src/client-web/src/api/pcTracker.ts`: add classification API functions.
- Modify `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`: use backend-provided classification and expose classification explanation in tooltip.
- Modify `src/client-web/src/pages/PcTrackerPage.tsx`: stop passing category rules to `CategoryTimeline`.

Test files:

- Create `tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs`.
- Create `tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs`.
- Create `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`.
- Create `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`.
- Modify `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`.

Verification commands:

- `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj`
- `npm --prefix src/client-web run build`

---

### Task 1: URL Sanitizer

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs`

- [ ] **Step 1: Write failing sanitizer tests**

Create `tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs`:

```csharp
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityUrlSanitizerTests
{
    [Fact]
    public void Sanitize_RemovesQueryFragmentAndUserInfo()
    {
        var result = ActivityUrlSanitizer.Sanitize("https://alice:secret@example.com/docs/page?token=abc&x=1#section");

        Assert.Equal("https://example.com/docs/page", result);
    }

    [Fact]
    public void Sanitize_RedactsOpaquePathSegments()
    {
        var result = ActivityUrlSanitizer.Sanitize("https://example.com/session/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9/profile/12345");

        Assert.Equal("https://example.com/session/[redacted]/profile/12345", result);
    }

    [Fact]
    public void Sanitize_ReturnsNullForInvalidUrl()
    {
        var result = ActivityUrlSanitizer.Sanitize("not a url");

        Assert.Null(result);
    }
}
```

- [ ] **Step 2: Run sanitizer tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityUrlSanitizerTests
```

Expected: FAIL because `ActivityUrlSanitizer` does not exist.

- [ ] **Step 3: Implement sanitizer**

Create `src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs`:

```csharp
using System.Text.RegularExpressions;

namespace Pim.Module.PcTracker.Services;

public static partial class ActivityUrlSanitizer
{
    public static string? Sanitize(string? url)
    {
        if (string.IsNullOrWhiteSpace(url))
            return null;

        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri))
            return null;

        var builder = new UriBuilder(uri)
        {
            UserName = string.Empty,
            Password = string.Empty,
            Query = string.Empty,
            Fragment = string.Empty
        };

        var segments = uri.AbsolutePath
            .Split('/', StringSplitOptions.RemoveEmptyEntries)
            .Select(segment => LooksSensitive(segment) ? "[redacted]" : segment);

        builder.Path = string.Join('/', segments);
        return builder.Uri.ToString().TrimEnd('/');
    }

    private static bool LooksSensitive(string segment)
    {
        var decoded = Uri.UnescapeDataString(segment);
        return decoded.Length >= 24
            && (OpaqueTokenRegex().IsMatch(decoded) || decoded.Count(char.IsDigit) >= 8);
    }

    [GeneratedRegex("^[A-Za-z0-9_-]{24,}$")]
    private static partial Regex OpaqueTokenRegex();
}
```

- [ ] **Step 4: Run sanitizer tests and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityUrlSanitizerTests
```

Expected: PASS.

- [ ] **Step 5: Commit sanitizer**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivityUrlSanitizer.cs tests/Pim.UnitTests/Services/ActivityUrlSanitizerTests.cs
git commit -m "feat: add activity URL sanitizer"
```

---

### Task 2: Classification DTOs And Entities

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Create: `src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs`
- Create: `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs`

- [ ] **Step 1: Write a failing compile test for new DTO names**

Create `tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs`:

```csharp
using Pim.Module.PcTracker.DTOs;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassificationRuleEvaluatorTests
{
    [Fact]
    public void ActivityClassificationResult_HasFallbackDefaults()
    {
        var result = ActivityClassificationResult.Fallback();

        Assert.Equal("其他", result.CategoryName);
        Assert.Equal("#64748b", result.CategoryColor);
        Assert.Null(result.ProjectTag);
        Assert.Equal("fallback", result.Source);
        Assert.True(result.Confidence < 0.5);
    }
}
```

- [ ] **Step 2: Run compile test and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRuleEvaluatorTests
```

Expected: FAIL because `ActivityClassificationResult` does not exist.

- [ ] **Step 3: Create classification DTOs**

Create `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`:

```csharp
namespace Pim.Module.PcTracker.DTOs;

public record ActivityClassificationResult(
    string CategoryName,
    string CategoryColor,
    string? ProjectTag,
    double Confidence,
    string Source,
    string Explanation,
    Guid? SourceRuleId = null)
{
    public static ActivityClassificationResult Fallback() =>
        new("其他", "#64748b", null, 0.2, "fallback", "No rule or heuristic matched.");
}

public record ActivityClassificationRuleDto(
    Guid Id,
    string RuleName,
    string Scope,
    string? CategoryName,
    string? ProjectTag,
    string Color,
    int Priority,
    string Source,
    string Status,
    string ConditionsJson,
    double Confidence,
    string? Explanation);

public record SaveActivityClassificationRuleRequest(
    string RuleName,
    string Scope,
    string? CategoryName,
    string? ProjectTag,
    string Color,
    int Priority,
    string ConditionsJson,
    double Confidence,
    string? Explanation);

public record ActivityClassificationSuggestionDto(
    Guid Id,
    string ClusterKey,
    int SampleCount,
    double TotalDurationSeconds,
    string SampleRecordsJson,
    string SanitizedContextJson,
    string? CurrentCategory,
    string? SuggestedCategory,
    string? SuggestedProjectTag,
    string? SuggestedRulesJson,
    string? UserFeedback,
    string? LlmResponseJson,
    string Status);

public record AcceptActivityClassificationSuggestionRequest(
    string RuleName,
    string Scope,
    string? CategoryName,
    string? ProjectTag,
    string Color,
    int Priority,
    string ConditionsJson,
    double Confidence,
    string? Explanation);
```

- [ ] **Step 4: Create new entities**

Create `src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_activity_category_rules")]
public class ActivityCategoryRuleEntity
{
    [Key][Column("id")] public Guid Id { get; set; }
    [Column("rule_name")][MaxLength(128)] public string RuleName { get; set; } = string.Empty;
    [Column("scope")][MaxLength(16)] public string Scope { get; set; } = "activity";
    [Column("category_name")][MaxLength(64)] public string? CategoryName { get; set; }
    [Column("project_tag")][MaxLength(128)] public string? ProjectTag { get; set; }
    [Column("color")][MaxLength(7)] public string Color { get; set; } = "#64748b";
    [Column("priority")] public int Priority { get; set; }
    [Column("source")][MaxLength(32)] public string Source { get; set; } = "user";
    [Column("status")][MaxLength(16)] public string Status { get; set; } = "active";
    [Column("conditions_json", TypeName = "jsonb")] public string ConditionsJson { get; set; } = "{}";
    [Column("confidence")] public double Confidence { get; set; } = 1;
    [Column("explanation")] public string? Explanation { get; set; }
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

Create `src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs`:

```csharp
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Pim.Module.PcTracker.Entities;

[Table("pc_activity_classification_suggestions")]
public class ActivityClassificationSuggestionEntity
{
    [Key][Column("id")] public Guid Id { get; set; }
    [Column("cluster_key")][MaxLength(256)] public string ClusterKey { get; set; } = string.Empty;
    [Column("sample_count")] public int SampleCount { get; set; }
    [Column("total_duration_seconds")] public double TotalDurationSeconds { get; set; }
    [Column("sample_records_json", TypeName = "jsonb")] public string SampleRecordsJson { get; set; } = "[]";
    [Column("sanitized_context_json", TypeName = "jsonb")] public string SanitizedContextJson { get; set; } = "{}";
    [Column("current_category")][MaxLength(64)] public string? CurrentCategory { get; set; }
    [Column("suggested_category")][MaxLength(64)] public string? SuggestedCategory { get; set; }
    [Column("suggested_project_tag")][MaxLength(128)] public string? SuggestedProjectTag { get; set; }
    [Column("suggested_rules_json", TypeName = "jsonb")] public string? SuggestedRulesJson { get; set; }
    [Column("user_feedback")] public string? UserFeedback { get; set; }
    [Column("llm_response_json", TypeName = "jsonb")] public string? LlmResponseJson { get; set; }
    [Column("status")][MaxLength(16)] public string Status { get; set; } = "pending";
    [Column("created_at")] public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    [Column("updated_at")] public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
```

- [ ] **Step 5: Add EF configuration**

Append to `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`:

```csharp
public class ActivityCategoryRuleEntityConfiguration : IEntityTypeConfiguration<ActivityCategoryRuleEntity>
{
    public void Configure(EntityTypeBuilder<ActivityCategoryRuleEntity> builder)
    {
        builder.ToTable("pc_activity_category_rules");
        builder.HasIndex(e => e.RuleName)
            .IsUnique()
            .HasDatabaseName("ux_pc_activity_category_rules_rule_name");
        builder.HasIndex(e => e.Status).HasDatabaseName("ix_pc_activity_category_rules_status");
        builder.HasIndex(e => e.Priority).HasDatabaseName("ix_pc_activity_category_rules_priority");
        builder.HasIndex(e => e.CategoryName).HasDatabaseName("ix_pc_activity_category_rules_category_name");
        builder.HasIndex(e => e.ProjectTag).HasDatabaseName("ix_pc_activity_category_rules_project_tag");
    }
}

public class ActivityClassificationSuggestionEntityConfiguration : IEntityTypeConfiguration<ActivityClassificationSuggestionEntity>
{
    public void Configure(EntityTypeBuilder<ActivityClassificationSuggestionEntity> builder)
    {
        builder.ToTable("pc_activity_classification_suggestions");
        builder.HasIndex(e => e.ClusterKey).HasDatabaseName("ix_pc_activity_classification_suggestions_cluster_key");
        builder.HasIndex(e => e.Status).HasDatabaseName("ix_pc_activity_classification_suggestions_status");
        builder.HasIndex(e => e.UpdatedAt).HasDatabaseName("ix_pc_activity_classification_suggestions_updated_at");
    }
}
```

- [ ] **Step 6: Extend schema initializer**

In `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`, add this SQL before the closing `"""` of `SchemaSql`:

```sql
CREATE TABLE IF NOT EXISTS pc_app_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_pattern VARCHAR(128) NOT NULL,
    category_name VARCHAR(64) NOT NULL,
    color VARCHAR(7) DEFAULT '#6B5EE4',
    priority INT DEFAULT 0,
    is_builtin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS pc_activity_category_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(128) NOT NULL,
    scope VARCHAR(16) NOT NULL DEFAULT 'activity',
    category_name VARCHAR(64),
    project_tag VARCHAR(128),
    color VARCHAR(7) NOT NULL DEFAULT '#64748b',
    priority INT NOT NULL DEFAULT 0,
    source VARCHAR(32) NOT NULL DEFAULT 'user',
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    conditions_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_pc_activity_category_rules_status ON pc_activity_category_rules (status);
CREATE INDEX IF NOT EXISTS ix_pc_activity_category_rules_priority ON pc_activity_category_rules (priority);
CREATE INDEX IF NOT EXISTS ix_pc_activity_category_rules_category_name ON pc_activity_category_rules (category_name);
CREATE INDEX IF NOT EXISTS ix_pc_activity_category_rules_project_tag ON pc_activity_category_rules (project_tag);
CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_activity_category_rules_rule_name ON pc_activity_category_rules (rule_name);
CREATE TABLE IF NOT EXISTS pc_activity_classification_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cluster_key VARCHAR(256) NOT NULL,
    sample_count INT NOT NULL DEFAULT 0,
    total_duration_seconds DOUBLE PRECISION NOT NULL DEFAULT 0,
    sample_records_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    sanitized_context_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    current_category VARCHAR(64),
    suggested_category VARCHAR(64),
    suggested_project_tag VARCHAR(128),
    suggested_rules_json JSONB,
    user_feedback TEXT,
    llm_response_json JSONB,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classification_suggestions_cluster_key ON pc_activity_classification_suggestions (cluster_key);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classification_suggestions_status ON pc_activity_classification_suggestions (status);
CREATE INDEX IF NOT EXISTS ix_pc_activity_classification_suggestions_updated_at ON pc_activity_classification_suggestions (updated_at);
INSERT INTO pc_activity_category_rules (rule_name, scope, category_name, project_tag, color, priority, source, status, conditions_json, confidence, explanation) VALUES
('Builtin: VS Code', 'activity', '编程', NULL, '#6B5EE4', 300, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"equals","value":"code"}]}'::jsonb, 0.9, 'Builtin app rule.'),
('Builtin: Rider', 'activity', '编程', NULL, '#6B5EE4', 300, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"equals","value":"rider"}]}'::jsonb, 0.9, 'Builtin app rule.'),
('Builtin: Terminal', 'activity', '终端', NULL, '#E05A7A', 300, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"containsAny","value":["windowsterminal","terminal","powershell","cmd"]}]}'::jsonb, 0.85, 'Builtin terminal rule.'),
('Builtin: Chat apps', 'activity', '沟通', NULL, '#F5935A', 300, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"containsAny","value":["wechat","dingtalk","qq","telegram","slack","discord","teams"]}]}'::jsonb, 0.85, 'Builtin communication rule.'),
('Builtin: Office apps', 'activity', '办公', NULL, '#F59E0B', 300, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"containsAny","value":["winword","excel","powerpnt","notion","obsidian","typora"]}]}'::jsonb, 0.85, 'Builtin office rule.'),
('Builtin: File managers', 'activity', '文件', NULL, '#3B82F6', 300, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"containsAny","value":["explorer","everything","totalcommander"]}]}'::jsonb, 0.85, 'Builtin file rule.'),
('Builtin: Browser apps', 'activity', '浏览', NULL, '#0EA8A0', 100, 'builtin', 'active', '{"all":[{"field":"appNameNormalized","op":"containsAny","value":["msedge","chrome","firefox","brave","opera"]}]}'::jsonb, 0.6, 'Low-priority browser fallback rule.')
ON CONFLICT DO NOTHING;
INSERT INTO pc_activity_category_rules (rule_name, scope, category_name, project_tag, color, priority, source, status, conditions_json, confidence, explanation)
SELECT
    'Migrated app rule: ' || app_pattern,
    'activity',
    category_name,
    NULL,
    color,
    priority,
    CASE WHEN is_builtin THEN 'builtin' ELSE 'user' END,
    'active',
    jsonb_build_object('all', jsonb_build_array(jsonb_build_object('field', 'appNameNormalized', 'op', 'equals', 'value', lower(replace(app_pattern, '.exe', ''))))),
    0.95,
    'Migrated from pc_app_categories.'
FROM pc_app_categories
WHERE NOT EXISTS (
    SELECT 1
    FROM pc_activity_category_rules r
    WHERE r.rule_name = 'Migrated app rule: ' || pc_app_categories.app_pattern
);
```

- [ ] **Step 7: Run DTO/entity test and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRuleEvaluatorTests
```

Expected: PASS.

- [ ] **Step 8: Commit DTOs and entities**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs
git commit -m "feat: add activity classification persistence"
```

---

### Task 3: Rule Evaluator

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs`
- Modify: `tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs`

- [ ] **Step 1: Add failing evaluator tests**

Replace `tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs` with:

```csharp
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassificationRuleEvaluatorTests
{
    [Fact]
    public void ActivityClassificationResult_HasFallbackDefaults()
    {
        var result = ActivityClassificationResult.Fallback();

        Assert.Equal("其他", result.CategoryName);
        Assert.Equal("#64748b", result.CategoryColor);
        Assert.Null(result.ProjectTag);
        Assert.Equal("fallback", result.Source);
        Assert.True(result.Confidence < 0.5);
    }

    [Fact]
    public void Matches_ReturnsTrueForDomainSuffixAndTitleContainsAny()
    {
        var context = new ActivityClassificationContext(
            "web-page",
            null,
            null,
            "docs.activitywatch.net",
            "/en/latest/api/rest.html",
            "REST API - ActivityWatch",
            null,
            null,
            "web.tab.current");
        const string conditions = """
        {
          "all": [
            { "field": "domain", "op": "domainSuffix", "value": "activitywatch.net" },
            { "field": "title", "op": "containsAny", "value": ["REST API", "Guide"] }
          ]
        }
        """;

        Assert.True(ActivityClassificationRuleEvaluator.Matches(conditions, context));
    }

    [Fact]
    public void Matches_ReturnsFalseWhenAnyAllConditionFails()
    {
        var context = new ActivityClassificationContext(
            "web-page",
            null,
            null,
            "example.com",
            "/docs",
            "REST API",
            null,
            null,
            "web.tab.current");
        const string conditions = """
        {
          "all": [
            { "field": "domain", "op": "domainSuffix", "value": "activitywatch.net" },
            { "field": "title", "op": "contains", "value": "REST API" }
          ]
        }
        """;

        Assert.False(ActivityClassificationRuleEvaluator.Matches(conditions, context));
    }
}
```

- [ ] **Step 2: Run evaluator tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRuleEvaluatorTests
```

Expected: FAIL because `ActivityClassificationContext` and evaluator do not exist.

- [ ] **Step 3: Implement evaluator**

Create `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs`:

```csharp
using System.Text.Json;
using System.Text.RegularExpressions;

namespace Pim.Module.PcTracker.Services;

public record ActivityClassificationContext(
    string RecordType,
    string? AppName,
    string? AppNameNormalized,
    string? Domain,
    string? UrlPath,
    string? Title,
    string? WindowTitle,
    string? FilePath,
    string? BucketType);

public static class ActivityClassificationRuleEvaluator
{
    public static bool Matches(string? conditionsJson, ActivityClassificationContext context)
    {
        if (string.IsNullOrWhiteSpace(conditionsJson))
            return false;

        try
        {
            using var doc = JsonDocument.Parse(conditionsJson);
            if (!doc.RootElement.TryGetProperty("all", out var all) || all.ValueKind != JsonValueKind.Array)
                return false;

            foreach (var condition in all.EnumerateArray())
            {
                if (!MatchesCondition(condition, context))
                    return false;
            }

            return true;
        }
        catch (JsonException)
        {
            return false;
        }
        catch (ArgumentException)
        {
            return false;
        }
    }

    private static bool MatchesCondition(JsonElement condition, ActivityClassificationContext context)
    {
        var field = GetString(condition, "field");
        var op = GetString(condition, "op");
        var actual = GetFieldValue(field, context);

        if (string.IsNullOrWhiteSpace(field) || string.IsNullOrWhiteSpace(op) || string.IsNullOrWhiteSpace(actual))
            return false;

        return op switch
        {
            "equals" => string.Equals(actual, GetString(condition, "value"), StringComparison.OrdinalIgnoreCase),
            "contains" => actual.Contains(GetString(condition, "value") ?? string.Empty, StringComparison.OrdinalIgnoreCase),
            "containsAny" => GetStringArray(condition, "value").Any(value => actual.Contains(value, StringComparison.OrdinalIgnoreCase)),
            "startsWith" => actual.StartsWith(GetString(condition, "value") ?? string.Empty, StringComparison.OrdinalIgnoreCase),
            "endsWith" => actual.EndsWith(GetString(condition, "value") ?? string.Empty, StringComparison.OrdinalIgnoreCase),
            "domainSuffix" => DomainMatches(actual, GetString(condition, "value")),
            "pathPrefix" => actual.StartsWith(GetString(condition, "value") ?? string.Empty, StringComparison.OrdinalIgnoreCase),
            "regex" => Regex.IsMatch(actual, GetString(condition, "value") ?? "$a", RegexOptions.IgnoreCase | RegexOptions.CultureInvariant, TimeSpan.FromMilliseconds(100)),
            _ => false
        };
    }

    private static bool DomainMatches(string actual, string? suffix)
    {
        if (string.IsNullOrWhiteSpace(suffix))
            return false;

        return string.Equals(actual, suffix, StringComparison.OrdinalIgnoreCase)
            || actual.EndsWith("." + suffix, StringComparison.OrdinalIgnoreCase);
    }

    private static string? GetFieldValue(string? field, ActivityClassificationContext context)
    {
        return field switch
        {
            "recordType" => context.RecordType,
            "appName" => context.AppName,
            "appNameNormalized" => context.AppNameNormalized,
            "domain" => context.Domain,
            "urlPath" => context.UrlPath,
            "title" => context.Title,
            "windowTitle" => context.WindowTitle,
            "filePath" => context.FilePath,
            "bucketType" => context.BucketType,
            _ => null
        };
    }

    private static string? GetString(JsonElement element, string property)
    {
        return element.TryGetProperty(property, out var value) && value.ValueKind == JsonValueKind.String
            ? value.GetString()
            : null;
    }

    private static IEnumerable<string> GetStringArray(JsonElement element, string property)
    {
        if (!element.TryGetProperty(property, out var value))
            return [];

        if (value.ValueKind == JsonValueKind.String)
            return [value.GetString() ?? string.Empty];

        if (value.ValueKind != JsonValueKind.Array)
            return [];

        return value.EnumerateArray()
            .Where(item => item.ValueKind == JsonValueKind.String)
            .Select(item => item.GetString() ?? string.Empty)
            .Where(item => !string.IsNullOrWhiteSpace(item));
    }
}
```

- [ ] **Step 4: Run evaluator tests and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassificationRuleEvaluatorTests
```

Expected: PASS.

- [ ] **Step 5: Commit evaluator**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs tests/Pim.UnitTests/Services/ActivityClassificationRuleEvaluatorTests.cs
git commit -m "feat: evaluate activity classification rules"
```

---

### Task 4: Local Classifier

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`

- [ ] **Step 1: Write failing classifier tests**

Create `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`:

```csharp
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityClassifierTests
{
    [Fact]
    public void Classify_UserRuleBeatsHeuristic()
    {
        var rule = new ActivityCategoryRuleEntity
        {
            Id = Guid.NewGuid(),
            RuleName = "ActivityWatch docs",
            Scope = "both",
            CategoryName = "学习",
            ProjectTag = "ActivityWatch",
            Color = "#14b8a6",
            Priority = 500,
            Source = "user",
            Status = "active",
            ConditionsJson = """{"all":[{"field":"domain","op":"domainSuffix","value":"docs.activitywatch.net"}]}""",
            Confidence = 0.96,
            Explanation = "User confirmed docs domain."
        };
        var context = new ActivityClassificationContext("web-page", null, null, "docs.activitywatch.net", "/en/latest/api/rest.html", "REST API", null, null, "web.tab.current");

        var result = ActivityClassifier.Classify(context, [rule]);

        Assert.Equal("学习", result.CategoryName);
        Assert.Equal("ActivityWatch", result.ProjectTag);
        Assert.Equal("rule", result.Source);
        Assert.Equal(rule.Id, result.SourceRuleId);
    }

    [Fact]
    public void Classify_GithubRepoBecomesProgrammingWithProjectTag()
    {
        var context = new ActivityClassificationContext("web-page", null, null, "github.com", "/owner/projectGPT/pull/1", "Pull request", null, null, "web.tab.current");

        var result = ActivityClassifier.Classify(context, []);

        Assert.Equal("编程", result.CategoryName);
        Assert.Equal("projectGPT", result.ProjectTag);
        Assert.Equal("heuristic", result.Source);
    }

    [Fact]
    public void Classify_DocsPageBecomesLearning()
    {
        var context = new ActivityClassificationContext("web-page", "msedge.exe", "msedge", "docs.activitywatch.net", "/en/latest/api/rest.html", "REST API", null, null, "web.tab.current");

        var result = ActivityClassifier.Classify(context, []);

        Assert.Equal("学习", result.CategoryName);
        Assert.Equal("ActivityWatch", result.ProjectTag);
        Assert.Equal("heuristic", result.Source);
    }

    [Fact]
    public void Classify_UnknownReturnsFallback()
    {
        var context = new ActivityClassificationContext("window", "mystery.exe", "mystery", null, null, "Unknown", "Unknown", null, "currentwindow");

        var result = ActivityClassifier.Classify(context, []);

        Assert.Equal("其他", result.CategoryName);
        Assert.Equal("fallback", result.Source);
    }
}
```

- [ ] **Step 2: Run classifier tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassifierTests
```

Expected: FAIL because `ActivityClassifier` does not exist.

- [ ] **Step 3: Implement classifier**

Create `src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs`:

```csharp
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public static class ActivityClassifier
{
    private static readonly Dictionary<string, (string Category, string Color)> AppCategories = new(StringComparer.OrdinalIgnoreCase)
    {
        ["code"] = ("编程", "#6B5EE4"),
        ["devenv"] = ("编程", "#6B5EE4"),
        ["rider"] = ("编程", "#6B5EE4"),
        ["cursor"] = ("编程", "#6B5EE4"),
        ["windowsterminal"] = ("终端", "#E05A7A"),
        ["terminal"] = ("终端", "#E05A7A"),
        ["cmd"] = ("终端", "#E05A7A"),
        ["powershell"] = ("终端", "#E05A7A"),
        ["wechat"] = ("沟通", "#F5935A"),
        ["dingtalk"] = ("沟通", "#F5935A"),
        ["qq"] = ("沟通", "#F5935A"),
        ["telegram"] = ("沟通", "#F5935A"),
        ["slack"] = ("沟通", "#F5935A"),
        ["discord"] = ("沟通", "#F5935A"),
        ["teams"] = ("沟通", "#F5935A"),
        ["explorer"] = ("文件", "#3B82F6"),
        ["everything"] = ("文件", "#3B82F6"),
        ["winword"] = ("办公", "#F59E0B"),
        ["excel"] = ("办公", "#F59E0B"),
        ["powerpnt"] = ("办公", "#F59E0B"),
        ["notion"] = ("办公", "#F59E0B"),
        ["obsidian"] = ("办公", "#F59E0B"),
        ["spotify"] = ("娱乐", "#10B981")
    };

    public static ActivityClassificationResult Classify(
        ActivityClassificationContext context,
        IReadOnlyCollection<ActivityCategoryRuleEntity> rules)
    {
        foreach (var rule in rules
            .Where(r => r.Status == "active")
            .OrderByDescending(r => r.Priority))
        {
            if (!ActivityClassificationRuleEvaluator.Matches(rule.ConditionsJson, context))
                continue;

            return new ActivityClassificationResult(
                rule.CategoryName ?? "其他",
                rule.Color,
                rule.ProjectTag,
                rule.Confidence,
                "rule",
                rule.Explanation ?? $"Matched rule {rule.RuleName}.",
                rule.Id);
        }

        var heuristic = ClassifyWithHeuristics(context);
        return heuristic ?? ActivityClassificationResult.Fallback();
    }

    private static ActivityClassificationResult? ClassifyWithHeuristics(ActivityClassificationContext context)
    {
        var normalizedApp = AppNameNormalizer.Normalize(context.AppNameNormalized ?? context.AppName);
        var domain = context.Domain ?? string.Empty;
        var path = context.UrlPath ?? string.Empty;
        var title = context.Title ?? context.WindowTitle ?? string.Empty;

        if (domain.Equals("github.com", StringComparison.OrdinalIgnoreCase)
            || domain.Equals("gitlab.com", StringComparison.OrdinalIgnoreCase))
        {
            return new ActivityClassificationResult("编程", "#6B5EE4", ExtractRepoTag(path), 0.82, "heuristic", "Code hosting domain matched.");
        }

        if (domain.Contains("docs.", StringComparison.OrdinalIgnoreCase)
            || path.Contains("/docs", StringComparison.OrdinalIgnoreCase)
            || path.Contains("/api", StringComparison.OrdinalIgnoreCase)
            || title.Contains("docs", StringComparison.OrdinalIgnoreCase)
            || title.Contains("REST API", StringComparison.OrdinalIgnoreCase))
        {
            return new ActivityClassificationResult("学习", "#14b8a6", GuessProjectFromDomain(domain), 0.72, "heuristic", "Documentation or API learning signals matched.");
        }

        if (domain is "localhost" or "127.0.0.1" or "::1")
            return new ActivityClassificationResult("编程", "#6B5EE4", null, 0.75, "heuristic", "Local development host matched.");

        if (AppCategories.TryGetValue(normalizedApp, out var appCategory))
            return new ActivityClassificationResult(appCategory.Category, appCategory.Color, ExtractProjectFromText(title), 0.7, "heuristic", "Known application category matched.");

        if (title.Contains("meeting", StringComparison.OrdinalIgnoreCase)
            || title.Contains("calendar", StringComparison.OrdinalIgnoreCase)
            || title.Contains("mail", StringComparison.OrdinalIgnoreCase))
            return new ActivityClassificationResult("沟通", "#F5935A", null, 0.65, "heuristic", "Communication title signal matched.");

        return null;
    }

    private static string? ExtractRepoTag(string path)
    {
        var parts = path.Split('/', StringSplitOptions.RemoveEmptyEntries);
        return parts.Length >= 2 ? parts[1] : null;
    }

    private static string? GuessProjectFromDomain(string domain)
    {
        if (domain.Contains("activitywatch", StringComparison.OrdinalIgnoreCase))
            return "ActivityWatch";

        return null;
    }

    private static string? ExtractProjectFromText(string text)
    {
        if (text.Contains("projectGPT", StringComparison.OrdinalIgnoreCase))
            return "projectGPT";
        if (text.Contains("PIM", StringComparison.OrdinalIgnoreCase))
            return "PIM";
        return null;
    }
}
```

- [ ] **Step 4: Run classifier tests and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivityClassifierTests
```

Expected: PASS.

- [ ] **Step 5: Commit classifier**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs tests/Pim.UnitTests/Services/ActivityClassifierTests.cs
git commit -m "feat: classify activity with local signals"
```

---

### Task 5: Apply Classification To Detail Records And Timeline DTOs

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`

- [ ] **Step 1: Add failing timeline classification tests**

In `tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs`, add this test near the existing `GetTimelineAsync_UsesBrowserPageRecords` test:

```csharp
[Fact]
public async Task GetTimelineAsync_ReturnsBackendClassificationForWebPages()
{
    PimDbContext.RegisterModuleAssembly(typeof(AwEventEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    db.Set<AwEventEntity>().AddRange(
        WindowEvent("2026-05-20T05:00:00+00:00", 60, "msedge.exe", "Docs - Edge"),
        WebEvent(1, "2026-05-20T05:00:05+00:00", 10, "https://docs.activitywatch.net/en/latest/api/rest.html", "REST API"));
    await db.SaveChangesAsync();

    var service = new PcTrackerService(db);
    var timeline = await service.GetTimelineAsync(new DateTime(2026, 5, 20), CancellationToken.None);

    var item = Assert.Single(timeline);
    Assert.Equal("docs.activitywatch.net", item.AppName);
    Assert.Equal("学习", item.CategoryName);
    Assert.Equal("#14b8a6", item.CategoryColor);
    Assert.Equal("ActivityWatch", item.ProjectTag);
    Assert.Equal("heuristic", item.ClassificationSource);
    Assert.True(item.ClassificationConfidence > 0.5);
    Assert.Contains("Documentation", item.ClassificationExplanation);
}
```

- [ ] **Step 2: Run timeline test and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter GetTimelineAsync_ReturnsBackendClassificationForWebPages
```

Expected: FAIL because `TimelineItem` does not have classification fields.

- [ ] **Step 3: Extend `TimelineItem` and `PcDetailRecord`**

In `src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs`, replace `TimelineItem` with:

```csharp
public record TimelineItem(
    string Start,
    string End,
    double DurationMinutes,
    string AppName,
    string? WindowTitle,
    string CategoryName,
    string CategoryColor,
    string? ProjectTag,
    double ClassificationConfidence,
    string ClassificationSource,
    string ClassificationExplanation
);
```

In the same file, add these optional parameters to the end of `PcDetailRecord`:

```csharp
    string? CategoryColor = null,
    string? ProjectTag = null,
    double? ClassificationConfidence = null,
    string? ClassificationSource = null,
    string? ClassificationExplanation = null
```

The final constructor tail should look like:

```csharp
    List<long>? SourceWebEventIds = null,
    List<long>? SourceWindowEventIds = null,
    string? CategoryColor = null,
    string? ProjectTag = null,
    double? ClassificationConfidence = null,
    string? ClassificationSource = null,
    string? ClassificationExplanation = null
);
```

- [ ] **Step 4: Update `BrowserPageTimelineBuilder` signatures**

In `src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs`, change:

```csharp
public static List<PcDetailRecord> BuildInterpretedAwRecords(
    List<AwEventEntity> awEvents,
    List<AppCategoryRule> rules)
```

to:

```csharp
public static List<PcDetailRecord> BuildInterpretedAwRecords(
    List<AwEventEntity> awEvents,
    IReadOnlyCollection<ActivityCategoryRuleEntity> rules)
```

Change:

```csharp
public static PcDetailRecord ToRawAwRecord(AwEventEntity e, List<AppCategoryRule> rules)
```

to:

```csharp
public static PcDetailRecord ToRawAwRecord(AwEventEntity e, IReadOnlyCollection<ActivityCategoryRuleEntity> rules)
```

Change `BuildWebPageClusters` to accept the same rule collection:

```csharp
private static List<WebPageCluster> BuildWebPageClusters(
    List<AwEventEntity> webEvents,
    IReadOnlyCollection<ActivityCategoryRuleEntity> rules)
```

Update the call site:

```csharp
var webPages = BuildWebPageClusters(webEvents, rules)
```

Add this helper near the bottom of the file:

```csharp
private static ActivityClassificationContext BuildContext(
    string recordType,
    string? appName,
    string? normalizedApp,
    string? domain,
    string? path,
    string? title,
    string? windowTitle,
    string? bucketType)
{
    return new ActivityClassificationContext(
        recordType,
        appName,
        normalizedApp,
        domain,
        path,
        title,
        windowTitle,
        path,
        bucketType);
}
```

Replace the classification block in `ToRawAwRecord`:

```csharp
var category = ClassifyApp(normalizedApp, rules);
var webData = IsWebEvent(e) ? ParseWebData(e) : null;
var recordType = IsWebEvent(e) ? "web" : e.EventType;
```

with:

```csharp
var webData = IsWebEvent(e) ? ParseWebData(e) : null;
var recordType = IsWebEvent(e) ? "web" : e.EventType;
var classification = ActivityClassifier.Classify(
    BuildContext(recordType, e.AppName, normalizedApp, webData?.Domain, webData?.Path, webData?.Title ?? e.WindowTitle, e.WindowTitle, e.BucketType),
    rules);
```

In the `PcDetailRecord` constructor call in `ToRawAwRecord`, replace `category` with `classification.CategoryName`, and append:

```csharp
            CategoryColor: classification.CategoryColor,
            ProjectTag: classification.ProjectTag,
            ClassificationConfidence: classification.Confidence,
            ClassificationSource: classification.Source,
            ClassificationExplanation: classification.Explanation
```

In `WebPageCluster.ToDetailPage`, before creating `record`, add:

```csharp
var classification = ActivityClassifier.Classify(
    BuildContext("web-page", browserWindow?.AppName, browserName, data.Domain, data.Path, data.Title ?? Primary.WindowTitle, browserWindow?.WindowTitle, Primary.BucketType),
    Rules);
```

Change the `WebPageCluster` record to carry rules:

```csharp
private sealed record WebPageCluster(
    AwEventEntity Primary,
    List<AwEventEntity> LeadingShortEvents,
    List<AwEventEntity> TrailingShortEvents,
    IReadOnlyCollection<ActivityCategoryRuleEntity> Rules)
```

Update cluster creation:

```csharp
clusters.Add(new WebPageCluster(webEvent, leadingShortEvents, new List<AwEventEntity>(), rules));
```

Update `FromShortEvents`:

```csharp
public static WebPageCluster FromShortEvents(List<AwEventEntity> shortEvents, IReadOnlyCollection<ActivityCategoryRuleEntity> rules)
{
    return new WebPageCluster(shortEvents[^1], shortEvents, new List<AwEventEntity>(), rules);
}
```

Update the call:

```csharp
clusters.Add(WebPageCluster.FromShortEvents(pendingShortEvents, rules));
```

In the web-page `PcDetailRecord` constructor, replace the current `null` category argument with `classification.CategoryName`, and append the same classification named arguments.

Delete the old private `ClassifyApp` method from `BrowserPageTimelineBuilder`.

- [ ] **Step 5: Update `PcTrackerService` rule loading and timeline mapping**

In `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`, replace the field:

```csharp
private List<AppCategoryRule>? _cachedRules;
```

with:

```csharp
private List<AppCategoryRule>? _cachedLegacyRules;
private List<ActivityCategoryRuleEntity>? _cachedActivityRules;
```

Add this method near `GetCategoryRulesAsync`:

```csharp
private async Task<List<ActivityCategoryRuleEntity>> GetActivityCategoryRulesAsync(CancellationToken ct)
{
    if (_cachedActivityRules is not null) return _cachedActivityRules;

    _cachedActivityRules = await _db.Set<ActivityCategoryRuleEntity>()
        .Where(r => r.Status == "active")
        .OrderByDescending(r => r.Priority)
        .ToListAsync(ct);
    return _cachedActivityRules;
}
```

Update `BuildInterpretedAwDetailRecordsAsync`:

```csharp
private async Task<List<PcDetailRecord>> BuildInterpretedAwDetailRecordsAsync(List<AwEventEntity> awEvents, CancellationToken ct)
{
    return BrowserPageTimelineBuilder.BuildInterpretedAwRecords(awEvents, await GetActivityCategoryRulesAsync(ct));
}
```

In `QueryCompleteDetailAsync`, replace:

```csharp
var rules = await GetCategoryRulesAsync(ct);
```

with:

```csharp
var rules = await GetActivityCategoryRulesAsync(ct);
```

Update `ToTimelineItem` to return classification fields:

```csharp
return new TimelineItem(
    record.Start,
    record.End ?? record.Start,
    (record.DurationSeconds ?? 0) / 60,
    appName,
    record.Title,
    record.CategoryName ?? "其他",
    record.CategoryColor ?? "#64748b",
    record.ProjectTag,
    record.ClassificationConfidence ?? 0.2,
    record.ClassificationSource ?? "fallback",
    record.ClassificationExplanation ?? "No rule or heuristic matched.");
```

Update legacy rule cache methods:

```csharp
private async Task<List<AppCategoryRule>> GetCategoryRulesAsync(CancellationToken ct)
{
    if (_cachedLegacyRules is not null) return _cachedLegacyRules;
    _cachedLegacyRules = await GetAllCategoriesAsync(ct);
    return _cachedLegacyRules;
}
```

When saving or deleting legacy categories, set both caches to null:

```csharp
_cachedLegacyRules = null;
_cachedActivityRules = null;
```

- [ ] **Step 6: Run timeline classification test and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter GetTimelineAsync_ReturnsBackendClassificationForWebPages
```

Expected: PASS.

- [ ] **Step 7: Run complete PC tracker tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter PcTrackerCompleteCaptureTests
```

Expected: PASS.

- [ ] **Step 8: Commit classified records**

```powershell
git add src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs
git commit -m "feat: return classified pc activity records"
```

---

### Task 6: Rules API And Legacy Category Compatibility

**Files:**
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`

- [ ] **Step 1: Add failing service tests for rule save/list**

Add these `using` directives at the top of `tests/Pim.UnitTests/Services/ActivityClassifierTests.cs` if they are not already present:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
```

Then add this method inside the existing `ActivityClassifierTests` class:

```csharp
[Fact]
public async Task SaveActivityClassificationRuleAsync_PersistsAndListsRule()
{
    PimDbContext.RegisterModuleAssembly(typeof(ActivityCategoryRuleEntity).Assembly);
    var options = new DbContextOptionsBuilder<PimDbContext>()
        .UseInMemoryDatabase(Guid.NewGuid().ToString())
        .Options;

    using var db = new PimDbContext(options);
    var service = new PcTrackerService(db);

    await service.SaveActivityClassificationRuleAsync(new SaveActivityClassificationRuleRequest(
        "ActivityWatch docs",
        "both",
        "学习",
        "ActivityWatch",
        "#14b8a6",
        500,
        """{"all":[{"field":"domain","op":"domainSuffix","value":"docs.activitywatch.net"}]}""",
        0.95,
        "User confirmed docs."), CancellationToken.None);

    var rules = await service.GetActivityClassificationRulesAsync(CancellationToken.None);
    var rule = Assert.Single(rules);
    Assert.Equal("ActivityWatch docs", rule.RuleName);
    Assert.Equal("学习", rule.CategoryName);
    Assert.Equal("ActivityWatch", rule.ProjectTag);
}
```

- [ ] **Step 2: Run rule service test and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter SaveActivityClassificationRuleAsync_PersistsAndListsRule
```

Expected: FAIL because service methods do not exist.

- [ ] **Step 3: Add rule methods to `PcTrackerService`**

In `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`, add public methods near `GetAllCategoriesAsync`:

```csharp
public async Task<List<ActivityClassificationRuleDto>> GetActivityClassificationRulesAsync(CancellationToken ct)
{
    return await _db.Set<ActivityCategoryRuleEntity>()
        .OrderByDescending(r => r.Priority)
        .Select(r => new ActivityClassificationRuleDto(
            r.Id,
            r.RuleName,
            r.Scope,
            r.CategoryName,
            r.ProjectTag,
            r.Color,
            r.Priority,
            r.Source,
            r.Status,
            r.ConditionsJson,
            r.Confidence,
            r.Explanation))
        .ToListAsync(ct);
}

public async Task<ActivityClassificationRuleDto> SaveActivityClassificationRuleAsync(
    SaveActivityClassificationRuleRequest req,
    CancellationToken ct)
{
    var entity = new ActivityCategoryRuleEntity
    {
        RuleName = req.RuleName,
        Scope = req.Scope,
        CategoryName = req.CategoryName,
        ProjectTag = req.ProjectTag,
        Color = req.Color,
        Priority = req.Priority,
        Source = "user",
        Status = "active",
        ConditionsJson = req.ConditionsJson,
        Confidence = req.Confidence,
        Explanation = req.Explanation,
        CreatedAt = DateTimeOffset.UtcNow,
        UpdatedAt = DateTimeOffset.UtcNow
    };

    _db.Set<ActivityCategoryRuleEntity>().Add(entity);
    await _db.SaveChangesAsync(ct);
    _cachedActivityRules = null;

    return new ActivityClassificationRuleDto(
        entity.Id,
        entity.RuleName,
        entity.Scope,
        entity.CategoryName,
        entity.ProjectTag,
        entity.Color,
        entity.Priority,
        entity.Source,
        entity.Status,
        entity.ConditionsJson,
        entity.Confidence,
        entity.Explanation);
}
```

- [ ] **Step 4: Map rules endpoints**

In `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`, add endpoints before `/heatmap/grid`:

```csharp
readGroup.MapGet("/classification/rules", async (
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var rules = await svc.GetActivityClassificationRulesAsync(ct);
    return Results.Ok(ApiResponse<List<ActivityClassificationRuleDto>>.Ok(rules));
});

writeGroup.MapPost("/classification/rules", async (
    [FromBody] SaveActivityClassificationRuleRequest req,
    [FromServices] PcTrackerService svc,
    CancellationToken ct) =>
{
    var rule = await svc.SaveActivityClassificationRuleAsync(req, ct);
    return Results.Ok(ApiResponse<ActivityClassificationRuleDto>.Ok(rule));
});

writeGroup.MapPost("/classification/recompute", () =>
{
    return Results.Ok(ApiResponse<string>.Ok("classification is computed on query in this version"));
});
```

- [ ] **Step 5: Run rule service test and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter SaveActivityClassificationRuleAsync_PersistsAndListsRule
```

Expected: PASS.

- [ ] **Step 6: Commit rules API**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivityClassifierTests.cs
git commit -m "feat: add activity classification rules API"
```

---

### Task 7: Unknown Cluster Suggestions And Manual Accept

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`

- [ ] **Step 1: Write failing suggestion tests**

Create `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivitySuggestionServiceTests
{
    [Fact]
    public async Task BuildSuggestionsAsync_GroupsFallbackWebRecordsByDomain()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityClassificationSuggestionEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        using var db = new PimDbContext(options);
        var service = new ActivitySuggestionService(db);
        var records = new List<PcDetailRecord>
        {
            NewFallbackWebRecord("https://unknown.example.com/a?token=secret", "unknown.example.com", "/a", "Alpha", 60),
            NewFallbackWebRecord("https://unknown.example.com/b?token=secret", "unknown.example.com", "/b", "Beta", 120)
        };

        var suggestions = await service.BuildSuggestionsAsync(records, CancellationToken.None);

        var suggestion = Assert.Single(suggestions);
        Assert.Equal("web:unknown.example.com", suggestion.ClusterKey);
        Assert.Equal(2, suggestion.SampleCount);
        Assert.Equal(180, suggestion.TotalDurationSeconds);
        Assert.DoesNotContain("token=secret", suggestion.SanitizedContextJson);
    }

    [Fact]
    public async Task AcceptSuggestionAsync_CreatesActiveRuleAndMarksAccepted()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityClassificationSuggestionEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        using var db = new PimDbContext(options);
        var suggestion = new ActivityClassificationSuggestionEntity
        {
            ClusterKey = "web:unknown.example.com",
            SampleCount = 2,
            TotalDurationSeconds = 180,
            SanitizedContextJson = "{}",
            SampleRecordsJson = "[]",
            Status = "pending"
        };
        db.Set<ActivityClassificationSuggestionEntity>().Add(suggestion);
        await db.SaveChangesAsync();
        var service = new ActivitySuggestionService(db);

        var rule = await service.AcceptSuggestionAsync(suggestion.Id, new AcceptActivityClassificationSuggestionRequest(
            "Unknown docs",
            "activity",
            "学习",
            null,
            "#14b8a6",
            400,
            """{"all":[{"field":"domain","op":"domainSuffix","value":"unknown.example.com"}]}""",
            0.9,
            "User accepted cluster."), CancellationToken.None);

        Assert.Equal("学习", rule.CategoryName);
        Assert.Equal("accepted", suggestion.Status);
        Assert.Single(db.Set<ActivityCategoryRuleEntity>());
    }

    private static PcDetailRecord NewFallbackWebRecord(string url, string domain, string path, string title, double seconds)
    {
        return new PcDetailRecord(
            "web-page",
            "2026-05-20T05:00:00.0000000Z",
            "2026-05-20T05:01:00.0000000Z",
            seconds,
            "pc",
            null,
            domain,
            "其他",
            title,
            null,
            null,
            null,
            null,
            null,
            null,
            url,
            domain,
            path,
            false,
            CategoryColor: "#64748b",
            ClassificationConfidence: 0.2,
            ClassificationSource: "fallback",
            ClassificationExplanation: "No rule or heuristic matched.");
    }
}
```

- [ ] **Step 2: Run suggestion tests and verify failure**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivitySuggestionServiceTests
```

Expected: FAIL because `ActivitySuggestionService` does not exist.

- [ ] **Step 3: Implement suggestion service**

Create `src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs`:

```csharp
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public sealed class ActivitySuggestionService
{
    private readonly PimDbContext _db;

    public ActivitySuggestionService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<List<ActivityClassificationSuggestionDto>> BuildSuggestionsAsync(
        IReadOnlyCollection<PcDetailRecord> records,
        CancellationToken ct)
    {
        var candidates = records
            .Where(r => string.Equals(r.ClassificationSource, "fallback", StringComparison.OrdinalIgnoreCase)
                || (r.ClassificationConfidence ?? 1) < 0.5)
            .GroupBy(ClusterKey)
            .Where(g => g.Key is not null)
            .Select(g => BuildEntity(g.Key!, g.ToList()))
            .OrderByDescending(s => s.TotalDurationSeconds)
            .ToList();

        foreach (var candidate in candidates)
        {
            var existing = await _db.Set<ActivityClassificationSuggestionEntity>()
                .FirstOrDefaultAsync(s => s.ClusterKey == candidate.ClusterKey && s.Status == "pending", ct);

            if (existing is null)
                _db.Set<ActivityClassificationSuggestionEntity>().Add(candidate);
            else
            {
                existing.SampleCount = candidate.SampleCount;
                existing.TotalDurationSeconds = candidate.TotalDurationSeconds;
                existing.SampleRecordsJson = candidate.SampleRecordsJson;
                existing.SanitizedContextJson = candidate.SanitizedContextJson;
                existing.UpdatedAt = DateTimeOffset.UtcNow;
            }
        }

        await _db.SaveChangesAsync(ct);
        return await GetSuggestionsAsync(ct);
    }

    public async Task<List<ActivityClassificationSuggestionDto>> GetSuggestionsAsync(CancellationToken ct)
    {
        return await _db.Set<ActivityClassificationSuggestionEntity>()
            .OrderByDescending(s => s.TotalDurationSeconds)
            .Select(s => ToDto(s))
            .ToListAsync(ct);
    }

    public async Task<ActivityClassificationRuleDto> AcceptSuggestionAsync(
        Guid id,
        AcceptActivityClassificationSuggestionRequest req,
        CancellationToken ct)
    {
        var suggestion = await _db.Set<ActivityClassificationSuggestionEntity>().FindAsync(new object[] { id }, ct)
            ?? throw new InvalidOperationException("Suggestion not found.");

        var rule = new ActivityCategoryRuleEntity
        {
            RuleName = req.RuleName,
            Scope = req.Scope,
            CategoryName = req.CategoryName,
            ProjectTag = req.ProjectTag,
            Color = req.Color,
            Priority = req.Priority,
            Source = "user",
            Status = "active",
            ConditionsJson = req.ConditionsJson,
            Confidence = req.Confidence,
            Explanation = req.Explanation,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };

        suggestion.Status = "accepted";
        suggestion.UpdatedAt = DateTimeOffset.UtcNow;
        _db.Set<ActivityCategoryRuleEntity>().Add(rule);
        await _db.SaveChangesAsync(ct);

        return new ActivityClassificationRuleDto(rule.Id, rule.RuleName, rule.Scope, rule.CategoryName, rule.ProjectTag, rule.Color, rule.Priority, rule.Source, rule.Status, rule.ConditionsJson, rule.Confidence, rule.Explanation);
    }

    public async Task RejectSuggestionAsync(Guid id, CancellationToken ct)
    {
        var suggestion = await _db.Set<ActivityClassificationSuggestionEntity>().FindAsync(new object[] { id }, ct)
            ?? throw new InvalidOperationException("Suggestion not found.");

        suggestion.Status = "rejected";
        suggestion.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);
    }

    private static ActivityClassificationSuggestionEntity BuildEntity(string clusterKey, List<PcDetailRecord> records)
    {
        var samples = records.Take(5).Select(r => new
        {
            r.RecordType,
            r.AppName,
            r.DisplayName,
            r.Domain,
            Url = ActivityUrlSanitizer.Sanitize(r.Url),
            r.Path,
            r.Title,
            r.DurationSeconds
        }).ToList();

        var context = new
        {
            ClusterKey = clusterKey,
            Domains = records.Select(r => r.Domain).Where(v => !string.IsNullOrWhiteSpace(v)).Distinct().ToList(),
            Apps = records.Select(r => r.AppName ?? r.BrowserAppName).Where(v => !string.IsNullOrWhiteSpace(v)).Distinct().ToList(),
            Titles = records.Select(r => r.Title).Where(v => !string.IsNullOrWhiteSpace(v)).Take(10).ToList(),
            Urls = records.Select(r => ActivityUrlSanitizer.Sanitize(r.Url)).Where(v => !string.IsNullOrWhiteSpace(v)).Distinct().Take(10).ToList()
        };

        return new ActivityClassificationSuggestionEntity
        {
            ClusterKey = clusterKey,
            SampleCount = records.Count,
            TotalDurationSeconds = records.Sum(r => r.DurationSeconds ?? 0),
            SampleRecordsJson = JsonSerializer.Serialize(samples, new JsonSerializerOptions(JsonSerializerDefaults.Web)),
            SanitizedContextJson = JsonSerializer.Serialize(context, new JsonSerializerOptions(JsonSerializerDefaults.Web)),
            CurrentCategory = "其他",
            Status = "pending",
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };
    }

    private static string? ClusterKey(PcDetailRecord record)
    {
        if (!string.IsNullOrWhiteSpace(record.Domain))
            return $"web:{record.Domain.ToLowerInvariant()}";

        var app = AppNameNormalizer.Normalize(record.AppName ?? record.BrowserAppName);
        return string.IsNullOrWhiteSpace(app) ? null : $"app:{app.ToLowerInvariant()}";
    }

    private static ActivityClassificationSuggestionDto ToDto(ActivityClassificationSuggestionEntity s)
    {
        return new ActivityClassificationSuggestionDto(
            s.Id,
            s.ClusterKey,
            s.SampleCount,
            s.TotalDurationSeconds,
            s.SampleRecordsJson,
            s.SanitizedContextJson,
            s.CurrentCategory,
            s.SuggestedCategory,
            s.SuggestedProjectTag,
            s.SuggestedRulesJson,
            s.UserFeedback,
            s.LlmResponseJson,
            s.Status);
    }
}
```

- [ ] **Step 4: Register service**

In `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`, add to `RegisterServices`:

```csharp
services.AddScoped<ActivitySuggestionService>();
```

- [ ] **Step 5: Add suggestion endpoints**

In `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`, add endpoints before `/heatmap/grid`:

```csharp
readGroup.MapGet("/classification/suggestions", async (
    [FromQuery] string? date,
    [FromServices] PcTrackerService pcService,
    [FromServices] ActivitySuggestionService suggestionService,
    CancellationToken ct) =>
{
    var d = date is not null ? DateTime.Parse(date) : DateTime.Today;
    var detail = await pcService.QueryCompleteDetailAsync(new DetailQueryParams(
        d.ToString("yyyy-MM-dd"),
        d.ToString("yyyy-MM-dd"),
        null,
        null,
        null,
        null,
        null,
        null,
        "date",
        "asc",
        1,
        500,
        View: "interpreted"), ct);
    var records = detail.Items
        .Where(r => r.ClassificationSource == "fallback" || (r.ClassificationConfidence ?? 1) < 0.5)
        .ToList();
    var suggestions = await suggestionService.BuildSuggestionsAsync(records, ct);
    return Results.Ok(ApiResponse<List<ActivityClassificationSuggestionDto>>.Ok(suggestions));
});

writeGroup.MapPost("/classification/suggestions/{id:guid}/accept", async (
    Guid id,
    [FromBody] AcceptActivityClassificationSuggestionRequest req,
    [FromServices] ActivitySuggestionService suggestionService,
    CancellationToken ct) =>
{
    var rule = await suggestionService.AcceptSuggestionAsync(id, req, ct);
    return Results.Ok(ApiResponse<ActivityClassificationRuleDto>.Ok(rule));
});

writeGroup.MapPost("/classification/suggestions/{id:guid}/reject", async (
    Guid id,
    [FromServices] ActivitySuggestionService suggestionService,
    CancellationToken ct) =>
{
    await suggestionService.RejectSuggestionAsync(id, ct);
    return Results.Ok(ApiResponse<string>.Ok("rejected"));
});
```

- [ ] **Step 6: Run suggestion tests and verify pass**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter ActivitySuggestionServiceTests
```

Expected: PASS.

- [ ] **Step 7: Commit suggestion service**

```powershell
git add src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs
git commit -m "feat: add activity classification suggestions"
```

---

### Task 8: Frontend Timeline Uses Backend Classification

**Files:**
- Modify: `src/client-web/src/types/index.ts`
- Modify: `src/client-web/src/api/pcTracker.ts`
- Modify: `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`

- [ ] **Step 1: Update frontend types**

In `src/client-web/src/types/index.ts`, replace `TimelineItem` with:

```ts
export interface TimelineItem {
  start: string;
  end: string;
  durationMinutes: number;
  appName: string;
  windowTitle: string | null;
  categoryName: string;
  categoryColor: string;
  projectTag: string | null;
  classificationConfidence: number;
  classificationSource: string;
  classificationExplanation: string;
}
```

Add to `PcDetailRecord`:

```ts
  categoryColor?: string | null;
  projectTag?: string | null;
  classificationConfidence?: number | null;
  classificationSource?: string | null;
  classificationExplanation?: string | null;
```

Add these interfaces near `AppCategoryRule`:

```ts
export interface ActivityClassificationRule {
  id: string;
  ruleName: string;
  scope: string;
  categoryName: string | null;
  projectTag: string | null;
  color: string;
  priority: number;
  source: string;
  status: string;
  conditionsJson: string;
  confidence: number;
  explanation: string | null;
}

export interface ActivityClassificationSuggestion {
  id: string;
  clusterKey: string;
  sampleCount: number;
  totalDurationSeconds: number;
  sampleRecordsJson: string;
  sanitizedContextJson: string;
  currentCategory: string | null;
  suggestedCategory: string | null;
  suggestedProjectTag: string | null;
  suggestedRulesJson: string | null;
  userFeedback: string | null;
  llmResponseJson: string | null;
  status: string;
}
```

- [ ] **Step 2: Add classification API functions**

In `src/client-web/src/api/pcTracker.ts`, add `ActivityClassificationRule` and `ActivityClassificationSuggestion` to the type import.

Add:

```ts
export function getActivityClassificationRules() {
  return apiGet<ApiResponse<ActivityClassificationRule[]>>('/pc/classification/rules').then(r => r.data);
}

export function getActivityClassificationSuggestions(date: string) {
  return apiGet<ApiResponse<ActivityClassificationSuggestion[]>>(`/pc/classification/suggestions?date=${date}`).then(r => r.data);
}
```

- [ ] **Step 3: Rewrite `CategoryTimeline` classification logic**

In `src/client-web/src/components/pc-tracker/CategoryTimeline.tsx`, remove `CategorySummary` and `AppCategoryRule` imports. Replace:

```ts
import type { TimelineItem, CategorySummary, AppCategoryRule } from '../../types';
```

with:

```ts
import type { TimelineItem } from '../../types';
```

Replace the `CategoryBlock` interface with:

```ts
interface CategoryBlock {
  start: Date;
  end: Date;
  categoryName: string;
  projectTag: string | null;
  color: string;
  apps: { name: string; share: number }[];
  totalMinutes: number;
  confidence: number;
  source: string;
  explanation: string;
}
```

Delete `buildAppCategoryMap`.

Replace `buildCategoryBlocks` with:

```ts
function buildCategoryBlocks(timeline: TimelineItem[]): CategoryBlock[] {
  if (!timeline.length) return [];

  const sorted = [...timeline].sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
  const blocks: CategoryBlock[] = [];
  let current: CategoryBlock | null = null;

  for (const item of sorted) {
    const category = item.categoryName || '其他';
    const color = item.categoryColor || '#64748b';
    const projectTag = item.projectTag ?? null;

    if (current && current.categoryName === category && current.projectTag === projectTag) {
      current.end = new Date(item.end);
      current.totalMinutes += item.durationMinutes;
      current.confidence = Math.min(current.confidence, item.classificationConfidence);
      const existing = current.apps.find(a => a.name === item.appName);
      if (existing) existing.share += item.durationMinutes;
      else current.apps.push({ name: item.appName, share: item.durationMinutes });
    } else {
      if (current) blocks.push(current);
      current = {
        start: new Date(item.start),
        end: new Date(item.end),
        categoryName: category,
        projectTag,
        color,
        apps: [{ name: item.appName, share: item.durationMinutes }],
        totalMinutes: item.durationMinutes,
        confidence: item.classificationConfidence,
        source: item.classificationSource,
        explanation: item.classificationExplanation,
      };
    }
  }
  if (current) blocks.push(current);

  for (const block of blocks) {
    const total = block.apps.reduce((sum, app) => sum + app.share, 0);
    for (const app of block.apps) app.share = total > 0 ? Math.round((app.share / total) * 100) : 0;
  }

  return blocks;
}
```

Replace props:

```ts
interface Props {
  timeline: TimelineItem[];
}
```

Replace component signature:

```ts
export default function CategoryTimeline({ timeline }: Props) {
  const blocks = useMemo(() => buildCategoryBlocks(timeline), [timeline]);
```

In the tooltip content, after the duration line, add:

```tsx
{block.projectTag && <div className="text-slate-300">项目：{block.projectTag}</div>}
<div className="text-slate-400">来源：{block.source} · 置信度 {Math.round(block.confidence * 100)}%</div>
<div className="max-w-[260px] whitespace-normal text-slate-400">{block.explanation}</div>
```

- [ ] **Step 4: Stop fetching category rules on PC tracker page**

In `src/client-web/src/pages/PcTrackerPage.tsx`, remove `getPcCategories` from the import:

```ts
import { getPcSummary, getPcHeatmapGrid } from '../api/pcTracker';
```

Remove the `catRulesData` query:

```ts
const { data: catRulesData } = useQuery({
  queryKey: ['pc-categories'],
  queryFn: () => getPcCategories(),
  staleTime: 300000,
});
```

Replace:

```tsx
<CategoryTimeline
  timeline={data?.timeline || []}
  categories={data?.categories || []}
  rules={catRulesData ?? undefined}
/>
```

with:

```tsx
<CategoryTimeline timeline={data?.timeline || []} />
```

- [ ] **Step 5: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 6: Commit frontend timeline change**

```powershell
git add src/client-web/src/types/index.ts src/client-web/src/api/pcTracker.ts src/client-web/src/components/pc-tracker/CategoryTimeline.tsx src/client-web/src/pages/PcTrackerPage.tsx
git commit -m "feat: render backend pc activity classification"
```

---

### Task 9: Final Verification And Follow-Up Plan Stub

**Files:**
- Create: `docs/superpowers/plans/2026-05-21-pc-activity-llm-classification-followup.md`

- [ ] **Step 1: Create follow-up LLM plan stub**

Create `docs/superpowers/plans/2026-05-21-pc-activity-llm-classification-followup.md`:

```markdown
# PC Activity LLM Classification Follow-Up

This follow-up plan should be written after the local classifier step lands.

Required scope:

- Add an LLM provider interface and configuration.
- Add request/response contracts for cluster suggestions.
- Use `ActivityUrlSanitizer` for all provider-bound URLs.
- Add `/classification/suggestions/{id}/llm`.
- Add `/classification/suggestions/{id}/correct`.
- Support natural-language correction that revises a draft rule without activating it.
- Add impact preview before accepting a draft.
- Verify that query strings, fragments, userinfo, and token-like URL data never reach provider payloads.
```

- [ ] **Step 2: Run backend tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj
```

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 4: Check final status**

Run:

```powershell
git status --short --branch
```

Expected: only the follow-up plan stub is uncommitted.

- [ ] **Step 5: Commit follow-up stub**

```powershell
git add docs/superpowers/plans/2026-05-21-pc-activity-llm-classification-followup.md
git commit -m "docs: outline llm classification follow-up"
```

---

## Self-Review Notes

Spec coverage:

- Backend-owned classification: Tasks 4 and 5.
- Multi-field rules: Tasks 2 and 3.
- Seed/migrate old app categories: Task 2.
- Timeline DTO classification fields: Task 5.
- Frontend stops reclassifying: Task 8.
- URL sanitization foundation: Task 1.
- Unknown clusters and manual confirmation: Task 7.
- Recompute endpoint: Task 6 returns an explicit query-time-computed response for this first delivery.
- LLM suggestions and natural-language correction: captured in Task 9 follow-up stub, not implemented in this first local-classifier plan.

Type consistency:

- `ActivityClassificationContext` is introduced in Task 3 and reused in Task 4 and Task 5.
- `ActivityClassificationResult` is introduced in Task 2 and reused in Task 4 and Task 5.
- `ActivityCategoryRuleEntity` is introduced in Task 2 and reused in Task 3 through Task 7.
- Timeline fields added in Task 5 match frontend fields added in Task 8.
