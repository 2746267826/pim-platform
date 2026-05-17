// src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx
import type { KeystatsSummary } from '../../types';

const GREEN_STOPS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];
const MODIFIER_KEYS = new Set(['LCtrl', 'RCtrl', 'LWin', 'RWin', 'LAlt', 'RAlt', 'Esc', 'Tab', 'CapsLock']);

function keyColor(count: number, max: number): string {
  if (count === 0 || max === 0) return GREEN_STOPS[0];
  const ratio = Math.min(count / max, 1);
  const idx = ratio * (GREEN_STOPS.length - 1);
  const low = Math.floor(idx);
  const high = Math.min(low + 1, GREEN_STOPS.length - 1);
  const t = idx - low;
  const l = parseInt(GREEN_STOPS[low].slice(1), 16);
  const h = parseInt(GREEN_STOPS[high].slice(1), 16);
  const r = Math.round(((l >> 16) & 0xff) + t * (((h >> 16) & 0xff) - ((l >> 16) & 0xff)));
  const g = Math.round(((l >> 8) & 0xff) + t * (((h >> 8) & 0xff) - ((l >> 8) & 0xff)));
  const b = Math.round((l & 0xff) + t * ((h & 0xff) - (l & 0xff)));
  return `rgb(${r},${g},${b})`;
}

function textColor(count: number, max: number): string {
  return count > max * 0.4 ? '#fff' : '#374151';
}

// ANSI 104-key layout rows
const KEYBOARD_ROWS = [
  ['Esc', '', '', '', '', '', '', '', '', '', '', '', '', 'Backspace', ''],
  ['Tab', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '[', ']', '\\', ''],
  ['Caps', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ';', "'", '', 'Enter', ''],
  ['Shift', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/', '', 'Shift', '', ''],
  ['Ctrl', 'Win', 'Alt', '', 'Space', '', '', 'Alt', 'Win', 'Ctrl', '', '', '', '↑', ''],
  ['', '', '', '', '', '', '', '', '', '', '', '←', '↓', '→']
];

const KEY_WIDTHS: Record<string, number> = {
  'Backspace': 1.5, 'Tab': 1.3, 'Caps': 1.5, 'Enter': 1.7,
  'Shift': 1.8, 'Ctrl': 1.2, 'Win': 1.1, 'Alt': 1.1,
  'Space': 5, '↑': 1, '↓': 1, '←': 1, '→': 1,
};

interface Props {
  keystats: KeystatsSummary | null;
}

export default function KeyboardHeatmap({ keystats }: Props) {
  if (!keystats) return <div className="py-8 text-center text-gray-400">暂无按键数据</div>;

  const keyCounts = new Map(keystats.topKeys.map(k => [k.keyName, k.count]));
  const allCounts = keystats.topKeys.map(k => k.count);
  const maxKey = Math.max(...allCounts, 1);

  const shortcuts = keystats.topKeys
    .filter(k => k.keyName.includes('+'))
    .sort((a, b) => b.count - a.count);

  return (
    <div className="space-y-4">
      <div className="flex justify-center">
        <div className="flex flex-col gap-[2px]" style={{ maxWidth: 680 }}>
          {KEYBOARD_ROWS.map((row, ri) => (
            <div key={ri} className="flex gap-[2px]">
              {row.map((key, ki) => {
                if (!key) return <div key={ki} style={{ width: 16 }} />;
                const count = keyCounts.get(key) || 0;
                const isMod = MODIFIER_KEYS.has(key);
                const bg = isMod ? '#e5e7eb' : keyColor(count, maxKey);
                const color = isMod ? '#6b7280' : textColor(count, maxKey);
                const width = (KEY_WIDTHS[key] || 1) * 38;
                return (
                  <div key={ki} className="h-8 rounded flex items-center justify-center text-[10px] font-mono relative group"
                    style={{ backgroundColor: bg, color, width, minWidth: 26 }}>
                    {key.length <= 3 ? key : key.slice(0, 3)}
                    {count > 0 && (
                      <span className="absolute -bottom-1 right-0.5 text-[8px] leading-none" style={{ color }}>{count}</span>
                    )}
                    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-1 hidden group-hover:block bg-gray-800 text-white text-[10px] px-2 py-1 rounded whitespace-nowrap z-10">
                      {key}: {count.toLocaleString()}
                    </div>
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>

      {/* Mouse clicks */}
      <div className="flex justify-center gap-6 text-xs text-gray-500 pt-2 border-t border-gray-100">
        <span>🖱 左键 {keystats.leftClicks.toLocaleString()}</span>
        <span>右键 {keystats.rightClicks.toLocaleString()}</span>
        <span>中键 {keystats.middleClicks}</span>
        <span>侧后退 {keystats.sideBackClicks}</span>
        <span>侧前进 {keystats.sideForwardClicks}</span>
        <span className="font-medium ml-2">总点击 {keystats.totalClicks.toLocaleString()}</span>
      </div>

      {/* Shortcuts */}
      {shortcuts.length > 0 && (
        <div className="pt-2 border-t border-dashed border-gray-100">
          <div className="text-xs text-gray-400 mb-2">快捷键统计</div>
          <div className="flex flex-wrap gap-2">
            {shortcuts.map(s => (
              <span key={s.keyName} className="px-2 py-1 bg-red-50 border border-red-100 rounded text-xs text-red-600">
                {s.keyName} <span className="text-red-400">{s.count}</span>
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
