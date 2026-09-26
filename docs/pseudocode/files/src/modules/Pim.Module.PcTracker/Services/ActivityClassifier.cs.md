# src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：静态活动分类器：先匹配高优先级规则，再启发式（文档/代码托管/应用名等），最后低置信 builtin 规则或 Fallback。
- 主要依赖：
  - `ActivityClassificationContext`、`ActivityClassificationResult`（DTO）
  - `ActivityCategoryRuleEntity`、`ActivityClassificationRuleEvaluator`、`AppNameNormalizer`
  - `ILogger`（可选）
- 被谁使用：PcTracker 活动分类流水线/服务

## 函数级结构化伪代码

### ActivityClassifier（static）
#### 常量
- 延迟规则：Priority≤100 且 Confidence≤0.65 且 Source=builtin
- 中文类别名与颜色：编程/学习/终端/沟通/办公/文件/娱乐

#### `Classify(context, rules, logger?)`
- 输入：分类上下文、规则集合
- 输出：`ActivityClassificationResult`
- 副作用：无（规则匹配可能打日志）
- 步骤：
  1. 过滤 Status=active 且 `CanClassifyActivity`；按 Priority 降序。
  2. 非延迟规则 `TryClassifyWithRules` 命中即返回。
  3. `ClassifyWithHeuristics` 非 null 即返回。
  4. 延迟规则再试；否则 `Fallback()`。
- 分支与异常：rules null → 空集合
- 调用：TryClassifyWithRules、ClassifyWithHeuristics、IsDeferredFallbackRule

#### `TryClassifyWithRules`
- 遍历规则：`ActivityClassificationRuleEvaluator.Matches(ConditionsJson, context)`
- 命中 → CategoryName（空则 Fallback 名）、Color、ProjectTag、Confidence、source=`rule`、Explanation、RuleId
- 全未命中 → false + Fallback 占位

#### `IsDeferredFallbackRule` / `CanClassifyActivity`
- 延迟：builtin + 低优先级/低置信
- Scope 空或 activity/both/app 可分类

#### `ClassifyWithHeuristics`
- 文档信号 → 学习 + 可能 ActivityWatch 项目标签
- 代码托管域名 → 编程 + 仓库名段作为 ProjectTag
- localhost → 编程
- 标题会议/邮件信号 → 沟通
- 应用名（Normalize）：编码/终端/沟通/办公/文件/娱乐应用列表
- 全否 → null

#### 辅助
- `DeriveRepositoryProjectTag`：URL path 第二段
- `InferDocumentationProjectTag`：docs.activitywatch / 文本含 activitywatch
- `NormalizeDomain`、`IsCodeHostingDomain`、`IsLocalhost`、`IsDocumentationSignal`
- `ContainsAny` / `JoinForSearch`
- 静态 needle 数组：DocumentationSignals、MeetingTitleSignals、Coding/Terminal/Communication/Office/File/Entertainment Apps

## 近逐行中文伪代码

1. 静态类；规则优先 → 启发式 → 延迟 builtin → Fallback。
2. 规则匹配委托 `ActivityClassificationRuleEvaluator`；结果 source=`rule`。
3. 启发式按域名/URL/标题/应用名映射中文类别与固定色与置信度。
4. Scope 限制仅 activity/both/app（或空）参与活动分类。
5. 低优先级 builtin 规则故意延后，避免过早吞掉启发式。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs",
      "label": "ActivityClassifier",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleEvaluator.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppNameNormalizer.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "to": "Microsoft.Extensions.Logging", "type": "depends_on" }
  ]
}
```
