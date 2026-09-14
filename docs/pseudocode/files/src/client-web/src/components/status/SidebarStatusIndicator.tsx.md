# src/client-web/src/components/status/SidebarStatusIndicator.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：侧栏系统状态点：每 60s 拉 status summary，展示 label/message。
- 主要依赖：getStatusSummary、getHealthStatusLabel
- 被谁使用：Sidebar

## 函数级结构化伪代码

### SidebarStatusIndicator
- query status-summary refetch 60s
- isError→Unknown；loading 文案「检查中」
- 按 status 着色圆点与文字

## 近逐行中文伪代码

1. 拉汇总状态。
2. 计算 status/label/message/classes。
3. role=status 卡片渲染点+标签+截断消息。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/status/SidebarStatusIndicator.tsx",
      "label": "SidebarStatusIndicator",
      "path": "src/client-web/src/components/status/SidebarStatusIndicator.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/status/SidebarStatusIndicator.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/status/SidebarStatusIndicator.tsx", "to": "src/client-web/src/api/status.ts", "type": "depends_on" }
  ]
}
```
