# src/client-web/src/dialogs/common.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：通用 Dialog 遮罩与 Field 表单标签。
- 主要依赖：ReactNode
- 被谁使用：各 EditorDialog

## 函数级结构化伪代码

### Dialog
- open 假返回 null；否则 fixed 遮罩 + 白卡片 title/children；点遮罩 onClose

### Field
- label + children 的 block label

## 近逐行中文伪代码

1. Dialog 居中 max-w-lg 可滚动。
2. Field 灰标签包裹控件。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/dialogs/common.tsx",
      "label": "dialogs/common",
      "path": "src/client-web/src/dialogs/common.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/dialogs/common.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
