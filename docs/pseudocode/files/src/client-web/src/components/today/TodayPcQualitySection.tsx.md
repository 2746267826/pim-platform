# src/client-web/src/components/today/TodayPcQualitySection.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：`TodayPcQualitySection`：见源文件职责（TodayPcQualitySection.tsx）。
- 主要依赖：`src/client-web/src/types`、`src/client-web/src/ui/EmptyState.tsx`、`src/client-web/src/ui/StatusBadge.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### statusTone
#### statusTone(status?: TodaySectionStatus | string)
- 输入：status?: TodaySectionStatus | string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `statusTone`
  2. 执行：if (status === 'critical' || status === 'Critical') return 'danger';
  3. 执行：if (status === 'warning' || status === 'Warning') return 'warning';
  4. 执行：if (status === 'normal' || status === 'Healthy') return 'activity';
  5. 返回 'neutral'
- 分支与异常：if (status === 'critical' || status === 'Critical') return 'danger';；if (status === 'warning' || status === 'Warning') return 'warning';；if (status === 'normal' || status === 'Healthy') return 'activity';
- 调用：statusTone

### TodayPcQualitySection
#### TodayPcQualitySection({ section }: { section: TodaySection<PcQualityTodayData> })
- 输入：{ section }: { section: TodaySection<PcQualityTodayData> }
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `TodayPcQualitySection`
  2. 赋值 `{ quality, issueCount }` = section.data
  3. 赋值 `firstNextStep` = quality.nextSteps[0] || quality.issues.find(issue => issue.nextStep)?.nextStep
  4. 返回 JSX/结构
  5. 执行：<section className="pim-panel min-w-0 p-4">
  6. 执行：<div className="mb-3 flex items-center justify-between gap-3">
  7. 执行：<h2 className="font-semibold text-slate-900">PC 数据质量</h2>
  8. 执行：<StatusBadge tone={statusTone(section.status === 'normal' ? section.status : quality.overallStatus)}>
  9. 执行：{quality.label}
  10. 执行：</StatusBadge>
  11. 执行：<div className="space-y-3">
  12. 执行：<p className="text-sm leading-6 text-slate-600">{quality.message}</p>
  13. 执行：<div className="grid grid-cols-2 gap-3">
  14. 执行：<div className="rounded-xl bg-slate-50 p-3">
  15. 执行：<p className="text-xs text-slate-500">问题</p>
  16. 执行：<p className="mt-1 text-lg font-semibold text-slate-900">{issueCount}</p>
  17. 执行：<p className="text-xs text-slate-500">组件</p>
  18. 执行：<p className="mt-1 text-lg font-semibold text-slate-900">{quality.components.length}</p>
  19. 执行：{firstNextStep ? (
  20. 执行：<p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-800">
  21. 执行：{firstNextStep}
  22. 执行：<EmptyState title="暂无处理建议" description="PC 数据质量当前没有需要处理的下一步。" />
  23. 执行：<Link className="text-sm font-medium text-blue-600 hover:text-blue-700" to="/pc-tracker">
  24. 执行：</section>
- 分支与异常：无显著分支
- 调用：TodayPcQualitySection、quality.issues.find、statusTone

## 近逐行中文伪代码

1. [L6] 定义函数 `statusTone`
2. [L7] 执行：if (status === 'critical' || status === 'Critical') return 'danger';
3. [L8] 执行：if (status === 'warning' || status === 'Warning') return 'warning';
4. [L9] 执行：if (status === 'normal' || status === 'Healthy') return 'activity';
5. [L10] 返回 'neutral'
6. [L13] 默认导出函数 `TodayPcQualitySection`
7. [L14] 赋值 `{ quality, issueCount }` = section.data
8. [L15] 赋值 `firstNextStep` = quality.nextSteps[0] || quality.issues.find(issue => issue.nextStep)?.nextStep
9. [L17] 返回 JSX/结构
10. [L18] 执行：<section className="pim-panel min-w-0 p-4">
11. [L19] 执行：<div className="mb-3 flex items-center justify-between gap-3">
12. [L20] 执行：<h2 className="font-semibold text-slate-900">PC 数据质量</h2>
13. [L21] 执行：<StatusBadge tone={statusTone(section.status === 'normal' ? section.status : quality.overallStatus)}>
14. [L22] 执行：{quality.label}
15. [L23] 执行：</StatusBadge>
16. [L26] 执行：<div className="space-y-3">
17. [L27] 执行：<p className="text-sm leading-6 text-slate-600">{quality.message}</p>
18. [L28] 执行：<div className="grid grid-cols-2 gap-3">
19. [L29] 执行：<div className="rounded-xl bg-slate-50 p-3">
20. [L30] 执行：<p className="text-xs text-slate-500">问题</p>
21. [L31] 执行：<p className="mt-1 text-lg font-semibold text-slate-900">{issueCount}</p>
22. [L33] 执行：<div className="rounded-xl bg-slate-50 p-3">
23. [L34] 执行：<p className="text-xs text-slate-500">组件</p>
24. [L35] 执行：<p className="mt-1 text-lg font-semibold text-slate-900">{quality.components.length}</p>
25. [L39] 执行：{firstNextStep ? (
26. [L40] 执行：<p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-800">
27. [L41] 执行：{firstNextStep}
28. [L44] 执行：<EmptyState title="暂无处理建议" description="PC 数据质量当前没有需要处理的下一步。" />
29. [L47] 执行：<Link className="text-sm font-medium text-blue-600 hover:text-blue-700" to="/pc-tracker">
30. [L51] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodayPcQualitySection.tsx",
      "label": "TodayPcQualitySection",
      "path": "src/client-web/src/components/today/TodayPcQualitySection.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodayPcQualitySection.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodayPcQualitySection.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayPcQualitySection.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayPcQualitySection.tsx",
      "to": "src/client-web/src/ui/StatusBadge.tsx",
      "type": "depends_on"
    }
  ]
}
```
