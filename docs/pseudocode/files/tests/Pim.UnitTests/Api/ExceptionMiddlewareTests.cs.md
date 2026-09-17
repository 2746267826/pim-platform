# tests/Pim.UnitTests/Api/ExceptionMiddlewareTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：ExceptionMiddleware 将 DomainException 映射 404/400。
- 主要依赖：ExceptionMiddleware / DomainException
- 被谁使用：dotnet test

## 函数级结构化伪代码

### InvokeAsync_MapsKnownNotFoundDomainErrorsTo404
### InvokeAsync_MapsValidationDomainErrorsTo400

## 近逐行中文伪代码

1. 已知 not-found 错误码 → 404
2. 校验类 DomainException → 400

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Api/ExceptionMiddlewareTests.cs",
      "label": "ExceptionMiddlewareTests.cs",
      "path": "tests/Pim.UnitTests/Api/ExceptionMiddlewareTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Api/ExceptionMiddlewareTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Api/ExceptionMiddlewareTests.cs","to":"src/Pim.Api/Infrastructure/ExceptionMiddleware.cs","type":"tests"}
}
```