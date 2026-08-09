# tests/client-web/quickNoteFloatingState.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：校验速记浮层 localStorage 键名与面板位置 clamp 边界。
- 主要依赖：`quickNoteFloatingState` 的键常量与 `clampPanelPosition`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：
  1. 断言草稿键 `pim.quickNotes.floatingDraft`
  2. 断言位置键 `pim.quickNotes.panelPosition`
  3. 越界坐标 clamp 到边距内
  4. 合法坐标原样返回

## 近逐行中文伪代码

1. [L1-6] 导入常量与 clampPanelPosition
2. [L8-9] 键名 equal
3. [L10-13] x=-50,y=9999 → {12,368}（视口 1200×800，面板 360×420）
4. [L14-17] 合法 {500,200} 不变

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/quickNoteFloatingState.test.ts",
      "label": "quickNoteFloatingState.test",
      "path": "tests/client-web/quickNoteFloatingState.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/quickNoteFloatingState.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/client-web/quickNoteFloatingState.test.ts",
      "to": "src/client-web/src/components/quick-notes/quickNoteFloatingState.ts",
      "type": "tests"
    }
  ]
}
```
