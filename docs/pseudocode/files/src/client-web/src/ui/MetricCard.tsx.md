# src/client-web/src/ui/MetricCard.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：通用指标卡：label/value/helper + tone 着色。
- 主要依赖：ReactNode
- 被谁使用：各仪表盘页面

## 函数级结构化伪代码

### MetricCard
- 输入：label、value、helper?、tone(default neutral)
- 输出：pim-card section
- 步骤：按 tone 映射 value 文本色；渲染 label、value、可选 helper

## 近逐行中文伪代码

1. tone 五档 CSS 类。
2. 卡片截断 label；大号 value；可选 helper。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/MetricCard.tsx",
      "label": "MetricCard",
      "path": "src/client-web/src/ui/MetricCard.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/MetricCard.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
