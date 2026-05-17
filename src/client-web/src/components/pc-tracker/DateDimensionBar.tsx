import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';

const DIMENSIONS = [
  { key: 'hour' as const, label: '时' },
  { key: 'day' as const, label: '日' },
  { key: 'month' as const, label: '月' },
  { key: 'year' as const, label: '年' },
];

interface Props {
  date: Date;
  dimension: 'hour' | 'day' | 'month' | 'year';
  onDateChange: (d: Date) => void;
  onDimensionChange: (dim: 'hour' | 'day' | 'month' | 'year') => void;
}

export default function DateDimensionBar({ date, dimension, onDateChange, onDimensionChange }: Props) {
  return (
    <div className="flex items-center justify-between bg-white rounded-xl px-4 py-3 shadow-sm border">
      <div className="flex items-center gap-2">
        <button className="px-3 py-1 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          onClick={() => onDateChange(new Date())}>今天</button>
        <button className="px-2 py-1 text-sm border rounded-lg hover:bg-gray-50"
          onClick={() => onDateChange(new Date(date.getTime() - 86400000))}>‹</button>
        <button className="px-2 py-1 text-sm border rounded-lg hover:bg-gray-50"
          onClick={() => onDateChange(new Date(date.getTime() + 86400000))}>›</button>
        <span className="font-bold text-lg ml-3">
          {format(date, 'yyyy年M月d日 EEEE', { locale: zhCN })}
        </span>
      </div>
      <div className="flex items-center gap-1 bg-gray-100 rounded-lg p-0.5">
        {DIMENSIONS.map(d => (
          <button key={d.key}
            className={`px-3 py-1 text-xs rounded-md transition-colors ${
              dimension === d.key ? 'bg-white text-gray-800 shadow-sm font-medium' : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => onDimensionChange(d.key)}>
            {d.label}
          </button>
        ))}
      </div>
    </div>
  );
}
