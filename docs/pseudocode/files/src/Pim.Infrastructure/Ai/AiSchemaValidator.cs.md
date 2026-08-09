# src/Pim.Infrastructure/Ai/AiSchemaValidator.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：用 JSON Schema 校验 AI 模型原始响应文本；成功时返回规范化 JSON，失败时汇总错误列表。
- 主要依赖：`System.Text.Json`；`Json.Schema`（`JsonSchema`、`EvaluationResults`、`EvaluationOptions`、`OutputFormat`）
- 被谁使用：
  - `src/Pim.Infrastructure/Ai/AiGateway.cs`（网关在结构化输出路径调用 `Validate`）
  - `tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs`（单元测试）

## 函数级结构化伪代码

### AiSchemaValidationResult
#### AiSchemaValidationResult(IsValid, ParsedOutputJson, Errors)
- 输入：是否通过、规范化后的输出 JSON（失败为 null）、错误消息只读列表
- 输出：校验结果记录
- 副作用：无
- 步骤：
  1. 作为 `Validate` 的统一返回载体
- 分支与异常：无
- 调用：无

### AiSchemaValidator
#### Validate(string responseText, string schemaJson) → AiSchemaValidationResult
- 输入：模型响应文本、JSON Schema 文本
- 输出：`AiSchemaValidationResult`（成功含 `ParsedOutputJson`；失败含 `Errors`）
- 副作用：无（纯函数；临时 `JsonDocument` 在 `using` 内释放）
- 步骤：
  1. `try`：`JsonDocument.Parse(responseText)` 解析响应
  2. `ParseSchema(schemaJson)` 得到 `JsonSchema`
  3. `EvaluateSchema(schema, document.RootElement)` 评估实例
  4. 若 `results.IsValid`：序列化根元素为 JSON，返回 `IsValid=true`、空错误列表
  5. 否则：`CollectErrors` 递归收集并 `Distinct`；若无具体错误则回退默认消息 `"JSON did not match schema."`
  6. `catch JsonException`：返回无效 JSON 错误
  7. `catch InvalidSchemaException`：返回无效 schema 错误
- 分支与异常：
  - 响应非 JSON → `Invalid JSON: …`
  - schema 非法/不支持 → `Invalid schema: …`
  - schema 合法但实例不匹配 → 位置 + 关键字错误列表
- 调用：`ParseSchema`、`EvaluateSchema`、`CollectErrors`、`JsonDocument.Parse`、`JsonSerializer.Serialize`

#### ParseSchema(string schemaJson) → JsonSchema（private）
- 输入：schema 文本
- 输出：`JsonSchema` 实例
- 副作用：无
- 步骤：
  1. `try`：`JsonSchema.FromText(schemaJson)`
  2. `catch JsonSchemaException | JsonException`：包装为 `InvalidSchemaException` 再抛出
- 分支与异常：见上
- 调用：`JsonSchema.FromText`

#### EvaluateSchema(JsonSchema schema, JsonElement response) → EvaluationResults（private）
- 输入：已解析 schema、响应根元素
- 输出：评估结果（List 输出格式）
- 副作用：无
- 步骤：
  1. `try`：`schema.Evaluate(response, new EvaluationOptions { OutputFormat = OutputFormat.List })`
  2. `catch JsonSchemaException | ArgumentException | NotSupportedException`：包装为 `InvalidSchemaException`
- 分支与异常：评估期 schema/选项异常统一上抛为无效 schema
- 调用：`JsonSchema.Evaluate`

#### CollectErrors(EvaluationResults results) → IEnumerable&lt;string&gt;（private）
- 输入：评估结果节点
- 输出：错误消息序列
- 副作用：无
- 步骤：
  1. 若 `results.Errors` 非空：对每个键值对 yield `"$location: key value"`（location 经 `FormatInstanceLocation`）
  2. 若 `results.Details` 为空：结束
  3. 否则对每个子 `detail` 递归 `CollectErrors` 并 yield
- 分支与异常：无异常抛出
- 调用：`FormatInstanceLocation`、自身递归

#### FormatInstanceLocation(EvaluationResults results) → string（private）
- 输入：评估结果
- 输出：实例路径字符串；空白时返回 `"$"`
- 副作用：无
- 步骤：
  1. `results.InstanceLocation.ToString()`
  2. 空白则返回根路径 `$`
- 分支与异常：无
- 调用：无

### InvalidSchemaException（private sealed）
#### InvalidSchemaException(string message, Exception innerException)
- 输入：消息与内部异常
- 输出：内部异常类型实例
- 副作用：无
- 步骤：
  1. 主构造函数转发到 `Exception(message, innerException)`，仅在本类内使用
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 `System.Text.Json` 与 `Json.Schema`
2. 命名空间 `Pim.Infrastructure.Ai`
3. 定义结果记录 `AiSchemaValidationResult(IsValid, ParsedOutputJson, Errors)`
4. 静态类 `AiSchemaValidator` 开始
5. 公开 `Validate`：
6.   尝试将 `responseText` 解析为 `JsonDocument`
7.   调用 `ParseSchema` 得到 schema
8.   调用 `EvaluateSchema` 评估根元素
9.   若有效：序列化根元素，返回成功结果与空错误
10.  若无效：收集去重错误；无错误时使用默认不匹配消息；返回失败且 `ParsedOutputJson=null`
11.  捕获 `JsonException` → 无效 JSON 失败结果
12.  捕获 `InvalidSchemaException` → 无效 schema 失败结果
13. `ParseSchema`：`FromText`；将 schema/JSON 解析异常包装为 `InvalidSchemaException`
14. `EvaluateSchema`：以 `OutputFormat.List` 评估；将 schema/参数/不支持异常包装为 `InvalidSchemaException`
15. `CollectErrors`：输出本层 Errors（带实例路径）；再递归 Details
16. `FormatInstanceLocation`：路径空白则用 `$`
17. 私有嵌套异常类 `InvalidSchemaException`：携带 message + inner
18. 类结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiSchemaValidator.cs",
      "label": "AiSchemaValidator",
      "path": "src/Pim.Infrastructure/Ai/AiSchemaValidator.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiSchemaValidator.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiGateway.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaValidator.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Ai/AiSchemaValidatorTests.cs", "to": "src/Pim.Infrastructure/Ai/AiSchemaValidator.cs", "type": "tests" }
  ]
}
```
