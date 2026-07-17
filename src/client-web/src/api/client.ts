const BASE = '/api/v1';

let accessToken: string | null = null;
let refreshToken: string | null = null;
let onAuthChange: (() => void) | null = null;
let bridgeClient: import('../embed/androidBridge').AndroidBridgeClient | null | undefined = undefined;
let bridgeInitPromise: Promise<void> | null = null;
let initialTokenPromise: Promise<void> | null = null;
let refreshPromise: Promise<boolean> | null = null;
let tokenProvenance: 'embed' | 'desktop' | null = null;
let generation = 0;

function isEmbed(): boolean {
  try {
    return typeof window !== 'undefined'
      && typeof window.location !== 'undefined'
      && window.location.pathname.startsWith('/embed/android/');
  } catch { return false; }
}

async function getBridgeClient(): Promise<import('../embed/androidBridge').AndroidBridgeClient | null> {
  if (!isEmbed()) return null;
  if (bridgeClient !== undefined) return bridgeClient;
  if (!bridgeInitPromise) {
    bridgeInitPromise = (async () => {
      try {
        const { AndroidBridgeClient } = await import('../embed/androidBridge');
        bridgeClient = new AndroidBridgeClient();
      } catch { bridgeClient = null; }
    })();
  }
  await bridgeInitPromise;
  return bridgeClient!;
}

export function setTokens(access: string, refresh: string) {
  accessToken = access;
  refreshToken = refresh;
  tokenProvenance = isEmbed() ? 'embed' : 'desktop';
  if (!isEmbed()) {
    localStorage.setItem('accessToken', access);
    localStorage.setItem('refreshToken', refresh);
  }
}

export function loadTokens(): boolean {
  if (isEmbed()) {
    accessToken = null;
    refreshToken = null;
    tokenProvenance = null;
    return false;
  }
  accessToken = localStorage.getItem('accessToken');
  refreshToken = localStorage.getItem('refreshToken');
  tokenProvenance = accessToken ? 'desktop' : null;
  return !!accessToken;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  tokenProvenance = null;
  generation++;
  bridgeClient?.setAccessToken(null);
  if (!isEmbed()) {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }
}

export function onTokensChanged(cb: () => void) { onAuthChange = cb; }

export function getEmbedBridgeClient(): Promise<import('../embed/androidBridge').AndroidBridgeClient | null> {
  return getBridgeClient();
}

async function refreshAccessToken(): Promise<boolean> {
  if (isEmbed()) {
    if (refreshPromise) return refreshPromise;
    const savedGen = generation;
    const currentPromise = (async () => {
      try {
        const bridge = await getBridgeClient();
        if (!bridge) {
          if (generation === savedGen) {
            clearTokens();
            onAuthChange?.();
          }
          return false;
        }
        const newToken = await bridge.refreshToken(accessToken ?? undefined);
        if (generation !== savedGen) {
          bridge.setAccessToken(null);
          return false;
        }
        if (newToken) {
          accessToken = newToken;
          return true;
        }
      } catch {
        // bridge transport failure
      }
      if (generation === savedGen) {
        accessToken = null;
        clearTokens();
        onAuthChange?.();
      }
      return false;
    })();
    refreshPromise = currentPromise;
    currentPromise.then(
      () => { if (refreshPromise === currentPromise) refreshPromise = null; },
      () => { if (refreshPromise === currentPromise) refreshPromise = null; },
    );
    return refreshPromise;
  }
  if (!refreshToken) return false;
  const savedGen = generation;
  const savedRefreshToken = refreshToken;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: savedRefreshToken })
    });
    if (!res.ok) return false;
    const json = await res.json();
    const d = json.data;
    if (generation !== savedGen || refreshToken !== savedRefreshToken) {
      return false;
    }
    setTokens(d.accessToken, d.refreshToken);
    return true;
  } catch { return false; }
}

export async function apiGet<T>(path: string): Promise<T> {
  return authedFetch<T>(path);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return authedFetch<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  return authedFetch<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined });
}

export async function apiDelete<T>(path: string): Promise<T> {
  return authedFetch<T>(path, { method: 'DELETE' });
}

export async function apiUpload<T>(path: string, body: BodyInit): Promise<T> {
  return apiFetchRaw<T>(path, { method: 'POST', body });
}

export async function apiDownloadBlob(path: string): Promise<Blob> {
  const res = await apiFetchResponse(path);
  return res.blob();
}

function logApi(method: string, path: string, duration: number, status?: number) {
  const msg = `[API] ${method} ${path} → ${status || '???'} (${duration}ms)`;
  const dev = (import.meta as { env?: { DEV?: boolean } }).env?.DEV;
  if (dev) console.log(msg);
}

export async function authedFetch<T>(path: string, opts: RequestInit = {}, acceptedStatuses?: readonly number[]): Promise<T> {
  return apiFetchRaw<T>(path, opts, true, acceptedStatuses);
}

async function apiFetchRaw<T>(
  path: string,
  opts: RequestInit = {},
  includeJsonContentType = false,
  acceptedStatuses?: readonly number[],
): Promise<T> {
  const res = await apiFetchResponse(path, opts, includeJsonContentType, acceptedStatuses);

  if (res.status === 204 || res.headers.get('content-length') === '0') return undefined as T;
  return res.json();
}

async function apiFetchResponse(
  path: string,
  opts: RequestInit = {},
  includeJsonContentType = false,
  acceptedStatuses?: readonly number[],
): Promise<Response> {
  const start = performance.now();
  const method = opts.method || 'GET';

  if (isEmbed() && tokenProvenance !== 'embed') {
    if (!initialTokenPromise) {
      const savedGen = generation;
      const currentPromise = (async () => {
        const bridge = await getBridgeClient();
        if (!bridge) return;
        if (generation !== savedGen) {
          bridge.setAccessToken(null);
          return;
        }
        accessToken = null;
        bridge.setAccessToken(null);
        const token = await bridge.requestToken();
        if (generation !== savedGen) {
          bridge.setAccessToken(null);
          return;
        }
        if (token) {
          accessToken = token;
          tokenProvenance = 'embed';
        }
      })();
      initialTokenPromise = currentPromise;
      currentPromise.then(
        () => { if (initialTokenPromise === currentPromise) initialTokenPromise = null; },
        () => { if (initialTokenPromise === currentPromise) initialTokenPromise = null; },
      );
    }
    await initialTokenPromise;
  }

  const headers = new Headers(opts.headers);
  if (includeJsonContentType && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  let res = await fetch(`${BASE}${path}`, { ...opts, headers });

  if (res.status === 401) {
    const ok = await refreshAccessToken();
    if (ok) {
      headers.set('Authorization', `Bearer ${accessToken}`);
      res = await fetch(`${BASE}${path}`, { ...opts, headers });
    } else {
      if (!isEmbed()) {
        clearTokens();
        onAuthChange?.();
      }
      throw new Error('登录已过期，请重新登录');
    }
  }

  const elapsed = Math.round(performance.now() - start);
  logApi(method, path, elapsed, res.status);

  if (!res.ok && !(acceptedStatuses?.includes(res.status))) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  return res;
}
