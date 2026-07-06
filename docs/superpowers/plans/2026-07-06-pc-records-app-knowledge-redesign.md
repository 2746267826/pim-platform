# PC Records App Knowledge Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This plan requires multiple synchronized subagents. The maximum number of concurrently running subagents is 14.

**Goal:** Rework PC Records into a review-first page and move user-facing classification knowledge into an app-centric App Knowledge experience with Category Tree nested beneath it.

**Architecture:** Preserve the Route 3 safety foundation: preview, apply, recompute, audit, stable record keys, and existing classifier rules. Add an App Knowledge vocabulary and context-pattern layer above the existing classifier rule layer, then reorganize the frontend navigation, PC Records layout, App Knowledge page, and suggestion confirmation copy around that vocabulary.

**Tech Stack:** .NET 9, ASP.NET Minimal APIs, EF Core, PostgreSQL-compatible schema initializer, xUnit, React 19, TypeScript, TanStack Query, React Router, Vite.

---

## Goal-Mode Objective

Use this objective when starting goal mode:

> Implement `docs/superpowers/specs/2026-07-06-pc-records-app-knowledge-redesign.md`: make PC Records review-first, remove standalone Classification Management and Category Tree from main navigation, nest Category Tree under App Knowledge, expand App Knowledge into an app-centric context-pattern knowledge center, add App Knowledge suggestion preview/apply wrappers that persist learned app/domain/title context knowledge while preserving preview-apply-recompute-audit safety, update copy and feedback to say the system saved knowledge, add focused backend/frontend tests, run local verification, commit, push when requested, and coordinate the work through multiple synchronized subagents with at most 14 running concurrently.

## Mandatory Subagent Strategy

Execution must use `superpowers:subagent-driven-development`. Do not execute this plan as a single inline coding pass unless the user explicitly replaces the requirement.

Recommended synchronized worker groups after Task 0:

1. **Navigation agent:** Task 1.
2. **App shell/category agent:** Task 2.
3. **PC review layout agent:** Tasks 3-4.
4. **Frontend API/types agent:** Task 5.
5. **Backend model/schema agent:** Task 6.
6. **Backend service/endpoint agent:** Task 7.
7. **Suggestion wrapper agent:** Task 8.
8. **App Knowledge UI agent:** Task 9.
9. **Integration copy agent:** Task 10.
10. **Frontend tests agent:** Task 11.
11. **Backend tests agent:** Task 12.
12. **Verification agent:** Task 13.
13. **Code review agent A:** Review frontend changes after Tasks 1-5 and 9-11.
14. **Code review agent B:** Review backend changes after Tasks 6-8 and 12.

Concurrent cap: run no more than 14 subagents at once. If a task touches the same file as an active worker, queue it behind that worker instead of editing in parallel.

Dependency guardrails:

- Task 0 must complete first.
- Task 1 can run with Tasks 3, 5, and 6 after preflight.
- Task 2 depends on Task 1 route decisions.
- Task 7 depends on Task 6.
- Task 8 depends on Tasks 6 and 7.
- Task 9 depends on Tasks 5 and 7.
- Task 10 depends on Tasks 3, 4, 8, and 9.
- Task 13 runs after all implementation and test tasks merge.

## File Structure

Frontend files to create:

- `src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx`: App Knowledge local navigation between Apps and Category Tree.
- `src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx`: renders domain/title/project context patterns for a selected app.
- `src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx`: shared compact impact metrics for app rows and context rows.
- `src/client-web/src/components/pc-tracker/PcReviewSummary.tsx`: first-screen daily review metric strip.
- `src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx`: App Knowledge-oriented suggestion cards for PC Records.
- `src/client-web/src/api/appKnowledge.ts`: typed App Knowledge context and suggestion endpoints.
- `tests/client-web/appKnowledgeNavigation.test.tsx`
- `tests/client-web/appKnowledgeApiPath.test.ts`
- `tests/client-web/appKnowledgeTypes.test.ts`
- `tests/client-web/appKnowledgeComponents.test.tsx`
- `tests/client-web/pcRecordsReviewLayout.test.tsx`

Frontend files to modify:

- `src/client-web/src/layout/Sidebar.tsx`: remove standalone classification/category items and export primary nav for tests.
- `src/client-web/src/layout/AppLayout.tsx`: add nested App Knowledge category route and legacy redirects.
- `src/client-web/src/pages/AppKnowledgeBasePage.tsx`: convert from a simple signature table into app-centric knowledge center.
- `src/client-web/src/pages/CategoryTreePage.tsx`: retitle as Category Tree and remove standalone classification-management wording.
- `src/client-web/src/pages/PcTrackerPage.tsx`: reorder around review summary, main timeline, and context confirmations.
- `src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx`: change copy to App Knowledge confirmation, keep stale-preview guard.
- `src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx`: retitle impact summary as knowledge preview impact.
- `src/client-web/src/api/pcTracker.ts`: keep compatibility exports, add invalidation keys when App Knowledge apply succeeds.
- `src/client-web/src/types/index.ts`: add App Knowledge DTOs if they are not kept in `api/appKnowledge.ts`.

Backend files to create:

- `src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs`: persisted domain/title/context pattern knowledge.
- `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs`: list/save/delete context patterns and recent impact summaries.
- `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs`: wrapper that converts classification suggestions into App Knowledge previews and applies.
- `tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs`
- `tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs`

Backend files to modify:

- `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`: add App Knowledge context and suggestion DTOs.
- `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`: configure `AppKnowledgeContextEntity`.
- `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`: add SQL for the new table and indexes.
- `src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs`: include context counts and recent impact in app signature list DTOs.
- `src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs`: expose enough draft metadata for recommended scope summaries.
- `src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs`: preserve existing apply path; support App Knowledge wrapper without bypassing recompute/audit.
- `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`: register services and map App Knowledge endpoints.
- `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs`
- `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`
- `tests/Pim.UnitTests/Services/ActivitySuggestionServiceTests.cs`

Generated files:

- EF migration under `src/Pim.Infrastructure/Data/Migrations/` for App Knowledge contexts after entity configuration changes.

Do not commit:

- `.superpowers/brainstorm/`
- `src/Pim.Api/wwwroot/`
- `bin/`, `obj/`, `build/`, `dist/`, `.dotnet-*`, npm caches, or publish artifacts.

---

### Task 0: Preflight, Baseline, And Worktree Discipline

**Files:**
- Read: `AGENTS.md`
- Read: `docs/superpowers/specs/2026-07-06-pc-records-app-knowledge-redesign.md`
- Read: `docs/superpowers/plans/2026-07-06-pc-records-app-knowledge-redesign.md`

- [ ] **Step 1: Confirm git state**

Run:

```powershell
git status --short --branch
git fetch --all --prune
git status --short --branch
```

Expected: branch and dirty files are known. If `master` is behind `origin/master`, pull before continuing unless the user explicitly says not to.

- [ ] **Step 2: Create an implementation branch**

Run:

```powershell
git checkout -b codex/pc-records-app-knowledge-redesign
```

Expected: branch switches to `codex/pc-records-app-knowledge-redesign`.

- [ ] **Step 3: Run baseline frontend checks**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3ApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Types.test.ts
npm --prefix src/client-web run build
```

Expected: PASS. If a baseline check fails before edits, record the exact failing command and output in the task notes before proceeding.

- [ ] **Step 4: Run baseline backend checks**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "ActivityClassification|ActivitySuggestion|AppSignature|PcCategory"
```

Expected: PASS. If a baseline check fails before edits, record the exact failing test and output in the task notes before proceeding.

- [ ] **Step 5: Commit only if branch metadata changed**

No source commit is expected in this task. Run:

```powershell
git status --short --branch
```

Expected: no new source changes.

---

### Task 1: Main Navigation And Route Compatibility

**Files:**
- Modify: `src/client-web/src/layout/Sidebar.tsx`
- Modify: `src/client-web/src/layout/AppLayout.tsx`
- Test: `tests/client-web/appKnowledgeNavigation.test.tsx`

- [ ] **Step 1: Write failing navigation test**

Create `tests/client-web/appKnowledgeNavigation.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import { renderToStaticMarkup } from 'react-dom/server';
import { Navigate } from 'react-router-dom';
import { primaryNavItems } from '../../src/client-web/src/layout/Sidebar';
import AppKnowledgeTabs from '../../src/client-web/src/components/app-knowledge/AppKnowledgeTabs';

test('sidebar exposes app knowledge but not standalone classification pages', () => {
  const labels = primaryNavItems.map(item => item.label);

  assert.equal(labels.includes('App知识库'), true);
  assert.equal(labels.includes('分类管理'), false);
  assert.equal(labels.includes('分类树'), false);
});

test('app knowledge tabs include category tree as a secondary page', () => {
  const html = renderToStaticMarkup(<AppKnowledgeTabs active="categories" />);

  assert.equal(html.includes('App 列表'), true);
  assert.equal(html.includes('分类树'), true);
  assert.equal(html.includes('/app-knowledge-base/categories'), true);
});

test('legacy category route can redirect to nested app knowledge category route', () => {
  const html = renderToStaticMarkup(<Navigate to="/app-knowledge-base/categories" replace />);

  assert.equal(typeof html, 'string');
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeNavigation.test.tsx
```

Expected: FAIL because `primaryNavItems` and `AppKnowledgeTabs` do not exist.

- [ ] **Step 3: Export primary nav items without standalone classification entries**

Modify the top of `src/client-web/src/layout/Sidebar.tsx`:

```tsx
export const primaryNavItems = [
  { label: '今日', path: '/today', short: '今' },
  { label: '日历', path: '/calendar', short: '历' },
  { label: '快速记录', path: '/quick-notes', short: '记' },
  { label: '文件', path: '/files', short: '文' },
  { label: '任务', path: '/tasks', short: '任' },
  { label: 'PC记录', path: '/pc-tracker', short: 'PC' },
  { label: 'App知识库', path: '/app-knowledge-base', short: '库' },
  { label: '状态信息', path: '/status', short: '态' },
  { label: '设置', path: '/settings', short: '设' },
];
```

Replace `navItems.map` with `primaryNavItems.map`.

- [ ] **Step 4: Create App Knowledge tabs**

Create `src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx`:

```tsx
import { Link } from 'react-router-dom';

interface Props {
  active: 'apps' | 'categories';
}

const tabs = [
  { id: 'apps' as const, label: 'App 列表', path: '/app-knowledge-base' },
  { id: 'categories' as const, label: '分类树', path: '/app-knowledge-base/categories' },
];

export default function AppKnowledgeTabs({ active }: Props) {
  return (
    <nav className="flex flex-wrap gap-2" aria-label="App 知识库导航">
      {tabs.map(tab => (
        <Link
          key={tab.id}
          to={tab.path}
          aria-current={tab.id === active ? 'page' : undefined}
          className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
            tab.id === active
              ? 'bg-blue-600 text-white'
              : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
          }`}
        >
          {tab.label}
        </Link>
      ))}
    </nav>
  );
}
```

- [ ] **Step 5: Add nested route and legacy redirect**

Modify `src/client-web/src/layout/AppLayout.tsx` route section:

```tsx
<Route path="/app-knowledge-base" element={<AppKnowledgeBasePage />} />
<Route path="/app-knowledge-base/categories" element={<CategoryTreePage />} />
<Route path="/pc-categories" element={<Navigate to="/app-knowledge-base/categories" replace />} />
<Route path="/pc-classification" element={<Navigate to="/app-knowledge-base" replace />} />
```

Remove the direct route to `PcClassificationPage` from user-visible navigation. Keep the import only if the page is still used by an internal route. If not used, remove the import.

- [ ] **Step 6: Run focused navigation test**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeNavigation.test.tsx
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-web/src/layout/Sidebar.tsx src/client-web/src/layout/AppLayout.tsx src/client-web/src/components/app-knowledge/AppKnowledgeTabs.tsx tests/client-web/appKnowledgeNavigation.test.tsx
git commit -m "feat: nest category tree under app knowledge"
```

Expected: commit succeeds.

---

### Task 2: Category Tree As App Knowledge Secondary Page

**Files:**
- Modify: `src/client-web/src/pages/CategoryTreePage.tsx`
- Modify: `src/client-web/src/pages/AppKnowledgeBasePage.tsx`
- Test: `tests/client-web/appKnowledgeComponents.test.tsx`

- [ ] **Step 1: Write failing category page copy test**

Create or append to `tests/client-web/appKnowledgeComponents.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import { renderToStaticMarkup } from 'react-dom/server';
import AppKnowledgeTabs from '../../src/client-web/src/components/app-knowledge/AppKnowledgeTabs';

test('category tree secondary navigation uses app knowledge language', () => {
  const html = renderToStaticMarkup(<AppKnowledgeTabs active="categories" />);

  assert.equal(html.includes('App 列表'), true);
  assert.equal(html.includes('分类树'), true);
  assert.equal(html.includes('分类管理'), false);
});
```

- [ ] **Step 2: Run test to verify current behavior**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeComponents.test.tsx
```

Expected: PASS for `AppKnowledgeTabs`; the page itself still needs integration.

- [ ] **Step 3: Add tabs to App Knowledge page**

In `src/client-web/src/pages/AppKnowledgeBasePage.tsx`, import:

```tsx
import AppKnowledgeTabs from '../components/app-knowledge/AppKnowledgeTabs';
```

Add tabs directly below `PageHeader`:

```tsx
<AppKnowledgeTabs active="apps" />
```

Update the subtitle:

```tsx
subtitle="管理应用、域名、标题模式和分类归属知识"
```

- [ ] **Step 4: Add tabs and copy to Category Tree page**

In `src/client-web/src/pages/CategoryTreePage.tsx`, import:

```tsx
import AppKnowledgeTabs from '../components/app-knowledge/AppKnowledgeTabs';
```

Replace the outer header with:

```tsx
<div className="space-y-4">
  <PageHeader title="分类树" subtitle="作为 App 知识库的目标分类结构" />
  <AppKnowledgeTabs active="categories" />
  <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
```

Remove the existing `mt-4` on the grid because the outer `space-y-4` now handles spacing. Close the new wrapper at the end of the component.

- [ ] **Step 5: Run build-focused check**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-web/src/pages/AppKnowledgeBasePage.tsx src/client-web/src/pages/CategoryTreePage.tsx tests/client-web/appKnowledgeComponents.test.tsx
git commit -m "feat: present category tree as app knowledge page"
```

Expected: commit succeeds.

---

### Task 3: PC Records Review Summary Component

**Files:**
- Create: `src/client-web/src/components/pc-tracker/PcReviewSummary.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Test: `tests/client-web/pcRecordsReviewLayout.test.tsx`

- [ ] **Step 1: Write failing review summary test**

Create `tests/client-web/pcRecordsReviewLayout.test.tsx`:

```tsx
import assert from 'node:assert/strict';
import { renderToStaticMarkup } from 'react-dom/server';
import PcReviewSummary from '../../src/client-web/src/components/pc-tracker/PcReviewSummary';
import type { ActivityClassificationSuggestion, PcSummaryResponse } from '../../src/client-web/src/types';

const summary: PcSummaryResponse = {
  keystats: null,
  heatmap: [],
  appRanking: [],
  timeline: [],
  sessions: [],
  metrics: {
    totalRecordedDuration: '8h 12m',
    activeInputDuration: '5h 40m',
    idleDuration: '2h 32m',
    sessionCount: 4,
    activeAppCount: 9,
    totalKeyPresses: 1234,
    totalClicks: 456,
    appSwitchCount: 78,
    switchFrequency: 9.5,
    mostFocusedApp: 'Code.exe',
    keyClickRatio: 2.7,
  },
  categories: [
    { categoryName: '工作 / 开发', color: '#2563eb', share: 0.62, keyPresses: 1000, totalClicks: 300 },
  ],
};

const suggestion: ActivityClassificationSuggestion = {
  id: 'suggestion-1',
  clusterKey: 'msedge.exe|github.com',
  sampleCount: 42,
  totalDurationSeconds: 3600,
  sampleRecordsJson: '[]',
  sanitizedContextJson: '{}',
  currentCategory: '其他',
  suggestedCategory: '工作 / 开发',
  suggestedProjectTag: null,
  suggestedRulesJson: null,
  userFeedback: null,
  llmResponseJson: null,
  status: 'pending',
  appDisplayName: 'Edge',
  appIcon: null,
  recognitionSource: 'system',
};

test('pc review summary renders high signal daily review metrics', () => {
  const html = renderToStaticMarkup(
    <PcReviewSummary summary={summary} pendingSuggestions={[suggestion]} />
  );

  assert.equal(html.includes('今日复盘'), true);
  assert.equal(html.includes('记录时长'), true);
  assert.equal(html.includes('主要分类'), true);
  assert.equal(html.includes('待确认上下文'), true);
  assert.equal(html.includes('工作 / 开发'), true);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRecordsReviewLayout.test.tsx
```

Expected: FAIL because `PcReviewSummary` does not exist.

- [ ] **Step 3: Create review summary component**

Create `src/client-web/src/components/pc-tracker/PcReviewSummary.tsx`:

```tsx
import type { ActivityClassificationSuggestion, PcSummaryResponse } from '../../types';

interface Props {
  summary: PcSummaryResponse | undefined;
  pendingSuggestions: ActivityClassificationSuggestion[];
}

function formatCount(value: number) {
  return value.toLocaleString('zh-CN');
}

function mainCategory(summary: PcSummaryResponse | undefined) {
  const category = summary?.categories?.[0];
  return category?.categoryName || '暂无';
}

export default function PcReviewSummary({ summary, pendingSuggestions }: Props) {
  const metrics = summary?.metrics;
  const totalInputs = (metrics?.totalKeyPresses ?? 0) + (metrics?.totalClicks ?? 0);

  const cards = [
    { label: '记录时长', value: metrics?.totalRecordedDuration ?? '-' },
    { label: '有效输入', value: metrics?.activeInputDuration ?? '-' },
    { label: '主要分类', value: mainCategory(summary) },
    { label: '上下文切换', value: metrics ? formatCount(metrics.appSwitchCount) : '-' },
    { label: '待确认上下文', value: formatCount(pendingSuggestions.length) },
    { label: '输入活跃度', value: formatCount(totalInputs) },
  ];

  return (
    <section className="pim-panel p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <h2 className="text-base font-semibold text-slate-950">今日复盘</h2>
          <p className="mt-1 text-sm text-slate-500">
            先看今天的活动结构，再确认需要写入 App 知识库的上下文。
          </p>
        </div>
        {metrics?.mostFocusedApp && (
          <div className="rounded-lg border border-blue-100 bg-blue-50 px-3 py-2 text-xs font-medium text-blue-700">
            最聚焦：{metrics.mostFocusedApp}
          </div>
        )}
      </div>
      <div className="mt-4 grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        {cards.map(card => (
          <div key={card.label} className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
            <div className="text-xs font-medium text-slate-500">{card.label}</div>
            <div className="mt-1 min-h-7 break-words text-lg font-semibold text-slate-950">{card.value}</div>
          </div>
        ))}
      </div>
    </section>
  );
}
```

- [ ] **Step 4: Integrate into PC Records page**

In `src/client-web/src/pages/PcTrackerPage.tsx`, import:

```tsx
import PcReviewSummary from '../components/pc-tracker/PcReviewSummary';
```

Place it after `PcQualitySummary` and before the timeline/context grid:

```tsx
<PcReviewSummary summary={data} pendingSuggestions={suggestions} />
```

Keep the existing `MetricCard` block only until Task 4 reorders the page. If the old metric block duplicates the new summary after Task 4, remove the old block there.

- [ ] **Step 5: Run focused test**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRecordsReviewLayout.test.tsx
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-web/src/components/pc-tracker/PcReviewSummary.tsx src/client-web/src/pages/PcTrackerPage.tsx tests/client-web/pcRecordsReviewLayout.test.tsx
git commit -m "feat: add pc records daily review summary"
```

Expected: commit succeeds.

---

### Task 4: Context Confirmation Panel And Review-First Ordering

**Files:**
- Create: `src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx`
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Test: `tests/client-web/pcRecordsReviewLayout.test.tsx`

- [ ] **Step 1: Append failing context panel test**

Append to `tests/client-web/pcRecordsReviewLayout.test.tsx`:

```tsx
import ContextConfirmationPanel from '../../src/client-web/src/components/pc-tracker/ContextConfirmationPanel';

test('context confirmation panel uses app knowledge language', () => {
  const html = renderToStaticMarkup(
    <ContextConfirmationPanel
      suggestions={[suggestion]}
      isLoading={false}
      onPreview={() => undefined}
      onReject={() => undefined}
    />
  );

  assert.equal(html.includes('待确认上下文'), true);
  assert.equal(html.includes('写入 App 知识库'), true);
  assert.equal(html.includes('规则'), false);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRecordsReviewLayout.test.tsx
```

Expected: FAIL because `ContextConfirmationPanel` does not exist.

- [ ] **Step 3: Create context confirmation panel**

Create `src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx`:

```tsx
import type { ActivityClassificationSuggestion } from '../../types';

interface Props {
  suggestions: ActivityClassificationSuggestion[];
  isLoading: boolean;
  onPreview: (suggestion: ActivityClassificationSuggestion) => void;
  onReject: (suggestion: ActivityClassificationSuggestion) => void;
}

function minutes(seconds: number) {
  return `${Math.round(seconds / 60).toLocaleString('zh-CN')} 分钟`;
}

function contextTitle(suggestion: ActivityClassificationSuggestion) {
  return suggestion.appDisplayName || suggestion.clusterKey || '未识别上下文';
}

function targetText(suggestion: ActivityClassificationSuggestion) {
  const category = suggestion.suggestedCategory || '待选择分类';
  const project = suggestion.suggestedProjectTag ? `，项目 ${suggestion.suggestedProjectTag}` : '';
  return `${category}${project}`;
}

export default function ContextConfirmationPanel({ suggestions, isLoading, onPreview, onReject }: Props) {
  if (isLoading) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        正在加载待确认上下文...
      </div>
    );
  }

  const visible = suggestions.slice(0, 6);

  if (visible.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        暂无需要写入 App 知识库的上下文。
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div>
        <h2 className="text-sm font-semibold text-slate-950">待确认上下文</h2>
        <p className="mt-1 text-xs text-slate-500">确认后会写入 App 知识库，并重算受影响记录。</p>
      </div>
      {visible.map(suggestion => (
        <article key={suggestion.id} className="rounded-lg border border-slate-200 bg-white p-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <h3 className="break-words text-sm font-semibold text-slate-950">{contextTitle(suggestion)}</h3>
              <p className="mt-1 text-xs text-slate-600">
                建议写入 App 知识库：{targetText(suggestion)}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {suggestion.sampleCount.toLocaleString('zh-CN')} 条样本 · {minutes(suggestion.totalDurationSeconds)}
              </p>
            </div>
            <div className="flex shrink-0 flex-col gap-2">
              <button type="button" onClick={() => onPreview(suggestion)} className="pim-button-primary min-h-8 px-3 text-xs">
                预览并确认
              </button>
              <button type="button" onClick={() => onReject(suggestion)} className="pim-button-secondary min-h-8 px-3 text-xs">
                忽略
              </button>
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
```

- [ ] **Step 4: Reorder PC Records page**

Modify `src/client-web/src/pages/PcTrackerPage.tsx`:

- Replace `ClassificationActionQueue` import with `ContextConfirmationPanel`.
- Remove the old standalone "分类建议" `AnalysisCard`.
- Move `CategoryTimeline` into a grid beside `ContextConfirmationPanel` directly after `PcReviewSummary`.
- Remove the old `MetricCard` import and metric card block because `PcReviewSummary` now owns the high-signal metrics.

Use this first-screen block:

```tsx
<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.55fr)_minmax(360px,0.85fr)]">
  <AnalysisCard
    title="分类时间线"
    subtitle="今日活动结构、主要分类和待确认片段"
    actions={
      <button
        type="button"
        onClick={() => setTimelineDialogOpen(true)}
        className="pim-button-primary h-8 px-3 text-xs font-medium"
      >
        查看详情
      </button>
    }
  >
    <CategoryTimeline timeline={data?.timeline || []} />
  </AnalysisCard>

  <AnalysisCard title="待确认上下文" subtitle="确认后写入 App 知识库">
    <ContextConfirmationPanel
      suggestions={suggestions}
      isLoading={suggestionsLoading}
      onPreview={handleCorrectSuggestion}
      onReject={suggestion => rejectMutation.mutate(suggestion.id)}
    />
  </AnalysisCard>
</div>
```

Keep `ActivityAnalysisHeatmap`, `ActivityHeatmap`, `DailyActivityPanel`, and `KeyboardHeatmap` below this first-screen block.

- [ ] **Step 5: Run focused tests and build**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRecordsReviewLayout.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx src/client-web/src/pages/PcTrackerPage.tsx tests/client-web/pcRecordsReviewLayout.test.tsx
git commit -m "feat: make pc records review first"
```

Expected: commit succeeds.

---

### Task 5: Frontend App Knowledge API And Types

**Files:**
- Create: `src/client-web/src/api/appKnowledge.ts`
- Modify: `src/client-web/src/api/appSignatures.ts`
- Test: `tests/client-web/appKnowledgeApiPath.test.ts`
- Test: `tests/client-web/appKnowledgeTypes.test.ts`

- [ ] **Step 1: Write failing API path test**

Create `tests/client-web/appKnowledgeApiPath.test.ts`:

```ts
import assert from 'node:assert/strict';
import { appKnowledgeApiPaths } from '../../src/client-web/src/api/appKnowledge';

assert.equal(appKnowledgeApiPaths.apps(), '/pc/app-knowledge/apps');
assert.equal(appKnowledgeApiPaths.apps('code'), '/pc/app-knowledge/apps?search=code');
assert.equal(appKnowledgeApiPaths.appContexts('app-1'), '/pc/app-knowledge/apps/app-1/contexts');
assert.equal(appKnowledgeApiPaths.suggestionPreview('suggestion-1'), '/pc/app-knowledge/suggestions/suggestion-1/preview');
assert.equal(appKnowledgeApiPaths.suggestionApply('suggestion-1'), '/pc/app-knowledge/suggestions/suggestion-1/apply');
```

- [ ] **Step 2: Write failing type test**

Create `tests/client-web/appKnowledgeTypes.test.ts`:

```ts
import assert from 'node:assert/strict';
import type {
  AppKnowledgeApp,
  AppKnowledgeContextPattern,
  AppKnowledgeSuggestionPreview,
} from '../../src/client-web/src/api/appKnowledge';

const app: AppKnowledgeApp = {
  id: 'app-1',
  processName: 'msedge.exe',
  displayName: 'Edge',
  categoryPath: '工作 / 开发',
  productivity: 'productive',
  description: null,
  source: 'builtin',
  confidence: 0.9,
  icon: null,
  lastSeenAt: null,
  createdAt: '2026-07-06T00:00:00Z',
  contextCount: 2,
  pendingContextCount: 1,
  recentAffectedDurationSeconds: 3600,
};

const context: AppKnowledgeContextPattern = {
  id: 'context-1',
  appId: 'app-1',
  processName: 'msedge.exe',
  patternType: 'domain',
  patternValue: 'github.com',
  targetCategoryName: '工作 / 开发',
  projectTag: null,
  scopeSummary: 'Edge / github.com',
  source: 'user-confirmed',
  confidence: 1,
  enabled: true,
  affectedRecordCount: 42,
  affectedDurationSeconds: 7200,
  lastMatchedAt: null,
};

const preview: AppKnowledgeSuggestionPreview = {
  suggestionId: 'suggestion-1',
  recommendedContext: context,
  alternatives: [context],
  preview: {
    affectedRecordCount: 42,
    affectedDurationSeconds: 7200,
    currentCategoryCounts: { 其他: 42 },
    newCategoryCounts: { '工作 / 开发': 42 },
    samples: [],
    requiresConfirmation: true,
    summary: '将写入 App 知识库并重算 42 条记录。',
  },
};

assert.equal(app.contextCount, 2);
assert.equal(context.patternType, 'domain');
assert.equal(preview.recommendedContext.scopeSummary, 'Edge / github.com');
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeTypes.test.ts
```

Expected: FAIL because `api/appKnowledge.ts` does not exist.

- [ ] **Step 4: Create App Knowledge API module**

Create `src/client-web/src/api/appKnowledge.ts`:

```ts
import { apiGet, apiPost, apiDelete } from './client';
import type { ApiResponse, ActivityClassificationPreview } from '../types';

export type AppKnowledgePatternType = 'app-default' | 'domain' | 'title' | 'url-path' | 'source-family';

export interface AppKnowledgeApp {
  id: string;
  processName: string;
  displayName: string;
  categoryPath: string | null;
  productivity: string | null;
  description: string | null;
  source: string;
  confidence: number;
  icon: string | null;
  lastSeenAt: string | null;
  createdAt: string;
  contextCount: number;
  pendingContextCount: number;
  recentAffectedDurationSeconds: number;
}

export interface AppKnowledgeContextPattern {
  id: string;
  appId: string;
  processName: string;
  patternType: AppKnowledgePatternType;
  patternValue: string;
  targetCategoryName: string | null;
  projectTag: string | null;
  scopeSummary: string;
  source: string;
  confidence: number;
  enabled: boolean;
  affectedRecordCount: number;
  affectedDurationSeconds: number;
  lastMatchedAt: string | null;
}

export interface SaveAppKnowledgeContextRequest {
  appId?: string | null;
  processName: string;
  patternType: AppKnowledgePatternType;
  patternValue: string;
  targetCategoryName: string | null;
  projectTag?: string | null;
  confidence?: number;
  enabled?: boolean;
}

export interface AppKnowledgeSuggestionPreview {
  suggestionId: string;
  recommendedContext: AppKnowledgeContextPattern;
  alternatives: AppKnowledgeContextPattern[];
  preview: ActivityClassificationPreview;
}

export interface AppKnowledgeSuggestionApply {
  suggestionId: string;
  savedContext: AppKnowledgeContextPattern;
  preview: ActivityClassificationPreview;
  auditId: string;
  suggestionStatus: string;
  message: string;
}

export const appKnowledgeApiPaths = {
  apps: (search?: string) => `/pc/app-knowledge/apps${search ? `?search=${encodeURIComponent(search)}` : ''}`,
  appContexts: (appId: string) => `/pc/app-knowledge/apps/${appId}/contexts`,
  contexts: '/pc/app-knowledge/contexts',
  suggestionPreview: (id: string) => `/pc/app-knowledge/suggestions/${id}/preview`,
  suggestionApply: (id: string) => `/pc/app-knowledge/suggestions/${id}/apply`,
} as const;

export function getAppKnowledgeApps(search?: string) {
  return apiGet<ApiResponse<AppKnowledgeApp[]>>(appKnowledgeApiPaths.apps(search)).then(r => r.data);
}

export function getAppKnowledgeContexts(appId: string) {
  return apiGet<ApiResponse<AppKnowledgeContextPattern[]>>(appKnowledgeApiPaths.appContexts(appId)).then(r => r.data);
}

export function saveAppKnowledgeContext(request: SaveAppKnowledgeContextRequest) {
  return apiPost<ApiResponse<AppKnowledgeContextPattern>>(appKnowledgeApiPaths.contexts, request).then(r => r.data);
}

export function deleteAppKnowledgeContext(id: string) {
  return apiDelete<ApiResponse<string>>(`${appKnowledgeApiPaths.contexts}/${id}`).then(r => r.data);
}

export function previewAppKnowledgeSuggestion(id: string, request: {
  categoryName: string | null;
  projectTag: string | null;
  range: { mode: 'today' | 'range'; dateFrom?: string | null; dateTo?: string | null };
}) {
  return apiPost<ApiResponse<AppKnowledgeSuggestionPreview>>(appKnowledgeApiPaths.suggestionPreview(id), request)
    .then(r => r.data);
}

export function applyAppKnowledgeSuggestion(id: string, request: {
  categoryName: string | null;
  projectTag: string | null;
  range: { mode: 'today' | 'range'; dateFrom?: string | null; dateTo?: string | null };
}) {
  return apiPost<ApiResponse<AppKnowledgeSuggestionApply>>(appKnowledgeApiPaths.suggestionApply(id), request)
    .then(r => r.data);
}
```

- [ ] **Step 5: Keep legacy app signatures API available**

Do not remove `src/client-web/src/api/appSignatures.ts`. It remains a compatibility API for existing code until `AppKnowledgeBasePage` migrates in Task 9.

- [ ] **Step 6: Run tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeTypes.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-web/src/api/appKnowledge.ts tests/client-web/appKnowledgeApiPath.test.ts tests/client-web/appKnowledgeTypes.test.ts
git commit -m "feat: add app knowledge client contracts"
```

Expected: commit succeeds.

---

### Task 6: Backend App Knowledge Context Model

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`
- Modify: `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`
- Test: `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs`

- [ ] **Step 1: Add failing EF model test**

Append to `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs`:

```csharp
[Fact]
public void AppKnowledgeContextEntity_HasExpectedTableAndIndexes()
{
    using var db = CreateDbContext();
    var entity = db.Model.FindEntityType(typeof(AppKnowledgeContextEntity));

    Assert.NotNull(entity);
    Assert.Equal("pc_app_knowledge_contexts", entity!.GetTableName());
    Assert.Contains(entity.GetIndexes(), index =>
        index.GetDatabaseName() == "ix_pc_app_knowledge_contexts_app_pattern" &&
        index.Properties.Select(property => property.Name).SequenceEqual([
            nameof(AppKnowledgeContextEntity.ProcessName),
            nameof(AppKnowledgeContextEntity.PatternType),
            nameof(AppKnowledgeContextEntity.PatternValue)
        ]));
    Assert.Contains(entity.GetIndexes(), index =>
        index.GetDatabaseName() == "ix_pc_app_knowledge_contexts_category");
}
```

Add the missing using if needed:

```csharp
using Pim.Module.PcTracker.Entities;
```

- [ ] **Step 2: Run model test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter AppKnowledgeContextEntity_HasExpectedTableAndIndexes
```

Expected: FAIL because `AppKnowledgeContextEntity` does not exist.

- [ ] **Step 3: Create entity**

Create `src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs`:

```csharp
namespace Pim.Module.PcTracker.Entities;

public class AppKnowledgeContextEntity
{
    public Guid Id { get; set; }
    public Guid? AppSignatureId { get; set; }
    public string ProcessName { get; set; } = string.Empty;
    public string PatternType { get; set; } = string.Empty;
    public string PatternValue { get; set; } = string.Empty;
    public string? TargetCategoryName { get; set; }
    public string? ProjectTag { get; set; }
    public string ScopeSummary { get; set; } = string.Empty;
    public string Source { get; set; } = "user-confirmed";
    public double Confidence { get; set; } = 1.0;
    public bool Enabled { get; set; } = true;
    public int AffectedRecordCount { get; set; }
    public double AffectedDurationSeconds { get; set; }
    public DateTimeOffset? LastMatchedAt { get; set; }
    public Guid? SourceRuleId { get; set; }
    public Guid? SourceSuggestionId { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }

    public AppSignatureEntity? AppSignature { get; set; }
}
```

- [ ] **Step 4: Add DTOs**

Append to `src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs` after `SaveAppSignatureRequest`:

```csharp
public record AppKnowledgeAppDto(
    Guid Id,
    string ProcessName,
    string DisplayName,
    string? CategoryPath,
    string? Productivity,
    string? Description,
    string Source,
    double Confidence,
    string? Icon,
    DateTimeOffset? LastSeenAt,
    DateTimeOffset CreatedAt,
    int ContextCount,
    int PendingContextCount,
    double RecentAffectedDurationSeconds);

public record AppKnowledgeContextDto(
    Guid Id,
    Guid? AppId,
    string ProcessName,
    string PatternType,
    string PatternValue,
    string? TargetCategoryName,
    string? ProjectTag,
    string ScopeSummary,
    string Source,
    double Confidence,
    bool Enabled,
    int AffectedRecordCount,
    double AffectedDurationSeconds,
    DateTimeOffset? LastMatchedAt);

public record SaveAppKnowledgeContextRequest(
    Guid? AppId,
    string ProcessName,
    string PatternType,
    string PatternValue,
    string? TargetCategoryName,
    string? ProjectTag,
    double? Confidence,
    bool? Enabled);

public record AppKnowledgeSuggestionPreviewDto(
    Guid SuggestionId,
    AppKnowledgeContextDto RecommendedContext,
    IReadOnlyList<AppKnowledgeContextDto> Alternatives,
    ActivityClassificationPreviewDto Preview);

public record AppKnowledgeSuggestionApplyDto(
    Guid SuggestionId,
    AppKnowledgeContextDto SavedContext,
    ActivityClassificationPreviewDto Preview,
    Guid AuditId,
    string SuggestionStatus,
    string Message);
```

- [ ] **Step 5: Configure entity**

Append to `src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`:

```csharp
public class AppKnowledgeContextEntityConfiguration : IEntityTypeConfiguration<AppKnowledgeContextEntity>
{
    public void Configure(EntityTypeBuilder<AppKnowledgeContextEntity> builder)
    {
        builder.ToTable("pc_app_knowledge_contexts");
        builder.HasKey(item => item.Id);
        builder.Property(item => item.ProcessName).HasMaxLength(256).IsRequired();
        builder.Property(item => item.PatternType).HasMaxLength(64).IsRequired();
        builder.Property(item => item.PatternValue).HasMaxLength(512).IsRequired();
        builder.Property(item => item.TargetCategoryName).HasMaxLength(256);
        builder.Property(item => item.ProjectTag).HasMaxLength(256);
        builder.Property(item => item.ScopeSummary).HasMaxLength(512).IsRequired();
        builder.Property(item => item.Source).HasMaxLength(64).IsRequired();
        builder.HasIndex(item => new { item.ProcessName, item.PatternType, item.PatternValue })
            .HasDatabaseName("ix_pc_app_knowledge_contexts_app_pattern");
        builder.HasIndex(item => item.TargetCategoryName)
            .HasDatabaseName("ix_pc_app_knowledge_contexts_category");
        builder.HasIndex(item => item.SourceSuggestionId)
            .HasDatabaseName("ix_pc_app_knowledge_contexts_source_suggestion");
        builder.HasOne(item => item.AppSignature)
            .WithMany()
            .HasForeignKey(item => item.AppSignatureId)
            .OnDelete(DeleteBehavior.SetNull);
    }
}
```

If the namespace already has `Microsoft.EntityFrameworkCore` and `Microsoft.EntityFrameworkCore.Metadata.Builders`, reuse existing imports.

- [ ] **Step 6: Add schema initializer SQL**

In `src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`, add SQL with the other PC tracker table creation statements:

```sql
CREATE TABLE IF NOT EXISTS pc_app_knowledge_contexts (
    id uuid PRIMARY KEY,
    app_signature_id uuid NULL REFERENCES pc_app_signatures(id) ON DELETE SET NULL,
    process_name varchar(256) NOT NULL,
    pattern_type varchar(64) NOT NULL,
    pattern_value varchar(512) NOT NULL,
    target_category_name varchar(256) NULL,
    project_tag varchar(256) NULL,
    scope_summary varchar(512) NOT NULL,
    source varchar(64) NOT NULL,
    confidence double precision NOT NULL,
    enabled boolean NOT NULL,
    affected_record_count integer NOT NULL DEFAULT 0,
    affected_duration_seconds double precision NOT NULL DEFAULT 0,
    last_matched_at timestamptz NULL,
    source_rule_id uuid NULL,
    source_suggestion_id uuid NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_pc_app_knowledge_contexts_app_pattern
    ON pc_app_knowledge_contexts(process_name, pattern_type, pattern_value);
CREATE INDEX IF NOT EXISTS ix_pc_app_knowledge_contexts_category
    ON pc_app_knowledge_contexts(target_category_name);
CREATE INDEX IF NOT EXISTS ix_pc_app_knowledge_contexts_source_suggestion
    ON pc_app_knowledge_contexts(source_suggestion_id);
```

- [ ] **Step 7: Generate EF migration**

Run:

```powershell
dotnet ef migrations add AddPcAppKnowledgeContexts --project src/Pim.Infrastructure/Pim.Infrastructure.csproj --startup-project src/Pim.Api/Pim.Api.csproj --context PimDbContext
```

Expected: migration is generated under `src/Pim.Infrastructure/Data/Migrations/`.

- [ ] **Step 8: Run model test**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter AppKnowledgeContextEntity_HasExpectedTableAndIndexes
```

Expected: PASS.

- [ ] **Step 9: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs src/Pim.Infrastructure/Data/Migrations tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs
git commit -m "feat: add app knowledge context model"
```

Expected: commit succeeds.

---

### Task 7: Backend App Knowledge Context Service And Endpoints

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Test: `tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs`

- [ ] **Step 1: Write failing service tests**

Create `tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class AppKnowledgeContextServiceTests
{
    [Fact]
    public async Task SaveAsync_CreatesDomainContextWithScopeSummary()
    {
        using var db = CreateDb();
        var app = new AppSignatureEntity
        {
            Id = Guid.NewGuid(),
            ProcessName = "msedge.exe",
            DisplayName = "Edge",
            Source = "manual",
            Confidence = 1,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };
        db.Set<AppSignatureEntity>().Add(app);
        await db.SaveChangesAsync();

        var service = new AppKnowledgeContextService(db);
        var result = await service.SaveAsync(new SaveAppKnowledgeContextRequest(
            app.Id,
            "msedge.exe",
            "domain",
            "github.com",
            "工作 / 开发",
            "PIM",
            1,
            true), CancellationToken.None);

        Assert.Equal("msedge.exe", result.ProcessName);
        Assert.Equal("domain", result.PatternType);
        Assert.Equal("github.com", result.PatternValue);
        Assert.Equal("Edge / github.com", result.ScopeSummary);
        Assert.Equal("工作 / 开发", result.TargetCategoryName);
        Assert.Equal("PIM", result.ProjectTag);
    }

    [Fact]
    public async Task GetByAppAsync_ReturnsOnlyAppContexts()
    {
        using var db = CreateDb();
        var app = new AppSignatureEntity
        {
            Id = Guid.NewGuid(),
            ProcessName = "code.exe",
            DisplayName = "Code",
            Source = "manual",
            Confidence = 1,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };
        db.Set<AppSignatureEntity>().Add(app);
        db.Set<AppKnowledgeContextEntity>().AddRange(
            NewContext(app.Id, "code.exe", "title", "PIM"),
            NewContext(null, "msedge.exe", "domain", "github.com"));
        await db.SaveChangesAsync();

        var service = new AppKnowledgeContextService(db);
        var result = await service.GetByAppAsync(app.Id, CancellationToken.None);

        var item = Assert.Single(result);
        Assert.Equal("code.exe", item.ProcessName);
        Assert.Equal("PIM", item.PatternValue);
    }

    private static AppKnowledgeContextEntity NewContext(Guid? appId, string processName, string type, string value) =>
        new()
        {
            Id = Guid.NewGuid(),
            AppSignatureId = appId,
            ProcessName = processName,
            PatternType = type,
            PatternValue = value,
            ScopeSummary = $"{processName} / {value}",
            Source = "user-confirmed",
            Confidence = 1,
            Enabled = true,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        };

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter AppKnowledgeContextServiceTests
```

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement context service**

Create `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public class AppKnowledgeContextService
{
    private readonly PimDbContext _db;

    public AppKnowledgeContextService(PimDbContext db)
    {
        _db = db;
    }

    public async Task<List<AppKnowledgeContextDto>> GetByAppAsync(Guid appId, CancellationToken ct)
    {
        return await _db.Set<AppKnowledgeContextEntity>()
            .Where(item => item.AppSignatureId == appId)
            .OrderBy(item => item.PatternType)
            .ThenBy(item => item.PatternValue)
            .Select(item => ToDto(item))
            .ToListAsync(ct);
    }

    public async Task<AppKnowledgeContextDto> SaveAsync(SaveAppKnowledgeContextRequest request, CancellationToken ct)
    {
        if (string.IsNullOrWhiteSpace(request.ProcessName))
            throw new ArgumentException("ProcessName is required.");
        if (string.IsNullOrWhiteSpace(request.PatternType))
            throw new ArgumentException("PatternType is required.");
        if (string.IsNullOrWhiteSpace(request.PatternValue))
            throw new ArgumentException("PatternValue is required.");

        var processName = request.ProcessName.Trim();
        var patternType = request.PatternType.Trim();
        var patternValue = request.PatternValue.Trim();

        var existing = await _db.Set<AppKnowledgeContextEntity>()
            .FirstOrDefaultAsync(item =>
                item.ProcessName == processName &&
                item.PatternType == patternType &&
                item.PatternValue == patternValue, ct);

        var now = DateTimeOffset.UtcNow;
        if (existing is null)
        {
            existing = new AppKnowledgeContextEntity
            {
                Id = Guid.NewGuid(),
                CreatedAt = now
            };
            _db.Set<AppKnowledgeContextEntity>().Add(existing);
        }

        existing.AppSignatureId = request.AppId;
        existing.ProcessName = processName;
        existing.PatternType = patternType;
        existing.PatternValue = patternValue;
        existing.TargetCategoryName = string.IsNullOrWhiteSpace(request.TargetCategoryName) ? null : request.TargetCategoryName.Trim();
        existing.ProjectTag = string.IsNullOrWhiteSpace(request.ProjectTag) ? null : request.ProjectTag.Trim();
        existing.ScopeSummary = await BuildScopeSummaryAsync(request.AppId, processName, patternValue, ct);
        existing.Source = "user-confirmed";
        existing.Confidence = request.Confidence ?? 1.0;
        existing.Enabled = request.Enabled ?? true;
        existing.UpdatedAt = now;

        await _db.SaveChangesAsync(ct);
        return ToDto(existing);
    }

    public async Task<bool> DeleteAsync(Guid id, CancellationToken ct)
    {
        var entity = await _db.Set<AppKnowledgeContextEntity>().FindAsync(new object[] { id }, ct);
        if (entity is null)
            return false;

        _db.Set<AppKnowledgeContextEntity>().Remove(entity);
        await _db.SaveChangesAsync(ct);
        return true;
    }

    internal static AppKnowledgeContextDto ToDto(AppKnowledgeContextEntity item) =>
        new(
            item.Id,
            item.AppSignatureId,
            item.ProcessName,
            item.PatternType,
            item.PatternValue,
            item.TargetCategoryName,
            item.ProjectTag,
            item.ScopeSummary,
            item.Source,
            item.Confidence,
            item.Enabled,
            item.AffectedRecordCount,
            item.AffectedDurationSeconds,
            item.LastMatchedAt);

    private async Task<string> BuildScopeSummaryAsync(Guid? appId, string processName, string patternValue, CancellationToken ct)
    {
        if (appId is Guid id)
        {
            var displayName = await _db.Set<AppSignatureEntity>()
                .Where(item => item.Id == id)
                .Select(item => item.DisplayName)
                .FirstOrDefaultAsync(ct);
            if (!string.IsNullOrWhiteSpace(displayName))
                return $"{displayName} / {patternValue}";
        }

        return $"{processName} / {patternValue}";
    }
}
```

- [ ] **Step 4: Register service and endpoints**

In `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`, register:

```csharp
services.AddScoped<AppKnowledgeContextService>();
```

In `src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs`, add an App Knowledge list method:

```csharp
public async Task<List<AppKnowledgeAppDto>> GetKnowledgeAppsAsync(string? search, CancellationToken ct)
{
    var apps = await GetAllAsync(search, ct);
    var appIds = apps.Select(app => app.Id).ToList();
    var contextStats = await _db.Set<AppKnowledgeContextEntity>()
        .Where(context => context.AppSignatureId.HasValue && appIds.Contains(context.AppSignatureId.Value))
        .GroupBy(context => context.AppSignatureId!.Value)
        .Select(group => new
        {
            AppId = group.Key,
            ContextCount = group.Count(),
            RecentAffectedDurationSeconds = group.Sum(item => item.AffectedDurationSeconds)
        })
        .ToDictionaryAsync(item => item.AppId, ct);

    return apps.Select(app =>
    {
        contextStats.TryGetValue(app.Id, out var stats);
        return new AppKnowledgeAppDto(
            app.Id,
            app.ProcessName,
            app.DisplayName,
            app.CategoryPath,
            app.Productivity,
            app.Description,
            app.Source,
            app.Confidence,
            app.Icon,
            app.LastSeenAt,
            app.CreatedAt,
            stats?.ContextCount ?? 0,
            pendingContextCount: 0,
            stats?.RecentAffectedDurationSeconds ?? 0);
    }).ToList();
}
```

Map endpoints near App Knowledge Base endpoints:

```csharp
var appKnowledgeRead = endpoints.MapGroup("/api/v1/pc/app-knowledge").AllowAnonymous();
var appKnowledgeWrite = endpoints.MapGroup("/api/v1/pc/app-knowledge").RequireAuthorization();

appKnowledgeRead.MapGet("/apps", async (
    [FromQuery] string? search,
    [FromServices] AppSignatureService svc,
    CancellationToken ct) =>
{
    var apps = await svc.GetKnowledgeAppsAsync(search, ct);
    return Results.Ok(ApiResponse<List<AppKnowledgeAppDto>>.Ok(apps));
});

appKnowledgeRead.MapGet("/apps/{appId:guid}/contexts", async (
    Guid appId,
    [FromServices] AppKnowledgeContextService svc,
    CancellationToken ct) =>
{
    var contexts = await svc.GetByAppAsync(appId, ct);
    return Results.Ok(ApiResponse<List<AppKnowledgeContextDto>>.Ok(contexts));
});

appKnowledgeWrite.MapPost("/contexts", async (
    [FromBody] SaveAppKnowledgeContextRequest req,
    [FromServices] AppKnowledgeContextService svc,
    CancellationToken ct) =>
{
    try
    {
        var result = await svc.SaveAsync(req, ct);
        return Results.Ok(ApiResponse<AppKnowledgeContextDto>.Ok(result));
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(ApiResponse<string>.Error(400, ex.Message));
    }
});

appKnowledgeWrite.MapDelete("/contexts/{id:guid}", async (
    Guid id,
    [FromServices] AppKnowledgeContextService svc,
    CancellationToken ct) =>
{
    var ok = await svc.DeleteAsync(id, ct);
    return ok
        ? Results.Ok(ApiResponse<string>.Ok("已删除"))
        : Results.NotFound(ApiResponse<string>.Error(404, "未找到"));
});
```

- [ ] **Step 5: Run service tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter AppKnowledgeContextServiceTests
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs
git commit -m "feat: add app knowledge context service"
```

Expected: commit succeeds.

---

### Task 8: App Knowledge Suggestion Preview And Apply Wrappers

**Files:**
- Create: `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs`
- Modify: `src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`
- Modify: `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs`
- Test: `tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs`

- [ ] **Step 1: Write failing wrapper test**

Create `tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs`:

```csharp
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class AppKnowledgeSuggestionServiceTests
{
    [Fact]
    public async Task BuildRecommendedContextAsync_PrefersDomainWhenSanitizedContextHasDomain()
    {
        using var db = CreateDb();
        var suggestionId = Guid.NewGuid();
        db.Set<ActivityClassificationSuggestionEntity>().Add(new ActivityClassificationSuggestionEntity
        {
            Id = suggestionId,
            ClusterKey = "msedge.exe|github.com",
            SampleCount = 42,
            TotalDurationSeconds = 7200,
            SampleRecordsJson = "[]",
            SanitizedContextJson = """{"appName":"msedge.exe","domain":"github.com"}""",
            CurrentCategory = "其他",
            SuggestedCategory = "工作 / 开发",
            Status = "pending",
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        });
        db.Set<AppSignatureEntity>().Add(new AppSignatureEntity
        {
            Id = Guid.NewGuid(),
            ProcessName = "msedge.exe",
            DisplayName = "Edge",
            Source = "builtin",
            Confidence = 1,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow
        });
        await db.SaveChangesAsync();

        var service = new AppKnowledgeSuggestionService(
            db,
            new AppKnowledgeContextService(db),
            NullLogger<AppKnowledgeSuggestionService>.Instance);

        var result = await service.BuildRecommendedContextAsync(
            suggestionId,
            new SuggestionClassificationPreviewRequest(
                "工作 / 开发",
                null,
                new ActivityClassificationApplyRangeRequest("today", "2026-07-06", "2026-07-06")),
            preview: null,
            CancellationToken.None);

        Assert.Equal(suggestionId, result.SuggestionId);
        Assert.Equal("domain", result.RecommendedContext.PatternType);
        Assert.Equal("github.com", result.RecommendedContext.PatternValue);
        Assert.Equal("工作 / 开发", result.RecommendedContext.TargetCategoryName);
        Assert.Contains(result.Alternatives, item => item.PatternType == "title");
    }

    private static PimDbContext CreateDb()
    {
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new PimDbContext(options);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter AppKnowledgeSuggestionServiceTests
```

Expected: FAIL because `AppKnowledgeSuggestionService` does not exist.

- [ ] **Step 3: Implement suggestion wrapper service**

Create `src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs`:

```csharp
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;

namespace Pim.Module.PcTracker.Services;

public class AppKnowledgeSuggestionService
{
    private readonly PimDbContext _db;
    private readonly AppKnowledgeContextService _contexts;
    private readonly ILogger<AppKnowledgeSuggestionService> _logger;

    public AppKnowledgeSuggestionService(
        PimDbContext db,
        AppKnowledgeContextService contexts,
        ILogger<AppKnowledgeSuggestionService> logger)
    {
        _db = db;
        _contexts = contexts;
        _logger = logger;
    }

    public async Task<AppKnowledgeSuggestionPreviewDto> BuildRecommendedContextAsync(
        Guid suggestionId,
        SuggestionClassificationPreviewRequest request,
        ActivityClassificationPreviewDto? preview,
        CancellationToken ct)
    {
        var suggestion = await _db.Set<ActivityClassificationSuggestionEntity>()
            .FirstOrDefaultAsync(item => item.Id == suggestionId, ct)
            ?? throw new KeyNotFoundException($"Suggestion '{suggestionId}' was not found.");

        var context = ParseContext(suggestion.SanitizedContextJson);
        var processName = context.TryGetValue("appName", out var appName) && !string.IsNullOrWhiteSpace(appName)
            ? appName
            : suggestion.ClusterKey.Split('|', StringSplitOptions.RemoveEmptyEntries).FirstOrDefault() ?? suggestion.ClusterKey;
        var domain = context.GetValueOrDefault("domain");
        var title = context.GetValueOrDefault("title");
        var app = await _db.Set<AppSignatureEntity>()
            .FirstOrDefaultAsync(item => item.ProcessName.ToLower() == processName.ToLower(), ct);

        var recommended = BuildContext(
            suggestion,
            app,
            processName,
            string.IsNullOrWhiteSpace(domain) ? "title" : "domain",
            string.IsNullOrWhiteSpace(domain) ? title ?? suggestion.ClusterKey : domain,
            request,
            preview);

        var alternatives = new List<AppKnowledgeContextDto> { recommended };
        if (!string.IsNullOrWhiteSpace(title))
        {
            alternatives.Add(BuildContext(suggestion, app, processName, "title", title, request, preview));
        }
        if (!string.IsNullOrWhiteSpace(domain) && !string.Equals(domain, processName, StringComparison.OrdinalIgnoreCase))
        {
            alternatives.Add(BuildContext(suggestion, app, processName, "app-default", processName, request, preview));
        }

        return new AppKnowledgeSuggestionPreviewDto(
            suggestionId,
            recommended,
            alternatives
                .GroupBy(item => $"{item.PatternType}:{item.PatternValue}", StringComparer.OrdinalIgnoreCase)
                .Select(group => group.First())
                .ToList(),
            preview ?? EmptyPreview());
    }

    public async Task<AppKnowledgeContextDto> SaveRecommendedContextAsync(
        AppKnowledgeSuggestionPreviewDto suggestionPreview,
        CancellationToken ct)
    {
        var context = suggestionPreview.RecommendedContext;
        var saved = await _contexts.SaveAsync(new SaveAppKnowledgeContextRequest(
            context.AppId,
            context.ProcessName,
            context.PatternType,
            context.PatternValue,
            context.TargetCategoryName,
            context.ProjectTag,
            context.Confidence,
            context.Enabled), ct);

        var entity = await _db.Set<AppKnowledgeContextEntity>()
            .FirstAsync(item => item.Id == saved.Id, ct);
        entity.SourceSuggestionId = suggestionPreview.SuggestionId;
        entity.AffectedRecordCount = suggestionPreview.Preview.AffectedRecordCount;
        entity.AffectedDurationSeconds = suggestionPreview.Preview.AffectedDurationSeconds;
        entity.UpdatedAt = DateTimeOffset.UtcNow;
        await _db.SaveChangesAsync(ct);

        return AppKnowledgeContextService.ToDto(entity);
    }

    private static AppKnowledgeContextDto BuildContext(
        ActivityClassificationSuggestionEntity suggestion,
        AppSignatureEntity? app,
        string processName,
        string patternType,
        string? patternValue,
        SuggestionClassificationPreviewRequest request,
        ActivityClassificationPreviewDto? preview)
    {
        var value = string.IsNullOrWhiteSpace(patternValue) ? suggestion.ClusterKey : patternValue.Trim();
        var appLabel = app?.DisplayName ?? processName;
        return new AppKnowledgeContextDto(
            Guid.Empty,
            app?.Id,
            processName,
            patternType,
            value,
            request.CategoryName ?? suggestion.SuggestedCategory,
            request.ProjectTag ?? suggestion.SuggestedProjectTag,
            $"{appLabel} / {value}",
            "system-suggested",
            0.8,
            true,
            preview?.AffectedRecordCount ?? suggestion.SampleCount,
            preview?.AffectedDurationSeconds ?? suggestion.TotalDurationSeconds,
            null);
    }

    private static Dictionary<string, string> ParseContext(string json)
    {
        try
        {
            return JsonSerializer.Deserialize<Dictionary<string, string>>(json) ?? [];
        }
        catch (JsonException)
        {
            return [];
        }
    }

    private static ActivityClassificationPreviewDto EmptyPreview() =>
        new(0, 0, new Dictionary<string, int>(), new Dictionary<string, int>(), [], false, "尚未运行影响预览。");
}
```

- [ ] **Step 4: Make context DTO conversion public inside assembly**

In `AppKnowledgeContextService`, ensure `ToDto` is callable from `AppKnowledgeSuggestionService`:

```csharp
internal static AppKnowledgeContextDto ToDto(AppKnowledgeContextEntity item) => ...
```

This was already specified in Task 7. If it was made private, change it to `internal`.

- [ ] **Step 5: Register service and map wrapper endpoints**

In `PcTrackerModule.RegisterServices`:

```csharp
services.AddScoped<AppKnowledgeSuggestionService>();
```

In endpoint mapping:

```csharp
appKnowledgeWrite.MapPost("/suggestions/{id:guid}/preview", async (
    Guid id,
    [FromBody] SuggestionClassificationPreviewRequest req,
    [FromServices] ActivityClassificationRecomputeService recompute,
    [FromServices] ClassificationRuleDraftService drafts,
    [FromServices] AppKnowledgeSuggestionService appKnowledge,
    CancellationToken ct) =>
{
    try
    {
        var classificationPreview = await recompute.PreviewSuggestionAsync(id, req, drafts, ct);
        var result = await appKnowledge.BuildRecommendedContextAsync(id, req, classificationPreview.Preview, ct);
        return Results.Ok(ApiResponse<AppKnowledgeSuggestionPreviewDto>.Ok(result));
    }
    catch (KeyNotFoundException ex)
    {
        return Results.NotFound(ApiResponse<string>.Error(404, ex.Message));
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(ApiResponse<string>.Error(400, ex.Message));
    }
});

appKnowledgeWrite.MapPost("/suggestions/{id:guid}/apply", async (
    Guid id,
    [FromBody] SuggestionClassificationApplyRequest req,
    [FromServices] ActivityClassificationRecomputeService recompute,
    [FromServices] ClassificationRuleDraftService drafts,
    [FromServices] AppKnowledgeSuggestionService appKnowledge,
    CancellationToken ct) =>
{
    try
    {
        var previewRequest = new SuggestionClassificationPreviewRequest(req.CategoryName, req.ProjectTag, req.Range);
        var classificationPreview = await recompute.PreviewSuggestionAsync(id, previewRequest, drafts, ct);
        var knowledgePreview = await appKnowledge.BuildRecommendedContextAsync(id, previewRequest, classificationPreview.Preview, ct);
        var savedContext = await appKnowledge.SaveRecommendedContextAsync(knowledgePreview, ct);
        var applied = await recompute.ApplySuggestionAsync(id, req, drafts, ct);
        var result = new AppKnowledgeSuggestionApplyDto(
            id,
            savedContext,
            applied.Preview,
            applied.AuditId,
            applied.SuggestionStatus,
            $"已写入 App 知识库：{savedContext.ScopeSummary} -> {savedContext.TargetCategoryName ?? "未分类"}。已重算 {applied.Preview.AffectedRecordCount} 条记录。");
        return Results.Ok(ApiResponse<AppKnowledgeSuggestionApplyDto>.Ok(result));
    }
    catch (KeyNotFoundException ex)
    {
        return Results.NotFound(ApiResponse<string>.Error(404, ex.Message));
    }
    catch (ArgumentException ex)
    {
        return Results.BadRequest(ApiResponse<string>.Error(400, ex.Message));
    }
    catch (InvalidOperationException ex)
    {
        return Results.Conflict(ApiResponse<string>.Error(409, ex.Message));
    }
});
```

- [ ] **Step 6: Run wrapper tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter AppKnowledgeSuggestionServiceTests
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs src/modules/Pim.Module.PcTracker/PcTrackerModule.cs tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs
git commit -m "feat: wrap classification suggestions as app knowledge"
```

Expected: commit succeeds.

---

### Task 9: App Knowledge UI Context Patterns

**Files:**
- Create: `src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx`
- Create: `src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx`
- Modify: `src/client-web/src/pages/AppKnowledgeBasePage.tsx`
- Test: `tests/client-web/appKnowledgeComponents.test.tsx`

- [ ] **Step 1: Append failing component test**

Append to `tests/client-web/appKnowledgeComponents.test.tsx`:

```tsx
import AppKnowledgeContextList from '../../src/client-web/src/components/app-knowledge/AppKnowledgeContextList';
import type { AppKnowledgeContextPattern } from '../../src/client-web/src/api/appKnowledge';

const context: AppKnowledgeContextPattern = {
  id: 'context-1',
  appId: 'app-1',
  processName: 'msedge.exe',
  patternType: 'domain',
  patternValue: 'github.com',
  targetCategoryName: '工作 / 开发',
  projectTag: 'PIM',
  scopeSummary: 'Edge / github.com',
  source: 'user-confirmed',
  confidence: 1,
  enabled: true,
  affectedRecordCount: 42,
  affectedDurationSeconds: 7200,
  lastMatchedAt: null,
};

test('app knowledge context list renders domain and title knowledge', () => {
  const html = renderToStaticMarkup(
    <AppKnowledgeContextList contexts={[context]} isLoading={false} onDelete={() => undefined} />
  );

  assert.equal(html.includes('上下文知识'), true);
  assert.equal(html.includes('github.com'), true);
  assert.equal(html.includes('工作 / 开发'), true);
  assert.equal(html.includes('PIM'), true);
});
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeComponents.test.tsx
```

Expected: FAIL because `AppKnowledgeContextList` does not exist.

- [ ] **Step 3: Create impact summary**

Create `src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx`:

```tsx
interface Props {
  affectedRecordCount: number;
  affectedDurationSeconds: number;
  pendingContextCount?: number;
}

function minutes(seconds: number) {
  return Math.round(seconds / 60).toLocaleString('zh-CN');
}

export default function AppKnowledgeImpactSummary({
  affectedRecordCount,
  affectedDurationSeconds,
  pendingContextCount,
}: Props) {
  return (
    <div className="flex flex-wrap gap-2 text-xs text-slate-500">
      <span>{affectedRecordCount.toLocaleString('zh-CN')} 条记录</span>
      <span>{minutes(affectedDurationSeconds)} 分钟</span>
      {pendingContextCount !== undefined && <span>{pendingContextCount.toLocaleString('zh-CN')} 个待确认上下文</span>}
    </div>
  );
}
```

- [ ] **Step 4: Create context list**

Create `src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx`:

```tsx
import type { AppKnowledgeContextPattern } from '../../api/appKnowledge';
import AppKnowledgeImpactSummary from './AppKnowledgeImpactSummary';

interface Props {
  contexts: AppKnowledgeContextPattern[];
  isLoading: boolean;
  onDelete: (id: string) => void;
}

const patternLabels: Record<string, string> = {
  'app-default': '默认分类',
  domain: '域名',
  title: '标题模式',
  'url-path': '路径',
  'source-family': '来源族',
};

export default function AppKnowledgeContextList({ contexts, isLoading, onDelete }: Props) {
  if (isLoading) {
    return <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">正在加载上下文知识...</div>;
  }

  if (contexts.length === 0) {
    return <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">暂无上下文知识。PC 记录确认后会自动沉淀到这里。</div>;
  }

  return (
    <section className="space-y-3">
      <div>
        <h2 className="text-sm font-semibold text-slate-950">上下文知识</h2>
        <p className="mt-1 text-xs text-slate-500">域名、标题模式和项目标签决定同一 App 内的不同分类。</p>
      </div>
      {contexts.map(context => (
        <article key={context.id} className="rounded-lg border border-slate-200 bg-white p-3">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                  {patternLabels[context.patternType] ?? context.patternType}
                </span>
                <h3 className="break-words text-sm font-semibold text-slate-950">{context.patternValue}</h3>
              </div>
              <p className="mt-1 text-xs text-slate-600">
                {context.targetCategoryName || '未分类'}
                {context.projectTag ? ` · 项目 ${context.projectTag}` : ''}
              </p>
              <div className="mt-2">
                <AppKnowledgeImpactSummary
                  affectedRecordCount={context.affectedRecordCount}
                  affectedDurationSeconds={context.affectedDurationSeconds}
                />
              </div>
            </div>
            <button type="button" onClick={() => onDelete(context.id)} className="pim-button-secondary min-h-8 px-3 text-xs">
              删除
            </button>
          </div>
        </article>
      ))}
    </section>
  );
}
```

- [ ] **Step 5: Integrate selected app contexts**

In `AppKnowledgeBasePage.tsx`:

- Replace `getAppSignatures` with `getAppKnowledgeApps`.
- Add selected app state:

```tsx
const [selectedAppId, setSelectedAppId] = useState<string | null>(null);
```

- Query contexts:

```tsx
const { data: contexts = [], isLoading: contextsLoading } = useQuery({
  queryKey: ['app-knowledge-contexts', selectedAppId],
  queryFn: () => selectedAppId ? getAppKnowledgeContexts(selectedAppId) : Promise.resolve([]),
  enabled: selectedAppId !== null,
});
```

- Add delete mutation:

```tsx
const deleteContextMut = useMutation({
  mutationFn: deleteAppKnowledgeContext,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['app-knowledge-contexts'] });
    queryClient.invalidateQueries({ queryKey: ['app-knowledge-apps'] });
  },
});
```

- Render the table and context panel in a responsive grid:

```tsx
<div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.25fr)_minmax(360px,0.75fr)]">
  <div className="overflow-x-auto rounded-lg border border-slate-200">
    {/* existing table, with row onClick={() => setSelectedAppId(sig.id)} */}
  </div>
  <AppKnowledgeContextList
    contexts={contexts}
    isLoading={contextsLoading}
    onDelete={id => deleteContextMut.mutate(id)}
  />
</div>
```

- [ ] **Step 6: Run component test and build**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeComponents.test.tsx
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-web/src/components/app-knowledge/AppKnowledgeContextList.tsx src/client-web/src/components/app-knowledge/AppKnowledgeImpactSummary.tsx src/client-web/src/pages/AppKnowledgeBasePage.tsx tests/client-web/appKnowledgeComponents.test.tsx
git commit -m "feat: show app knowledge context patterns"
```

Expected: commit succeeds.

---

### Task 10: App Knowledge Suggestion Dialog Copy And Refresh

**Files:**
- Modify: `src/client-web/src/pages/PcTrackerPage.tsx`
- Modify: `src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx`
- Modify: `src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx`
- Test: `tests/client-web/pcRoute3Components.test.tsx`

- [ ] **Step 1: Add failing dialog copy test**

Append to `tests/client-web/pcRoute3Components.test.tsx`:

```tsx
test('classification preview dialog copy frames apply as app knowledge writeback', () => {
  const html = renderToStaticMarkup(
    <ClassificationPreviewDialog
      suggestion={suggestion}
      date="2026-07-06"
      preview={preview}
      isPreviewing={false}
      isApplying={false}
      errorMessage={null}
      categories={categories}
      onClose={() => undefined}
      onPreview={() => undefined}
      onApply={() => undefined}
    />
  );

  assert.equal(html.includes('写入 App 知识库'), true);
  assert.equal(html.includes('创建规则'), false);
});
```

Use the existing `suggestion`, `preview`, and `categories` fixtures in that test file. If names differ, adapt the fixture names without changing the assertion intent.

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: FAIL because dialog copy still says classification preview/apply.

- [ ] **Step 3: Change preview dialog labels**

In `ClassificationPreviewDialog.tsx`, update visible strings:

```tsx
<h2 id={titleId} className="text-base font-semibold text-slate-950">
  写入 App 知识库预览
</h2>
```

Replace the category label with:

```tsx
<span className="mb-1 block text-xs font-medium text-slate-500">目标分类</span>
```

Replace the project label with:

```tsx
<span className="mb-1 block text-xs font-medium text-slate-500">项目标签</span>
```

Replace footer button labels:

```tsx
{isPreviewing ? '预览中' : '预览影响'}
{isApplying ? '写入中' : '写入 App 知识库'}
```

- [ ] **Step 4: Change impact panel copy**

In `RuleImpactPreviewPanel.tsx`, change the header to:

```tsx
<h3 className="text-sm font-semibold text-slate-950">知识写入影响</h3>
```

Ensure summary text still displays `preview.summary`.

- [ ] **Step 5: Use App Knowledge suggestion API in PC Records**

In `PcTrackerPage.tsx`, replace imports:

```tsx
import {
  applyAppKnowledgeSuggestion,
  previewAppKnowledgeSuggestion,
} from '../api/appKnowledge';
```

Use these functions in `previewMutation` and `applyMutation` instead of the classification suggestion API functions. Keep the request object shape the same.

On apply success, invalidate:

```tsx
queryClient.invalidateQueries({ queryKey: ['pc-summary'] });
queryClient.invalidateQueries({ queryKey: ['pc-activity-analysis'] });
queryClient.invalidateQueries({ queryKey: ['pc-classification-suggestions'] });
queryClient.invalidateQueries({ queryKey: ['pc-recent-project-tags'] });
queryClient.invalidateQueries({ queryKey: ['productivity-dashboard'] });
queryClient.invalidateQueries({ queryKey: ['app-knowledge-apps'] });
queryClient.invalidateQueries({ queryKey: ['app-knowledge-contexts'] });
```

Because the App Knowledge endpoint returns a preview-focused object rather than a rule-focused object, modify the dialog prop type to accept a preview-only object:

```tsx
type PreviewLike = { preview: ActivityClassificationPreview };
```

Then change `PcTrackerPage` preview state to:

```tsx
const [preview, setPreview] = useState<PreviewLike | null>(null);
```

Keep all internal dialog references using `preview.preview`. Do not remove stale-preview confirmation logic.

- [ ] **Step 6: Run tests and build**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add src/client-web/src/pages/PcTrackerPage.tsx src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx tests/client-web/pcRoute3Components.test.tsx
git commit -m "feat: frame pc suggestions as app knowledge"
```

Expected: commit succeeds.

---

### Task 11: Frontend Integration Tests

**Files:**
- Modify: `tests/client-web/appKnowledgeNavigation.test.tsx`
- Modify: `tests/client-web/appKnowledgeApiPath.test.ts`
- Modify: `tests/client-web/appKnowledgeTypes.test.ts`
- Modify: `tests/client-web/appKnowledgeComponents.test.tsx`
- Modify: `tests/client-web/pcRecordsReviewLayout.test.tsx`
- Modify: `tests/client-web/pcRoute3Components.test.tsx`

- [ ] **Step 1: Run all focused frontend tests**

Run:

```powershell
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeNavigation.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeTypes.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeComponents.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/pcRecordsReviewLayout.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: PASS.

- [ ] **Step 2: Run full frontend build**

Run:

```powershell
npm --prefix src/client-web run build
```

Expected: PASS.

- [ ] **Step 3: Commit any test-only repairs**

If the previous tasks left test fixture adjustments unstaged, commit them:

```powershell
git add tests/client-web/appKnowledgeNavigation.test.tsx tests/client-web/appKnowledgeApiPath.test.ts tests/client-web/appKnowledgeTypes.test.ts tests/client-web/appKnowledgeComponents.test.tsx tests/client-web/pcRecordsReviewLayout.test.tsx tests/client-web/pcRoute3Components.test.tsx
git commit -m "test: cover app knowledge pc records flow"
```

Expected: commit succeeds if there are staged changes. If there are no staged changes, skip this commit.

---

### Task 12: Backend Integration Tests

**Files:**
- Modify: `tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs`
- Modify: `tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs`

- [ ] **Step 1: Add apply persistence test**

Append to `tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs`:

```csharp
[Fact]
public async Task SaveRecommendedContextAsync_PersistsSuggestionImpact()
{
    using var db = CreateDb();
    var app = new AppSignatureEntity
    {
        Id = Guid.NewGuid(),
        ProcessName = "msedge.exe",
        DisplayName = "Edge",
        Source = "builtin",
        Confidence = 1,
        CreatedAt = DateTimeOffset.UtcNow,
        UpdatedAt = DateTimeOffset.UtcNow
    };
    db.Set<AppSignatureEntity>().Add(app);
    await db.SaveChangesAsync();

    var suggestionId = Guid.NewGuid();
    var context = new AppKnowledgeContextDto(
        Guid.Empty,
        app.Id,
        "msedge.exe",
        "domain",
        "github.com",
        "工作 / 开发",
        null,
        "Edge / github.com",
        "system-suggested",
        0.8,
        true,
        42,
        7200,
        null);
    var preview = new ActivityClassificationPreviewDto(
        42,
        7200,
        new Dictionary<string, int> { ["其他"] = 42 },
        new Dictionary<string, int> { ["工作 / 开发"] = 42 },
        [],
        true,
        "将写入 App 知识库。");

    var service = new AppKnowledgeSuggestionService(
        db,
        new AppKnowledgeContextService(db),
        NullLogger<AppKnowledgeSuggestionService>.Instance);

    var saved = await service.SaveRecommendedContextAsync(
        new AppKnowledgeSuggestionPreviewDto(suggestionId, context, [context], preview),
        CancellationToken.None);

    Assert.NotEqual(Guid.Empty, saved.Id);
    Assert.Equal(42, saved.AffectedRecordCount);
    Assert.Equal(7200, saved.AffectedDurationSeconds);
    var persisted = await db.Set<AppKnowledgeContextEntity>().SingleAsync();
    Assert.Equal(suggestionId, persisted.SourceSuggestionId);
}
```

- [ ] **Step 2: Run backend focused tests**

Run:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter "AppKnowledge|ActivityClassificationRecomputeServiceTests|PimPcTrackerModelTests"
```

Expected: PASS.

- [ ] **Step 3: Commit backend test repairs**

If there are unstaged backend test changes:

```powershell
git add tests/Pim.UnitTests/Services/AppKnowledgeContextServiceTests.cs tests/Pim.UnitTests/Services/AppKnowledgeSuggestionServiceTests.cs tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs
git commit -m "test: cover app knowledge backend persistence"
```

Expected: commit succeeds if there are staged changes. If there are no staged changes, skip this commit.

---

### Task 13: Final Verification, Diff Review, And Delivery

**Files:**
- Verify: all touched source, tests, migrations, and docs.

- [ ] **Step 1: Run full backend verification**

Run:

```powershell
dotnet test Pim.sln
```

Expected: PASS.

- [ ] **Step 2: Run full frontend verification**

Run:

```powershell
npm --prefix src/client-web run build
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeNavigation.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeApiPath.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeTypes.test.ts
npm --prefix src/client-web exec tsx -- tests/client-web/appKnowledgeComponents.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/pcRecordsReviewLayout.test.tsx
npm --prefix src/client-web exec tsx -- tests/client-web/pcRoute3Components.test.tsx
```

Expected: PASS.

- [ ] **Step 3: Check generated artifacts and whitespace**

Run:

```powershell
git status --short --branch
git diff --check
git diff --name-only
```

Expected:

- no `.superpowers/brainstorm/`
- no `src/Pim.Api/wwwroot/`
- no `bin/`, `obj/`, `build/`, `dist/`, publish artifacts, or npm cache files
- `git diff --check` reports no whitespace errors

- [ ] **Step 4: Review implementation against spec**

Read:

```powershell
Get-Content -Raw docs/superpowers/specs/2026-07-06-pc-records-app-knowledge-redesign.md
```

Confirm these are implemented:

- PC Records first screen is review-first.
- standalone Classification Management is not in main navigation.
- standalone Category Tree is not in main navigation.
- Category Tree is reachable under App Knowledge.
- context confirmations say they write to App Knowledge.
- App Knowledge shows context patterns.
- App Knowledge suggestion apply persists context knowledge and still recomputes/audits.

- [ ] **Step 5: Commit final integration changes if needed**

If there are intentional unstaged changes after verification:

```powershell
git add <intentional files>
git commit -m "feat: complete pc app knowledge redesign"
```

Expected: commit succeeds. If there are no unstaged changes, skip this commit.

- [ ] **Step 6: Final branch status**

Run:

```powershell
git status --short --branch
git log --oneline -8
```

Expected: working tree is clean and recent commits show the App Knowledge redesign.

- [ ] **Step 7: Push only when requested**

If the user asks to update GitHub:

```powershell
git push -u origin codex/pc-records-app-knowledge-redesign
```

Expected: push succeeds. If direct `master` integration is requested, merge only after successful verification and then push `master` as instructed by `AGENTS.md`.

## Self-Review Checklist

- Spec coverage: Tasks 1-2 cover information architecture; Tasks 3-4 cover review-first PC Records; Tasks 5-9 cover App Knowledge contracts, model, backend, and UI; Task 10 covers writeback copy and query refresh; Tasks 11-13 cover verification.
- Placeholder scan: this plan contains concrete file paths, command lines, and code snippets for each implementation task.
- Type consistency: frontend `AppKnowledgeContextPattern` maps to backend `AppKnowledgeContextDto`; frontend `AppKnowledgeSuggestionPreview` maps to backend `AppKnowledgeSuggestionPreviewDto`; suggestion preview/apply requests reuse existing classification request shapes to preserve Route 3 safety.
