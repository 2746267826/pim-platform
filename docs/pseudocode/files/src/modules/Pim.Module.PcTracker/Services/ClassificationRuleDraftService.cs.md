# src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：将待处理的活动分类建议转为可保存的规则草稿请求（条件 JSON、类别色、优先级 900、置信度 0.95）。
- 主要依赖：`PimDbContext`、`ActivityClassificationSuggestionEntity`、`PcCategoryEntity`、ActivityClassification DTO
- 被谁使用：PcTracker 分类建议预览/应用端点

## 函数级结构化伪代码

### ClassificationRuleDraftService
#### 构造与常量
- 输入：db
- 输出：服务实例；MaxRuleNameLength=128；DefaultCategoryColor=`#64748b`
- 副作用：保存 `_db`
- 步骤：赋值
- 分支与异常：无
- 调用：无

#### `BuildSuggestionDraftAsync(suggestionId, request, ct)`
- 输入：建议 Id、`SuggestionClassificationPreviewRequest`（可选覆盖 CategoryName/ProjectTag）
- 输出：`SaveActivityClassificationRuleRequest`
- 副作用：只读查建议与类别色
- 步骤：
  1. 按 Id 加载 `ActivityClassificationSuggestionEntity`，不存在 → KeyNotFoundException（中文消息）。
  2. Status 必须为 `"pending"`，否则 InvalidOperationException。
  3. condition = BuildCondition(ClusterKey)。
  4. category = request.CategoryName ?? SuggestedCategory ?? CurrentCategory；trim 空白→null。
  5. projectTag = request.ProjectTag ?? SuggestedProjectTag；trim。
  6. ruleName = BuildRuleName(suggestion)；color = ResolveCategoryColorAsync(category)。
  7. 返回 Save 请求：Name、Scope=`"activity"`、category、projectTag、color、Priority=900、ConditionsJson=`{ all: [condition] }`、Confidence=0.95、说明含 suggestion.Id。
- 分支与异常：见上
- 调用：BuildCondition、BuildRuleName、ResolveCategoryColorAsync

#### `BuildCondition(clusterKey)`
- 输入：形如 `kind:value` 的聚类键
- 输出：匿名条件对象
- 副作用：无
- 步骤：找首个 `:`；非法/空 value → ArgumentException；kind 小写：
  - `web` → `{ field=domain, op=domainSuffix, value }`
  - `app` → `{ field=appNameNormalized, op=equals, value }`
  - 其它 → ArgumentException
- 分支与异常：不支持键抛 ArgumentException
- 调用：无

#### `ResolveCategoryColorAsync(categoryName, ct)`
- 输入：可选类别名
- 输出：颜色字符串
- 副作用：只读查 `PcCategoryEntity`
- 步骤：空名 → 默认色；否则 Name 匹配取 Color，无则默认色
- 分支与异常：无
- 调用：EF FirstOrDefaultAsync

#### `BuildRuleName(suggestion)`
- 输入：建议实体
- 输出：≤128 字符规则名
- 副作用：无
- 步骤：前缀 `"Suggestion: "` + 截断 ClusterKey + `" {Id:N}"`；超长 ClusterKey 尾部 `...`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 Json、EF、Data、PcTracker DTO/实体。
2. 服务仅依赖 Db；规则名最长 128；默认灰蓝颜色。
3. BuildSuggestionDraftAsync：待处理建议 → 条件/类别/项目标签/颜色 → Save 请求草稿。
4. BuildCondition 解析 web/app 聚类键为规则条件。
5. 从 PcCategory 解析颜色；规则名带 Suggestion 前缀与 Id 后缀并截断。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs",
      "label": "ClassificationRuleDraftService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
