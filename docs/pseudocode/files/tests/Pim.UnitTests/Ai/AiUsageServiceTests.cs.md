# tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：AI 请求日志筛选/用量汇总；健康检查在禁用与缺默认模型时的行为。
- 主要依赖：`AiUsageService`、`AiProviderHealthService`、InMemory Db、StubHttp*
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ListRequestsAsync_FiltersByModuleAndStatus
- 步骤：按 module+Succeeded 过滤 TotalCount=1

### GetUsageSummaryAsync_GroupsByModulePurposeModelAndStatus
- 步骤：RequestCount/Success/Failure/TotalTokens；ByModule/ByStatus 分组

### CheckAsync_WhenAiDisabled_DoesNotCallProviderAndStoresDisabledStatus
- 步骤：Enabled=false；Status=disabled；RequestCount=0；清 LastError

### CheckAsync_WhenDefaultModelIsMissing_StoresHealthError
- 步骤：模型列表无 pim-default → error；错误含 default model；不含 secret-key

### 工厂与 Stub
- 步骤：MakeLog；StubHttpClientFactory/Handler 计数

## 近逐行中文伪代码

1. [L14-30] 列表筛选
2. [L32-49] 汇总
3. [L51-71] 禁用不发 HTTP
4. [L73-90] 缺默认模型
5. [L92-168] CreateDb/Service/Health/MakeLog/Stub

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs",
      "label": "AiUsageServiceTests",
      "path": "tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs", "to": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Ai/AiUsageServiceTests.cs", "to": "src/Pim.Infrastructure/Ai/AiProviderHealthService.cs", "type": "tests" }
  ]
}
```
