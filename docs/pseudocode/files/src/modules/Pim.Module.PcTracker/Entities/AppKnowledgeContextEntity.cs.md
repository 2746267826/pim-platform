# src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：App 知识库上下文规则实体——按进程与模式（标题/URL 等）映射目标类别/项目标签，记录来源、置信度与匹配统计。
- 主要依赖：`AppSignatureEntity`（导航属性）
- 被谁使用：`AppKnowledgeContextService`、`AppKnowledgeSuggestionService`、分类重算、`PimDbContext`

## 函数级结构化伪代码

### AppKnowledgeContextEntity
#### 属性
- 输入：无（EF 实体）
- 输出：字段
- 副作用：无
- 步骤：
  1. `Id`；可选 `AppSignatureId`
  2. `ProcessName`、`PatternType`、`PatternValue`
  3. 可选 `TargetCategoryName`、`ProjectTag`
  4. `ScopeSummary` 范围摘要
  5. `Source` 默认 `"user-confirmed"`；`Confidence` 默认 1.0；`Enabled` 默认 true
  6. `AffectedRecordCount`、`AffectedDurationSeconds`、`LastMatchedAt`
  7. 可选 `SourceRuleId`、`SourceSuggestionId`
  8. `CreatedAt`、`UpdatedAt`
  9. 导航 `AppSignature`
- 分支与异常：无运行时逻辑
- 调用：无

## 近逐行中文伪代码

1. 命名空间 Entities；类 AppKnowledgeContextEntity
2. Id、AppSignatureId、ProcessName、PatternType/Value
3. TargetCategoryName、ProjectTag、ScopeSummary
4. Source/Confidence/Enabled 默认值
5. 影响统计与 LastMatchedAt
6. SourceRuleId、SourceSuggestionId、时间戳
7. 导航到 AppSignatureEntity

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs",
      "label": "AppKnowledgeContextEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs", "type": "depends_on" }
  ]
}
```
