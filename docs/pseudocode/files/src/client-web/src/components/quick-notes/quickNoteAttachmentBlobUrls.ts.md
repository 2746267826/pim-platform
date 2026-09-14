# src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：`extractQuickNoteAttachmentIds`：见源文件职责（quickNoteAttachmentBlobUrls.ts）。
- 主要依赖：`src/client-web/src/types`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### extractQuickNoteAttachmentIds
#### extractQuickNoteAttachmentIds(markdown: string)
- 输入：markdown: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `extractQuickNoteAttachmentIds`
  2. 赋值 `ids` = new Set<string>()
  3. 循环 for (const match of markdown.matchAll(attachmentDownloadPattern))
  4. 执行：ids.add(match[1]);
  5. 返回 Array.from(ids)
- 分支与异常：无显著分支
- 调用：extractQuickNoteAttachmentIds、markdown.matchAll、ids.add、Array.from

### rewriteQuickNoteAttachmentUrls
#### rewriteQuickNoteAttachmentUrls(markdown: string, objectUrlsByAttachmentId: Map<string, string>)
- 输入：markdown: string, objectUrlsByAttachmentId: Map<string, string>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `rewriteQuickNoteAttachmentUrls`
  2. 执行：attachmentDownloadPattern.lastIndex = 0;
  3. 返回 markdown.replace(attachmentDownloadPattern, (url, id: string) => (
  4. 执行：objectUrlsByAttachmentId.get(id) ?? url
- 分支与异常：无显著分支
- 调用：rewriteQuickNoteAttachmentUrls、markdown.replace、objectUrlsByAttachmentId.get

### getQuickNoteAttachmentIdFromDownloadUrl
#### getQuickNoteAttachmentIdFromDownloadUrl(url: string)
- 输入：url: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `getQuickNoteAttachmentIdFromDownloadUrl`
  2. 赋值 `match` = exactAttachmentDownloadPattern.exec(url)
  3. 返回 match?.[1] ?? null
- 分支与异常：无显著分支
- 调用：getQuickNoteAttachmentIdFromDownloadUrl、exactAttachmentDownloadPattern.exec

### buildQuickNoteUpdatePayload
#### buildQuickNoteUpdatePayload(contentMarkdown: string)
- 输入：contentMarkdown: string
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `buildQuickNoteUpdatePayload`
  2. 返回 JSX/结构
- 分支与异常：无显著分支
- 调用：buildQuickNoteUpdatePayload

## 近逐行中文伪代码

1. [L3] 赋值 `attachmentDownloadPattern` = /\/api\/v1\/quick-notes\/attachments\/([0-9a-fA-F-]{36})\/download/g
2. [L4] 赋值 `exactAttachmentDownloadPattern` = /^\/api\/v1\/quick-notes\/attachments\/([0-9a-fA-F-]{36})\/download$/
3. [L6] 导出函数 `extractQuickNoteAttachmentIds`
4. [L7] 赋值 `ids` = new Set<string>()
5. [L9] 循环 for (const match of markdown.matchAll(attachmentDownloadPattern))
6. [L10] 执行：ids.add(match[1]);
7. [L13] 返回 Array.from(ids)
8. [L16] 导出函数 `rewriteQuickNoteAttachmentUrls`
9. [L17] 执行：attachmentDownloadPattern.lastIndex = 0;
10. [L19] 返回 markdown.replace(attachmentDownloadPattern, (url, id: string) => (
11. [L20] 执行：objectUrlsByAttachmentId.get(id) ?? url
12. [L24] 导出函数 `getQuickNoteAttachmentIdFromDownloadUrl`
13. [L25] 赋值 `match` = exactAttachmentDownloadPattern.exec(url)
14. [L26] 返回 match?.[1] ?? null
15. [L29] 导出函数 `buildQuickNoteUpdatePayload`
16. [L30] 返回 JSX/结构

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts",
      "label": "extractQuickNoteAttachmentIds",
      "path": "src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/quick-notes/quickNoteAttachmentBlobUrls.ts",
      "to": "src/client-web/src/types",
      "type": "depends_on"
    }
  ]
}
```
