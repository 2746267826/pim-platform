# tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 `KeyStatsHealthProbe.Evaluate` 各 DetailState 与上传能力。
- 主要依赖：`KeyStatsHealthProbe`、`KeyStatsProcessInfo`、`KeyStatsCounterSnapshot`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 无进程 → MissingProcess / 不可上传
2. API 错误 → ApiUnreachable
3. 全零无增长 → ApiOkButStaleZero / skipReason stale-zero
4. 非零计数 → Available
5. 相对 previous 增长 → Available
6. 含外会话进程 → HasForeignSessionProcess

## 近逐行中文伪代码

1. [L1-L22] MissingProcess
2. [L24-L41] ApiUnreachable
3. [L43-L70] StaleZero
4. [L72-L121] Available 两路径
5. [L123-L142] 外会话标志

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs",
      "label": "KeyStatsHealthProbeTests",
      "path": "tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/KeyStatsHealthProbeTests.cs", "to": "Pim.Client.Core.Services.KeyStatsHealthProbe", "type": "tests" }
  ]
}
```
