# tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：`AiRequestLogWriter` 持久化脱敏、状态映射、时长/哈希与非法 JSON 包装。
- 主要依赖：`AiRequestLogWriter`、`AiRedactor`（间接）
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 失败日志 redact payload/metadata
2. 成功保留 plain ResponseText
3. 全字符串字段 canary 清除
4. 非法 JSON 可 Parse 且脱敏
5. DurationMs/InputChars/OutputChars/SHA256 确定性
6. StatusCases Theory 映射
7. ResponseText token 与 key=value/前缀密钥脱敏

## 近逐行中文伪代码

1. [L1-L176] 失败/明文/全字段 redact
2. [L178-L282] 非法 JSON 与哈希
3. [L284-L478] 状态与明文密钥
4. [L480-L484] Sha256 helper

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs",
      "label": "AiRequestLogWriterTests",
      "path": "tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Ai/AiRequestLogWriterTests.cs", "to": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "type": "tests" }
  ]
}
```
