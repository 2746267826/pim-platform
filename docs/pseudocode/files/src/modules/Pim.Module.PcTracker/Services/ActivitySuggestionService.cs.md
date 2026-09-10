# src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：基于 PC 明细记录构建/刷新分类建议、查询 pending 建议、拒绝建议、汇总近期项目标签；接受建议已禁用。
- 主要依赖：`PimDbContext`、`AppSignatureService`、`ActivityClassificationSuggestionEntity`、相关 Rule/Classification 实体与 DTO、`AppNameNormalizer`、`ActivityUrlSanitizer`
- 被谁使用：PcTracker 模块端点/编排服务

## 函数级结构化伪代码

### ActivitySuggestionService
#### 字段与构造
- 常量 `PendingStatus = "pending"`；Web 默认 `JsonSerializerOptions`。
- 注入 `_db`、`_appSignatures`。

#### `Task<List<ActivityClassificationSuggestionDto>> BuildSuggestionsAsync(records, recommendedMinimumMinutes, ct)`
- 输入：明细集合、建议最短分钟、取消令牌
- 输出：当前全部 pending 建议 DTO 列表
- 副作用：可能新增/更新建议实体并 `SaveChanges`
- 步骤：
  1. 过滤 `NeedsSuggestion`；算 `GetClusterKey`；去掉 null key；按 key 忽略大小写分组。
  2. 对每组：查同 ClusterKey 全部建议；优先找 pending 实体。
  3. 无 pending：若已有非 pending 历史则 `continue`；否则新建 pending 并 Add。
  4. 更新 SampleCount、TotalDurationSeconds、SampleRecordsJson（Top5 样本）、SanitizedContextJson、CurrentCategory、UpdatedAt。
  5. SaveChanges；返回 `GetSuggestionsAsync`。
- 分支与异常：EF/序列化异常向上
- 调用：NeedsSuggestion、GetClusterKey、BuildSampleRecords、BuildSanitizedContext、GetSuggestionsAsync

#### `Task<List<ActivityClassificationSuggestionDto>> GetSuggestionsAsync(ct)`
- 输入：取消令牌
- 输出：pending 建议，按 TotalDurationSeconds 降序，并尽量挂上应用签名展示字段
- 副作用：只读查询（含签名查找）
- 步骤：
  1. 查 Status==pending，按时长降序。
  2. 每条 ToSuggestionDto；`ExtractAppName` 后 `LookupByProcessNameAsync`，有则 with 更新 AppDisplayName/AppIcon/RecognitionSource。
- 调用：`_appSignatures.LookupByProcessNameAsync`、ToSuggestionDto、ExtractAppName

#### `static string? ExtractAppName(clusterKey)`
- 以 `app:` 前缀（忽略大小写）则返回后缀，否则 null。

#### `Task<List<string>> GetRecentProjectTagsAsync(ct)`
- 输入：取消令牌
- 输出：最多 20 个去重项目标签
- 步骤：规则表最近 20 个非空 ProjectTag + 分类快照最近 20；Concat→Trim→Distinct 忽略大小写→Take 20

#### `Task<ActivityClassificationRuleDto> AcceptSuggestionAsync(id, req, ct)`
- 查找建议，不存在 KeyNotFoundException；EnsurePending；然后固定抛 `InvalidOperationException`（禁用直接接受，要求预览/应用流程）

#### `Task RejectSuggestionAsync(id, ct)`
- 查找 + EnsurePending；Status=rejected；UpdatedAt=UtcNow；SaveChanges

#### `static EnsurePending` / `NeedsSuggestion` / `GetClusterKey`
- EnsurePending：非 pending 抛 InvalidOperationException（含当前状态）。
- NeedsSuggestion：时长 ≥ 推荐分钟*60，且 (source==fallback 或 confidence<0.5)。
- GetClusterKey：有 Domain → `web:{lower}`；否则 AppName/BrowserAppName 规范化 → `app:{normalized}`；皆无 null。

#### `BuildSampleRecords` / `BuildSanitizedContext` / `ToSuggestionDto` / `ToRuleDto`
- Sample：按时长降序取 5 条匿名对象（含 Sanitize URL）。
- Context：cluster 统计 + domains/apps/urls/titles 去重排序（titles 最多 10）。
- ToSuggestionDto：实体字段映射 DTO。
- ToRuleDto：规则实体映射（本文件 Accept 路径未再使用，因 Accept 直接抛错）。

## 近逐行中文伪代码

1. 注入 Db 与 AppSignatureService；JSON Web 选项；Pending 常量。
2. BuildSuggestionsAsync：筛需建议记录 → 聚类 → 有 pending 则刷新，无历史则新建，有已处理历史则跳过 → 保存 → 返回列表。
3. GetSuggestionsAsync：pending 按时长排序；按 app: cluster 补签名展示字段。
4. ExtractAppName 解析 app: 前缀。
5. GetRecentProjectTagsAsync 合并规则与快照标签去重取 20。
6. AcceptSuggestionAsync 仅校验 pending 后抛禁用异常。
7. RejectSuggestionAsync 置 rejected 并保存。
8. NeedsSuggestion/GetClusterKey/样本与上下文构建/DTO 映射辅助。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs",
      "label": "ActivitySuggestionService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivitySuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationEntity.cs", "type": "depends_on" }
  ]
}
```
