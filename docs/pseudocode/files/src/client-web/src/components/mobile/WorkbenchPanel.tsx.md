# src/client-web/src/components/mobile/WorkbenchPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：移动工作台通用面板壳：标题、可选描述、可选 action 槽与 children 内容区。
- 主要依赖：React `ReactNode`
- 被谁使用：mobile 工作台各子面板布局

## 函数级结构化伪代码

### WorkbenchPanelProps
- title: string
- description?: string
- action?: ReactNode
- children: ReactNode

### WorkbenchPanel(props)
- 输入：标题/描述/动作/子节点
- 输出：带边框的 section
- 副作用：无
- 步骤：
  1. section：白底边框 overflow-hidden
  2. 顶栏 flex：左 title + 可选 description；右 action
  3. 渲染 children（无额外 padding 包装）
- 分支与异常：description 缺省不渲染 p
- 调用：无

## 近逐行中文伪代码

1. 定义 props：title、可选 description/action、children
2. 外层 section 卡片样式
3. 顶栏左右布局标题区与 action
4. 下方直接放 children

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/WorkbenchPanel.tsx",
      "label": "WorkbenchPanel",
      "path": "src/client-web/src/components/mobile/WorkbenchPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/WorkbenchPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
