# tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证 `KeyStatsCollectorService.ShouldUpload` 对健康探测结果的上传门禁。
- 主要依赖：`KeyStatsHealthProbe`、`KeyStatsCollectorService`、`KeyStatsProcessInfo`/`KeyStatsCounterSnapshot`
- 被谁使用：xUnit

## 函数级结构化伪代码

### KeyStatsCollectorUploadGateTests
#### ShouldUpload_IsFalse_ForStaleZero
- 进程在、计数全 0 的健康评估 → ShouldUpload false
#### ShouldUpload_IsTrue_ForAvailable
- 有有效计数快照 → ShouldUpload true

## 近逐行中文伪代码

1. [L1-L8] using 与类
2. [L9-L20] StaleZero 场景
3. [L22-L33] Available 场景

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs",
      "label": "KeyStatsCollectorUploadGateTests",
      "path": "tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs", "to": "src/client-windows", "type": "tests" },
    { "from": "tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs", "to": "Pim.Client.Core.Services.KeyStatsCollectorService", "type": "tests" },
    { "from": "tests/Pim.UnitTests/ClientWindows/KeyStatsCollectorUploadGateTests.cs", "to": "Pim.Client.Core.Services.KeyStatsHealthProbe", "type": "tests" }
  ]
}
```
