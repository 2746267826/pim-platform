# src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `RuleImpactPreviewPanel`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### formatMinutes
#### formatMinutes(seconds: number)
- 输入：seconds: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatMinutes`
  2. 赋值 `minutes` = Math.round((seconds / 60) * 10) / 10
  3. 返回 `${minutes.toLocaleString('zh-CN', { maximumFractionDigits: 1 })} 分钟`
- 分支与异常：无显著分支
- 调用：formatMinutes、Math.round、minutes.toLocaleString

### formatCounts
#### formatCounts(counts: Record<string, number>)
- 输入：counts: Record<string, number>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `formatCounts`
  2. 赋值 `text` = Object.entries(counts)
  3. 执行：.sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
  4. 执行：.map(([name, count]) => `${name || '未分类'} ${count.toLocaleString('zh-CN')}`)
  5. 执行：.join(' | ');
  6. 返回 text || '无'
- 分支与异常：无显著分支
- 调用：formatCounts、Object.entries、sort、localeCompare、map、count.toLocaleString、join

### RuleImpactPreviewPanel
#### RuleImpactPreviewPanel({ preview }: Props)
- 输入：{ preview }: Props
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `RuleImpactPreviewPanel`
  2. 返回 JSX/结构
  3. 执行：<section className="rounded-lg border border-cyan-200 bg-cyan-50 p-3 text-sm text-cyan-950">
  4. 执行：<div className="flex flex-wrap items-center justify-between gap-2">
  5. 执行：<h3 className="text-sm font-semibold">知识库写入影响</h3>
  6. 执行：{preview.requiresConfirmation && (
  7. 执行：<span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-8
  8. 执行：<p className="mt-2 text-sm text-cyan-900">
  9. 执行：将影响 {preview.affectedRecordCount.toLocaleString('zh-CN')} 条记录，合计{' '}
  10. 执行：{formatMinutes(preview.affectedDurationSeconds)}。
  11. 执行：{preview.summary && (
  12. 执行：<p className="mt-1 break-words text-xs text-cyan-800">{preview.summary}</p>
  13. 执行：<div className="mt-3 grid gap-1 text-xs text-cyan-900">
  14. 执行：<p className="break-words">当前：{formatCounts(preview.currentCategoryCounts)}</p>
  15. 执行：<p className="break-words">应用后：{formatCounts(preview.newCategoryCounts)}</p>
  16. 执行：{preview.samples.length > 0 && (
  17. 执行：<div className="mt-3 border-t border-cyan-200 pt-2">
  18. 执行：<p className="text-xs font-semibold text-cyan-900">样本记录</p>
  19. 执行：<ul className="mt-1 space-y-1">
  20. 执行：{preview.samples.slice(0, 3).map((sample, index) => (
  21. 执行：<li key={sample.recordKey || `${sample.start}-${index}`} className="min-w-0 break-words text-xs text-cyan-800"
  22. 执行：<span className="font-medium">{sample.displayName || sample.appName || sample.domain || '活动'}</span>
  23. 执行：{sample.title && <span className="ml-1 text-cyan-700">{sample.title}</span>}
  24. 执行：</section>
- 分支与异常：无显著分支
- 调用：RuleImpactPreviewPanel、preview.affectedRecordCount.toLocaleString、formatMinutes、formatCounts、preview.samples.slice、map

## 近逐行中文伪代码

1. [L3] 定义类型 `Props`
2. [L4] 执行：preview: ActivityClassificationPreview;
3. [L7] 定义函数 `formatMinutes`
4. [L8] 赋值 `minutes` = Math.round((seconds / 60) * 10) / 10
5. [L9] 返回 `${minutes.toLocaleString('zh-CN', { maximumFractionDigits: 1 })} 分钟`
6. [L12] 定义函数 `formatCounts`
7. [L13] 赋值 `text` = Object.entries(counts)
8. [L14] 执行：.sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
9. [L15] 执行：.map(([name, count]) => `${name || '未分类'} ${count.toLocaleString('zh-CN')}`)
10. [L16] 执行：.join(' | ');
11. [L18] 返回 text || '无'
12. [L21] 默认导出函数 `RuleImpactPreviewPanel`
13. [L22] 返回 JSX/结构
14. [L23] 执行：<section className="rounded-lg border border-cyan-200 bg-cyan-50 p-3 text-sm text-cyan-950">
15. [L24] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
16. [L25] 执行：<h3 className="text-sm font-semibold">知识库写入影响</h3>
17. [L26] 执行：{preview.requiresConfirmation && (
18. [L27] 执行：<span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-8
19. [L33] 执行：<p className="mt-2 text-sm text-cyan-900">
20. [L34] 执行：将影响 {preview.affectedRecordCount.toLocaleString('zh-CN')} 条记录，合计{' '}
21. [L35] 执行：{formatMinutes(preview.affectedDurationSeconds)}。
22. [L38] 执行：{preview.summary && (
23. [L39] 执行：<p className="mt-1 break-words text-xs text-cyan-800">{preview.summary}</p>
24. [L42] 执行：<div className="mt-3 grid gap-1 text-xs text-cyan-900">
25. [L43] 执行：<p className="break-words">当前：{formatCounts(preview.currentCategoryCounts)}</p>
26. [L44] 执行：<p className="break-words">应用后：{formatCounts(preview.newCategoryCounts)}</p>
27. [L47] 执行：{preview.samples.length > 0 && (
28. [L48] 执行：<div className="mt-3 border-t border-cyan-200 pt-2">
29. [L49] 执行：<p className="text-xs font-semibold text-cyan-900">样本记录</p>
30. [L50] 执行：<ul className="mt-1 space-y-1">
31. [L51] 执行：{preview.samples.slice(0, 3).map((sample, index) => (
32. [L52] 执行：<li key={sample.recordKey || `${sample.start}-${index}`} className="min-w-0 break-words text-xs text-cyan-800"
33. [L53] 执行：<span className="font-medium">{sample.displayName || sample.appName || sample.domain || '活动'}</span>
34. [L54] 执行：{sample.title && <span className="ml-1 text-cyan-700">{sample.title}</span>}
35. [L60] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx",
      "label": "RuleImpactPreviewPanel",
      "path": "src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
