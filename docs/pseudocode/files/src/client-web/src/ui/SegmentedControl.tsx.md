# src/client-web/src/ui/SegmentedControl.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：无障碍分段控件：radio 组、方向键/Home/End 导航。
- 主要依赖：React useRef
- 被谁使用：Today/Calendar/Workbench 等

## 函数级结构化伪代码

### SegmentedControl
- radiogroup；选中 tabIndex0；方向键循环 onChange 并 focus

## 近逐行中文伪代码

1. 找 selectedIndex。
2. 键盘移动选项。
3. 点击切换 value。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/SegmentedControl.tsx",
      "label": "SegmentedControl",
      "path": "src/client-web/src/ui/SegmentedControl.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/SegmentedControl.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
`
