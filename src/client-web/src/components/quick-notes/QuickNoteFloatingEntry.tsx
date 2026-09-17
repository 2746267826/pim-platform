import { lazy, Suspense, useState } from 'react';

// 面板内含 Markdown 编辑器（体积大），懒加载以保持它不进入应用入口 bundle。
const LazyQuickNoteGlobalPanel = lazy(() => import('./QuickNoteGlobalPanel'));

/**
 * 已有自己右下角悬浮按钮（黑色 +）的页面：这些路径不再渲染全局入口，
 * 否则会出现两个按钮重叠（#280 的现象）。
 */
export const PAGE_FAB_PATHS: readonly string[] = ['/quick-notes'];

interface QuickNoteFloatingEntryProps {
  /** 当前路由路径；命中 PAGE_FAB_PATHS 时不渲染入口。 */
  pathname?: string;
}

/**
 * 全站快速记录入口（#280）：
 * - 只有**一个**悬浮按钮，样式与快速记录页内的黑色按钮一致（不再是旧的蓝色按钮）；
 * - 点击打开可拖动的编辑卡片；草稿保留与位置记忆沿用 quickNoteFloatingState；
 * - 页面自己已有 FAB 时（/quick-notes）不渲染，避免两个按钮重叠。
 */
export function QuickNoteFloatingEntry({ pathname }: QuickNoteFloatingEntryProps) {
  const [open, setOpen] = useState(false);
  const currentPath = pathname ?? (typeof window === 'undefined' ? '' : window.location.pathname);

  if (PAGE_FAB_PATHS.includes(currentPath)) {
    return null;
  }

  return (
    <>
      <button
        type="button"
        aria-label="打开快速记录"
        title="打开快速记录"
        onClick={() => setOpen(true)}
        className="fixed bottom-6 right-6 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-zinc-900 text-white shadow-lg transition-transform hover:scale-105 hover:bg-zinc-800 focus:outline-none focus:ring-4 focus:ring-zinc-300"
      >
        <span aria-hidden="true" className="text-2xl font-light leading-none">+</span>
      </button>
      {open && (
        <Suspense fallback={null}>
          <LazyQuickNoteGlobalPanel onClose={() => setOpen(false)} />
        </Suspense>
      )}
    </>
  );
}

export default QuickNoteFloatingEntry;
