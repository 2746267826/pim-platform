# src/client-web/src/components/schedule/DataCenterBatchPreview.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `DataCenterBatchPreview`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/api/calendar.ts`、`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### DataCenterBatchPreview
#### DataCenterBatchPreview({ selected }: DataCenterBatchPreviewProps)
- 输入：{ selected }: DataCenterBatchPreviewProps
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `DataCenterBatchPreview`
  2. 赋值 `previewMutation` = useMutation({
  3. 执行：mutationFn: () => previewDataCenterBatch({
  4. 执行：action: 'archive',
  5. 执行：objects: selected ? [{ objectType: selected.objectType, objectId: selected.objectId }] : [],
  6. 执行：reason: '数据中心批量影响预览',
  7. 返回 JSX/结构
  8. 执行：<section className="pim-panel min-w-0 p-4" aria-label="批量影响预览">
  9. 执行：<div className="flex flex-wrap items-center justify-between gap-2">
  10. 执行：<h2 className="text-sm font-semibold text-slate-950">批量影响预览</h2>
  11. 执行：type="button"
  12. 执行：disabled={!selected || previewMutation.isPending}
  13. 执行：onClick={() => previewMutation.mutate()}
  14. 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-50"
  15. 执行：</button>
  16. 执行：{previewMutation.data ? (
  17. 执行：<div className="mt-3 space-y-2 text-sm">
  18. 执行：<p className="font-semibold text-slate-800">{previewMutation.data.riskLevel}</p>
  19. 执行：<p className="text-slate-500">{previewMutation.data.summary}</p>
  20. 执行：<p className="text-xs text-red-600">
  21. 执行：严格确认：{previewMutation.data.requiresStrictConfirmation ? '需要' : '不需要'}
  22. 执行：<p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-5
  23. 执行：选择对象后可生成批量影响预览。
  24. 执行：</section>
- 分支与异常：无显著分支
- 调用：DataCenterBatchPreview、useMutation、previewDataCenterBatch、previewMutation.mutate

## 近逐行中文伪代码

1. [L5] 定义类型 `DataCenterBatchPreviewProps`
2. [L6] 执行：selected?: DataCenterItem;
3. [L9] 默认导出函数 `DataCenterBatchPreview`
4. [L10] 赋值 `previewMutation` = useMutation({
5. [L11] 执行：mutationFn: () => previewDataCenterBatch({
6. [L12] 执行：action: 'archive',
7. [L13] 执行：objects: selected ? [{ objectType: selected.objectType, objectId: selected.objectId }] : [],
8. [L14] 执行：reason: '数据中心批量影响预览',
9. [L18] 返回 JSX/结构
10. [L19] 执行：<section className="pim-panel min-w-0 p-4" aria-label="批量影响预览">
11. [L20] 执行：<div className="flex flex-wrap items-center justify-between gap-2">
12. [L21] 执行：<h2 className="text-sm font-semibold text-slate-950">批量影响预览</h2>
13. [L23] 执行：type="button"
14. [L24] 执行：disabled={!selected || previewMutation.isPending}
15. [L25] 执行：onClick={() => previewMutation.mutate()}
16. [L26] 执行：className="pim-button-secondary px-3 py-1.5 text-xs disabled:cursor-not-allowed disabled:opacity-50"
17. [L29] 执行：</button>
18. [L31] 执行：{previewMutation.data ? (
19. [L32] 执行：<div className="mt-3 space-y-2 text-sm">
20. [L33] 执行：<p className="font-semibold text-slate-800">{previewMutation.data.riskLevel}</p>
21. [L34] 执行：<p className="text-slate-500">{previewMutation.data.summary}</p>
22. [L35] 执行：<p className="text-xs text-red-600">
23. [L36] 执行：严格确认：{previewMutation.data.requiresStrictConfirmation ? '需要' : '不需要'}
24. [L40] 执行：<p className="mt-3 rounded-lg border border-dashed border-slate-200 px-3 py-6 text-center text-sm text-slate-5
25. [L41] 执行：选择对象后可生成批量影响预览。
26. [L44] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/schedule/DataCenterBatchPreview.tsx",
      "label": "DataCenterBatchPreview",
      "path": "src/client-web/src/components/schedule/DataCenterBatchPreview.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/schedule/DataCenterBatchPreview.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/schedule/DataCenterBatchPreview.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/schedule/DataCenterBatchPreview.tsx",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
