# src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：从速记 Markdown 正文中提取附件下载 URL 所引用的附件 Guid，去重后返回。
- 主要依赖：`System.Text.RegularExpressions`（`GeneratedRegex`）
- 被谁使用：`QuickNoteService`（保存/更新时解析正文引用）

## 函数级结构化伪代码

### QuickNoteMarkdownReferences
#### static ExtractAttachmentIds(string? markdown)
- 输入：`markdown` 可选正文
- 输出：`IReadOnlyList<Guid>`，按首次出现顺序、去重
- 副作用：无
- 步骤：
  1. 若 `markdown` 为空或仅空白 → 返回空数组。
  2. 初始化 `ids` 列表与 `seen` 集合。
  3. 对 `AttachmentUrlRegex()` 在正文中的每个匹配：
     - 解析命名组 `id` 为 Guid；失败则跳过。
     - 若 `seen.Add(id)` 成功则追加到 `ids`。
  4. 返回 `ids`。
- 分支与异常：`Guid.TryParse` 失败静默跳过；无抛异常路径
- 调用：`AttachmentUrlRegex().Matches`、`Guid.TryParse`

#### private static partial AttachmentUrlRegex()
- 输入：无
- 输出：编译期生成的 `Regex`
- 副作用：无
- 步骤：匹配形如 `/api/v1/quick-notes/attachments/{guid}/download` 的 URL 片段，要求前后为行首/空白/`(`/行尾/空白/`)`
- 分支与异常：无
- 调用：源生成器

## 近逐行中文伪代码

1. 定义静态分部类 `QuickNoteMarkdownReferences`。
2. `ExtractAttachmentIds`：空正文直接返回空列表。
3. 建 `List<Guid>` 与 `HashSet<Guid>` 去重。
4. 用生成正则扫描全部匹配；命名组 `id` 解析失败则 continue。
5. 未见过的 id 加入结果列表。
6. 返回结果。
7. `AttachmentUrlRegex`：`GeneratedRegex` 编译匹配附件 download 路径中的 36 位 Guid。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs",
      "label": "QuickNoteMarkdownReferences",
      "path": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs.md",
      "layer": "module.quicknotes",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteMarkdownReferences.cs", "type": "calls" }
  ]
}
```
