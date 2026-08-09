# tests/Pim.UnitTests/Ai/AiRedactorTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 `AiRedactor` JSON/明文脱敏：密钥字段、token 形态、非密钥 token 计数保留。
- 主要依赖：`AiRedactor`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. RedactJson 已知凭据字段 → [REDACTED]
2. 扩展 client_secret / x-api-key
3. 归一变体 openai_api_key/privateKey/accessToken 等
4. 保留 max_tokens/prompt_tokens/completion_tokens
5. 非法 JSON 包 raw 可解析
6. RedactPlainText Bearer/sk 与 key=value 片段；标签后仍脱敏

## 近逐行中文伪代码

1. [L1-L72] JSON 字段脱敏三测
2. [L74-L103] token 计数与非法 JSON
3. [L105-L154] 明文脱敏

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiRedactorTests.cs",
      "label": "AiRedactorTests",
      "path": "tests/Pim.UnitTests/Ai/AiRedactorTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiRedactorTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiRedactorTests.cs", "to": "src/Pim.Infrastructure/Ai/AiRedactor.cs", "type": "tests" }
  ]
}
```
