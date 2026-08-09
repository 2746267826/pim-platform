# tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Hangfire 失败数映射与 GetSummary 健康/警告/存储失败。
- 主要依赖：`HangfireJobStatusService`、`IHangfireMonitoringClient`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. MapFailedCount 0→Healthy / 1→Warning
2. 健康快照中文消息与计数
3. Failed>0 Warning
4. 存储抛错 Critical 零计数

## 近逐行中文伪代码

1. [L1-L61] 四测
2. [L63-L79] Fake/Throwing client

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs",
      "label": "HangfireJobStatusServiceTests",
      "path": "tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/HangfireJobStatusServiceTests.cs", "to": "src/Pim.Infrastructure/Operations/HangfireJobStatusService.cs", "type": "tests" }
  ]
}
```
