# tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖守护进程心跳 upsert、JSON 校验、平台独立与畸形状态容错。
- 主要依赖：`DaemonHeartbeatService`、`DaemonHeartbeatRequest`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 同设备 upsert 替换仅一行，Version/KeyStats 更新
2. 非法 StatusJson → DomainException 3010
3. 同 DeviceId windows/android 独立两行
4. 畸形 KeyStatsState 读回 Unknown，ActivityWatch 可解析

## 近逐行中文伪代码

1. [L1-L56] 替换 upsert
2. [L58-L85] 非法 JSON
3. [L87-L134] 平台独立
4. [L136-L162] 畸形状态容错

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs",
      "label": "DaemonHeartbeatServiceTests",
      "path": "tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/DaemonHeartbeatServiceTests.cs", "to": "src/Pim.Infrastructure/Operations/DaemonHeartbeatService.cs", "type": "tests" }
  ]
}
```
