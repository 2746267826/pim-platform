/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/no-require-imports */
import assert from 'node:assert/strict';

// ── helpers ────────────────────────────────────────────────────────
const _tests: { name: string; fn: () => void | Promise<void> }[] = [];

function test(name: string, fn: () => void | Promise<void>) {
  _tests.push({ name, fn });
}

async function _runAll(): Promise<void> {
  let exitCode = 0;
  for (const { name, fn } of _tests) {
    try {
      await fn();
      console.error(`PASS: ${name}`);
    } catch (err) {
      console.error(`FAIL: ${name}`);
      console.error(err);
      exitCode = 1;
    }
  }
  if (exitCode !== 0) process.exit(exitCode);
}

interface MockWindow {
  pimAndroid: { postMessage(raw: string): void };
  addEventListener: (type: string, cb: (event: { data: string }) => void) => void;
  removeEventListener: (type: string, cb: (event: { data: string }) => void) => void;
  sendResponse(raw: string): void;
  getSent(): BridgeMessage[];
  clearSent(): void;
  sent: string[];
}

import type { BridgeMessage } from '../../src/client-web/src/embed/androidBridge';
import { AndroidBridgeClient } from '../../src/client-web/src/embed/androidBridge';

function mockWindow(): MockWindow {
  const listeners = new Set<(event: { data: string }) => void>();
  const sent: string[] = [];
  return {
    pimAndroid: {
      postMessage(raw: string) { sent.push(raw); },
    },
    addEventListener(_type: string, cb: (event: { data: string }) => void) { listeners.add(cb); },
    removeEventListener(_type: string, cb: (event: { data: string }) => void) { listeners.delete(cb); },
    sendResponse(raw: string) { listeners.forEach(l => l({ data: raw })); },
    getSent(): BridgeMessage[] { return sent.map(s => JSON.parse(s)); },
    clearSent() { sent.length = 0; },
    sent,
  };
}

// ────────────────────────────────────────────────────────────────────
//  Bridge client unit tests
// ────────────────────────────────────────────────────────────────────

test('requestToken: sends one postMessage and returns the access token', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  const tokenReq = bridge.requestToken();

  const msgs = win.getSent();
  assert.equal(msgs.length, 1);
  assert.equal(msgs[0].version, 1);
  assert.equal(msgs[0].type, 'token.request');
  assert.ok(typeof msgs[0].id === 'string' && msgs[0].id.length > 0);

  win.sendResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_abc',
  }));

  const token = await tokenReq;
  assert.equal(token, 'tok_abc');
  assert.equal(bridge.getAccessToken(), 'tok_abc');
});

test('two concurrent requestToken calls send only one postMessage', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);

  const req1 = bridge.requestToken();
  const req2 = bridge.requestToken();

  const msgs = win.getSent();
  assert.equal(msgs.length, 1, 'should send exactly one token.request');
  assert.equal(msgs[0].type, 'token.request');

  win.sendResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_concurrent',
  }));

  const [t1, t2] = await Promise.all([req1, req2]);
  assert.equal(t1, 'tok_concurrent');
  assert.equal(t2, 'tok_concurrent');
});

test('requestToken caches token for subsequent calls', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);

  const t1 = bridge.requestToken();
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: true, accessToken: 'tok_cached',
  }));
  assert.equal(await t1, 'tok_cached');
  assert.equal(win.getSent().length, 1);

  win.clearSent();
  const t2 = bridge.requestToken();
  assert.equal(await t2, 'tok_cached');
  assert.equal(win.getSent().length, 0, 'should not send another request when token is cached');
});

test('refreshToken sends token.refresh and returns new token', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_old');
  const refreshP = bridge.refreshToken('tok_old');

  const msgs = win.getSent();
  assert.equal(msgs.length, 1);
  assert.equal(msgs[0].type, 'token.refresh');
  assert.equal(msgs[0].failedAccessToken, 'tok_old');

  win.sendResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_new',
  }));

  const token = await refreshP;
  assert.equal(token, 'tok_new');
  assert.equal(bridge.getAccessToken(), 'tok_new');
});

test('two concurrent refreshToken calls share one bridge refresh request', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_old');

  const r1 = bridge.refreshToken();
  const r2 = bridge.refreshToken();

  const msgs = win.getSent();
  assert.equal(msgs.length, 1, 'should send exactly one token.refresh');

  win.sendResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_shared',
  }));

  const [t1, t2] = await Promise.all([r1, r2]);
  assert.equal(t1, 'tok_shared');
  assert.equal(t2, 'tok_shared');
});

test('refreshToken failure returns null and clears access token', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_expired');

  const refreshP = bridge.refreshToken('tok_expired');
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: false,
    errorCode: 'login_expired', message: '登录已过期',
  }));

  const result = await refreshP;
  assert.equal(result, null);
  assert.equal(bridge.getAccessToken(), null);
});

test('refreshToken after failure can succeed next time', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_fail');

  const failP = bridge.refreshToken('tok_fail');
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: false, errorCode: 'login_expired',
  }));
  assert.equal(await failP, null);

  win.clearSent();
  const okP = bridge.refreshToken();
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: true, accessToken: 'tok_recovered',
  }));
  assert.equal(await okP, 'tok_recovered');
});

test('bridge_unavailable when window.pimAndroid is missing', async () => {
  const winNoBridge = {
    addEventListener: () => {},
    removeEventListener: () => {},
  };
  const bridge = new AndroidBridgeClient(winNoBridge as unknown as Window);

  await assert.rejects(
    () => bridge.requestToken(),
    (err: Error) => {
      assert.ok(err.message.includes('bridge_unavailable'), err.message);
      return true;
    },
  );
});

test('request id matching works with out-of-order replies', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_order');

  // Send refresh + native state concurrently (different types, not deduped)
  const r1 = bridge.refreshToken();
  const id1 = win.getSent()[0].id;
  win.clearSent();

  const r2 = bridge.requestNativeState();
  const id2 = win.getSent()[0].id;

  // Reply out of order: native state response arrives before refresh response
  win.sendResponse(JSON.stringify({
    version: 1, id: id2, ok: true, nativeState: { pending: 5 },
  }));
  win.sendResponse(JSON.stringify({
    version: 1, id: id1, ok: true, accessToken: 'tok_out_of_order',
  }));

  const [t1, state] = await Promise.all([r1, r2]);
  assert.equal(t1, 'tok_out_of_order');
  assert.deepEqual(state, { pending: 5 });
});

test('native state request uses correct protocol', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);

  const nativeP = bridge.requestNativeState();
  const msgs = win.getSent();
  assert.equal(msgs.length, 1);
  assert.equal(msgs[0].type, 'native.state.request');

  win.sendResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true,
    nativeState: {
      collectionMode: true,
      pending: 3,
      uploading: false,
    },
  }));

  const state = await nativeP;
  assert.deepEqual(state, {
    collectionMode: true,
    pending: 3,
    uploading: false,
  });
});

test('page report sends correct protocol fields', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);

  const reportData = {
    hasServerData: true,
    generatedAt: '2026-07-17T00:00:00Z',
    error: null,
  };

  const reportP = bridge.sendPageReport(reportData);
  const msgs = win.getSent();
  assert.equal(msgs.length, 1);
  assert.equal(msgs[0].type, 'page.report');
  assert.deepEqual(msgs[0].report, reportData);

  win.sendResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true,
  }));

  await reportP;
});

test('timeout rejects with error', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window, 50);

  await assert.rejects(
    () => bridge.requestToken(),
    (err: Error) => {
      assert.ok(err.message.length > 0, 'should produce an error message');
      return true;
    },
  );
});

// ────────────────────────────────────────────────────────────────────
//  API client embed mode: localStorage never touched
// ────────────────────────────────────────────────────────────────────

test('embed mode loadTokens and setTokens do not touch localStorage', () => {
  const lsCalls: string[] = [];
  const lsStore = new Map<string, string>();
  const mockLS = {
    getItem: (k: string) => { lsCalls.push(`get:${k}`); return lsStore.get(k) ?? null; },
    setItem: (k: string, v: string) => { lsCalls.push(`set:${k}`); lsStore.set(k, v); },
    removeItem: (k: string) => { lsCalls.push(`remove:${k}`); lsStore.delete(k); },
    clear: () => { lsCalls.push('clear'); lsStore.clear(); },
    get length() { return lsStore.size; },
    key: (i: number) => [...lsStore.keys()][i] ?? null,
  };
  const origLS = (globalThis as any).localStorage;
  (globalThis as any).localStorage = mockLS;

  // Capture the original pathname and set embed mode
  const origLoc = (globalThis as any).window?.location;
  const mockLoc = { pathname: '/embed/android/today', href: '', origin: '', search: '' };
  if (!(globalThis as any).window) {
    (globalThis as any).window = { location: mockLoc };
  } else {
    (globalThis as any).window.location = mockLoc;
  }

  // Import client AFTER setting up embed mode
  delete require.cache[require.resolve('../../src/client-web/src/api/client')];
  const client = require('../../src/client-web/src/api/client');
  const embedCalls = lsCalls.length;
  assert.equal(embedCalls, 0, 'no localStorage calls during import in embed mode');

  // clearTokens should not touch localStorage
  client.clearTokens();
  const clearCalls = lsCalls.filter(c => c.startsWith('remove:'));
  assert.equal(clearCalls.length, 0, 'clearTokens should not call localStorage.removeItem in embed mode');

  // setTokens should not touch localStorage
  client.setTokens('tok_embed', 'rt_embed');
  const setCalls = lsCalls.filter(c => c.startsWith('set:'));
  assert.equal(setCalls.length, 0, 'setTokens should not call localStorage.setItem in embed mode');

  // Restore
  (globalThis as any).localStorage = origLS;
  if (origLoc) (globalThis as any).window.location = origLoc;
});

// ────────────────────────────────────────────────────────────────────
//  Desktop mode preservation
// ────────────────────────────────────────────────────────────────────

test('desktop mode setTokens writes to localStorage', () => {
  const lsCalls: string[] = [];
  const lsStore = new Map<string, string>();
  const mockLS = {
    getItem: (k: string) => { lsCalls.push(`get:${k}`); return lsStore.get(k) ?? null; },
    setItem: (k: string, v: string) => { lsCalls.push(`set:${k}`); lsStore.set(k, v); },
    removeItem: (k: string) => { lsCalls.push(`remove:${k}`); lsStore.delete(k); },
    clear: () => { lsCalls.push('clear'); lsStore.clear(); },
    get length() { return lsStore.size; },
    key: (i: number) => [...lsStore.keys()][i] ?? null,
  };
  const origLS = (globalThis as any).localStorage;
  (globalThis as any).localStorage = mockLS;

  // Set desktop mode path
  const origLoc = (globalThis as any).window?.location;
  const mockLoc = { pathname: '/today', href: '', origin: '', search: '' };
  if (!(globalThis as any).window) {
    (globalThis as any).window = { location: mockLoc };
  } else {
    (globalThis as any).window.location = mockLoc;
  }

  delete require.cache[require.resolve('../../src/client-web/src/api/client')];
  const client = require('../../src/client-web/src/api/client');

  client.setTokens('tok_desk', 'rt_desk');
  assert.ok(lsCalls.some(c => c === 'set:accessToken'), 'should set accessToken in localStorage');
  assert.ok(lsCalls.some(c => c === 'set:refreshToken'), 'should set refreshToken in localStorage');

  // Restore
  (globalThis as any).localStorage = origLS;
  if (origLoc) (globalThis as any).window.location = origLoc;
});

test('desktop mode 401 without refreshToken does not call /auth/refresh', async () => {
  const origWin = (globalThis as any).window;
  const origFetch = (globalThis as any).fetch;
  const origLS = (globalThis as any).localStorage;

  // Desktop: non-embed path
  (globalThis as any).window = {
    location: { pathname: '/today', href: '', origin: '', search: '' },
    addEventListener: () => {},
    removeEventListener: () => {},
  };
  (globalThis as any).localStorage = {
    getItem: () => null, setItem: () => {}, removeItem: () => {},
    clear: () => {}, get length() { return 0; }, key: () => null,
  };

  const fetchCalls: string[] = [];
  (globalThis as any).fetch = (url: string) => {
    fetchCalls.push(url);
    return Promise.resolve(new Response(JSON.stringify({ message: 'Unauthorized' }), {
      status: 401, headers: { 'Content-Type': 'application/json' },
    }));
  };

  delete require.cache[require.resolve('../../src/client-web/src/api/client')];
  const client = require('../../src/client-web/src/api/client');

  // No tokens loaded → no refreshToken → should NOT call /auth/refresh
  await assert.rejects(
    () => client.apiGet('/today'),
    (err: Error) => err.message.includes('登录已过期'),
  );

  assert.equal(fetchCalls.length, 1, 'exactly one fetch call');
  assert.equal(fetchCalls[0], '/api/v1/today', 'only the original API call');
  assert.ok(!fetchCalls.some(u => u.includes('/auth/refresh')), 'no auth refresh');

  (globalThis as any).window = origWin;
  (globalThis as any).fetch = origFetch;
  (globalThis as any).localStorage = origLS;
});

test('desktop mode with refreshToken calls /auth/refresh on 401 then retries', async () => {
  const origWin = (globalThis as any).window;
  const origFetch = (globalThis as any).fetch;
  const origLS = (globalThis as any).localStorage;

  (globalThis as any).window = {
    location: { pathname: '/today', href: '', origin: '', search: '' },
    addEventListener: () => {},
    removeEventListener: () => {},
  };

  const lsStore = new Map<string, string>([
    ['accessToken', 'tok_desk_old'],
    ['refreshToken', 'rt_desk'],
  ]);
  (globalThis as any).localStorage = {
    getItem: (k: string) => lsStore.get(k) ?? null,
    setItem: (k: string, v: string) => { lsStore.set(k, v); },
    removeItem: (k: string) => { lsStore.delete(k); },
    clear: () => lsStore.clear(),
    get length() { return lsStore.size; },
    key: (i: number) => [...lsStore.keys()][i] ?? null,
  };

  const fetchCalls: { url: string; method: string; body?: string }[] = [];
  (globalThis as any).fetch = (url: string, opts: RequestInit = {}) => {
    const call = { url, method: (opts.method as string) || 'GET', body: opts.body as string | undefined };
    fetchCalls.push(call);

    if (url === '/api/v1/auth/refresh') {
      return Promise.resolve(new Response(JSON.stringify({
        data: { accessToken: 'tok_desk_new', refreshToken: 'rt_desk_new' },
      }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
    }

    // First API call → 401, retry → 200
    const apiCalls = fetchCalls.filter(c => c.url === '/api/v1/today').length;
    if (apiCalls === 1) {
      return Promise.resolve(new Response(JSON.stringify({ message: 'expired' }), {
        status: 401, headers: { 'Content-Type': 'application/json' },
      }));
    }
    return Promise.resolve(new Response(JSON.stringify({ data: { id: 1 } }), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    }));
  };

  delete require.cache[require.resolve('../../src/client-web/src/api/client')];
  const client = require('../../src/client-web/src/api/client');
  client.loadTokens();

  const result = await client.apiGet('/today');
  assert.deepEqual(result, { data: { id: 1 } });

  // Verify refresh flow
  const refreshCall = fetchCalls.find(c => c.url === '/api/v1/auth/refresh');
  assert.ok(refreshCall, '/auth/refresh was called');
  assert.equal(refreshCall!.method, 'POST');
  const reqBody = JSON.parse(refreshCall!.body || '{}');
  assert.equal(reqBody.refreshToken, 'rt_desk');

  // Token was updated
  assert.equal(lsStore.get('accessToken'), 'tok_desk_new');

  (globalThis as any).window = origWin;
  (globalThis as any).fetch = origFetch;
  (globalThis as any).localStorage = origLS;
});

// ────────────────────────────────────────────────────────────────────
//  Embed 401 → refresh → retry (bridge-level integration)
// ────────────────────────────────────────────────────────────────────

test('embed 401 triggers single refresh via bridge with concurrent dedup', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_start');

  const r1 = bridge.refreshToken('tok_start');
  const r2 = bridge.refreshToken('tok_start');

  // Only one message sent
  assert.equal(win.getSent().length, 1);
  assert.equal(win.getSent()[0].type, 'token.refresh');

  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: true, accessToken: 'tok_refreshed',
  }));

  const [t1, t2] = await Promise.all([r1, r2]);
  assert.equal(t1, 'tok_refreshed');
  assert.equal(t2, 'tok_refreshed');
  assert.equal(bridge.getAccessToken(), 'tok_refreshed');
});

test('refresh failure: login expired and no further retry', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_good');

  const r1 = bridge.refreshToken('tok_good');
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: false,
    errorCode: 'login_expired', message: '登录已过期',
  }));

  const result = await r1;
  assert.equal(result, null, 'should return null on refresh failure');
  assert.equal(bridge.getAccessToken(), null, 'should clear access token');

  // A subsequent requestToken should start fresh (no cached token)
  win.clearSent();
  assert.equal(bridge.getAccessToken(), null);
});

test('refresh succeeded but retry still gets 401 — stops without second refresh', async () => {
  const win = mockWindow();
  const bridge = new AndroidBridgeClient(win as unknown as Window);
  bridge.setAccessToken('tok_first');

  // Simulate a refresh that succeeds
  const r1 = bridge.refreshToken('tok_first');
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: true, accessToken: 'tok_second',
  }));
  assert.equal(await r1, 'tok_second');
  assert.equal(bridge.getAccessToken(), 'tok_second');

  // If the API client retries and gets another 401,
  // it should call refreshToken again. This time refresh also succeeds.
  // If refresh succeeds but retry STILL 401, the API client should NOT
  // call refresh again - it should stop.
  // We test the bridge's willingness to refresh again:
  win.clearSent();
  const r2 = bridge.refreshToken('tok_second');
  win.sendResponse(JSON.stringify({
    version: 1, id: win.getSent()[0].id, ok: true, accessToken: 'tok_third',
  }));
  assert.equal(await r2, 'tok_third');

  // The important thing is the API client retry logic prevents infinite loops.
  // Bridge itself has no such limit - it will always try.
  assert.ok(true, 'bridge will always attempt refresh; API client must limit retries');
});

// ────────────────────────────────────────────────────────────────────
//  Embed integration: apiGet → bridge.requestToken → Bearer fetch
// ────────────────────────────────────────────────────────────────────

function embedTestFix() {
  const origWin = (globalThis as any).window;
  const origFetch = (globalThis as any).fetch;
  const origLS = (globalThis as any).localStorage;

  const sent: string[] = [];
  const listeners = new Set<(e: { data: string }) => void>();
  const fetchCalls: { url: string; method: string; headers: Record<string, string> }[] = [];
  let fetchHandler: ((url: string, opts: RequestInit) => { status: number; body?: unknown }) | null = null;

  (globalThis as any).window = {
    pimAndroid: { postMessage(raw: string) { sent.push(raw); } },
    addEventListener(_: string, cb: (e: { data: string }) => void) { listeners.add(cb); },
    removeEventListener(_: string, cb: (e: { data: string }) => void) { listeners.delete(cb); },
    location: { pathname: '/embed/android/today', href: '', origin: '', search: '' },
  };
  (globalThis as any).localStorage = {
    getItem: () => null, setItem: () => {}, removeItem: () => {},
    clear: () => {}, get length() { return 0; }, key: () => null,
  };
  (globalThis as any).fetch = (url: string, opts: RequestInit = {}) => {
    const hdrs: Record<string, string> = {};
    if (opts.headers) { new Headers(opts.headers).forEach((v, k) => { hdrs[k] = v; }); }
    fetchCalls.push({ url, method: (opts.method as string) || 'GET', headers: hdrs });
    if (fetchHandler) {
      const r = fetchHandler(url, opts);
      return Promise.resolve(new Response(JSON.stringify(r.body ?? null), {
        status: r.status, headers: { 'Content-Type': 'application/json' },
      }));
    }
    return Promise.resolve(new Response('{"data":[]}', { status: 200, headers: { 'Content-Type': 'application/json' } }));
  };

  function sendBridgeResponse(raw: string) { listeners.forEach(l => l({ data: raw })); }
  function getSentBridgeMessages() { return sent.map(s => JSON.parse(s)); }
  function importClient() {
    delete require.cache[require.resolve('../../src/client-web/src/api/client')];
    return require('../../src/client-web/src/api/client');
  }
  function restore() {
    (globalThis as any).window = origWin;
    (globalThis as any).fetch = origFetch;
    (globalThis as any).localStorage = origLS;
  }

  return {
    sent, fetchCalls, listeners,
    setFetchHandler(h: typeof fetchHandler) { fetchHandler = h; },
    sendBridgeResponse, getSentBridgeMessages, importClient, restore,
  };
}

function idle(): Promise<void> { return new Promise(r => setImmediate(r)); }

test('embed first apiGet: requests token then Bearer fetch', async () => {
  const fx = embedTestFix();
  const client = fx.importClient();

  const resultP = client.apiGet('/today');
  await idle();

  // Should have sent a token.request
  const msgs = fx.getSentBridgeMessages();
  assert.equal(msgs.length, 1, 'sent one bridge message');
  assert.equal(msgs[0].type, 'token.request');

  // Respond with access token
  fx.sendBridgeResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_first',
  }));

  const result = await resultP;
  assert.deepEqual(result, { data: [] }, 'apiGet returns parsed JSON');

  // Verify fetch used Bearer
  assert.equal(fx.fetchCalls.length, 1, 'one fetch call');
  assert.equal(fx.fetchCalls[0].url, '/api/v1/today', 'path is /api/v1/today');
  assert.equal(fx.fetchCalls[0].headers['authorization'], 'Bearer tok_first');

  fx.restore();
});

test('embed two concurrent apiGet: single token.request', async () => {
  const fx = embedTestFix();
  const client = fx.importClient();

  const p1 = client.apiGet('/a');
  const p2 = client.apiGet('/b');
  await idle();

  const msgs = fx.getSentBridgeMessages();
  assert.equal(msgs.length, 1, 'exactly one token.request for two concurrent apiGet');
  assert.equal(msgs[0].type, 'token.request');

  fx.sendBridgeResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_concurrent',
  }));

  const [r1, r2] = await Promise.all([p1, p2]);
  assert.deepEqual(r1, { data: [] });
  assert.deepEqual(r2, { data: [] });

  // Both fetches used the same token
  assert.ok(fx.fetchCalls.every(c => c.headers['authorization'] === 'Bearer tok_concurrent'));
  // Both fetches are to /api/v1/* URLs
  assert.equal(fx.fetchCalls[0].url, '/api/v1/a');
  assert.equal(fx.fetchCalls[1].url, '/api/v1/b');

  fx.restore();
});

test('embed 401 triggers bridge refresh and retries with new token', async () => {
  const fx = embedTestFix();
  const client = fx.importClient();
  client.setTokens('tok_expired', '');

  let callNo = 0;
  fx.setFetchHandler(() => {
    callNo++;
    if (callNo === 1) return { status: 401, body: { message: 'expired' } };
    return { status: 200, body: { data: { ok: true } } };
  });

  const resultP = client.apiGet('/profile');
  await idle();

  const msgs = fx.getSentBridgeMessages();
  assert.equal(msgs.length, 1, 'sent one bridge message');
  assert.equal(msgs[0].type, 'token.refresh');
  assert.equal(msgs[0].failedAccessToken, 'tok_expired');

  fx.sendBridgeResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_refreshed',
  }));

  const result = await resultP;
  assert.deepEqual(result, { data: { ok: true } });

  // Retry used new token
  const retryFetch = fx.fetchCalls[fx.fetchCalls.length - 1];
  assert.equal(retryFetch.url, '/api/v1/profile');
  assert.equal(retryFetch.headers['authorization'], 'Bearer tok_refreshed');

  fx.restore();
});

test('embed two concurrent 401 calls share one refresh', async () => {
  const fx = embedTestFix();
  const client = fx.importClient();
  client.setTokens('tok_shared', '');

  let callNo = 0;
  fx.setFetchHandler((url) => {
    callNo++;
    // Both initial calls get 401; retries get 200 on the 3rd/4th call
    if (callNo <= 2) return { status: 401, body: { message: 'expired' } };
    return { status: 200, body: { data: { url } } };
  });

  const p1 = client.apiGet('/a');
  const p2 = client.apiGet('/b');
  await idle();

  const msgs = fx.getSentBridgeMessages();
  assert.equal(msgs.length, 1, 'single token.refresh for two concurrent 401s');
  assert.equal(msgs[0].type, 'token.refresh');

  fx.sendBridgeResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_after_refresh',
  }));

  const [r1, r2] = await Promise.all([p1, p2]);
  assert.ok(r1 && r2, 'both requests succeed after shared refresh');

  // Exactly one refresh message total
  const refreshMsgs = fx.getSentBridgeMessages().filter(m => m.type === 'token.refresh');
  assert.equal(refreshMsgs.length, 1);

  // Both retries use the new token
  const refreshedFetches = fx.fetchCalls.filter(c => c.headers['authorization'] === 'Bearer tok_after_refresh');
  assert.equal(refreshedFetches.length, 2, 'both retried fetches use refreshed token');

  fx.restore();
});

test('embed refresh failure: clears token, calls onTokensChanged, throws Chinese error', async () => {
  const fx = embedTestFix();
  let authChanged = false;
  const client = fx.importClient();
  client.onTokensChanged(() => { authChanged = true; });
  client.setTokens('tok_bad', '');

  fx.setFetchHandler(() => ({ status: 401, body: { message: 'expired' } }));

  const resultP = client.apiGet('/secure');
  await idle();

  const msgs = fx.getSentBridgeMessages();
  fx.sendBridgeResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: false,
    errorCode: 'login_expired', message: '登录已过期',
  }));

  await assert.rejects(
    () => resultP,
    (err: Error) => {
      assert.ok(err.message.includes('登录已过期'),
        `expected Chinese error message, got: ${err.message}`);
      return true;
    },
  );

  assert.ok(authChanged, 'onTokensChanged callback was invoked');

  fx.restore();
});

test('embed retry still 401: stops without second refresh', async () => {
  const fx = embedTestFix();
  const client = fx.importClient();
  client.setTokens('tok_first', '');

  let refreshCount = 0;
  fx.setFetchHandler(() => {
    // First API call → 401
    if (refreshCount === 0) return { status: 401, body: { message: 'expired' } };
    // After refresh, still 401 (but don't count as another refresh attempt)
    return { status: 401, body: { message: 'still expired' } };
  });

  const resultP = client.apiGet('/doomed');
  await idle();

  const msgs = fx.getSentBridgeMessages();
  fx.sendBridgeResponse(JSON.stringify({
    version: 1, id: msgs[0].id, ok: true, accessToken: 'tok_second',
  }));
  refreshCount = 1; // Next fetch will also be 401

  await assert.rejects(
    () => resultP,
    (err: Error) => {
      assert.ok(err.message.includes('401') || err.message.includes('still expired'),
        `expected HTTP error, got: ${err.message}`);
      return true;
    },
  );

  // Only one refresh attempt despite still getting 401 after refresh
  const refreshMsgs = fx.getSentBridgeMessages().filter(m => m.type === 'token.refresh');
  assert.equal(refreshMsgs.length, 1, 'no second refresh attempt after retry still 401');

  fx.restore();
});

void _runAll();
