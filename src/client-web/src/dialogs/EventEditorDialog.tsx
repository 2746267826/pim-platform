import { useState, useRef, useId, useEffect, type FormEvent, type KeyboardEvent } from 'react';
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { createEvent, updateEvent, deleteEvent, getCalendars, writeOutlookEvent } from '../api/calendar';
import EditorDrawer from '../ui/EditorDrawer';
import ConfirmActionDialog, { type DeleteConfirmationInput } from '../ui/ConfirmActionDialog';
import BeforeAfterDiff from '../components/schedule/BeforeAfterDiff';
import { Field } from './common';
import { useCalendarVisibility } from '../context/CalendarVisibilityContext';
import { resolveCalendarId, hasWritableCalendar, noWritableCalendarMessage } from '../utils/calendarSelection';
import { isoToDatetimeLocal, datetimeLocalToUtcIso, isEndAfterStart, minimumEndValue } from '../utils/dateTimeInput';
import { looksLikeHtml, sanitizeDescriptionHtml } from '../utils/safeHtml';
import type { EventResponse, OutlookWriteRequest, OutlookEventDraft } from '../types';

interface Props {
  open: boolean;
  onClose: () => void;
  event?: EventResponse;
  defaultStart?: string;
  defaultEnd?: string;
}

export default function EventEditorDialog(props: Props) {
  const formKey = [
    props.open ? 'open' : 'closed',
    props.event?.id || 'new',
    props.defaultStart || 'none',
    props.defaultEnd || 'none',
  ].join(':');

  return <EventEditorForm key={formKey} {...props} />;
}

type WritebackPhase =
  | { type: 'idle' }
  | { type: 'preview' }
  | { type: 'submitting' }
  | { type: 'conflict'; latestOutlookJson: string; latestEtag?: string | null; errorMessage?: string | null }
  | { type: 'error'; message: string };

function buildEventJson(event: EventResponse): string {
  return JSON.stringify({
    calendarId: event.calendarId,
    title: event.title,
    description: event.description,
    location: event.location,
    dtStart: event.dtStart,
    dtEnd: event.dtEnd,
    isAllDay: Boolean(event.isAllDay),
    timeZoneId: event.timeZoneId,
  }, null, 2);
}

function buildDraftJson(draft: OutlookEventDraft): string {
  return JSON.stringify({
    calendarId: draft.calendarId,
    title: draft.title,
    description: draft.description,
    location: draft.location,
    dtStart: draft.dtStart,
    dtEnd: draft.dtEnd,
    isAllDay: draft.isAllDay,
    timeZoneId: draft.timeZoneId,
  }, null, 2);
}

function buildDeleteAfterJson(): string {
  return JSON.stringify({ '操作': '删除此 Outlook 日程' }, null, 2);
}

function EventEditorForm({ open, onClose, event, defaultStart, defaultEnd }: Props) {
  const [title, setTitle] = useState(event?.title || '');
  const [description, setDescription] = useState(event?.description || '');
  const [location, setLocation] = useState(event?.location || '');
  const [dtStart, setDtStart] = useState(event ? isoToDatetimeLocal(event.dtStart, event.timeZoneId) : (defaultStart || ''));
  const [dtEnd, setDtEnd] = useState(event ? isoToDatetimeLocal(event.dtEnd, event.timeZoneId) : (defaultEnd || ''));
  const [isAllDay, setIsAllDay] = useState(Boolean(event?.isAllDay));
  const [calendarId, setCalendarId] = useState(event?.calendarId || '');
  const [deleteInput, setDeleteInput] = useState<DeleteConfirmationInput | null>(null);
  const queryClient = useQueryClient();
  const { hiddenCalendarIds } = useCalendarVisibility();

  const { data: calendars, isLoading } = useQuery({
    queryKey: ['calendars', 'calendar'],
    queryFn: () => getCalendars('calendar'),
    enabled: open
  });

  const showHtmlPreview = event && looksLikeHtml(event.description || '');
  const sanitizedPreviewHtml = showHtmlPreview ? sanitizeDescriptionHtml(event.description || '') : '';

  const [writebackPhase, setWritebackPhase] = useState<WritebackPhase>({ type: 'idle' });
  const [pendingRequest, setPendingRequest] = useState<OutlookWriteRequest | null>(null);
  const [outlookScope, setOutlookScope] = useState<'instance' | 'series'>(() =>
    event?.outlookEventType === 'seriesMaster' ? 'series' : 'instance',
  );
  const [diffBefore, setDiffBefore] = useState('{}');
  const [diffAfter, setDiffAfter] = useState('{}');
  const [writebackValidationError, setWritebackValidationError] = useState('');

  const selectedCalendarId = resolveCalendarId(
    calendars || [],
    calendarId || (event ? event.calendarId : undefined),
    hiddenCalendarIds,
  );

  const isOutlookExisting = event?.source === 'outlook' && !!event?.outlookCalendarBindingId;
  const selectedCalendar = calendars?.find(c => c.id === selectedCalendarId);
  const isNewOutlook = !event && !!selectedCalendar?.outlookCalendarBindingId;
  const isOutlook = isOutlookExisting || isNewOutlook;

  const eventCalendar = event ? calendars?.find(c => c.id === event.calendarId) : undefined;
  const isReadOnly = event ? (eventCalendar?.canEdit === false) : (selectedCalendar?.canEdit === false);
  const isFormDisabled = isReadOnly;

  const showScopeRadio = !!event && (event.outlookEventType === 'occurrence' || event.outlookEventType === 'exception' || event.outlookEventType === 'seriesMaster');

  function getCalendarBindingId(): string {
    return selectedCalendar?.outlookCalendarBindingId || event?.outlookCalendarBindingId || '';
  }

  function buildDraft(): OutlookEventDraft {
    return {
      calendarId: selectedCalendarId || event?.calendarId || '',
      title,
      description: description || undefined,
      location: location || undefined,
      dtStart: datetimeLocalToUtcIso(dtStart, event?.timeZoneId),
      dtEnd: datetimeLocalToUtcIso(dtEnd, event?.timeZoneId),
      isAllDay,
      timeZoneId: event?.timeZoneId || undefined,
      uid: event?.uid || undefined,
    };
  }

  function buildWritebackRequest(operation: 'create' | 'update' | 'delete'): OutlookWriteRequest {
    const req: OutlookWriteRequest = {
      operation,
      calendarBindingId: getCalendarBindingId(),
      scope: operation === 'create' ? 'instance' : outlookScope,
      clientOperationId: crypto.randomUUID(),
    };
    if (event) {
      req.eventId = event.id;
    }
    if (operation !== 'delete') {
      req.draft = buildDraft();
    }
    if (operation !== 'create') {
      req.expectedEtag = event?.outlookEtag || undefined;
    }
    return req;
  }

  function openWritebackPreview(operation: 'create' | 'update' | 'delete') {
    const req = buildWritebackRequest(operation);
    let before = '{}';
    let after: string;
    if (operation === 'create') {
      after = req.draft ? buildDraftJson(req.draft) : '{}';
    } else if (operation === 'delete') {
      before = event ? buildEventJson(event) : '{}';
      after = buildDeleteAfterJson();
    } else {
      before = event ? buildEventJson(event) : '{}';
      after = req.draft ? buildDraftJson(req.draft) : '{}';
    }
    setDiffBefore(before);
    setDiffAfter(after);
    setPendingRequest(req);
    setWritebackPhase({ type: 'preview' });
  }

  function invalidateWritebackQueries(operation: string) {
    queryClient.invalidateQueries({ queryKey: ['events'] });
    queryClient.invalidateQueries({ queryKey: ['events-paged'] });
    queryClient.invalidateQueries({ queryKey: ['calendars'] });
    queryClient.invalidateQueries({ queryKey: ['calendar-layers'] });
    queryClient.invalidateQueries({ queryKey: ['workbench-calendar-layers'] });
    queryClient.invalidateQueries({ queryKey: ['outlook-sync-batches'] });
    if (operation === 'delete') {
      queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
    }
  }

  async function confirmWriteback() {
    if (!pendingRequest) return;
    setWritebackPhase({ type: 'submitting' });
    try {
      const requestScope = pendingRequest.operation === 'create' ? 'instance' : outlookScope;
      const result = await writeOutlookEvent({ ...pendingRequest, scope: requestScope });
      const status = result.status;
      if (status === 'created' || status === 'updated' || status === 'deleted') {
        invalidateWritebackQueries(pendingRequest.operation);
        setWritebackPhase({ type: 'idle' });
        onClose();
        return;
      }
      if (status === 'conflict') {
        setDiffBefore(result.latestOutlookJson || '{}');
        setWritebackPhase({
          type: 'conflict',
          latestOutlookJson: result.latestOutlookJson || '{}',
          latestEtag: result.latestEtag,
          errorMessage: result.errorMessage,
        });
        return;
      }
      setWritebackPhase({ type: 'error', message: result.errorMessage || `操作失败：${status}` });
    } catch (e) {
      setWritebackPhase({ type: 'error', message: e instanceof Error ? e.message : '未知错误' });
    }
  }

  function retryWithLatest() {
    if (writebackPhase.type !== 'conflict' || !writebackPhase.latestEtag) return;
    if (!pendingRequest) return;
    setPendingRequest({ ...pendingRequest, expectedEtag: writebackPhase.latestEtag });
    setDiffBefore(writebackPhase.latestOutlookJson);
    setWritebackPhase({ type: 'preview' });
  }

  function cancelWriteback() {
    setWritebackPhase({ type: 'idle' });
  }

  const createMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => createEvent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      onClose();
    }
  });

  function invalidateEventDeleteQueries() {
    queryClient.invalidateQueries({ queryKey: ['events'] });
    queryClient.invalidateQueries({ queryKey: ['events-paged'] });
    queryClient.invalidateQueries({ queryKey: ['calendars'] });
    queryClient.invalidateQueries({ queryKey: ['calendar-recycle-bin'] });
  }

  const updateMut = useMutation({
    mutationFn: (data: Partial<EventResponse>) => updateEvent(event!.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] });
      queryClient.invalidateQueries({ queryKey: ['events-paged'] });
      queryClient.invalidateQueries({ queryKey: ['calendars'] });
      onClose();
    }
  });

  const deleteMut = useMutation({
    mutationFn: () => deleteEvent(event!.id),
    onSuccess: () => {
      invalidateEventDeleteQueries();
      setDeleteInput(null);
      onClose();
    },
    onError: () => setDeleteInput(null),
  });

  const mutationError = createMut.error || updateMut.error || deleteMut.error;
  const mutationErrorMessage = mutationError instanceof Error ? mutationError.message : null;

  function handleDelete() {
    if (!event) return;
    if (isOutlookExisting) {
      if (writebackPhase.type !== 'idle') return;
      if (!event.outlookEtag) {
        setWritebackValidationError('缺少版本标识，无法执行写回操作。');
        return;
      }
      openWritebackPreview('delete');
    } else {
      deleteMut.reset();
      setDeleteInput({
        targetType: 'event',
        title: event.title,
        affectedCount: 1,
        samples: [{
          id: event.id,
          type: 'event',
          title: event.title,
          start: event.dtStart,
          end: event.dtEnd,
        }],
      });
    }
  }

  function confirmDelete() {
    if (!event) return;
    deleteMut.mutate();
  }

  function cancelDelete() {
    if (deleteMut.isPending) return;
    setDeleteInput(null);
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (isReadOnly) return;

    if (!title.trim()) {
      setWritebackValidationError('请输入标题');
      return;
    }
    if (!dtStart || !dtEnd) {
      setWritebackValidationError('请选择开始和结束时间');
      return;
    }
    if (!event && !hasWritableCalendar(calendars || [], hiddenCalendarIds)) {
      setWritebackValidationError(noWritableCalendarMessage());
      return;
    }
    if (!isEndAfterStart(dtStart, dtEnd)) {
      setWritebackValidationError('结束时间必须晚于开始时间');
      return;
    }

    setWritebackValidationError('');

    const startUtc = datetimeLocalToUtcIso(dtStart, event?.timeZoneId);
    const endUtc = datetimeLocalToUtcIso(dtEnd, event?.timeZoneId);

    if (isOutlook) {
      if (writebackPhase.type !== 'idle') return;
      if (event && !event.outlookEtag) {
        setWritebackValidationError('缺少版本标识，无法执行写回操作。');
        return;
      }
      openWritebackPreview(event ? 'update' : 'create');
    } else {
      const data = { title, description, location, dtStart: startUtc, dtEnd: endUtc, isAllDay, calendarId: selectedCalendarId || undefined };
      if (event) updateMut.mutate(data);
      else createMut.mutate(data);
    }
  }

  const isWritebackActive = writebackPhase.type !== 'idle';
  const isSubmitting = writebackPhase.type === 'submitting';
  const isProcessing = createMut.isPending || updateMut.isPending || deleteMut.isPending || isSubmitting;

  const footer = (
    <>
      <div>
        {event && !isReadOnly && (
          <button type="button" onClick={handleDelete}
            disabled={isProcessing}
            className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600 disabled:opacity-50">
            删除
          </button>
        )}
      </div>
      <div className="flex gap-2">
        <button type="button" onClick={onClose}
          className="pim-button-secondary px-4 py-2 text-sm">取消</button>
        {!isReadOnly && (
          <button type="submit" form="event-editor-form"
            disabled={isProcessing || isLoading || (!event && !hasWritableCalendar(calendars || [], hiddenCalendarIds))}
            className="pim-button-primary px-4 py-2 text-sm disabled:opacity-50">
            {event ? '保存' : '创建'}
          </button>
        )}
      </div>
    </>
  );

  const titleText = isReadOnly ? `${event ? '编辑日程' : '新建日程'}（只读）` : (event ? '编辑日程' : '新建日程');

  return (
    <>
    <EditorDrawer open={open} onClose={onClose} title={titleText} footer={footer}>
      <form id="event-editor-form" onSubmit={handleSubmit} className="space-y-4" noValidate>
        {writebackValidationError && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {writebackValidationError}
          </div>
        )}
        {mutationErrorMessage && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {mutationErrorMessage}
          </div>
        )}
        {isReadOnly && (
          <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
            此日历为只读，无法编辑或删除。
          </div>
        )}
        {event?.source === 'outlook-ics' && (
          <div className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm leading-6 text-blue-700">
            这是从 Outlook ICS 导入的事件，会议上下文已保留，PIM 暂不处理会议接受/拒绝/参会状态。
          </div>
        )}
        <Field label="日历本">
          <select value={selectedCalendarId} onChange={e => setCalendarId(e.target.value)}
            disabled={isLoading || (!!event && (isFormDisabled || isOutlookExisting))}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500">
            {isLoading ? (
              <option value="" disabled>正在加载日历...</option>
            ) : calendars?.map(cal => (
              <option key={cal.id} value={cal.id}>{cal.name}{cal.outlookCalendarBindingId ? ' (Outlook)' : ''}</option>
            ))}
          </select>
          {!isLoading && !isReadOnly && !hasWritableCalendar(calendars || [], hiddenCalendarIds) && (
            <p className="mt-1 text-xs text-red-600">{noWritableCalendarMessage()}</p>
          )}
        </Field>
        <Field label="标题">
          <input type="text" value={title} onChange={e => setTitle(e.target.value)}
            disabled={isFormDisabled}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" required />
        </Field>
        <label className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={isAllDay}
            onChange={e => setIsAllDay(e.target.checked)}
            disabled={isFormDisabled}
            className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-200"
          />
          全天事件
        </label>
        <Field label="开始时间">
          <input type="datetime-local" value={dtStart} onChange={e => setDtStart(e.target.value)}
            disabled={isFormDisabled}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" required />
        </Field>
        <Field label="结束时间">
          <input type="datetime-local" value={dtEnd} onChange={e => setDtEnd(e.target.value)}
            min={minimumEndValue(dtStart)}
            disabled={isFormDisabled}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" required />
        </Field>
        <Field label="地点">
          <input type="text" value={location} onChange={e => setLocation(e.target.value)}
            disabled={isFormDisabled}
            className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" />
        </Field>
        <Field label="描述">
          {showHtmlPreview ? (
            <div data-description-html-preview
              dangerouslySetInnerHTML={{ __html: sanitizedPreviewHtml }}
              className="w-full border rounded px-3 py-2 text-sm bg-slate-50 min-h-[4rem]" />
          ) : (
            <textarea value={description} onChange={e => setDescription(e.target.value)}
              disabled={isFormDisabled}
              className="w-full border rounded px-3 py-2 text-sm disabled:bg-slate-100 disabled:text-slate-500" rows={3} />
          )}
        </Field>
      </form>
    </EditorDrawer>
    {!isOutlookExisting && (
      <ConfirmActionDialog
        open={deleteInput !== null}
        input={deleteInput}
        isPending={deleteMut.isPending}
        onCancel={cancelDelete}
        onConfirm={confirmDelete}
      />
    )}
    {isWritebackActive && (
      <OutlookWritebackConfirmDialog
        open={isWritebackActive}
        phase={writebackPhase}
        operation={pendingRequest?.operation || 'update'}
        beforeJson={diffBefore}
        afterJson={diffAfter}
        scope={outlookScope}
        showScope={showScopeRadio}
        onScopeChange={setOutlookScope}
        onConfirm={confirmWriteback}
        onCancel={cancelWriteback}
        onRetryWithLatest={retryWithLatest}
      />
    )}
    </>
  );
}

function OutlookWritebackConfirmDialog({
  open,
  phase,
  operation,
  beforeJson,
  afterJson,
  scope,
  showScope,
  onScopeChange,
  onConfirm,
  onCancel,
  onRetryWithLatest,
}: {
  open: boolean;
  phase: WritebackPhase;
  operation: string;
  beforeJson: string;
  afterJson: string;
  scope: 'instance' | 'series';
  showScope: boolean;
  onScopeChange: (value: 'instance' | 'series') => void;
  onConfirm: () => void;
  onCancel: () => void;
  onRetryWithLatest: () => void;
}) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocusedRef = useRef<HTMLElement | null>(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return;
    previouslyFocusedRef.current = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null;
    const dialog = dialogRef.current;
    dialog?.focus();
    return () => {
      previouslyFocusedRef.current?.focus();
      previouslyFocusedRef.current = null;
    };
  }, [open]);

  if (!open) return null;

  function getFocusableElements() {
    const dialog = dialogRef.current;
    if (!dialog) return [];
    return Array.from(
      dialog.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ),
    ).filter(element => !element.hasAttribute('aria-hidden'));
  }

  function handleKeyDown(e: KeyboardEvent<HTMLDivElement>) {
    if (e.key === 'Escape') {
      e.stopPropagation();
      if (phase.type === 'preview' || phase.type === 'conflict') {
        onCancel();
      }
      return;
    }
    if (e.key !== 'Tab') return;
    const focusableElements = getFocusableElements();
    if (focusableElements.length === 0) {
      e.preventDefault();
      dialogRef.current?.focus();
      return;
    }
    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];
    const activeElement = document.activeElement;
    if (e.shiftKey && (activeElement === firstElement || activeElement === dialogRef.current)) {
      e.preventDefault();
      lastElement.focus();
    } else if (!e.shiftKey && (activeElement === lastElement || activeElement === dialogRef.current)) {
      e.preventDefault();
      firstElement.focus();
    }
  }

  const isPending = phase.type === 'submitting';
  const isConflict = phase.type === 'conflict';
  const isError = phase.type === 'error';

  const opLabel = operation === 'delete' ? '删除' : operation === 'create' ? '创建' : '更新';

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/30 px-4 py-6">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={handleKeyDown}
        className="w-full max-w-lg max-h-[85vh] flex flex-col rounded-lg border border-slate-200 bg-white shadow-2xl"
      >
        <header className="border-b border-slate-200 px-5 py-4 shrink-0">
          <p className="text-xs font-semibold uppercase tracking-wide text-blue-600">Outlook 写回 确认</p>
          <h2 id={titleId} className="mt-1 text-base font-semibold text-slate-950">
            {opLabel} Outlook 日程
          </h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            {operation === 'delete' ? '此操作将同步删除 Outlook 中的日程，请确认。' : '以下变更将同步写回 Outlook，请确认后再提交。'}
          </p>
          {isConflict && (
            <div className="mt-3 rounded-lg border border-orange-200 bg-orange-50 px-3 py-2 text-sm text-orange-700">
              <p className="font-medium">变更冲突 (conflict)</p>
              <p className="mt-1">Outlook 中的日程已被其他人修改，以下是 Outlook 中的最新内容，请参考后重新比较并确认。</p>
              {(phase as { type: 'conflict'; errorMessage?: string | null }).errorMessage && (
                <p className="mt-1 text-xs">{(phase as { type: 'conflict'; errorMessage?: string | null }).errorMessage}</p>
              )}
            </div>
          )}
          {isError && (
            <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {(phase as { type: 'error'; message: string }).message}
            </div>
          )}
        </header>

        <section className="px-5 py-4 overflow-auto">
          {showScope && (
            <div className="mb-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <p className="text-sm font-medium text-slate-700 mb-2">变更范围</p>
              <div className="flex gap-4">
                <label className="flex items-center gap-2 text-sm text-slate-600">
                  <input
                    type="radio"
                    name="outlook-scope"
                    value="instance"
                    checked={scope === 'instance'}
                    onChange={() => onScopeChange('instance')}
                    disabled={isPending}
                    className="text-blue-600 focus:ring-blue-200"
                  />
                  仅此 实例
                </label>
                <label className="flex items-center gap-2 text-sm text-slate-600">
                  <input
                    type="radio"
                    name="outlook-scope"
                    value="series"
                    checked={scope === 'series'}
                    onChange={() => onScopeChange('series')}
                    disabled={isPending}
                    className="text-blue-600 focus:ring-blue-200"
                  />
                  整个 系列
                </label>
              </div>
            </div>
          )}

          {isConflict && (
            <div className="mb-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
              <p className="text-sm font-medium text-slate-700 mb-2">最新 Outlook 内容</p>
              <pre className="max-h-40 overflow-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-100">
                {beforeJson || '{}'}
              </pre>
            </div>
          )}

          <BeforeAfterDiff
            beforeJson={beforeJson}
            afterJson={afterJson}
            changedFields={null}
          />
        </section>

        <footer className="flex items-center justify-end gap-2 border-t border-slate-200 px-5 py-4 shrink-0">
          {isConflict ? (
            <>
              {phase.type === 'conflict' && phase.latestEtag ? (
                <button
                  type="button"
                  onClick={onRetryWithLatest}
                  className="rounded-md border border-blue-300 bg-blue-50 px-4 py-2 text-sm font-medium text-blue-700 hover:bg-blue-100"
                >
                  基于 Outlook 最新版本重新比较
                </button>
              ) : (
                <p className="text-xs text-red-600 mr-auto">无法获取最新版本标识，请关闭后重试。</p>
              )}
              <button
                type="button"
                onClick={onCancel}
                className="shrink-0 whitespace-nowrap rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                取消
              </button>
            </>
          ) : isError ? (
            <>
              <button
                type="button"
                onClick={onCancel}
                className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                关闭
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={onCancel}
                disabled={isPending}
                className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              >
                取消
              </button>
              <button
                type="button"
                onClick={onConfirm}
                disabled={isPending}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isPending ? '提交中' : '确认'}
              </button>
            </>
          )}
        </footer>
      </div>
    </div>
  );
}
