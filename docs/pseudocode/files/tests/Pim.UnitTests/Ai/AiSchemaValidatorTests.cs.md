# tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 `AiSchemaValidator.Validate` 合法/非法输出、紧凑 JSON、坏响应、坏 schema、$ref 不可解析。
- 主要依赖：`Pim.Infrastructure.Ai.AiSchemaValidator`、Xunit
- 被谁使用：dotnet test

## 函数级结构化伪代码

### 常量 SchemaJson
- 对象、required title、string、additionalProperties=false

### Validate_ValidOutput_ReturnsParsedCompactJson
- 输入：`{"title":"Inbox"}` + SchemaJson
- 步骤：IsValid；ParsedOutputJson 原样；Errors 空

### Validate_InvalidOutput_ReturnsSchemaErrors
- 输入：缺 title 的 `{"name":"Inbox"}`
- 步骤：IsValid=false；Parsed=null；错误含 title 与 `$`，不以 `:` 开头

### Validate_FormattedValidOutput_ReturnsCompactParsedJson
- 输入：带空白格式化 JSON
- 步骤：解析后紧凑为 `{"title":"Inbox"}`

### Validate_InvalidResponseJson_ReturnsInvalidJsonError
- 步骤：错误以 `Invalid JSON:` 开头

### Validate_InvalidSchemaJson_ReturnsInvalidSchemaError
- 步骤：错误以 `Invalid schema:` 开头

### Validate_UnresolvableSchemaReference_ReturnsInvalidSchemaError
- 输入：`$ref` 指向 missing
- 步骤：Invalid schema

## 近逐行中文伪代码

1. [L8-17] 固定 schema 字符串
2. [L19-29] 合法紧凑 JSON
3. [L31-41] 缺 required 字段
4. [L43-57] 格式化 JSON 压成紧凑
5. [L59-67] 残缺 JSON
6. [L69-77] 残缺 schema
7. [L79-87] 不可解析 $ref

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs",
      "label": "AiSchemaValidatorTests",
      "path": "tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs",
      "to": "src/Pim.Infrastructure/Ai/AiSchemaValidator.cs",
      "type": "tests"
    }
  ]
}
```
