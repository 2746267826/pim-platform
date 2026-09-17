# src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：为 PC 明细记录确保分类快照：按 RecordKey 加载/创建 `ActivityClassificationEntity`，调用 `ActivityClassifier`，保护人工/纠正来源不被覆盖，并将结果写回 `PcDetailRecord`。
- 主要依赖：`PimDbContext`、`ILogger`、`ActivityClassifier`、`ActivityClassificationRecordKey`、`PcActivityRecordKeyService`、`AppNameNormalizer`、PcTracker DTO/实体
- 被谁使用：PcTracker 时间线/明细查询与重算管线

## 函数级结构化伪代码

### ActivityClassificationSnapshotService
#### 常量与构造
- 输入：db、logger
- 输出：服务实例；`ClassifierVersion = "local-v1"`
- 副作用：保存依赖
- 步骤：字段赋值
- 分支与异常：无
- 调用：无

#### `EnsureClassificationsAsync(records, rules, auditId, ct, saveChanges=true)`
- 输入：明细记录集、分类规则、可选 auditId、是否保存
- 输出：带分类字段的 `List<PcDetailRecord>`
- 副作用：可能 Add/更新快照并 SaveChanges
- 步骤：
  1. records 空 → []。
  2. 对每条 TryCreateKeyedRecord（时长>0 且 Start/End 可解析）得 KeyedRecord 列表。
  3. 按 RecordKey 批量加载已有快照字典。
  4. 对每个 keyedRecord：`ActivityClassifier.Classify(ToContext(record), rules, logger)`。
  5. 快照不存在则 NewGuid + RecordKey 并 Add 到 newSnapshots。
  6. ApplySourceMetadata；若 IsProtectedSnapshot（manual/corrected/user_corrected/llm_corrected）则保留快照结果 ToClassificationResult，跳过重写。
  7. 否则 ApplySnapshot（类别/色/标签/置信度/来源/说明/版本/时间/audit）。
  8. saveChanges 且有 keyed → SaveChangesAsync。
  9. 映射原 records：有分类则 ApplyClassification(with 表达式)，否则原样返回。
- 分支与异常：解析失败记录被跳过（保持未分类）
- 调用：TryCreateKeyedRecord、ToContext、ApplySourceMetadata、IsProtectedSnapshot、ApplySnapshot、ToClassificationResult、ApplyClassification

#### 私有辅助
- TryCreateKeyedRecord：DurationSeconds>0；解析 Start/End；RecordKey=ActivityClassificationRecordKey.FromRecord。
- ToContext：规范化 App 名；组装 ActivityClassificationContext（类型/应用/域名/路径/标题/本地文件路径/BucketType）。
- ApplySnapshot：写分类结果字段 + ClassifierVersion + ClassifiedAt + AuditId；并 ApplySourceMetadata。
- ApplySourceMetadata：`PcActivityRecordKeyService.Build(record)` 填事件/bucket JSON、键版本/稳定性/源类型；时间起止；InterpretationVersion。
- IsProtectedSnapshot：Source 为人工/纠正类（字面与 IgnoreCase 双重判断）。
- ToClassificationResult / ApplyClassification：实体→结果；record with 更新 Category/Project/Confidence/Source/Explanation。
- KeyedRecord：Record + RecordKey + StartedAt + EndedAt。

## 近逐行中文伪代码

1. 引入 EF、Logging、Data、PcTracker DTO/实体。
2. 服务持有 Db 与 Logger；分类器版本 local-v1。
3. EnsureClassificationsAsync：过滤可键记录 → 加载快照 → 分类。
4. 新建或更新快照元数据；保护人工纠正来源。
5. 可选 SaveChanges；把分类结果合并回 PcDetailRecord。
6. 上下文由应用/域名/路径/标题构建；RecordKey 与源事件元数据由键服务生成。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs",
      "label": "ActivityClassificationSnapshotService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
