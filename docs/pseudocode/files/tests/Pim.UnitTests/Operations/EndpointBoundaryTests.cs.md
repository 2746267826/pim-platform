# tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：端点离线缓存边界、心跳用户隔离、通知动作历史与阻断计数。
- 主要依赖：`EndpointStatusService`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. CollectionSignals 可离线缓存；事实/写回不可
2. Heartbeat 用户作用域
3. 低风险 Executed / 高风险 OpenDetailRequired + OnlineOnlyBlockedCount
4. 拒绝动作仍落库 Result=Rejected

## 近逐行中文伪代码

1. [L1-L37] CanCacheOffline Theory
2. [L39-L103] 心跳与通知
3. [L105-L120] helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs",
      "label": "EndpointBoundaryTests",
      "path": "tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/EndpointBoundaryTests.cs", "to": "src/Pim.Infrastructure/Endpoints/EndpointStatusService.cs", "type": "tests" }
  ]
}
```
