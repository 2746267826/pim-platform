# tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：速记 CRUD：创建审计、列表筛选、非法状态、处理/归档/恢复/删除、跨用户拒绝。
- 主要依赖：QuickNoteService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### CreateAsync_CreatesInboxNoteAndAuditLog
### ListAsync_FiltersByStatusAndSearch
### UpdateAsync_RejectsInvalidStatus
### ProcessArchiveRestoreAndDelete_ApplyExpectedState
### GetAsync_RejectsOtherUsersNote

## 近逐行中文伪代码

1. 创建 inbox+审计
2. 筛选
3. 非法状态
4. 状态机流转
5. 跨用户

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs",
      "label": "QuickNoteServiceTests.cs",
      "path": "tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/QuickNotes/QuickNoteServiceTests.cs","to":"src/Pim.Module.QuickNotes/Services/QuickNoteService.cs","type":"tests"}
}
```