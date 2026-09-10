# src/client-web/src/components/pc-tracker/DateDimensionBar.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `DateDimensionBar`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/utils/pcBusinessDay.ts`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### DateDimensionBar
#### DateDimensionBar({ date, dimension, onDateChange, onDimensionChange }: Props)
- 输入：{ date, dimension, onDateChange, onDimensionChange }: Props
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `DateDimensionBar`
  2. 返回 JSX/结构
  3. 执行：<div className="flex max-w-full flex-wrap items-center justify-end gap-2">
  4. 执行：<div className="flex min-w-0 max-w-full flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-sla
  5. 执行：type="button"
  6. 执行：className="shrink-0 rounded-lg bg-blue-600 px-2.5 py-1.5 text-xs font-medium text-white transition-colors hove
  7. 执行：onClick={() => onDateChange(getPcBusinessDate())}
  8. 执行：</button>
  9. 执行：className="shrink-0 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-600 transition-
  10. 执行：onClick={() => onDateChange(new Date(date.getTime() - 86400000))}
  11. 执行：aria-label="前一天"
  12. 执行：onClick={() => onDateChange(new Date(date.getTime() + 86400000))}
  13. 执行：aria-label="后一天"
  14. 执行：<span className="min-w-0 max-w-[11rem] truncate px-1 text-sm font-semibold text-slate-900 sm:max-w-[15rem] sm:
  15. 执行：{format(date, 'yyyy年M月d日 EEEE', { locale: zhCN })}
  16. 执行：<div className="flex max-w-full shrink-0 flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-sl
  17. 执行：{DIMENSIONS.map(d => (
  18. 执行：key={d.key}
  19. 执行：className={`rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors sm:px-3 ${
  20. 执行：dimension === d.key
  21. 执行：? 'bg-teal-600 text-white shadow-sm'
  22. 执行：: 'text-slate-500 hover:bg-white hover:text-slate-800'
  23. 执行：onClick={() => onDimensionChange(d.key)}
  24. 执行：{d.label}
- 分支与异常：无显著分支
- 调用：DateDimensionBar、onDateChange、getPcBusinessDate、Date、date.getTime、format、DIMENSIONS.map、onDimensionChange

## 近逐行中文伪代码

1. [L5] 赋值 `DIMENSIONS` = [
2. [L6] 执行：{ key: 'hour' as const, label: '时' },
3. [L7] 执行：{ key: 'day' as const, label: '日' },
4. [L8] 执行：{ key: 'month' as const, label: '月' },
5. [L9] 执行：{ key: 'year' as const, label: '年' },
6. [L12] 定义类型 `Props`
7. [L13] 执行：date: Date;
8. [L14] 执行：dimension: 'hour' | 'day' | 'month' | 'year';
9. [L15] 执行：onDateChange: (d: Date) => void;
10. [L16] 执行：onDimensionChange: (dim: 'hour' | 'day' | 'month' | 'year') => void;
11. [L19] 默认导出函数 `DateDimensionBar`
12. [L20] 返回 JSX/结构
13. [L21] 执行：<div className="flex max-w-full flex-wrap items-center justify-end gap-2">
14. [L22] 执行：<div className="flex min-w-0 max-w-full flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-sla
15. [L24] 执行：type="button"
16. [L25] 执行：className="shrink-0 rounded-lg bg-blue-600 px-2.5 py-1.5 text-xs font-medium text-white transition-colors hove
17. [L26] 执行：onClick={() => onDateChange(getPcBusinessDate())}
18. [L29] 执行：</button>
19. [L31] 执行：type="button"
20. [L32] 执行：className="shrink-0 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-600 transition-
21. [L33] 执行：onClick={() => onDateChange(new Date(date.getTime() - 86400000))}
22. [L34] 执行：aria-label="前一天"
23. [L37] 执行：</button>
24. [L39] 执行：type="button"
25. [L40] 执行：className="shrink-0 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs text-slate-600 transition-
26. [L41] 执行：onClick={() => onDateChange(new Date(date.getTime() + 86400000))}
27. [L42] 执行：aria-label="后一天"
28. [L45] 执行：</button>
29. [L46] 执行：<span className="min-w-0 max-w-[11rem] truncate px-1 text-sm font-semibold text-slate-900 sm:max-w-[15rem] sm:
30. [L47] 执行：{format(date, 'yyyy年M月d日 EEEE', { locale: zhCN })}
31. [L51] 执行：<div className="flex max-w-full shrink-0 flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-sl
32. [L52] 执行：{DIMENSIONS.map(d => (
33. [L54] 执行：key={d.key}
34. [L55] 执行：type="button"
35. [L56] 执行：className={`rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors sm:px-3 ${
36. [L57] 执行：dimension === d.key
37. [L58] 执行：? 'bg-teal-600 text-white shadow-sm'
38. [L59] 执行：: 'text-slate-500 hover:bg-white hover:text-slate-800'
39. [L61] 执行：onClick={() => onDimensionChange(d.key)}
40. [L63] 执行：{d.label}
41. [L64] 执行：</button>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/DateDimensionBar.tsx",
      "label": "DateDimensionBar",
      "path": "src/client-web/src/components/pc-tracker/DateDimensionBar.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/DateDimensionBar.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/DateDimensionBar.tsx",
      "to": "src/client-web/src/utils/pcBusinessDay.ts",
      "type": "depends_on"
    }
  ]
}
```
