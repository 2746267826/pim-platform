# src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：解析活动分类规则 `conditions_json`（`{ "all": [ {field,op,value}, ... ] }`），对 `ActivityClassificationContext` 做全条件 AND 匹配。
- 主要依赖：`System.Text.Json`、`System.Text.RegularExpressions`、可选 `ILogger`
- 被谁使用：PcTracker 活动分类/规则应用管线（对规则 conditions 求值）

## 函数级结构化伪代码

### ActivityClassificationContext
#### record 字段
- 输入：RecordType、AppName、AppNameNormalized、Domain、UrlPath、Title、WindowTitle、FilePath、BucketType
- 输出：规则字段取值源
- 副作用：无
- 步骤：不可变上下文
- 分支与异常：无
- 调用：Matches 读取

### ActivityClassificationRuleEvaluator
#### Matches(conditionsJson, context, logger?)
- 输入：条件 JSON 字符串、上下文、可选日志
- 输出：bool 是否匹配
- 副作用：解析失败/超时写 Warning 日志
- 步骤：
  1. 空 JSON 或 null context → false。
  2. Parse 根对象；必须有非空数组属性 `all`。
  3. 对每个条件 MatchesCondition，任一失败即 false；全部通过 true。
  4. catch JsonException/ArgumentException/InvalidOperationException/RegexMatchTimeoutException → 日志 + false。
- 分支与异常：上述异常吞掉返回 false
- 调用：MatchesCondition

#### MatchesCondition(condition, context)
- 输入：单条 JSON 条件对象
- 输出：bool
- 副作用：无
- 步骤：
  1. 需 field/op 字符串与 value 属性；GetFieldValue 空 → false。
  2. op：equals/contains/containsAny/startsWith/endsWith/domainSuffix/pathPrefix/regex（100ms 超时）；未知 op false。
- 分支与异常：结构非法 false
- 调用：GetFieldValue、MatchesDomainSuffix、MatchesPathPrefix、Regex.IsMatch

#### GetFieldValue / TryGetString* / MatchesDomainSuffix / MatchesPathPrefix / NormalizePath
- 输入：字段名或字符串路径/域名
- 输出：上下文字符串或匹配 bool
- 副作用：无
- 步骤：
  1. field 名映射到 context 对应属性，未知 null。
  2. domainSuffix：trim 点后 exact 或 endsWith `.suffix`。
  3. pathPrefix：去 query/hash、补前导 `/`、去尾 `/`（根除外）；equal 或 startsWith prefix+`/`。
- 分支与异常：空串 false
- 调用：无

## 近逐行中文伪代码

1. record ActivityClassificationContext 九个可选字符串字段。
2. 静态评估器 RegexTimeout=100ms。
3. Matches：解析 all 数组 AND；异常日志后 false。
4. MatchesCondition：field/op/value；equals/contains/containsAny/startsWith/endsWith/domainSuffix/pathPrefix/regex。
5. GetFieldValue 字段映射；TryGetStringProperty/Value/Values。
6. 域名后缀与路径前缀规范化匹配。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs",
      "label": "ActivityClassificationRuleEvaluator",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs", "to": "System.Text.Json", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs", "to": "System.Text.RegularExpressions", "type": "depends_on" }
  ]
}
```
