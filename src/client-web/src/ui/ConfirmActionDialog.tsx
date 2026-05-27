import { useId } from 'react';
import type { CalendarOperationSample } from '../types';

export interface DeleteConfirmationInput {
  targetType: string;
  title: string;
  affectedCount: number;
  samples: CalendarOperationSample[];
}

export interface DeleteConfirmationCopy {
  title: string;
  description: string;
  confirmLabel: string;
  samples: CalendarOperationSample[];
}

interface ConfirmActionDialogProps {
  open: boolean;
  input: DeleteConfirmationInput | null;
  isPending: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

function getTargetTypeLabel(targetType: string) {
  if (targetType === 'calendar') return '日历本';
  if (targetType === 'task-book') return '任务本';
  if (targetType === 'task') return '任务';
  return '日程';
}

function getSampleTypeLabel(type: string) {
  if (type === 'calendar') return '日历本';
  if (type === 'task-book') return '任务本';
  if (type === 'task') return '任务';
  return '日程';
}

function formatSampleTime(sample: CalendarOperationSample) {
  if (sample.start && sample.end) return `${sample.start} - ${sample.end}`;
  return sample.start || sample.end || null;
}

export function buildDeleteConfirmationCopy(input: DeleteConfirmationInput): DeleteConfirmationCopy {
  const typeLabel = getTargetTypeLabel(input.targetType);

  if (input.affectedCount <= 1) {
    return {
      title: `删除${typeLabel}`,
      description: `${input.title} 将移动到回收站，可以在设置中恢复。`,
      confirmLabel: '移动到回收站',
      samples: input.samples,
    };
  }

  return {
    title: `删除${typeLabel}`,
    description: `${input.title} 和 ${input.affectedCount} 个关联项目将一起移动到回收站。`,
    confirmLabel: `确认移动 ${input.affectedCount} 项`,
    samples: input.samples,
  };
}

export default function ConfirmActionDialog({
  open,
  input,
  isPending,
  onCancel,
  onConfirm,
}: ConfirmActionDialogProps) {
  const titleId = useId();

  if (!open || !input) return null;

  const copy = buildDeleteConfirmationCopy(input);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 px-4 py-6">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-lg rounded-lg border border-slate-200 bg-white shadow-2xl"
      >
        <header className="border-b border-slate-200 px-5 py-4">
          <p className="text-xs font-semibold uppercase tracking-wide text-red-600">严格确认</p>
          <h2 id={titleId} className="mt-1 text-base font-semibold text-slate-950">
            {copy.title}
          </h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">{copy.description}</p>
        </header>

        <section className="px-5 py-4">
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-sm font-medium text-slate-800">受影响样例</h3>
            <span className="rounded-md bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">
              共 {input.affectedCount} 项
            </span>
          </div>

          {copy.samples.length > 0 ? (
            <ul className="mt-3 max-h-56 space-y-2 overflow-auto">
              {copy.samples.map(sample => {
                const sampleTime = formatSampleTime(sample);

                return (
                  <li key={`${sample.type}:${sample.id}`} className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-slate-900">{sample.title}</p>
                        <p className="mt-0.5 text-xs text-slate-500">
                          {getSampleTypeLabel(sample.type)}
                          {sample.bookName ? ` · ${sample.bookName}` : ''}
                        </p>
                      </div>
                      {sampleTime && <span className="shrink-0 text-xs text-slate-500">{sampleTime}</span>}
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : (
            <p className="mt-3 rounded-md border border-dashed border-slate-200 bg-slate-50 px-3 py-3 text-sm text-slate-500">
              暂无可预览项目。
            </p>
          )}
        </section>

        <footer className="flex items-center justify-end gap-2 border-t border-slate-200 px-5 py-4">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            取消
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPending}
            className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isPending ? '处理中' : copy.confirmLabel}
          </button>
        </footer>
      </div>
    </div>
  );
}
