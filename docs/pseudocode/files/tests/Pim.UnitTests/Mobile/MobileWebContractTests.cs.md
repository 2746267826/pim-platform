# tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Web/客户端 JSON 契约：Ingest ack、Summary、LocationHistory、LocationAnalyticsOverview。
- 主要依赖：Mobile DTO 序列化
- 被谁使用：dotnet test

## 函数级结构化伪代码

### IngestResponse_SerializesItemAcknowledgementsExpectedByMobileClients
### SummaryResponse_SerializesDashboardFieldsExpectedByWeb
### LocationHistoryResponse_SerializesPointsWrapperExpectedByWeb
### LocationAnalyticsOverviewResponse_SerializesWorkbenchFieldsExpectedByWeb

## 近逐行中文伪代码

1. 序列化样例字段名/结构
2. 四类响应契约

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs",
      "label": "MobileWebContractTests.cs",
      "path": "tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Mobile/MobileWebContractTests.cs","to":"src/Pim.Module.Mobile/DTOs","type":"tests"}
}
```