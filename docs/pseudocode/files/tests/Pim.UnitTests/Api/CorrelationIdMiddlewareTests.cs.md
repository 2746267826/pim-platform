# tests/Pim.UnitTests/Api/CorrelationIdMiddlewareTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：CorrelationId 合法 trim/字符；非法或过长替换为 32 位 N 格式 Guid。
- 主要依赖：`CorrelationIdMiddleware.ResolveCorrelationId`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### AcceptsValidIncomingId / ReplacesInvalid / ReplacesOversized
- 步骤：合法保留；非法/129 长 → 新 Guid N

## 近逐行中文伪代码

1. [L8-16] 合法
2. [L18-29] 非法
3. [L31-41] 过长

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Api/CorrelationIdMiddlewareTests.cs",
      "label": "CorrelationIdMiddlewareTests",
      "path": "tests/Pim.UnitTests/Api/CorrelationIdMiddlewareTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Api/CorrelationIdMiddlewareTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Api/CorrelationIdMiddlewareTests.cs", "to": "src/Pim.Api/Infrastructure/CorrelationIdMiddleware.cs", "type": "tests" }
  ]
}
```
