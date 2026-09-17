# tests/client-web/quickNotesEntryBundle.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：确保速记页与浮层 lazy 加载，不进入入口静态 import。
- 主要依赖：AppLayout.tsx
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：AppLayout 含 lazy QuickNotesPage / QuickNoteFloatingPanel；不匹配静态 import

## 近逐行中文伪代码

1. 读 AppLayout 源
2. match lazy 两行
3. doesNotMatch 静态 import

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/quickNotesEntryBundle.test.ts",
      "label": "quickNotesEntryBundle.test.ts",
      "path": "tests/client-web/quickNotesEntryBundle.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/quickNotesEntryBundle.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/client-web/quickNotesEntryBundle.test.ts","to":"src/client-web/src/layout/AppLayout.tsx","type":"tests"}
}
```