import type { HeatmapGridResponse } from '../../types';

const COLOR_STOPS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];

function linearColor(value: number, max: number): string {
  if (value === 0 || max === 0) return COLOR_STOPS[0];
  const ratio = Math.min(value / max, 1);
  const idx = ratio * (COLOR_STOPS.length - 1);
  const low = Math.floor(idx);
  const high = Math.min(low + 1, COLOR_STOPS.length - 1);
  const t = idx - low;
  const l = parseInt(COLOR_STOPS[low].slice(1), 16);
  const h = parseInt(COLOR_STOPS[high].slice(1), 16);
  const r = Math.round(((l >> 16) & 0xff) + t * (((h >> 16) & 0xff) - ((l >> 16) & 0xff)));
  const g = Math.round(((l >> 8) & 0xff) + t * (((h >> 8) & 0xff) - ((l >> 8) & 0xff)));
  const b = Math.round((l & 0xff) + t * ((h & 0xff) - (l & 0xff)));
  return `rgb(${r},${g},${b})`;
}

interface Props {
  data: HeatmapGridResponse | undefined;
  isLoading: boolean;
}

export default function ActivityHeatmap({ data, isLoading }: Props) {
  if (isLoading) return <div className="py-8 text-center text-gray-400">加载中...</div>;
  if (!data || !data.grid.length) return <div className="py-8 text-center text-gray-400">暂无活动数据</div>;

  const maxKey = data.maxKeyCount || 1;

  return (
    <div>
      <div className="flex items-center justify-end gap-1 mb-3 text-[11px] text-gray-400">
        <span>少</span>
        {COLOR_STOPS.map((c, i) => (
          <div key={i} className="w-3 h-3 rounded-sm" style={{ backgroundColor: c }} />
        ))}
        <span>多</span>
      </div>
      <div className="flex flex-col gap-[3px]">
        {data.grid.map((row, ri) => (
          <div key={ri} className="flex gap-[3px]">
            {row.map((cell, ci) => (
              <div key={ci} className="relative group flex-1 aspect-square rounded-sm cursor-pointer hover:ring-2 hover:ring-blue-400 transition-all"
                style={{ backgroundColor: linearColor(cell.intensityScore, maxKey) }}
                title={`${cell.start.slice(0, 10)} · ${cell.intensityScore.toLocaleString()} 键`}>
                <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover:block bg-gray-800 text-white text-[10px] px-2 py-1 rounded whitespace-nowrap z-10">
                  {cell.start.slice(0, 16)} · {cell.intensityScore.toLocaleString()} 键
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
