import EChartBox from '../charts/EChartBox';
import { buildAnalysisBlocksOption } from '../charts/pcHeatmapOptions';
import type { PcActivityAnalysisBlock, PcActivityAnalysisResponse } from '../../types';

interface Props {
  analysis: PcActivityAnalysisResponse | undefined;
  selectedStart: string | null;
  onSelectBlock: (block: PcActivityAnalysisBlock) => void;
}

function formatMinutes(seconds: number) {
  return Math.round(seconds / 60).toLocaleString('zh-CN');
}

function formatTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

export default function ActivityAnalysisHeatmap({ analysis, selectedStart, onSelectBlock }: Props) {
  const blocks = analysis?.blocks ?? [];
  const selected = blocks.find(block => block.start === selectedStart)
    ?? blocks.find(block => block.activeDurationSeconds > 0)
    ?? blocks[0];

  if (!analysis || blocks.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 bg-slate-50 px-4 py-5 text-sm text-slate-500">
        暂无活动分析数据。
      </div>
    );
  }

  const handleClick = (params: unknown) => {
    const p = params as { data?: { blockIndex?: number } | number[] } | undefined;
    const d = p?.data;
    const idx = Array.isArray(d) ? d[0] : d?.blockIndex;
    if (typeof idx === 'number' && blocks[idx]) {
      onSelectBlock(blocks[idx]);
    }
  };

  return (
    <div className="space-y-3">
      <EChartBox
        option={buildAnalysisBlocksOption(blocks, selectedStart)}
        height={116}
        ariaLabel="活动分析热力图"
        onEvents={{ click: handleClick }}
      />

      <div className="flex flex-wrap gap-3 text-xs text-slate-500">
        <span>活动分析</span>
        <span>颜色越深表示活动越密集</span>
        <span>琥珀色边框表示有待分类记录</span>
      </div>

      {selected && (
        <section className="rounded-lg border border-slate-200 bg-slate-50 p-3">
          <div className="text-sm font-semibold text-slate-950">
            {formatTime(selected.start)} - {formatTime(selected.end)}
          </div>
          <p className="mt-1 text-xs text-slate-600">
            {formatMinutes(selected.activeDurationSeconds)} 活跃分钟 | {selected.contextSwitchCount.toLocaleString('zh-CN')} 次上下文切换 | {selected.pendingClassificationCount.toLocaleString('zh-CN')} 条待分类
          </p>

          <div className="mt-2 grid gap-2 md:grid-cols-2">
            <div className="space-y-1 text-xs text-slate-600">
              {selected.categories.slice(0, 4).map(item => (
                <div key={item.categoryName} className="flex min-w-0 items-center justify-between gap-2">
                  <span className="min-w-0 break-words">{item.categoryName}</span>
                  <span className="shrink-0">{formatMinutes(item.durationSeconds)} 分钟</span>
                </div>
              ))}
            </div>
            <div className="space-y-1 text-xs text-slate-600">
              {selected.apps.slice(0, 4).map(item => (
                <div key={item.appName} className="flex min-w-0 items-center justify-between gap-2">
                  <span className="min-w-0 break-words">{item.appName}</span>
                  <span className="shrink-0">{formatMinutes(item.durationSeconds)} 分钟</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
