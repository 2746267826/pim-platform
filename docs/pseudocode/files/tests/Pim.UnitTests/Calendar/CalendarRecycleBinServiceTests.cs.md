# tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：回收站列表、冲突预览、恢复/副本恢复与父本约束。
- 主要依赖：`CalendarRecycleBinService`、`CalendarAuditWriter`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. List 仅已删事件/任务
2. 同标题时间冲突 → CanRestoreWithoutConflict false
3. RestoreAsCopy 清 Deleted*、换 Uid、清 SourceUid
4. 恢复日历仅同 operation 子项
5. 日历/任务本不支持副本
6. 父本已删时事件/任务恢复报「请先恢复所属本」

## 近逐行中文伪代码

1. [L1-L169] 八个 Fact 场景
2. [L171+] CreateDb/Service/Seed helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs",
      "label": "CalendarRecycleBinServiceTests",
      "path": "tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/CalendarRecycleBinServiceTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "type": "tests" }
  ]
}
```
