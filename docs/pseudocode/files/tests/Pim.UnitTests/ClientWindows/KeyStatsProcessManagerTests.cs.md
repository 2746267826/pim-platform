# tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 KeyStats 进程收敛：保留当前会话一个进程，否则启动。
- 主要依赖：`KeyStatsProcessManager`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 多进程：停外会话与多余当前会话，Keep 最小 id 20，不启动
2. 无当前会话进程：停全部并 ShouldStart

## 近逐行中文伪代码

1. [L1-L24] Keep one
2. [L26-L39] Start when none

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs",
      "label": "KeyStatsProcessManagerTests",
      "path": "tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/KeyStatsProcessManagerTests.cs", "to": "Pim.Client.Core.Services.KeyStatsProcessManager", "type": "tests" }
  ]
}
```
