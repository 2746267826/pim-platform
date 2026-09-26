# src/client-web/src/pages/PcDetailQueryPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：PC 详细数据页壳：标题 + PcDetailQueryPanel。
- 主要依赖：PcDetailQueryPanel
- 被谁使用：路由

## 函数级结构化伪代码

### PcDetailQueryPage
- 居中容器 h2 + 白卡片包裹查询面板

## 近逐行中文伪代码

1. 渲染标题「PC记录 详细数据」。
2. 嵌入 PcDetailQueryPanel。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/PcDetailQueryPage.tsx",
      "label": "PcDetailQueryPage",
      "path": "src/client-web/src/pages/PcDetailQueryPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/PcDetailQueryPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/PcDetailQueryPage.tsx", "to": "src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx", "type": "depends_on" }
  ]
}
```
