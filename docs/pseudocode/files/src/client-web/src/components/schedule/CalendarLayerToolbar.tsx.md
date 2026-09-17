# src/client-web/src/components/schedule/CalendarLayerToolbar.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：日历图层开关工具栏 + 仅微软同步复选。
- 主要依赖：CalendarLayerId
- 被谁使用：CalendarPage

## 函数级结构化伪代码

### CalendarLayerToolbar
- options map 按钮 aria-pressed；checkbox outlookOnly

## 近逐行中文伪代码

1. activeLayerIds 建 Set。
2. 点击 toggle 图层。
3. 右侧仅微软同步。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/schedule/CalendarLayerToolbar.tsx",
      "label": "CalendarLayerToolbar",
      "path": "src/client-web/src/components/schedule/CalendarLayerToolbar.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/schedule/CalendarLayerToolbar.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/schedule/CalendarLayerToolbar.tsx",
      "to": "src/client-web/src/types/index.ts",
      "type": "depends_on"
    }
  ]
}
`
