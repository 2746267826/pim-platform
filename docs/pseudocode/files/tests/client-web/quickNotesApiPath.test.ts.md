# tests/client-web/quickNotesApiPath.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：断言 quickNoteApiPaths 列表/详情/处理/归档/恢复/附件路径。
- 主要依赖：`src/client-web/src/api/quickNotes`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：list 带 status/search/page；detail/process/archive/restore；attachments 与 download

## 近逐行中文伪代码

1. [L1-2] 导入
2. [L4-10] 七条路径 equal

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/quickNotesApiPath.test.ts",
      "label": "quickNotesApiPath.test",
      "path": "tests/client-web/quickNotesApiPath.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/quickNotesApiPath.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/quickNotesApiPath.test.ts", "to": "src/client-web/src/api/quickNotes.ts", "type": "tests" }
  ]
}
```
