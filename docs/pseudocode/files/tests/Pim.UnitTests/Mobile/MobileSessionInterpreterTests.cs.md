# tests/Pim.UnitTests/Mobile/MobileSessionInterpreterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证会话重建在应用切换时关闭前台会话并打 quality 标志。
- 主要依赖：`MobileSessionInterpreter`、`MobileUsageEventEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### RebuildSessionsAsync_ClosesPreviousForegroundAppOnAppSwitchAndFlagsIt
- 事件：mail FG → chat FG → chat BG
- 两会话：mail 结束于 chat 开始且含 closed-by-app-switch

## 近逐行中文伪代码

1. [L1-L37] 测试主路径
2. [L39-L57] Event 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileSessionInterpreterTests.cs",
      "label": "MobileSessionInterpreterTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileSessionInterpreterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileSessionInterpreterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileSessionInterpreterTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileSessionInterpreter.cs", "type": "tests" }
  ]
}
```
