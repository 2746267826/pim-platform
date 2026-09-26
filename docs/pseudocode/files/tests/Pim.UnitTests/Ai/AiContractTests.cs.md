# tests/Pim.UnitTests/Ai/AiContractTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：AI 契约：尝试次数夹紧、FailedValidation 用户错误、状态字符串序列化。
- 主要依赖：Pim.Core.Ai 类型
- 被谁使用：dotnet test

## 函数级结构化伪代码

### AiGatewayRequest_ClampsAttemptsToFirstVersionHardLimit
### AiResult_FailedValidationIncludesUserFacingErrorAndLogId
### AiResult_SerializesStatusAsString

## 近逐行中文伪代码

1. 构造请求夹紧 MaxAttempts
2. FailedValidation 含错误与 LogId
3. JSON 状态为字符串

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiContractTests.cs",
      "label": "AiContractTests.cs",
      "path": "tests/Pim.UnitTests/Ai/AiContractTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiContractTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Ai/AiContractTests.cs","to":"src/Pim.Core/Ai","type":"tests"}
}
```