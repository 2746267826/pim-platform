import { toast } from 'sonner';

export function showApiError(error: unknown) {
  const message = parseErrorMessage(error);
  toast.error(message, {
    description: '请检查网络连接后重试',
    duration: 6000,
    action: {
      label: '重试',
      onClick: () => window.location.reload(),
    },
  });
}

export function parseErrorMessage(error: unknown): string {
  if (!error) return '未知错误';
  if (error instanceof TypeError && error.message.includes('fetch')) return '网络连接异常';
  if (error instanceof DOMException && error.name === 'AbortError') return '请求超时';
  if (error instanceof Error) {
    const msg = error.message ?? '';
    if (!msg) return '未知错误';
    if (msg.includes('fetch') || msg.includes('Failed to fetch') || msg.includes('NetworkError')) return '网络连接异常';
    if (msg.includes('AbortError') || msg.includes('timeout') || msg.includes('超时')) return '请求超时';
    if (msg.includes('401') || msg.includes('登录已过期') || msg.includes('Unauthorized')) return '登录已过期，请重新登录';
    if (msg.includes('403')) return '无权限访问';
    if (msg.includes('404')) return '请求的资源不存在';
    if (msg.includes('500') || msg.includes('502') || msg.includes('503') || msg.includes('504')) return '服务器异常，请稍后重试';
    if (msg.includes('HTTP')) return '服务器异常，请稍后重试';
    return msg;
  }
  if (typeof error === 'string') {
    if (!error.trim()) return '未知错误';
    return error;
  }
  return '服务器异常，请稍后重试';
}
