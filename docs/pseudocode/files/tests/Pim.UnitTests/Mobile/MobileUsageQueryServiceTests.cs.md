# tests/Pim.UnitTests/Mobile/MobileUsageQueryServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：用量摘要解释版本化批错误；窗口内位置计数；fallback SQL 可翻译。
- 主要依赖：`MobileUsageQueryService`、SyncBatch/Location 实体
- 被谁使用：dotnet test

## 函数级结构化伪代码

### GetSummaryAsync_InterpretsVersionedBatchErrorsWithoutMisreportingSuccess
- 步骤：envelope 错误拼接；legacy JSON 原样；成功批 ErrorMessage null

### GetSummaryAsync_ReturnsLocationCountsForBatchWindow
- 步骤：AcceptedEventCount 与 usable/rejected location 计数

### WhereFallbackSummaries_BuildsRelationalQueryWithoutClientMethod
- 步骤：ToQueryString 含 fallback/summary

## 近逐行中文伪代码

1. [L13-52] 批错误解释
2. [L54-92] 位置计数
3. [L94-109] SQL
4. [L111+] 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileUsageQueryServiceTests.cs",
      "label": "MobileUsageQueryServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileUsageQueryServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileUsageQueryServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileUsageQueryServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "type": "tests" }
  ]
}
```
