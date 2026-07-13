import assert from 'node:assert/strict';
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import { createRequire } from 'node:module';
import { createServer } from 'node:net';
import { fileURLToPath } from 'node:url';

const requireFromWeb = createRequire(new URL('../../src/client-web/package.json', import.meta.url));
const { chromium } = requireFromWeb('playwright') as typeof import('playwright');
type Browser = import('playwright').Browser;
type Page = import('playwright').Page;

const routes = [
  '/today',
  '/calendar',
  '/tasks',
  '/habits',
  '/reminders',
  '/reports',
  '/settings/sync',
  '/data-center',
  '/confirmations',
  '/audit/task/00000000-0000-0000-0000-000000000001',
  '/endpoint-shell',
] as const;

const viewports = [
  [390, 844],
  [768, 1024],
  [1440, 1000],
] as const;

const forbiddenHeadings = new Set([
  'Endpoint Shell',
  'Collection Quality',
  'Notification Action',
  'Confirmations Page',
  'Reports Page',
]);

async function main() {
  const port = await freePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  const server = startVite(port);
  let browser: Browser | undefined;

  try {
    await waitForServer(baseUrl);
    browser = await chromium.launch({ headless: true });

    for (const [width, height] of viewports) {
      const context = await browser.newContext({ viewport: { width, height } });
      await context.addInitScript(() => {
        localStorage.setItem('accessToken', 'schedule-workbench-visual-audit-token');
      });
      await context.route('**/api/v1/**', route => {
        const url = new URL(route.request().url());
        const fullPath = url.pathname + url.search;
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockApiResponse(fullPath)),
        });
      });

      for (const route of routes) {
        const page = await context.newPage();
        const consoleErrors: string[] = [];
        page.on('console', message => {
          if (message.type() === 'error') consoleErrors.push(message.text());
        });
        await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded' });
        await page.waitForLoadState('networkidle', { timeout: 4_000 }).catch(() => undefined);
        await page.waitForFunction(
          () => (document.querySelector('main')?.textContent ?? '').trim().length > 0,
          null,
          { timeout: 6_000 },
        );

        await assertRoute(page, route, width, height);
        assert.deepEqual(
          consoleErrors,
          [],
          `${route} at ${width}x${height} should not log browser errors`,
        );
        await page.close();
      }

      await context.close();
    }
  } finally {
    await browser?.close();
    stopServer(server);
  }
}

async function assertRoute(page: Page, route: string, width: number, height: number) {
  const result = await page.evaluate((forbidden) => {
    const mainText = (document.querySelector('main')?.textContent ?? '').trim();
    const bodyText = document.body.textContent ?? '';
    const clippedButtons = Array.from(document.querySelectorAll('button'))
      .filter(button => (button.textContent ?? '').trim().length > 0)
      .filter(button => {
        const style = getComputedStyle(button);
        return style.whiteSpace === 'nowrap'
          && button.scrollWidth > Math.ceil(button.clientWidth) + 2;
      })
      .map(button => (button.textContent ?? '').trim());
    const negativeBoxes = Array.from(document.querySelectorAll('body *'))
      .filter(element => {
        const rect = element.getBoundingClientRect();
        return rect.width < 0 || rect.height < 0;
      }).length;
    const englishHeadings = Array.from(document.querySelectorAll('h1,h2,[role="heading"]'))
      .map(element => (element.textContent ?? '').trim())
      .filter(text => forbidden.includes(text));

    return {
      bodyText,
      mainTextLength: mainText.length,
      clippedButtons,
      negativeBoxes,
      englishHeadings,
      horizontalOverflow: document.documentElement.scrollWidth - window.innerWidth,
    };
  }, Array.from(forbiddenHeadings));

  assert.ok(result.mainTextLength > 0, `${route} at ${width}x${height} should render main content`);
  assert.ok(!result.bodyText.includes('用户名') || result.bodyText.includes('端点'), `${route} should not render the login form`);
  assert.deepEqual(result.clippedButtons, [], `${route} at ${width}x${height} should not clip button text`);
  assert.equal(result.negativeBoxes, 0, `${route} at ${width}x${height} should not render negative boxes`);
  assert.deepEqual(result.englishHeadings, [], `${route} should not expose English workbench headings`);
  assert.ok(result.horizontalOverflow <= 4, `${route} at ${width}x${height} should not overflow horizontally by ${result.horizontalOverflow}px`);

  if (route === '/settings/sync') {
    await assertSyncPage(page, width, height);
  }
}

async function assertSyncPage(page: Page, width: number, height: number) {
  // Click "获取代码" to trigger device code display, then wait for it to render
  const getCodeButton = page.locator('button', { hasText: '获取代码' });
  if (await getCodeButton.isVisible()) {
    await getCodeButton.click();
    await page.waitForFunction(
      () => !!document.querySelector('[data-testid="device-code-status"]'),
      null,
      { timeout: 4_000 },
    ).catch(() => undefined);
  }

  // Device-code status area should have stable width (no shifting from changing digits)
  const deviceCodeWidth = await page.evaluate(() => {
    const codeEl = document.querySelector('[data-testid="device-code-status"]');
    if (!codeEl) return null;
    const style = getComputedStyle(codeEl);
    return { width: style.width, minWidth: style.minWidth };
  });
  assert.ok(deviceCodeWidth !== null, 'device-code-status element should exist');
  if (deviceCodeWidth) {
    assert.ok(deviceCodeWidth.minWidth !== '' || parseInt(String(deviceCodeWidth.width)) >= 120,
      'device-code-status should have stable width');
  }

  // Click "发现日历" to show binding groups
  const discoverButton = page.locator('button', { hasText: '发现日历' });
  if (await discoverButton.isVisible()) {
    await discoverButton.click();
    await page.waitForFunction(
      () => Array.from(document.querySelectorAll('input[type="checkbox"]')).length >= 4,
      null,
      { timeout: 4_000 },
    ).catch(() => undefined);
  }

  // Group selection: at least one group checkbox
  const hasGroupCheckboxes = await page.evaluate(() => {
    const checkboxes = document.querySelectorAll('input[type="checkbox"]');
    return checkboxes.length >= 2;
  });
  assert.ok(hasGroupCheckboxes, 'sync page should show grouped calendar checkboxes');

  // Read-only, paused, remote-missing states visible
  const hasStateIndicators = await page.evaluate(() => {
    const text = document.body.textContent ?? '';
    return text.includes('只读') || text.includes('暂停') || text.includes('缺失');
  });
  assert.ok(hasStateIndicators, 'sync page should show read-only/paused/remote-missing states');

  // Sync controls present
  const hasSyncControls = await page.evaluate(() => {
    const text = document.body.textContent ?? '';
    return text.includes('立即同步');
  });
  assert.ok(hasSyncControls, 'sync page should show sync controls');

  // Deep sync modes and manual force-all action
  const hasDeepModes = await page.evaluate(() => {
    const text = document.body.textContent ?? '';
    return text.includes('深度同步') && text.includes('强制获取全部日程');
  });
  assert.ok(hasDeepModes, 'sync page should show deep sync and manual force-all actions');
}

function startVite(port: number): ChildProcessWithoutNullStreams {
  const viteBin = fileURLToPath(new URL('../../src/client-web/node_modules/vite/bin/vite.js', import.meta.url));
  const child = spawn(
    process.execPath,
    [viteBin, '--host', '127.0.0.1', '--port', String(port)],
    { cwd: 'src/client-web', stdio: ['ignore', 'pipe', 'pipe'] },
  );

  child.stdout.on('data', chunk => process.stdout.write(chunk));
  child.stderr.on('data', chunk => process.stderr.write(chunk));
  return child;
}

function stopServer(server: ChildProcessWithoutNullStreams) {
  if (!server.killed) {
    server.kill('SIGTERM');
  }
}

async function waitForServer(baseUrl: string) {
  for (let attempt = 0; attempt < 80; attempt++) {
    try {
      const response = await fetch(baseUrl);
      if (response.ok) return;
    } catch {
      // Vite is still starting.
    }

    await delay(250);
  }

  throw new Error(`Timed out waiting for Vite at ${baseUrl}`);
}

async function freePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = createServer();
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      if (!address || typeof address === 'string') {
        reject(new Error('Could not allocate a local port'));
        return;
      }

      const port = address.port;
      server.close(() => resolve(port));
    });
  });
}

function delay(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function mockApiResponse(fullPath: string) {
  let data: unknown = [];
  if (fullPath.endsWith('/status/summary')) {
    data = { status: 'Healthy', checks: [] };
  } else if (fullPath.includes('/calendar/data-center/query')) {
    data = { items: [], page: 1, pageSize: 50, totalCount: 0 };
  } else if (fullPath.includes('/calendar/outlook/settings')) {
    data = {
      provider: 'outlook',
      tenantId: 'common',
      clientId: '11111111-1111-1111-1111-111111111111',
      scopes: 'Calendars.ReadWrite offline_access',
      status: 'connected',
      tokenHealth: 'Healthy',
      lastSyncedAt: '2026-07-13T08:00:00Z',
      lastError: null,
      uiStatus: 'connected',
      activeAuthorization: null,
    };
  } else if (fullPath.includes('/calendar/outlook/device-code')) {
    if (fullPath.endsWith('/poll')) {
      data = {
        id: 'd4e5f6a7-b8c9-0123-defa-234567890123',
        status: 'connected',
        verificationUri: 'https://microsoft.com/devicelogin',
        userCode: 'ABC123XYZ',
        expiresAt: '2026-07-13T12:00:00Z',
        accountDisplayName: 'Test User',
        accountLoginHint: 'user@example.com',
        errorCode: null,
        errorMessage: null,
        recoveryAction: null,
      };
    } else if (fullPath.endsWith('/cancel')) {
      data = 'cancelled';
    } else {
      data = {
        id: 'd4e5f6a7-b8c9-0123-defa-234567890123',
        status: 'waiting-for-user',
        verificationUri: 'https://microsoft.com/devicelogin',
        userCode: 'ABC123XYZ',
        expiresAt: '2026-07-13T12:00:00Z',
        accountDisplayName: null,
        accountLoginHint: null,
        errorCode: null,
        errorMessage: null,
        recoveryAction: null,
      };
    }
  } else if (fullPath.includes('/calendar/outlook/calendars/discover')) {
    data = [
      {
        id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        pimCalendarId: 'p1m2c3d4-e5f6-7890-abcd-ef1234567890',
        graphCalendarId: 'graph-cal-1',
        groupId: null,
        groupName: 'Work',
        name: '工作日历',
        color: '#0044CC',
        ownerName: null,
        ownerAddress: null,
        isDefault: true,
        canEdit: true,
        isSelected: true,
        remoteState: 'active',
        lastSyncedAt: '2026-07-13T08:00:00Z',
        lastError: null,
      },
      {
        id: 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        pimCalendarId: 'q2r3s4t5-u6v7-8901-bcde-f12345678901',
        graphCalendarId: 'graph-cal-2',
        groupId: null,
        groupName: 'Personal',
        name: '个人日历',
        color: '#00AA44',
        ownerName: null,
        ownerAddress: null,
        isDefault: false,
        canEdit: false,
        isSelected: false,
        remoteState: 'active',
        lastSyncedAt: null,
        lastError: null,
      },
      {
        id: 'c3d4e5f6-a7b8-9012-cdef-123456789012',
        pimCalendarId: 'r3s4t5u6-v7w8-9012-cdef-123456789012',
        graphCalendarId: 'graph-cal-3',
        groupId: null,
        groupName: 'Work',
        name: '团队日历',
        color: '#AA4400',
        ownerName: null,
        ownerAddress: null,
        isDefault: false,
        canEdit: true,
        isSelected: false,
        remoteState: 'paused',
        lastSyncedAt: '2026-07-10T08:00:00Z',
        lastError: null,
      },
      {
        id: 'd4e5f6a7-b8c9-0123-defa-234567890123',
        pimCalendarId: 's4t5u6v7-w8x9-0123-defa-234567890123',
        graphCalendarId: 'graph-cal-4',
        groupId: null,
        groupName: null,
        name: '已删除日历',
        color: '#888888',
        ownerName: null,
        ownerAddress: null,
        isDefault: false,
        canEdit: false,
        isSelected: false,
        remoteState: 'remote-missing',
        lastSyncedAt: null,
        lastError: null,
      },
    ];
  } else if (fullPath.includes('/calendar/outlook/calendars/selection')) {
    data = [
      {
        id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        pimCalendarId: 'p1m2c3d4-e5f6-7890-abcd-ef1234567890',
        graphCalendarId: 'graph-cal-1',
        groupId: null,
        groupName: 'Work',
        name: '工作日历',
        color: '#0044CC',
        ownerName: null,
        ownerAddress: null,
        isDefault: true,
        canEdit: true,
        isSelected: true,
        remoteState: 'active',
        lastSyncedAt: '2026-07-13T08:00:00Z',
        lastError: null,
      },
      {
        id: 'c3d4e5f6-a7b8-9012-cdef-123456789012',
        pimCalendarId: 'r3s4t5u6-v7w8-9012-cdef-123456789012',
        graphCalendarId: 'graph-cal-3',
        groupId: null,
        groupName: 'Work',
        name: '团队日历',
        color: '#AA4400',
        ownerName: null,
        ownerAddress: null,
        isDefault: false,
        canEdit: true,
        isSelected: true,
        remoteState: 'paused',
        lastSyncedAt: '2026-07-10T08:00:00Z',
        lastError: null,
      },
    ];
  } else if (fullPath.includes('/calendar/outlook/sync/batches')) {
    const batchItems = [
      {
        id: 'e5f6a7b8-c9d0-1234-efab-345678901234',
        provider: 'outlook',
        status: 'completed',
        readCount: 50,
        createdCount: 2,
        updatedCount: 5,
        conflictCount: 0,
        confirmationCount: 0,
        failureCount: 1,
        steps: [],
        errorSummary: null,
        startedAt: '2026-07-13T08:00:00Z',
        finishedAt: '2026-07-13T08:02:00Z',
        mode: 'normal',
        requestedWindowStart: null,
        requestedWindowEnd: null,
        perCalendarJson: JSON.stringify([
          {
            bindingId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
            calendarName: '工作日历',
            status: 'completed',
            readCount: 50,
            createdCount: 2,
            updatedCount: 5,
            deletedCount: 0,
            failureCount: 0,
            changes: [{ id: 'evt-10', title: 'Sync Event', action: 'created' }],
            failures: [],
          },
          {
            bindingId: 'd4e5f6a7-b8c9-0123-defa-234567890123',
            calendarName: '已删除日历',
            status: 'failed',
            readCount: 0,
            createdCount: 0,
            updatedCount: 0,
            deletedCount: 0,
            failureCount: 1,
            changes: [],
            failures: [{ eventId: 'evt-1', title: 'Meeting', code: 'AuthError', message: 'Permission denied' }],
          },
        ]),
        cancelRequested: false,
      },
    ];
    data = { items: batchItems, total: 1, page: 1, pageSize: 20 };
  } else if (fullPath.includes('/calendar/outlook/sync') && !fullPath.includes('/batches')) {
    if (fullPath.endsWith('/cancel')) {
      data = 'cancelled';
    } else {
      data = {
        id: 'f6a7b8c9-d0e1-2345-fabc-456789012345',
        provider: 'outlook',
        status: 'running',
        readCount: 0,
        createdCount: 0,
        updatedCount: 0,
        conflictCount: 0,
        confirmationCount: 0,
        failureCount: 0,
        steps: [],
        errorSummary: null,
        startedAt: '2026-07-13T09:00:00Z',
        finishedAt: null,
        mode: 'normal',
        requestedWindowStart: null,
        requestedWindowEnd: null,
        perCalendarJson: null,
        cancelRequested: false,
      };
    }
  } else if (fullPath.includes('/calendar/outlook/local-data/preview')) {
    data = { bindingCount: 3, calendarCount: 5, eventCount: 120 };
  } else if (fullPath.includes('/calendar/outlook/local-data')) {
    data = 'deleted';
  } else if (fullPath.includes('/calendar/outlook/disconnect')) {
    data = 'disconnected';
  } else if (fullPath.includes('/calendar/outlook/check')) {
    data = {
      provider: 'outlook',
      tenantId: 'common',
      clientId: '11111111-1111-1111-1111-111111111111',
      scopes: 'Calendars.ReadWrite offline_access',
      status: 'connected',
      tokenHealth: 'healthy',
      lastSyncedAt: '2026-07-13T08:00:00Z',
      lastError: null,
      uiStatus: 'connected',
      activeAuthorization: null,
    };
  } else if (fullPath.includes('/calendar/outlook/events/writeback')) {
    data = 'queued';
  } else if (fullPath.includes('/operations/audit/')) {
    data = { items: [] };
  } else if (fullPath.includes('/endpoints/') && fullPath.endsWith('/collection-quality')) {
    data = {
      deviceId: 'windows-companion',
      platform: 'windows',
      uploadStatus: 'Healthy',
      issueCount: 0,
      checkedAt: new Date().toISOString(),
    };
  } else if (fullPath.includes('/today/sections')) {
    data = [];
  }

  return { code: 0, message: 'OK', data, timestamp: new Date().toISOString() };
}

main().catch((error: unknown) => {
  console.error(error);
  process.exit(1);
});
