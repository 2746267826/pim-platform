# src/client-web/src/components/schedule/HabitRoutineEditor.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `HabitRoutineEditor`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：`src/client-web/src/api/calendar.ts`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### HabitRoutineEditor
#### HabitRoutineEditor({ onCreated }: HabitRoutineEditorProps)
- 输入：{ onCreated }: HabitRoutineEditorProps
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `HabitRoutineEditor`
  2. 赋值 `queryClient` = useQueryClient()
  3. 执行：const [title, setTitle] = useState('');
  4. 执行：const [cadence, setCadence] = useState('Daily');
  5. 赋值 `createMutation` = useMutation({
  6. 执行：mutationFn: () => createHabit({
  7. 执行：source: 'manual',
  8. 执行：status: 'Active',
  9. 执行：description: null,
  10. 执行：onSuccess: () => {
  11. 更新状态 setTitle('')
  12. 执行：queryClient.invalidateQueries({ queryKey: ['habits'] });
  13. 执行：queryClient.invalidateQueries({ queryKey: ['calendar-layers'] });
  14. 执行：onCreated?.();
  15. 返回 JSX/结构
  16. 执行：<section className="pim-panel p-4" aria-label="习惯规则编辑器">
  17. 执行：<h2 className="text-sm font-semibold text-slate-950">创建或编辑习惯规则</h2>
  18. 执行：<div className="mt-3 grid gap-3 md:grid-cols-[1fr_160px_auto]">
  19. 执行：type="text"
  20. 执行：value={title}
  21. 执行：onChange={event => setTitle(event.target.value)}
  22. 执行：placeholder="习惯名称"
  23. 执行：className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
  24. 执行：value={cadence}
  25. 执行：onChange={event => setCadence(event.target.value)}
  26. 执行：<option value="Daily">每天</option>
  27. 执行：<option value="Weekly">每周</option>
  28. 执行：<option value="Monthly">每月</option>
  29. 执行：</select>
  30. 执行：type="button"
- 分支与异常：无显著分支
- 调用：HabitRoutineEditor、useQueryClient、useState、useMutation、createHabit、setTitle、queryClient.invalidateQueries、setCadence、title.trim、createMutation.mutate

## 近逐行中文伪代码

1. [L5] 定义类型 `HabitRoutineEditorProps`
2. [L6] 执行：onCreated?: () => void;
3. [L9] 默认导出函数 `HabitRoutineEditor`
4. [L10] 赋值 `queryClient` = useQueryClient()
5. [L11] 执行：const [title, setTitle] = useState('');
6. [L12] 执行：const [cadence, setCadence] = useState('Daily');
7. [L14] 赋值 `createMutation` = useMutation({
8. [L15] 执行：mutationFn: () => createHabit({
9. [L18] 执行：source: 'manual',
10. [L19] 执行：status: 'Active',
11. [L20] 执行：description: null,
12. [L22] 执行：onSuccess: () => {
13. [L23] 更新状态 setTitle('')
14. [L24] 执行：queryClient.invalidateQueries({ queryKey: ['habits'] });
15. [L25] 执行：queryClient.invalidateQueries({ queryKey: ['calendar-layers'] });
16. [L26] 执行：onCreated?.();
17. [L30] 返回 JSX/结构
18. [L31] 执行：<section className="pim-panel p-4" aria-label="习惯规则编辑器">
19. [L32] 执行：<h2 className="text-sm font-semibold text-slate-950">创建或编辑习惯规则</h2>
20. [L33] 执行：<div className="mt-3 grid gap-3 md:grid-cols-[1fr_160px_auto]">
21. [L35] 执行：type="text"
22. [L36] 执行：value={title}
23. [L37] 执行：onChange={event => setTitle(event.target.value)}
24. [L38] 执行：placeholder="习惯名称"
25. [L39] 执行：className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
26. [L42] 执行：value={cadence}
27. [L43] 执行：onChange={event => setCadence(event.target.value)}
28. [L44] 执行：className="rounded-lg border border-slate-200 px-3 py-2 text-sm"
29. [L46] 执行：<option value="Daily">每天</option>
30. [L47] 执行：<option value="Weekly">每周</option>
31. [L48] 执行：<option value="Monthly">每月</option>
32. [L49] 执行：</select>
33. [L51] 执行：type="button"
34. [L52] 执行：disabled={!title.trim() || createMutation.isPending}
35. [L53] 执行：onClick={() => createMutation.mutate()}
36. [L54] 执行：className="pim-button-primary px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-50"
37. [L57] 执行：</button>
38. [L59] 执行：</section>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/schedule/HabitRoutineEditor.tsx",
      "label": "HabitRoutineEditor",
      "path": "src/client-web/src/components/schedule/HabitRoutineEditor.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/schedule/HabitRoutineEditor.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/schedule/HabitRoutineEditor.tsx",
      "to": "src/client-web/src/api/calendar.ts",
      "type": "depends_on"
    }
  ]
}
```
