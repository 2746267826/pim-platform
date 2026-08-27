import { toast } from 'sonner';

const lastToastAt = new Map<string, number>();
const DEDUPE_MS = 3000;
const MAX_DEDUPE_KEYS = 100;

function pruneDedupeMap() {
  if (lastToastAt.size <= MAX_DEDUPE_KEYS) return;
  const sorted = [...lastToastAt.entries()].sort((a, b) => a[1] - b[1]);
  const toRemove = lastToastAt.size - MAX_DEDUPE_KEYS;
  for (let i = 0; i < toRemove; i++) lastToastAt.delete(sorted[i][0]);
}

export function showApiError(error: unknown, opts?: { onRetry?: () => void; dedupeKey?: string }) {
  if (isSilentError(error)) return;
  const message = parseErrorMessage(error);
  const dedupeKey = opts?.dedupeKey ?? message;
  const now = Date.now();
  const last = lastToastAt.get(dedupeKey) ?? 0;
  if (now - last < DEDUPE_MS) return;
  lastToastAt.set(dedupeKey, now);
  pruneDedupeMap();

  const description = getDescriptionForMessage(message);
  toast.error(message, {
    id: dedupeKey,
    description,
    duration: 6000,
    ...(opts?.onRetry
      ? { action: { label: '重试', onClick: opts.onRetry } }
      : {}),
  });
}

function isSilentError(error: unknown): boolean {
  if (error instanceof DOMException && error.name === 'AbortError') return true;
  if (error instanceof Error) {
    const msg = error.message ?? '';
    if (msg.includes('AbortError')) return true;
    // 401 由 AuthContext 统一处理跳转到登录，不需要额外 toast 避免重复提示
    // 但为避免静默失败，仍允许在非 api/client 的 401 场景下提示，此处不过滤，由调用方决定
  }
  return false;
}

function getDescriptionForMessage(message: string): string {
  if (message === '登录已过期，请重新登录') return '请重新登录后继续操作';
  if (message === '无权限访问') return '请联系管理员获取权限';
  if (message === '请求的资源不存在') return '请检查请求地址是否正确';
  return '请检查网络连接后重试';
}

export function parseErrorMessage(error: unknown): string {
  if (!error) return '未知错误';
  if (error instanceof TypeError && error.message.includes('fetch')) return '网络连接异常';
  if (error instanceof DOMException && error.name === 'AbortError') return '请求超时';

  // 支持结构化错误对象 { status, message, detail }（如 ProblemDetails）
  if (typeof error === 'object' && error !== null) {
    const maybe = error as { status?: number; code?: string; message?: string; detail?: string; title?: string };
    if (typeof maybe.status === 'number') {
      if (maybe.status === 401) return '登录已过期，请重新登录';
      if (maybe.status === 403) return '无权限访问';
      if (maybe.status === 404) return '请求的资源不存在';
      if (maybe.status >= 500) return '服务器异常，请稍后重试';
    }
    if (typeof maybe.message === 'string' && maybe.message.trim()) {
      // 优先用结构化 message 再走字符串匹配
      const structured = maybe.message;
      const parsed = parseMessageString(structured);
      if (parsed !== structured) return parsed;
      // 若 message 是原始后端文案，直接返回但做脱敏截断
      return truncate(structured);
    }
  }

  if (error instanceof Error) {
    const msg = error.message ?? '';
    if (!msg) return '未知错误';
    const parsed = parseMessageString(msg);
    if (parsed !== msg) return parsed;
    return truncate(msg);
  }
  if (typeof error === 'string') {
    if (!error.trim()) return '未知错误';
    const parsed = parseMessageString(error);
    if (parsed !== error) return parsed;
    return truncate(error);
  }
  return '服务器异常，请稍后重试';
}

function parseMessageString(msg: string): string {
  if (!msg) return msg;
  if (msg.includes('fetch') || msg.includes('Failed to fetch') || msg.includes('NetworkError')) return '网络连接异常';
  if (msg.includes('AbortError') || /\btimeout\b/i.test(msg) || msg.includes('超时')) return '请求超时';
  if (/\b401\b/.test(msg) || msg.includes('登录已过期') || /\bUnauthorized\b/.test(msg)) return '登录已过期，请重新登录';
  if (/\b403\b/.test(msg) || /\bForbidden\b/.test(msg)) return '无权限访问';
  if (/\b404\b/.test(msg)) return '请求的资源不存在';
  if (/\b(500|502|503|504)\b/.test(msg)) return '服务器异常，请稍后重试';
  if (/\bHTTP\s*5\d{2}\b/.test(msg)) return '服务器异常，请稍后重试';
  return msg;
}

function truncate(msg: string, max = 120): string {
  const t = msg.trim();
  if (t.length <= max) return t;
  return `${t.slice(0, max)}…`;
}
