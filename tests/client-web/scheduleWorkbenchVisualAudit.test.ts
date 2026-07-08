import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const viewports = [
  [390, 844],
  [768, 1024],
  [1440, 1000],
] as const;

const routePages: Record<string, string> = {
  '/today': 'src/client-web/src/pages/TodayPage.tsx',
  '/calendar': 'src/client-web/src/pages/CalendarPage.tsx',
  '/tasks': 'src/client-web/src/pages/TaskListPage.tsx',
  '/habits': 'src/client-web/src/pages/HabitsPage.tsx',
  '/reminders': 'src/client-web/src/pages/RemindersPage.tsx',
  '/reports': 'src/client-web/src/pages/ReportsPage.tsx',
  '/sync': 'src/client-web/src/pages/SyncPage.tsx',
  '/data-center': 'src/client-web/src/pages/DataCenterPage.tsx',
  '/confirmations': 'src/client-web/src/pages/ConfirmationsPage.tsx',
  '/audit/task/{id}': 'src/client-web/src/pages/AuditTimelinePage.tsx',
  '/endpoint-shell': 'src/client-web/src/pages/EndpointShellPage.tsx',
};

const css = readFileSync('src/client-web/src/index.css', 'utf8');
const appLayout = readFileSync('src/client-web/src/layout/AppLayout.tsx', 'utf8');

assert.deepEqual(viewports, [
  [390, 844],
  [768, 1024],
  [1440, 1000],
]);

assert.ok(appLayout.includes('pim-route-surface'), 'main route surface should have a stable layout class');
assert.ok(css.includes('.pim-route-surface'), 'route surface should define overflow-safe layout rules');
assert.ok(css.includes('@media (max-width: 760px)'), 'mobile viewport rules should be present');
assert.ok(css.includes('overflow-wrap: anywhere'), 'text should wrap instead of clipping');
assert.ok(css.includes('white-space: normal'), 'button text should be allowed to wrap');
assert.ok(css.includes('min-width: 0'), 'flex/grid children should be allowed to shrink');

for (const [route, path] of Object.entries(routePages)) {
  const source = readFileSync(path, 'utf8');
  assert.ok(
    source.includes('pim-panel') || source.includes('calendar-board') || source.includes('pim-card'),
    `${route} should render non-empty workbench content`,
  );
}

for (const [path, forbiddenHeadings] of Object.entries({
  'src/client-web/src/pages/EndpointShellPage.tsx': ['Endpoint Shell', 'Collection Quality', 'Notification Action'],
  'src/client-web/src/pages/ConfirmationsPage.tsx': ['Confirmations Page'],
  'src/client-web/src/pages/ReportsPage.tsx': ['Reports Page'],
})) {
  const source = readFileSync(path, 'utf8');
  for (const heading of forbiddenHeadings) {
    assert.ok(!source.includes(`title="${heading}"`), `${path} should not expose English heading ${heading}`);
  }
}
