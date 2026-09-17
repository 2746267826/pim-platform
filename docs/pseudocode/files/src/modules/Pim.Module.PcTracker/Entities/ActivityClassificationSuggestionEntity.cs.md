# src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 活动分类建议实体（按 cluster 聚合样本、LLM/用户反馈与 pending 状态）。
- 主要依赖：DataAnnotations、Schema
- 被谁使用：`ActivitySuggestionService`、EF 映射与迁移

## 函数级结构化伪代码

### ActivityClassificationSuggestionEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务层赋值后由 EF 持久化
- 输出：`pc_activity_classification_suggestions` 表一行
- 副作用：部分字段有默认值
- 步骤：
  1. 主键 `Id`；`ClusterKey` 最长 256。
  2. 聚合：`SampleCount`、`TotalDurationSeconds`。
  3. JSON：`SampleRecordsJson` 默认 `"[]"`；`SanitizedContextJson` 默认 `"{}"`。
  4. 分类建议：`CurrentCategory`、`SuggestedCategory`、`SuggestedProjectTag`、`SuggestedRulesJson`。
  5. 反馈：`UserFeedback`、`LlmResponseJson`。
  6. `Status` 默认 `"pending"`；`CreatedAt`/`UpdatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：被 ActivitySuggestionService 增改查

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.Entities`；表 `pc_activity_classification_suggestions`。
2. Id、ClusterKey、SampleCount、TotalDurationSeconds。
3. SampleRecordsJson/SanitizedContextJson jsonb 默认 []/{}。
4. Current/Suggested 分类与项目标签、规则 JSON。
5. UserFeedback、LlmResponseJson；Status 默认 pending。
6. CreatedAt/UpdatedAt 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs",
      "label": "ActivityClassificationSuggestionEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs", "type": "depends_on" }
  ]
}
```
