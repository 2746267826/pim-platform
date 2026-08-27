import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider, QueryCache, MutationCache } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import App from './App'
import './index.css'
import { installInteractionDeferral } from './lib/autoRefresh'
import { showApiError } from './components/error/ApiErrorToast'

function shouldRetry(failureCount: number, error: unknown): boolean {
  if (failureCount >= 2) return false;
  if (error instanceof DOMException && error.name === 'AbortError') return false;
  // 支持结构化错误对象 { status: 404 } 直接判定 4xx 不重试
  if (typeof error === 'object' && error !== null) {
    const maybe = error as { status?: number };
    if (typeof maybe.status === 'number' && maybe.status >= 400 && maybe.status < 500) return false;
  }
  const msg = error instanceof Error ? error.message ?? '' : String(error ?? '');
  if (/\b(400|401|403|404|422)\b/.test(msg) || msg.includes('登录已过期') || msg.includes('Unauthorized')) return false;
  if (msg.includes('AbortError')) return false;
  return true;
}

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => {
      // 展览馆 query 已在 hook 内静默回退到 fakeData，不需要全局 toast（避免 9 卡洪水）
      const key = query?.queryKey?.[0];
      if (key === 'exh') return;
      showApiError(error);
    },
  }),
  mutationCache: new MutationCache({
    onError: (error, _variables, _context, mutation) => {
      // mutation 失败不做整页 reload，仅提示；action 由具体 mutation 的 onError 另行提供可重试回调
      const key = mutation?.options?.mutationKey ? String(mutation.options.mutationKey) : undefined;
      showApiError(error, { dedupeKey: key ? `mut:${key}` : undefined });
    },
  }),
  defaultOptions: {
    queries: {
      retry: shouldRetry,
      retryDelay: 1000,
      refetchIntervalInBackground: false,
      structuralSharing: true,
    },
  },
})

installInteractionDeferral()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
      <Toaster position="top-right" richColors closeButton toastOptions={{ className: 'pim-toast' }} />
    </QueryClientProvider>
  </React.StrictMode>
)
