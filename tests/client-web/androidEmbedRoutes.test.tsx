import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
const appSource = readFileSync(
  path.join(process.cwd(), 'src/client-web/src/App.tsx'),
  'utf8'
);

assert.ok(
  appSource.includes('/embed/android/today'),
  'App should define /embed/android/today route'
);
assert.ok(
  appSource.includes('/embed/android/tracks'),
  'App should define /embed/android/tracks route'
);

const authProviderTagIndex = appSource.indexOf('<AuthProvider>');
const embedTodayIndex = appSource.indexOf('/embed/android/today');
const embedTracksIndex = appSource.indexOf('/embed/android/tracks');

assert.ok(
  embedTodayIndex >= 0 && embedTracksIndex >= 0,
  'Embed route strings must be present'
);

assert.ok(
  authProviderTagIndex >= 0,
  'AuthProvider opening tag must be present'
);

assert.ok(
  embedTodayIndex < authProviderTagIndex && embedTracksIndex < authProviderTagIndex,
  'Embed routes should be declared before AuthProvider usage'
);

const layoutSource = readFileSync(
  path.join(process.cwd(), 'src/client-web/src/layout/AndroidEmbedLayout.tsx'),
  'utf8'
);

assert.ok(
  !layoutSource.includes('Sidebar'),
  'AndroidEmbedLayout should not reference Sidebar'
);
assert.ok(
  !layoutSource.includes('useAuth'),
  'AndroidEmbedLayout should not reference useAuth'
);
assert.ok(
  !layoutSource.includes('loadTokens'),
  'AndroidEmbedLayout should not call loadTokens'
);
assert.ok(
  layoutSource.includes('overflow') || layoutSource.includes('safe'),
  'AndroidEmbedLayout should have scroll or safe-area container'
);

// --- Runtime rendering assertions ---

function test(_name: string, run: () => void) {
  run();
}

if (typeof globalThis.localStorage === 'undefined') {
  const store = new Map<string, string>();
  Object.defineProperty(globalThis, 'localStorage', {
    value: {
      getItem: (key: string) => store.get(key) ?? null,
      setItem: (key: string, value: string) => { store.set(key, value); },
      removeItem: (key: string) => { store.delete(key); },
      clear: () => { store.clear(); },
      get length() { return store.size; },
      key: (index: number) => [...store.keys()][index] ?? null,
    },
    writable: true,
  });
}

const requireFromClient = createRequire(
  path.join(process.cwd(), 'src/client-web/package.json')
);
const React = requireFromClient('react');
const { renderToStaticMarkup } = requireFromClient('react-dom/server');
const { MemoryRouter } = requireFromClient('react-router-dom');
const {
  QueryClient,
  QueryClientProvider,
} = requireFromClient('@tanstack/react-query');
const reactGlobal = globalThis as typeof globalThis & { React: typeof React };
reactGlobal.React = React;

const { AppRoutes } = requireFromClient('./src/App.tsx');

const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderAppRoutes(initialEntries: string[]) {
  return renderToStaticMarkup(
    React.createElement(
      MemoryRouter,
      { initialEntries },
      React.createElement(
        QueryClientProvider,
        { client: qc },
        React.createElement(AppRoutes)
      )
    )
  );
}

test('embed /android/today route renders AndroidEmbedLayout without desktop chrome or TodayPage content', () => {
  const html = renderAppRoutes(['/embed/android/today']);
  assert.ok(html.includes('overflow-y-auto'), 'Should render AndroidEmbedLayout scroll container');
  assert.ok(!html.includes('Sidebar'), 'Should not render Sidebar');
  assert.ok(!html.includes('pim-shell'), 'Should not render desktop shell');
  assert.ok(!html.includes('日程任务工作台'), 'Should not render TodayPage title');
});

test('embed /android/tracks route renders embed-only content without desktop chrome', () => {
  const html = renderAppRoutes(['/embed/android/tracks']);
  assert.ok(html.includes('轨迹页面'), 'Should render tracks placeholder');
  assert.ok(!html.includes('Sidebar'), 'Should not render Sidebar');
  assert.ok(!html.includes('pim-shell'), 'Should not render desktop shell');
});

test('desktop /today route does not render embed layout', () => {
  const html = renderAppRoutes(['/today']);
  assert.ok(!html.includes('overflow-y-auto'), 'Should not render embed scroll container');
  assert.ok(!html.includes('轨迹页面'), 'Should not render embed content');
});
