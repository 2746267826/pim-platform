# tests/client-web/quickNotesAttachmentUrls.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：附件 URL 提取/改写/更新载荷。
- 主要依赖：`quickNoteAttachmentBlobUrls` 工具函数
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### extract / getFromDownloadUrl / rewrite / buildUpdatePayload
- 步骤：去重 id；仅相对路径有效；blob Map 改写；payload={contentMarkdown}

## 近逐行中文伪代码

1. [L1-17] 导入与 markdown 样例
2. [L19-24] 提取与 URL 解析
3. [L26-40] blob 改写
4. [L42] update payload

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/quickNotesAttachmentUrls.test.ts",
      "label": "quickNotesAttachmentUrls.test",
      "path": "tests/client-web/quickNotesAttachmentUrls.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/quickNotesAttachmentUrls.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/quickNotesAttachmentUrls.test.ts", "to": "src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts", "type": "tests" }
  ]
}
```
