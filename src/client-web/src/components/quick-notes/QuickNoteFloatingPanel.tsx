import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { createQuickNote } from '../../api/quickNotes';
import QuickNoteEditor from './QuickNoteEditor';
import {
  QUICK_NOTE_DRAFT_KEY,
  clampPanelPosition,
  loadPanelPosition,
  savePanelPosition,
  type PanelPoint,
  type PanelSize,
} from './quickNoteFloatingState';

const PANEL_SIZE: PanelSize = { width: 380, height: 460 };

interface QuickNoteFloatingPanelProps {
  onClose: () => void;
}

function getViewportSize(): PanelSize {
  return {
    width: window.innerWidth,
    height: window.innerHeight,
  };
}

function loadDraft() {
  try {
    return localStorage.getItem(QUICK_NOTE_DRAFT_KEY) ?? '';
  } catch {
    return '';
  }
}

export default function QuickNoteFloatingPanel({ onClose }: QuickNoteFloatingPanelProps) {
  const queryClient = useQueryClient();
  const [markdown, setMarkdown] = useState(loadDraft);
  const [position, setPosition] = useState<PanelPoint>(() => loadPanelPosition(getViewportSize(), PANEL_SIZE));
  const [error, setError] = useState<string | null>(null);
  const positionRef = useRef(position);
  const dragRef = useRef<{ pointerId: number; offsetX: number; offsetY: number } | null>(null);

  useEffect(() => {
    positionRef.current = position;
  }, [position]);

  useEffect(() => {
    const nextPosition = clampPanelPosition(positionRef.current, getViewportSize(), PANEL_SIZE);
    positionRef.current = nextPosition;
    setPosition(nextPosition);
  }, []);

  useEffect(() => {
    try {
      if (markdown) {
        localStorage.setItem(QUICK_NOTE_DRAFT_KEY, markdown);
      } else {
        localStorage.removeItem(QUICK_NOTE_DRAFT_KEY);
      }
    } catch {
      // Draft persistence is best-effort.
    }
  }, [markdown]);

  useEffect(() => {
    function handleResize() {
      setPosition(current => {
        const nextPosition = clampPanelPosition(current, getViewportSize(), PANEL_SIZE);
        positionRef.current = nextPosition;
        savePanelPosition(nextPosition);
        return nextPosition;
      });
    }

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const saveMutation = useMutation({
    mutationFn: (contentMarkdown: string) => createQuickNote({ contentMarkdown, source: 'web-floating' }),
    onSuccess: () => {
      setMarkdown('');
      setError(null);
      try {
        localStorage.removeItem(QUICK_NOTE_DRAFT_KEY);
      } catch {
        // Clearing the draft is best-effort.
      }
      queryClient.invalidateQueries({ queryKey: ['quick-notes'] });
    },
    onError: () => {
      setError('保存失败，请稍后重试。');
    },
  });

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = {
      pointerId: event.pointerId,
      offsetX: event.clientX - positionRef.current.x,
      offsetY: event.clientY - positionRef.current.y,
    };
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }

    const nextPosition = clampPanelPosition(
      {
        x: event.clientX - drag.offsetX,
        y: event.clientY - drag.offsetY,
      },
      getViewportSize(),
      PANEL_SIZE,
    );
    positionRef.current = nextPosition;
    setPosition(nextPosition);
  }

  function handlePointerUp(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) {
      return;
    }

    dragRef.current = null;
    event.currentTarget.releasePointerCapture(event.pointerId);
    savePanelPosition(positionRef.current);
  }

  function handleSave() {
    const trimmedMarkdown = markdown.trim();
    if (!trimmedMarkdown || saveMutation.isPending) {
      return;
    }

    saveMutation.mutate(markdown);
  }

  return (
    <section
      aria-label="快速记录"
      className="fixed z-50 flex max-h-[calc(100vh-24px)] flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-2xl shadow-slate-900/20"
      style={{
        left: position.x,
        top: position.y,
        width: 'min(380px, calc(100vw - 24px))',
        height: 'min(460px, calc(100vh - 24px))',
      }}
    >
      <div
        className="flex cursor-move touch-none select-none items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-3 py-2"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
      >
        <h2 className="truncate text-sm font-semibold text-slate-800">快速记录</h2>
        <button
          type="button"
          aria-label="关闭快速记录"
          title="关闭快速记录"
          onClick={onClose}
          onPointerDown={event => event.stopPropagation()}
          className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-xl leading-none text-slate-500 hover:bg-slate-200 hover:text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-200"
        >
          ×
        </button>
      </div>
      <div className="flex min-h-0 flex-1 flex-col gap-3 p-3">
        <div className="min-h-0 flex-1 overflow-auto">
          <QuickNoteEditor value={markdown} onChange={setMarkdown} minHeight={300} />
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <div className="flex items-center justify-end gap-2 border-t border-slate-100 pt-3">
          <button
            type="button"
            onClick={handleSave}
            disabled={!markdown.trim() || saveMutation.isPending}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {saveMutation.isPending ? '保存中...' : '保存'}
          </button>
        </div>
      </div>
    </section>
  );
}
