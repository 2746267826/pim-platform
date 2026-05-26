const BASE = '/api/v1';

let accessToken: string | null = null;
let refreshToken: string | null = null;
let onAuthChange: (() => void) | null = null;

export function setTokens(access: string, refresh: string) {
  accessToken = access;
  refreshToken = refresh;
  localStorage.setItem('accessToken', access);
  localStorage.setItem('refreshToken', refresh);
}

export function loadTokens(): boolean {
  accessToken = localStorage.getItem('accessToken');
  refreshToken = localStorage.getItem('refreshToken');
  return !!accessToken;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

export function onTokensChanged(cb: () => void) { onAuthChange = cb; }

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    if (!res.ok) return false;
    const json = await res.json();
    const d = json.data;
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

function logApi(method: string, path: string, duration: number, status?: number) {
  const msg = `[API] ${method} ${path} → ${status || '???'} (${duration}ms)`;
  if (import.meta.env.DEV) console.log(msg);
}

async function authedFetch<T>(path: string, opts: RequestInit = {}): Promise<T> {
  return apiFetchRaw<T>(path, opts, true);
}

async function apiFetchRaw<T>(
  path: string,
  opts: RequestInit = {},
  includeJsonContentType = false,
): Promise<T> {
  const start = performance.now();
  const method = opts.method || 'GET';
  const headers = new Headers(opts.headers);
  if (includeJsonContentType && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  let res = await fetch(`${BASE}${path}`, { ...opts, headers });

  if (res.status === 401 && refreshToken) {
    const ok = await refreshAccessToken();
    if (ok) {
      headers.set('Authorization', `Bearer ${accessToken}`);
      res = await fetch(`${BASE}${path}`, { ...opts, headers });
    } else {
      clearTokens();
      onAuthChange?.();
      throw new Error('Session expired');
    }
  }

  const elapsed = Math.round(performance.now() - start);
  logApi(method, path, elapsed, res.status);

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') return undefined as T;
  return res.json();
}
