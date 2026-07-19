# Outlook Calendar Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate `IsAllowedNextLink` path-shape false rejections for same-origin `/v1.0/` Microsoft Graph `@odata.nextLink` URLs, enabling multi-page calendar sync to complete, while preserving SSRF security boundaries.

**Architecture:** Remove the path-segment allowlist and `MatchesMeCollectionLeaf` method from `IsAllowedNextLink`, keeping only origin/version/port/dot-traversal guards. The remaining `/v1.0/...` path is treated as opaque. `GetPagesAsync` already passes `nextLink` unchanged to `HttpRequestMessage` — no changes needed there.

**Tech Stack:** .NET (C# 12), xUnit, `HttpClient`, `ScriptedHttpMessageHandler` test double.

---

### Task 1: Update NextLinkScenarios TheoryData (RED)

**Files:**
- Modify: `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs` lines 20-57

- [ ] **Flip path-only false entries to true**

  Change `false` to `true` for entries at current lines 43-47 and 49-50 (`me/drive/root`, `me/calendarGroups/1`, `me/calendarGroups/1/calendars/2`, `me/calendars/c1/calendarView/e1`, `me/calendars/c1/events/e1`, `me/calendars//calendarView?$skiptoken=x`, `users/u@t.com/calendars/c1/calendarView?$skiptoken=a`). The dot-traversal entries (lines 51-56) and fragment entry (line 48) remain `false`.

- [ ] **Add new true cases for trusted opaque paths**

  Insert `true` entries for:
  - `https://graph.microsoft.com/v1.0/users/u/calendars/c/calendarView?$skiptoken=a`
  - `https://graph.microsoft.com/v1.0/opaque/resource/path`

- [ ] **Add new false cases for safety boundaries**

  Insert `false` entries for:
  - `http://graph.microsoft.com/v1.0/me/calendarGroups` (non-HTTPS even though same host)
  - `https://graph.microsoft.com/v1.0` (empty remaining path after `/v1.0`)

  The resulting `NextLinkScenarios` property:

  ```csharp
  public static TheoryData<string, bool> NextLinkScenarios => new()
  {
      { "https://graph.microsoft.com/v1.0/me/calendarGroups?s=s", true },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/1/calendars?s=s", true },
      { "https://graph.microsoft.com/v1.0/me/calendars?s=s", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView?s=s", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/events?s=s", true },
      { "https://graph.microsoft.com:443/v1.0/me/calendars?s=s", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView/?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/AAMkA%2Fxxx%3D%3D/calendarView?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView?$skiptoken=" + new string('A', 2048), true },
      { "https://graph.microsoft.com/v1.0/me/calendars/AAMkA/xxx==/calendarView?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/AAMkA+B/C==/calendarView?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/part1/part2/part3/calendarView/?$skiptoken=p2", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/AAMkA/xxx==/events?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/g/a/calendars?$skiptoken=y", true },
      { "https://graph.microsoft.com/beta/me/calendarGroups", false },
      { "https://evil.com/v1.0/me/calendarGroups", false },
      { "https://graph.microsoft.us/v1.0/me/calendars?s=s", false },
      { "/v1.0/me/calendarGroups", false },
      { "https://graph.microsoft.com:8080/v1.0/me/calendarGroups", false },
      { "https://user:pass@graph.microsoft.com/v1.0/me/calendarGroups", false },
      { "https://graph.microsoft.com/v1.0/me/drive/root", true },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/1", true },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/1/calendars/2", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView/e1", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/events/e1", true },
      { "https://graph.microsoft.com/v1.0/me/calendars/c1/calendarView?s=s#frag", false },
      { "https://graph.microsoft.com/v1.0/me/calendars//calendarView?$skiptoken=x", true },
      { "https://graph.microsoft.com/v1.0/users/u@t.com/calendars/c1/calendarView?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/users/u/calendars/c/calendarView?$skiptoken=a", true },
      { "https://graph.microsoft.com/v1.0/opaque/resource/path", true },
      { "http://graph.microsoft.com/v1.0/me/calendarGroups", false },
      { "https://graph.microsoft.com/v1.0", false },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/../me/calendarGroups?s=s", false },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/%2e%2e/me/calendarGroups?s=s", false },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/%2E%2E/me/calendarGroups?s=s", false },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/%252e%252e/me/calendarGroups?s=s", false },
      { "https://graph.microsoft.com/v1.0/me/calendars/./calendarView?s=s", false },
      { "https://graph.microsoft.com/v1.0/me/calendarGroups/../calendars?$skiptoken=x", false },
  };
  ```

### Task 2: Add Pagination Integration Test (RED)

**Files:**
- Modify: `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs`

- [ ] **Add `Pagination_FollowsTrustedOpaqueNextLinkWithoutReconstruction` test**

  Insert this test after `Pagination_InvalidNextLink_Rejected` (after line 437):

  ```csharp
  [Fact]
  public async Task Pagination_FollowsTrustedOpaqueNextLinkWithoutReconstruction()
  {
      var (client, handler, _, _) = CreateClient();
      handler.Enqueue(HttpStatusCode.OK,
          """{"value":[{"id":"e1"}],"@odata.nextLink":"https://graph.microsoft.com/v1.0/users/u/calendars/c/calendarView?$skiptoken=abc&marker=keep"}""");
      handler.Enqueue(HttpStatusCode.OK, """{"value":[{"id":"e2"}]}""");

      var start = new DateTimeOffset(2026, 7, 1, 0, 0, 0, TimeSpan.Zero);
      var end = new DateTimeOffset(2026, 7, 31, 0, 0, 0, TimeSpan.Zero);
      var pages = await CollectPages(client.GetCalendarViewAsync(ConnectionId, "c", start, end, default));

      Assert.Equal(2, pages.Count);
      Assert.Equal(2, handler.Requests.Count);
      Assert.Equal(
          "https://graph.microsoft.com/v1.0/users/u/calendars/c/calendarView?$skiptoken=abc&marker=keep",
          handler.Requests[1].RequestUri!.AbsoluteUri);
      Assert.Equal("e1", pages[0].Items[0].GetProperty("id").GetString());
      Assert.Equal("e2", pages[1].Items[0].GetProperty("id").GetString());
  }
  ```

- [ ] **Run tests to confirm RED**

  Run both the TheoryData test and the new integration test separately to confirm failure:

  ```powershell
  dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GraphCalendarClientTests.IsAllowedNextLink" --no-restore
  ```

  Expected RED: Some test cases in the TheoryData that now expect `true` still receive `false` from the current implementation.

  ```powershell
  dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Pagination_FollowsTrustedOpaqueNextLinkWithoutReconstruction" --no-restore
  ```

  Expected RED: `InvalidOperationException("Invalid nextLink rejected")` thrown during pagination — `IsAllowedNextLink` returns `false` for the `users/` path, so the second page request is never made. Test fails with unhandled exception or assertion failure before reaching `Assert.Equal(2, pages.Count)`.

### Task 3: Minimal GREEN Implementation

**Files:**
- Modify: `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`

- [ ] **Strip path-segment pattern matching from `IsAllowedNextLink`**

  Delete lines 134-165 in `GraphCalendarClient.cs`. The deleted block is the segment-splitting decode loop, the `segments is [...]` allowlist patterns, and the final `return false;`. Replace with a single `return true;`.

  The revised `IsAllowedNextLink` method:

  ```csharp
  public static bool IsAllowedNextLink(string value)
  {
      if (!Uri.TryCreate(value, UriKind.Absolute, out var uri))
          return false;

      if (!string.Equals(uri.Scheme, "https", StringComparison.OrdinalIgnoreCase))
          return false;

      if (!string.Equals(uri.Host, "graph.microsoft.com", StringComparison.OrdinalIgnoreCase))
          return false;

      if (!uri.IsDefaultPort)
          return false;

      if (!string.IsNullOrEmpty(uri.UserInfo))
          return false;

      if (!string.IsNullOrEmpty(uri.Fragment))
          return false;

      if (HasRawDotSegments(uri))
          return false;

      var path = uri.AbsolutePath;
      if (!path.StartsWith("/v1.0/", StringComparison.Ordinal))
          return false;

      var remaining = path.AsSpan("/v1.0/".Length);
      while (remaining.Length > 0 && remaining[^1] == '/')
          remaining = remaining[..^1];
      if (remaining.Length == 0)
          return false;

      return true;
  }
  ```

- [ ] **Delete `MatchesMeCollectionLeaf` method**

  Remove the entire `MatchesMeCollectionLeaf` private method (lines 168-190 in original). It is no longer called from anywhere.

### Task 4: Run Tests GREEN

- [ ] **Run filtered TheoryData test**

  ```powershell
  dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GraphCalendarClientTests.IsAllowedNextLink" --no-restore
  ```

  Expected GREEN: Every `NextLinkScenarios` theory case passes. Previously-false path-only entries now return `true`; all security-rejection entries remain `false`.

- [ ] **Run new integration test**

  ```powershell
  dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~Pagination_FollowsTrustedOpaqueNextLinkWithoutReconstruction" --no-restore
  ```

  Expected GREEN: Two pages collected, two HTTP requests made, and the second request URI is exactly the canonical nextLink with no path/query reconstruction.

- [ ] **Run all GraphCalendarClientTests**

  ```powershell
  dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GraphCalendarClientTests" --no-restore
  ```

  Expected GREEN: All existing pagination tests (`Pagination_FollowsValidNextLink`, `Pagination_FollowsTrailingSlashCalendarViewNextLink`, `Pagination_FollowsUnencodedSlashImmutableIdCalendarViewNextLink`, `Pagination_InvalidNextLink_Rejected`) continue passing. All write/read/retry/dispose/sensitive-data tests pass.

- [ ] **Run full solution tests**

  ```powershell
  dotnet test Pim.sln --no-restore
  ```

  Expected GREEN. Baseline before changes was 1170/1170 passing. Final count may increase (new test was added) but must not decrease. No existing test regressions.

### Task 5: Independent Review

- [ ] **Inspect changes for security contract correctness**

  Review `IsAllowedNextLink` against the security contract in the approved design:
  - `Uri.TryCreate` with `UriKind.Absolute` — present
  - `uri.Scheme == "https"` — present
  - `uri.Host == "graph.microsoft.com"` — present
  - `uri.IsDefaultPort` — present (default 443 passes; `:8080` fails)
  - Empty `UserInfo` — present (user:pass fails)
  - Empty `Fragment` — present (`#frag` fails)
  - `HasRawDotSegments` — present (raw, encoded, double-encoded `../.` fail)
  - Starts with `/v1.0/` — present (`/beta/...` and `/v1.0` without trailing `/` fail)
  - Non-empty remaining path — present (trailing slash trimmed then checked)
  - No path-segment pattern matching — confirmed deleted
  - No `MatchesMeCollectionLeaf` — confirmed deleted

- [ ] **Verify TDD evidence**

  Confirm the captured command evidence shows both RED test commands failed for the expected path-shape rejection before `GraphCalendarClient.cs` changed, followed by GREEN results after the minimal implementation. Do not require a deliberately broken test-only commit.

- [ ] **Verify scope is contained**

  Only two functional files changed:
  - `src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs`
  - `tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs`

  No changes to: `OutlookCalendarSyncService`, any workflow file, API endpoints, UI, logging, configuration, `.github/workflows/`, `OutlookCalendarSyncServiceTests.cs`, or write methods.

- [ ] **Verify sensitive-data handling**

  Confirm `IsAllowedNextLink` does not log, persist, or expose the input `value`. Confirm `GetPagesAsync` passes `url = nextLink` to `HttpRequestMessage` without logging. No `WriteLine`, `ILogger`, or database write involving the nextLink string.

- [ ] **Check test gaps**

  Run:

  ```powershell
  dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "FullyQualifiedName~GraphCalendarClientTests" --no-restore
  ```

  Confirm all tests pass. If a review finding requires behavior to change, first add or adjust one focused test and verify the expected RED failure before changing production code, then rerun this command to GREEN.

### Task 6: Commit, Push, and PR

- [ ] **Stage and commit**

  ```powershell
  git add tests/Pim.UnitTests/Calendar/GraphCalendarClientTests.cs src/modules/Pim.Module.Calendar/Services/GraphCalendarClient.cs
  git status --short --branch
  ```

  Verify only the two intended source files are staged (no `bin/`, `obj/`, or generated outputs). Then:

  ```powershell
  git commit -m "fix: follow opaque Graph calendar nextLinks"
  ```

- [ ] **Push branch and open PR**

  ```powershell
  git push origin codex/fix-outlook-calendar-pagination
  gh pr create --base master --head codex/fix-outlook-calendar-pagination --title "fix: follow opaque Graph calendar nextLinks" --body "Replace path-shape allowlist in IsAllowedNextLink with origin/version-only validation, allowing trusted same-origin /v1.0/ paths as opaque. Existing security rejections (wrong host, non-HTTPS, non-default port, userinfo, fragment, dot-traversal, non-/v1.0/ prefix, empty remaining path) are preserved. Production evidence: three bindings experiencing 5-minute invalid-next-link failures should progress beyond page 1 after deploy. No binding IDs, calendar IDs, tokens, or continuation query values are included."
  ```

- [ ] **Wait for GitHub Actions checks**

  Monitor `gh pr checks --watch`. If workflows are triggered, confirm they pass. If no workflow is triggered because the changed files (`.cs` only) do not match workflow path filters, state that explicitly rather than waiting indefinitely.

- [ ] **Report outcome**

  If all checks pass (or no workflow triggered for `.cs` files), the branch is ready for merge. After merge/deploy from `master`, observe the three previously-failing bindings to confirm at least two pages are synced per calendar without `invalid-next-link` errors. Production acceptance is not complete until this observation is made — local and CI test passing does not constitute production acceptance.

### Production Acceptance

After the PR is merged and deployed from `master`:

1. Observe three previously-failing bindings' next sync cycle.
2. Confirm each calendar view progresses beyond page 1 without `InvalidOperationException("Invalid nextLink rejected")`.
3. Confirm `invalid-next-link` error count stops increasing in API health metrics.
4. Do not inject malicious nextLinks in production; security rejection remains an automated unit-test acceptance gate.
5. Production acceptance is verified by the absence of `invalid-next-link` logs for those bindings — this cannot be verified in local or CI tests alone.
