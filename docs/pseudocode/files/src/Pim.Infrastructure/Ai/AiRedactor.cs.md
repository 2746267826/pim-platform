# src/Pim.Infrastructure/Ai/AiRedactor.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：对 AI 请求/响应中的敏感键与 token 形态字符串做脱敏，供日志落库前使用
- 主要依赖：`System.Text`、`System.Text.Json`、`System.Text.RegularExpressions`（含源生成 Regex）
- 被谁使用：`AiRequestLogWriter` 写日志前脱敏；`AiRedactorTests` 单元测试

## 函数级结构化伪代码

### AiRedactor（static partial）
#### static RedactJson(string? json)
- 输入：JSON 字符串（可空/空白）
- 输出：脱敏后的 JSON 字符串；空输入返回 `"{}"`；非法 JSON 包装为 `{"raw":"..."}` 且 raw 已明文脱敏
- 副作用：解析与重写 JSON（内存流）
- 步骤：
  1. 若 `json` 空或空白 → 返回 `"{}"`
  2. `try`：`JsonDocument.Parse` 根元素
  3. 用 `Utf8JsonWriter` 调用 `WriteRedacted(root, writer, propertyName: null)`
  4. 将流字节解码为 UTF-8 字符串返回
  5. `catch JsonException`：序列化匿名对象 `{ raw = RedactPlainText(json) ?? "" }` 并返回
- 分支与异常：捕获 `JsonException` 走明文脱敏回退
- 调用：`JsonDocument.Parse`、`WriteRedacted`、`RedactPlainText`、`JsonSerializer.Serialize`

#### static RedactPlainText(string? text)
- 输入：任意明文（可空）
- 输出：脱敏后的字符串；`null` 入参返回 `null`
- 副作用：无
- 步骤：
  1. 若 `text is null` → 返回 `null`
  2. 用 `SensitiveKeyValueRegex` 匹配 `key=value` / `key:value` 片段
  3. 对每个匹配：若 `IsSensitiveKey(key)` 则保留边界/前缀/引号，值替换为 `[REDACTED]`；否则原样保留
  4. 再用 `TokenLikeValueRegex` 将 Bearer / sk- / JWT 形态串替换为 `[REDACTED]`
  5. 返回结果
- 分支与异常：无抛出分支
- 调用：`SensitiveKeyValueRegex`、`IsSensitiveKey`、`TokenLikeValueRegex`

#### private static WriteRedacted(JsonElement element, Utf8JsonWriter writer, string? propertyName)
- 输入：当前 JSON 元素、写入器、所属属性名（可空）
- 输出：无（写入 `writer`）
- 副作用：向 `writer` 写出脱敏结构
- 步骤：
  1. 若 `propertyName` 非空且 `IsSensitiveKey` → 写字符串 `"[REDACTED]"` 并返回（整值替换）
  2. 按 `ValueKind`：
     - Object：开始对象；对每个属性写名后递归 `WriteRedacted(value, writer, name)`；结束对象
     - Array：开始数组；对每项递归（`propertyName: null`）；结束数组
     - String：写 `RedactPlainText(GetString())`（null 则空串）
     - 其他：`element.WriteTo(writer)` 原样写出
- 分支与异常：无
- 调用：`IsSensitiveKey`、`RedactPlainText`、`JsonElement.WriteTo`

#### private static IsSensitiveKey(string key)
- 输入：属性/键名
- 输出：是否视为敏感
- 副作用：无
- 步骤：
  1. 若在 `NonSecretKeys`（如 token 计数字段）→ false
  2. 若在 `SensitiveKeys` 精确集合 → true
  3. `NormalizeKey` 后启发式：含 `apikey`/`token`/`secret`/`password`/`authorization`/`privatekey` → true
  4. 否则 false
- 分支与异常：无
- 调用：`NormalizeKey`

#### private static NormalizeKey(string key)
- 输入：键名
- 输出：去掉 `_`/`-`/`.`/空白 后的小写字符串
- 副作用：无
- 步骤：
  1. 遍历字符，跳过分隔与空白，其余 `ToLowerInvariant` 追加
  2. 返回构建结果
- 分支与异常：无
- 调用：无

#### private static partial TokenLikeValueRegex() / SensitiveKeyValueRegex()
- 输入：无（源生成正则工厂）
- 输出：对应 `Regex` 实例
- 副作用：无
- 步骤：
  1. `TokenLikeValueRegex`：匹配 Bearer 令牌、`sk-...`、JWT 三段式
  2. `SensitiveKeyValueRegex`：匹配边界后的敏感键名 + `=`/`:` + 值
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. using：`System.Text`、`System.Text.Json`、`System.Text.RegularExpressions`
2. 命名空间 `Pim.Infrastructure.Ai`
3. 静态分部类 `AiRedactor`
4. 静态集合 `SensitiveKeys`：authorization、api_key、token、password、secret、client_secret、private_key、virtual_key 等（忽略大小写）
5. 静态集合 `NonSecretKeys`：`max_tokens`、`prompt_tokens`、`completion_tokens`、`total_tokens`
6. `RedactJson`：空/空白 → `"{}"`
7.   尝试解析 JSON，递归 `WriteRedacted` 写出脱敏 JSON 并 UTF-8 返回
8.   解析失败：`{"raw": RedactPlainText(原串)}` 序列化返回
9. `RedactPlainText`：null → null
10.  先按敏感键值正则替换值为 `[REDACTED]`（键不敏感则保留）
11.  再替换 Bearer / sk- / JWT 形态为 `[REDACTED]`
12. `WriteRedacted`：属性名敏感则整值写 `[REDACTED]`
13.  对象/数组递归；字符串再走明文脱敏；数字布尔等原样 `WriteTo`
14. `IsSensitiveKey`：白名单非密钥 → 否；精确敏感集 → 是；规范化后子串启发式 → 是/否
15. `NormalizeKey`：去掉分隔符与空白并小写
16. 源生成正则：`TokenLikeValueRegex`、`SensitiveKeyValueRegex`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiRedactor.cs",
      "label": "AiRedactor",
      "path": "src/Pim.Infrastructure/Ai/AiRedactor.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiRedactor.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiRequestLogWriter.cs", "to": "src/Pim.Infrastructure/Ai/AiRedactor.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Ai/AiRedactorTests.cs", "to": "src/Pim.Infrastructure/Ai/AiRedactor.cs", "type": "tests" }
  ]
}
```
