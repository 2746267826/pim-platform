# src/client-web/src/ui/PageHeader.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：页面顶栏：标题/副标题 + beforeActions/actions 插槽。
- 主要依赖：ReactNode
- 被谁使用：多数业务页

## 函数级结构化伪代码

### PageHeader
- pim-panel header；左 title/subtitle；右 beforeActions 与 actions

## 近逐行中文伪代码

1. 接收 title/subtitle/插槽。
2. flex 布局两端对齐。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/PageHeader.tsx",
      "label": "PageHeader",
      "path": "src/client-web/src/ui/PageHeader.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/PageHeader.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
