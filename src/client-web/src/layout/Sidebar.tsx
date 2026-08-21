import { useRef, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getCalendars, createCalendar, updateCalendar, deleteCalendar, previewCalendarDelete } from '../api/calendar';
import { useAuth } from '../auth/AuthContext';
import { useCalendarVisibility } from '../context/CalendarVisibilityContext';
import SidebarStatusIndicator from '../components/status/SidebarStatusIndicator';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
import { NAV_ITEMS } from './navItems';

export const primaryNavItems = NAV_ITEMS;

function CalendarBookSection({
  title,
  books,
  queryKey,
  kind,
}: {
  title: string;
  books: Array<{ id: string; name: string; color: string }>;
  queryKey: string[];
  kind: string;
}) {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');
  const [newName, setNewName] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const activeDeletePreviewRequestRef = useRef<{ deleteId: string; requestId: number } | null>(null);
  const nextDeletePreviewRequestIdRef = useRef(0);
  const { hiddenCalendarIds, toggleCalendar } = useCalendarVisibility();

  const createMut = useMutation({
    mutationFn: (data: { name: string; color?: string; kind?: string }) => createCalendar(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setNewName('');
      setShowNew(false);
    }
  });

  const updateMut = useMutation({
    mutationFn: ({ id, data }: { id: string; data: { name?: string; color?: string } }) => updateCalendar(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setEditingId(null);
    }
  });

  const deleteMut = useMutation({
    mutationFn: deleteCalendar,
    onSuccess: () => {
      const affectedQueryKeys: string[][] = [
        queryKey,
        ['calendars'],
        ['calendar-recycle-bin'],
        ['events'],
        ['events-paged'],
        ['tasks'],
        ['today-sections'],
        ['today-section'],
      ];

      affectedQueryKeys.forEach(key => {
        queryClient.invalidateQueries({ queryKey: key });
      });

      activeDeletePreviewRequestRef.current = null;
      setDeleteInput(null);
      setDeleteId(null);
      setDeleteError(null);
    },
    onError: () => {
      activeDeletePreviewRequestRef.current = null;
      setDeleteInput(null);
      setDeleteId(null);
      setDeleteError('删除失败，请稍后重试。');
    }
  });

  const previewDeleteMut = useMutation({
    mutationFn: previewCalendarDelete,
  });

  function startRename(id: string, currentName: string) {
    setEditingId(id);
    setEditName(currentName);
  }

  function submitRename(id: string) {
    if (editName.trim()) updateMut.mutate({ id, data: { name: editName.trim() } });
  }

  function isActiveDeletePreviewRequest(id: string, requestId: number) {
    return activeDeletePreviewRequestRef.current?.deleteId === id
      && activeDeletePreviewRequestRef.current.requestId === requestId;
  }

  function requestDeletePreview(id: string) {
    const requestId = nextDeletePreviewRequestIdRef.current + 1;

    nextDeletePreviewRequestIdRef.current = requestId;
    activeDeletePreviewRequestRef.current = { deleteId: id, requestId };
    setDeleteId(id);
    setDeleteInput(null);
    setDeleteError(null);
    previewDeleteMut.mutate(id, {
      onSuccess: preview => {
        if (isActiveDeletePreviewRequest(id, requestId)) {
          setDeleteInput({
            targetType: preview.targetType,
            title: preview.title,
            affectedCount: Math.max(1, preview.affectedCount),
            samples: preview.samples,
          });
        }
      },
      onError: () => {
        if (isActiveDeletePreviewRequest(id, requestId)) {
          activeDeletePreviewRequestRef.current = null;
          setDeleteInput(null);
          setDeleteId(null);
          setDeleteError('删除预览失败，请稍后重试。');
        }
      },
    });
  }

  function cancelDelete() {
    activeDeletePreviewRequestRef.current = null;
    setDeleteInput(null);
    setDeleteId(null);
  }

  function confirmDelete() {
    if (deleteId) deleteMut.mutate(deleteId);
  }

  return (
    <div className="mt-4 border-t border-slate-200/80 pt-4">
      <div className="mb-2 flex items-center justify-between px-2">
        <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">{title}</p>
        <button
          onClick={() => setShowNew(!showNew)}
          className="h-6 w-6 rounded-full text-sm leading-none text-slate-400 transition-colors hover:bg-blue-50 hover:text-blue-600"
          aria-label={`新建${title}`}
        >
          +
        </button>
      </div>

      {showNew && (
        <div className="px-2 mb-2 flex gap-1">
          <input
            type="text" placeholder={`${title}名称`}
            value={newName}
            onChange={e => setNewName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && newName.trim()) createMut.mutate({ name: newName.trim(), kind }); }}
            className="min-w-0 flex-1 rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 outline-none transition-colors focus:border-blue-400"
            autoFocus
          />
          <button
            onClick={() => newName.trim() && createMut.mutate({ name: newName.trim(), kind })}
            disabled={createMut.isPending}
            className="rounded-lg bg-blue-600 px-2 py-1 text-xs text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
          >
            确定
          </button>
        </div>
      )}

      {deleteError && (
        <p className="px-2 pb-1 text-xs text-red-500">{deleteError}</p>
      )}

      {books?.map(book => {
        const hidden = hiddenCalendarIds.has(book.id);
        const deleteDisabled = previewDeleteMut.isPending || deleteMut.isPending;
        return (
          <div key={book.id} className={`group flex items-center gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-slate-100 ${hidden ? 'opacity-45' : ''}`}>
            <button
              onClick={() => toggleCalendar(book.id)}
              className="h-5 w-5 rounded-full border border-slate-200 text-[10px] leading-none text-slate-400 transition-colors hover:border-blue-300 hover:text-blue-600 flex-shrink-0"
              title={hidden ? '显示' : '隐藏'}
            >
              {hidden ? '○' : '●'}
            </button>
            <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: book.color }} />
            {editingId === book.id ? (
              <input
                type="text" value={editName}
                onChange={e => setEditName(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') submitRename(book.id); if (e.key === 'Escape') setEditingId(null); }}
                onBlur={() => submitRename(book.id)}
                className="min-w-0 flex-1 rounded border border-slate-200 bg-white px-1 py-0.5 text-xs text-slate-700 outline-none focus:border-blue-400"
                autoFocus
              />
            ) : (
              <span
                className="flex-1 truncate text-xs text-slate-600 cursor-pointer"
                onDoubleClick={() => startRename(book.id, book.name)}
                title="双击重命名"
              >
                {book.name}
              </span>
            )}
            <div className="hidden group-hover:flex items-center gap-0.5">
              <button
                onClick={() => startRename(book.id, book.name)}
                className="rounded px-1 text-xs leading-none text-slate-400 hover:bg-blue-50 hover:text-blue-600"
                title="重命名"
              >
                ✎
              </button>
              <button
                onClick={() => requestDeletePreview(book.id)}
                disabled={deleteDisabled}
                className="rounded px-1 text-xs leading-none text-slate-400 hover:bg-red-50 hover:text-red-500 disabled:cursor-not-allowed disabled:opacity-50"
                title="删除"
              >
                ✕
              </button>
            </div>
          </div>
        );
      })}

      {(!books || books.length === 0) && !showNew && (
        <p className="px-2 py-1 text-xs text-slate-400">暂无{title}，点击 + 创建</p>
      )}

      <ConfirmActionDialog
        open={deleteInput !== null}
        input={deleteInput}
        isPending={deleteMut.isPending}
        onCancel={cancelDelete}
        onConfirm={confirmDelete}
      />
    </div>
  );
}

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, username } = useAuth();

  const { data: calendars = [] } = useQuery({
    queryKey: ['calendars', 'calendar'],
    queryFn: () => getCalendars('calendar')
  });

  const { data: taskBooks = [] } = useQuery({
    queryKey: ['calendars', 'task'],
    queryFn: () => getCalendars('task')
  });

  return (
    <aside className="flex h-full w-[220px] flex-col border-r border-slate-200/80 bg-white/90">
      <div className="px-4 py-5">
        <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">PIM</p>
        <p className="mt-1 text-lg font-semibold text-slate-950">个人中枢</p>
      </div>
      <SidebarStatusIndicator />

      <nav className="flex-1 space-y-1 overflow-auto px-3 pb-3">
        {primaryNavItems.map(item => {
          const active = location.pathname === item.path || location.pathname.startsWith(`${item.path}/`);

          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              aria-current={active ? 'page' : undefined}
              className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition-colors ${
                active
                  ? 'bg-blue-50 text-blue-700 shadow-[inset_0_0_0_1px_rgba(37,99,235,0.12)]'
                  : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950'
              }`}
            >
              <span aria-hidden="true" className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs font-semibold ${
                active ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-500'
              }`}>
                {item.short}
              </span>
              <span>{item.label}</span>
            </button>
          );
        })}

        <CalendarBookSection
          title="日历本"
          books={calendars}
          queryKey={['calendars']}
          kind="calendar"
        />

        <CalendarBookSection
          title="任务本"
          books={taskBooks}
          queryKey={['calendars']}
          kind="task"
        />
      </nav>

      <div className="flex items-center justify-between border-t border-slate-200/80 p-3">
        <div className="min-w-0">
          <span className="truncate text-xs text-slate-500">{username}</span>
          <p className="mt-1 truncate text-[10px] text-slate-400" title={__APP_VERSION__}>
            {__APP_VERSION__}
          </p>
        </div>
        <button onClick={logout} className="rounded-lg px-2 py-1 text-xs text-slate-500 hover:bg-red-50 hover:text-red-500">退出</button>
      </div>
    </aside>
  );
}
