# src/client-web/src/pages/SettingsPage.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：页面组件 `SettingsPage`：路由级视图，负责数据加载与子面板编排。
- 主要依赖：`src/client-web/src/ui/PageHeader.tsx`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### SettingsPage
#### SettingsPage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `SettingsPage`
  2. 返回 JSX/结构
  3. 执行：<div className="mx-auto max-w-2xl space-y-4 pb-8">
  4. 执行：<PageHeader title="设置" subtitle="管理数据入口与本地记录" />
  5. 执行：{settingsLinks.map(link => (
  6. 执行：key={link.to}
  7. 执行：to={link.to}
  8. 执行：className="pim-card flex w-full cursor-pointer items-center justify-between gap-4 p-5 text-left transition-col
  9. 执行：<div className="flex min-w-0 items-center gap-4">
  10. 执行：<span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-blue-100
  11. 执行：{link.label}
  12. 执行：<span className="min-w-0">
  13. 执行：<span className="block truncate text-base font-semibold text-slate-950">{link.title}</span>
  14. 执行：<span className="mt-1 block break-words text-sm text-slate-500">{link.description}</span>
  15. 执行：<span className="shrink-0 text-xl text-slate-300" aria-hidden="true">
- 分支与异常：无显著分支
- 调用：SettingsPage、settingsLinks.map

## 近逐行中文伪代码

1. [L4] 赋值 `settingsLinks` = [
2. [L6] 执行：title: '管理日程数据',
3. [L7] 执行：description: '查看、筛选、导入导出全部日程数据',
4. [L8] 执行：label: '日程',
5. [L9] 执行：to: '/settings/calendar-data',
6. [L12] 执行：title: '回收站',
7. [L13] 执行：description: '恢复已删除的日程、任务、日历本和任务本',
8. [L14] 执行：label: '恢复',
9. [L15] 执行：to: '/settings/recycle-bin',
10. [L18] 执行：title: 'PC 记录详细数据',
11. [L19] 执行：description: '查询、筛选、导出全部 PC 记录数据',
12. [L20] 执行：label: 'PC',
13. [L21] 执行：to: '/settings/pc-data',
14. [L24] 执行：title: '同步设置',
15. [L25] 执行：description: '配置微软日历连接、设备代码登录、同步批次与冲突策略',
16. [L26] 执行：label: '同步',
17. [L27] 执行：to: '/settings/sync',
18. [L30] 执行：title: 'AI 设置',
19. [L31] 执行：description: 'LiteLLM 状态、用量、请求日志与详情',
20. [L32] 执行：label: 'AI',
21. [L33] 执行：to: '/settings/ai',
22. [L35] 执行：] as const;
23. [L37] 默认导出函数 `SettingsPage`
24. [L38] 返回 JSX/结构
25. [L39] 执行：<div className="mx-auto max-w-2xl space-y-4 pb-8">
26. [L40] 执行：<PageHeader title="设置" subtitle="管理数据入口与本地记录" />
27. [L42] 执行：{settingsLinks.map(link => (
28. [L44] 执行：key={link.to}
29. [L45] 执行：to={link.to}
30. [L46] 执行：className="pim-card flex w-full cursor-pointer items-center justify-between gap-4 p-5 text-left transition-col
31. [L48] 执行：<div className="flex min-w-0 items-center gap-4">
32. [L49] 执行：<span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-blue-100
33. [L50] 执行：{link.label}
34. [L52] 执行：<span className="min-w-0">
35. [L53] 执行：<span className="block truncate text-base font-semibold text-slate-950">{link.title}</span>
36. [L54] 执行：<span className="mt-1 block break-words text-sm text-slate-500">{link.description}</span>
37. [L57] 执行：<span className="shrink-0 text-xl text-slate-300" aria-hidden="true">

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/SettingsPage.tsx",
      "label": "SettingsPage",
      "path": "src/client-web/src/pages/SettingsPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/SettingsPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/pages/SettingsPage.tsx",
      "to": "src/client-web/src/ui/PageHeader.tsx",
      "type": "depends_on"
    }
  ]
}
```
