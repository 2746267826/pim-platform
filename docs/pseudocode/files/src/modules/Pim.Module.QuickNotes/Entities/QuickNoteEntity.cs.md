# src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：速记笔记主实体（表 `quick_notes`）及状态/来源常量；实现软删除。
- 主要依赖：
  - `Pim.Core.Data.ISoftDeletable`
  - `QuickNoteAttachmentEntity`（导航集合）
- 被谁使用：`QuickNoteService`、`PimDbContext`、QuickNotes 模块查询/写入路径

## 函数级结构化伪代码

### QuickNoteStatuses（static）
#### 常量
- 输入：无
- 输出：`Inbox` / `Processed` / `Archived` 字符串常量
- 副作用：无
- 步骤：
  1. 定义三种合法状态字面量。
- 分支与异常：无
- 调用：无

#### `IsValid(string status)`
- 输入：待校验状态字符串
- 输出：bool（是否为三种之一）
- 副作用：无
- 步骤：
  1. 用 pattern matching 判断是否等于 Inbox/Processed/Archived。
- 分支与异常：其他值返回 false
- 调用：无

### QuickNoteSources（static）
#### 常量
- 输入：无
- 输出：`WebFloating` / `WebPage` 来源常量
- 副作用：无
- 步骤：
  1. 定义 Web 浮窗与 Web 页面两种来源。
- 分支与异常：无
- 调用：无

### QuickNoteEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 主键 `Id` Guid，默认 `NewGuid()`。
  2. `UserId` 归属用户；`ContentMarkdown` 正文默认空串。
  3. `Status` 默认 Inbox；`Source` 默认 WebPage。
  4. `MetadataJson` jsonb 默认 `{}`。
  5. `CreatedAt`/`UpdatedAt` 默认 UtcNow；`ArchivedAt`/`DeletedAt` 可空。
  6. `Attachments` 一对多导航，默认空列表。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 `ISoftDeletable`。
2. `QuickNoteStatuses`：inbox / processed / archived；`IsValid` 三选一。
3. `QuickNoteSources`：web-floating / web-page。
4. 表映射 `quick_notes`，类实现 `ISoftDeletable`。
5. 列：id、user_id、content_markdown、status、source、metadata_json(jsonb)、时间戳与软删字段。
6. 导航集合 `Attachments` 指向附件实体。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs",
      "label": "QuickNoteEntity",
      "path": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs.md",
      "layer": "module.quicknotes",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "to": "src/Pim.Core/Data/ISoftDeletable.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteAttachmentEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.QuickNotes/Entities/QuickNoteEntity.cs", "type": "depends_on" }
  ]
}
```
