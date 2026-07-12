# src/client-web/src/pages/TodayPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：今日工作台：密度模式、待确认/同步摘要、按注册表渲染 TodaySectionHost、任务/日程编辑。
- 主要依赖：today/calendar/operations API、TodaySectionHost、编辑对话框
- 被谁使用：默认首页路由

## 函数级结构化伪代码

### useTodayDate / sortSections / RegistryErrorPanel
- 跨午夜刷新；按 todaySectionOrder 排序区块；错误条

### TodayPage
- 查 registry(30s)、pendingConfirmations、outlookSyncBatches
- density 影响网格列数与 compactItemLimit
- 顶栏四卡摘要；sections.map → TodaySectionHost（pc.activity 跨 2 列）
- 打开任务/日程项编辑；关闭事件编辑 invalidate today

## 近逐行中文伪代码

1. 今日日期串与密度分段。
2. 拉注册表与确认/同步。
3. 摘要区展示待确认与同步批次。
4. 加载中 EmptyState；否则按 kind 渲染区块。
5. 挂载 Task/Event 编辑对话框。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/TodayPage.tsx",
      "label": "TodayPage",
      "path": "src/client-web/src/pages/TodayPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/TodayPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/TodayPage.tsx", "to": "src/client-web/src/api/today.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TodayPage.tsx", "to": "src/client-web/src/api/calendar.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TodayPage.tsx", "to": "src/client-web/src/api/operations.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TodayPage.tsx", "to": "src/client-web/src/components/today/TodaySectionHost.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TodayPage.tsx", "to": "src/client-web/src/dialogs/TaskEditorDialog.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/TodayPage.tsx", "to": "src/client-web/src/dialogs/EventEditorDialog.tsx", "type": "depends_on" }
  ]
}
```
