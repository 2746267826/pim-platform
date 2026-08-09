# src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：集中注册 PcTracker 各实体的 EF Core `IEntityTypeConfiguration`：索引、唯一约束、默认值、外键与表名。
- 主要依赖：`Microsoft.EntityFrameworkCore`、本模块 Entities
- 被谁使用：`PimDbContext` / 模块模型装配（ApplyConfigurations）

## 函数级结构化伪代码

### KeystatsDailyEntityConfiguration
#### Configure(EntityTypeBuilder\<KeystatsDailyEntity\>)
- 输入：builder
- 输出：无
- 副作用：配置索引与级联
- 步骤：DeviceId、SnapshotDate 索引；`(DeviceId, SnapshotDate)` 唯一；KeyCounts/AppBreakdowns Cascade
- 调用：HasIndex / HasMany

### AwEventEntityConfiguration
#### Configure
- 步骤：DeviceId/Timestamp/EventType/BucketId/SourceEventId/AppNameNormalized 命名索引；`(DeviceId, BucketId, SourceEventId)` 唯一且过滤非空 bucket/source

### AwBucketEntityConfiguration
#### Configure
- 步骤：`(PimDeviceId, BucketId)` 唯一；BucketType/SeenAt 索引

### KeystatsSampleEntityConfiguration
#### Configure
- 步骤：`(PimDeviceId, SampledAtUtc)` 唯一；StatsDate 索引

### AppCategoryEntityConfiguration
#### Configure
- 步骤：表 `pc_app_categories`；Id 默认 `gen_random_uuid()`；CategoryName/Priority 索引

### ActivityCategoryRuleEntityConfiguration
#### Configure
- 步骤：表 `pc_activity_category_rules`；Id 默认 uuid；RuleName 唯一；Status/Priority/CategoryName/ProjectTag 索引

### ActivityClassificationSuggestionEntityConfiguration
#### Configure
- 步骤：表 `pc_activity_classification_suggestions`；ClusterKey 索引；pending 状态 ClusterKey 唯一过滤；Status/UpdatedAt 索引

### ActivityClassificationEntityConfiguration
#### Configure
- 步骤：表 `pc_activity_classifications`；多项默认值（RecordKeyVersion、SourceType、CategoryName/Color、Confidence、Explanation、ClassifierVersion 等）；RecordKey 唯一；StartedAt/DeviceId/CategoryName/ProjectTag/SourceRuleId/RecordKeyVersion/SourceType 索引

### ActivityClassificationAuditEntityConfiguration
#### Configure
- 步骤：表 `pc_activity_classification_audits`；AffectedRecordKeysJson 默认 `[]`；RuleId/SuggestionId/CreatedAt 索引

### ActivityClassificationSettingsEntityConfiguration
#### Configure
- 步骤：表 `pc_activity_classification_settings`；SettingsKey 默认 default；推荐最小分类时长默认 5 分钟；SettingsKey 唯一

### PcCategoryEntityConfiguration
#### Configure
- 步骤：表 `pc_categories`；列名显式映射；Parent 自引用 Restrict；ParentId/Name/SortOrder 索引；默认 Color/Productivity/SortOrder/IsBuiltin/时间戳

### AppSignatureEntityConfiguration
#### Configure
- 步骤：表 `pc_app_signatures`；ProcessName 唯一；DisplayName 索引；默认 Source=builtin、Confidence=1

### AppKnowledgeContextEntityConfiguration
#### Configure
- 步骤：表 `pc_app_knowledge_contexts`；`(ProcessName, PatternType, PatternValue)` 唯一；TargetCategoryName/AppSignatureId/SourceSuggestionId 索引；FK AppSignature SetNull

## 近逐行中文伪代码

1. 引入 EF Core 与命名空间 Entities。
2. 依次声明各 Entity 的 IEntityTypeConfiguration 实现类。
3. KeystatsDaily：设备日唯一与子表级联。
4. AwEvent/AwBucket/KeystatsSample：查询与幂等上传所需唯一/普通索引。
5. AppCategory 与 ActivityCategoryRule：表名、uuid、规则名唯一。
6. Classification/Suggestion/Audit/Settings：默认值与业务唯一约束（含 pending 过滤）。
7. PcCategory 树形外键 Restrict。
8. AppSignature 进程名唯一；AppKnowledgeContext 模式唯一并挂签名外键。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs",
      "label": "EntityConfigurations",
      "path": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs", "type": "depends_on" }
  ]
}
```
