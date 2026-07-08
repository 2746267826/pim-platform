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
  '/sync',
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
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockApiResponse(url.pathname)),
        });
      });

      for (const route of routes) {
        const page = await context.newPage();
        await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded' });
        await page.waitForLoadState('networkidle', { timeout: 4_000 }).catch(() => undefined);
        await page.waitForFunction(
          () => (document.querySelector('main')?.textContent ?? '').trim().length > 0,
          null,
          { timeout: 6_000 },
        );

        await assertRoute(page, route, width, height);
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

function mockApiResponse(pathname: string) {
  let data: unknown = [];
  if (pathname.endsWith('/status/summary')) {
    data = { status: 'Healthy', checks: [] };
  } else if (pathname.includes('/calendar/data-center/query')) {
    data = { items: [], page: 1, pageSize: 50, totalCount: 0 };
  } else if (pathname.includes('/calendar/outlook/settings')) {
    data = {
      provider: 'outlook',
      tenantId: 'common',
      clientId: null,
      scopes: 'Calendars.ReadWrite offline_access',
      status: 'not-connected',
      tokenHealth: 'missing',
      lastSyncedAt: null,
      lastError: null,
    };
  } else if (pathname.includes('/operations/audit/')) {
    data = { items: [] };
  } else if (pathname.includes('/endpoints/') && pathname.endsWith('/collection-quality')) {
    data = {
      deviceId: 'windows-companion',
      platform: 'windows',
      uploadStatus: 'Healthy',
      issueCount: 0,
      checkedAt: new Date().toISOString(),
    };
  } else if (pathname.includes('/today/sections')) {
    data = [];
  }

  return { code: 0, message: 'OK', data, timestamp: new Date().toISOString() };
}

main().catch((error: unknown) => {
  console.error(error);
  process.exit(1);
});
