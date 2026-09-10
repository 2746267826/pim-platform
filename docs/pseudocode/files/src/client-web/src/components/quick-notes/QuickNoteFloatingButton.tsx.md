# src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `QuickNoteFloatingButton`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### QuickNoteFloatingButton
#### QuickNoteFloatingButton({ onClick }: QuickNoteFloatingButtonProps)
- 输入：{ onClick }: QuickNoteFloatingButtonProps
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 默认导出函数 `QuickNoteFloatingButton`
  2. 返回 JSX/结构
  3. 执行：type="button"
  4. 执行：aria-label="打开快速记录"
  5. 执行：title="打开快速记录"
  6. 执行：onClick={onClick}
  7. 执行：className="fixed bottom-5 right-5 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-blue-600 tex
  8. 执行：</button>
- 分支与异常：无显著分支
- 调用：QuickNoteFloatingButton

## 近逐行中文伪代码

1. [L1] 定义类型 `QuickNoteFloatingButtonProps`
2. [L2] 执行：onClick: () => void;
3. [L5] 默认导出函数 `QuickNoteFloatingButton`
4. [L6] 返回 JSX/结构
5. [L8] 执行：type="button"
6. [L9] 执行：aria-label="打开快速记录"
7. [L10] 执行：title="打开快速记录"
8. [L11] 执行：onClick={onClick}
9. [L12] 执行：className="fixed bottom-5 right-5 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-blue-600 tex
10. [L15] 执行：</button>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx",
      "label": "QuickNoteFloatingButton",
      "path": "src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/quick-notes/QuickNoteFloatingButton.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
