# src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：活动分类规则预览/应用、建议预览/应用（含副作用钩子）、范围重算；事务内写规则/审计/快照，保护 manual/corrected 类快照。
- 主要依赖：`PimDbContext`、`ActivityClassificationSnapshotService`、`ActivityClassificationRuleService`、`ICurrentUserService`、`ILogger`、`ClassificationRuleDraftService`、`BrowserPageTimelineBuilder`/`ActivityClassifier`/`AppNameNormalizer`、PcTracker DTO/实体、`AuditLogEntity`
- 被谁使用：PcTracker 分类相关端点/服务

## 函数级结构化伪代码

### ActivityClassificationRecomputeService
#### 构造(db, snapshots, rules, currentUser, logger)
- 输入：五依赖
- 输出：实例
- 副作用：无
- 步骤：字段赋值；常量默认分类名「其他」、颜色 `#64748b`
- 分支与异常：无
- 调用：无

#### Task\<ActivityClassificationPreviewDto\> PreviewRuleAsync(ruleRequest, range, ct)
- 输入：待预览规则、应用范围
- 输出：影响条数/时长、前后分类计数、样本、是否有变化、摘要
- 副作用：只读校验与加载（不写规则）
- 步骤：
  1. `ValidateAsync`(不强制唯一名)
  2. 加载活跃规则 + 范围活动记录
  3. 候选规则 `ToEntity` 并与现有规则 `OrderRules` 合并为 after
  4. 加载受保护快照
  5. 每条记录前后分类对比，过滤有意义变化
  6. 聚合当前/新分类计数、时长、最多 5 条样本 ApplyClassification
  7. 组装 Preview DTO + BuildSummary
- 分支与异常：校验失败上抛
- 调用：rules、Load*、ClassifyForPreview

#### Task\<ActivityClassificationPreviewDto\> ApplyRuleAsync(...)
- 输入：规则+范围
- 输出：预览 DTO
- 副作用：事务写规则与重算
- 步骤：Preview → `ApplyRuleCoreAsync`(无 suggestion) → 返回 Preview
- 分支与异常：透传
- 调用：ApplyRuleCoreAsync

#### Task\<ActivityClassificationSuggestionPreviewDto\> PreviewSuggestionAsync(suggestionId, request, drafts, ct)
- 输入：建议 ID、预览请求、草稿服务
- 输出：规则草稿 + 预览
- 副作用：读建议草稿
- 步骤：`BuildSuggestionDraftAsync` → PreviewRuleAsync → 包装
- 分支与异常：透传
- 调用：drafts

#### Task\<ActivityClassificationSuggestionApplyDto\> ApplySuggestionAsync(...)
- 输入：建议 ID、应用请求、drafts
- 输出：规则 DTO + 预览 + auditId + suggestionStatus
- 副作用：事务应用规则并标记建议 accepted
- 步骤：构造 PreviewRequest → 草稿 → 预览 → ApplyRuleCore(suggestionId) → ToSuggestionApplyDto
- 分支与异常：透传
- 调用：ApplyRuleCoreAsync

#### Task\<(Applied, SideEffect)\> ApplySuggestionWithSideEffectAsync\<T\>(..., afterApply, ct)
- 输入：建议应用 + afterApply 回调
- 输出：Applied DTO 与副作用结果
- 副作用：在事务内、提交前调用 afterApply
- 步骤：
  1. null 检查 afterApply
  2. 草稿+预览
  3. ApplyRuleCore 中 afterApply 捕获 sideEffect
  4. 返回 (ToSuggestionApplyDto, sideEffect!)
- 分支与异常：ArgumentNullException
- 调用：ApplyRuleCoreAsync

#### Task\<ActivityClassificationRecomputeDto\> RecomputeAsync(range, ct)
- 输入：范围
- 输出：记录数、时长、auditId、中文消息
- 副作用：事务写 range.recompute 审计 + EnsureClassifications
- 步骤：LoadActive 规则与记录 → ExecuteInTransaction：CreatePcAudit → Save → snapshots.Ensure
- 分支与异常：透传
- 调用：snapshots、CreatePcAudit

#### Task\<ApplyRuleCoreResult\> ApplyRuleCoreAsync(ruleRequest, range, preview, suggestionId?, afterApply?, ct) [private]
- 输入：规则、范围、已算预览、可选建议、可选回调
- 输出：Rule/Preview/AuditId/SuggestionStatus
- 副作用：事务：可选 afterApply → 标记建议 → Add 规则 → 平台 AuditLog → 加载规则与记录 → PcAudit → EnsureClassifications(saveChanges:false) → Save
- 步骤：
  1. Validate 强制唯一规则名
  2. 事务内 ToEntity；预生成 pcAuditId；plannedResult
  3. afterApply 若有则先执行
  4. suggestionId 有则 MarkSuggestionAccepted
  5. Add 规则；AddAuditLog(pc.classification.rule.apply 元数据)
  6. 合并新规则 OrderRules；重新 Load 记录；CreatePcAudit(rule.apply)；EnsureClassifications
  7. SaveChanges 返回结果
- 分支与异常：建议不存在/非 pending
- 调用：rules、MarkSuggestion、snapshots、AddAuditLog

#### Task\<List\<ActivityCategoryRuleEntity\>\> LoadActiveRulesAsync(ct)
- 输入：ct
- 输出：活跃规则列表
- 副作用：只读
- 步骤：委托 `_rules.LoadActiveAsync`
- 分支与异常：无
- 调用：rules

#### MarkSuggestionAcceptedAsync / LoadActivityRecordsAsync / CreatePcAudit / AddAuditLog / ParseRange / TryParseDate / LoadProtectedSnapshotsAsync / ClassifyForPreview / ExecuteInTransactionAsync / OrderRules / IsProtectedSnapshot / ToClassificationResult / ToContext / ApplyClassification / HasMeaningfulChange / BuildSummary / FormatRangeMode / ToSuggestionApplyDto
- 输入：见签名
- 输出：状态串/记录/审计实体/范围/字典/分类结果/事务结果等
- 副作用：Mark 更新建议；Create/Add 审计实体入 ChangeTracker；事务 Begin/Commit/Rollback
- 步骤：
  1. Mark：InMemory 走实体更新；否则 ExecuteUpdate pending→accepted；失败区分不存在 vs 非 pending
  2. LoadActivity：ParseRange → AwEvent Duration>0 且时间窗 → BrowserPageTimelineBuilder.BuildInterpretedAwRecords
  3. CreatePcAudit：序列化 RecordKey 列表到 AffectedRecordKeysJson
  4. AddAuditLog：写 AuditLogs 行，Metadata JSON
  5. ParseRange：today 需 DateFrom=DateTo；range 需两端；业务日起点 `PcTrackerService.GetBusinessDayStartForQuery`
  6. Protected：Source 为 manual/corrected/user_corrected/llm_corrected（含字面匹配重复）
  7. ClassifyForPreview：受保护用快照，否则 ActivityClassifier.Classify
  8. ExecuteInTransaction：InMemory 跳过事务；否则 ExecutionStrategy + BeginTransaction
  9. OrderRules：Priority desc → CreatedAt desc → RuleName → Id
  10. ToContext：规范化 App 名等
  11. HasMeaningfulChange：分类名/项目/规则/源/颜色/置信度
  12. FormatRangeMode：today→今天，range→自定义范围
- 分支与异常：范围参数 ArgumentException；建议 KeyNotFound/InvalidOperation
- 调用：EF、JsonSerializer、ActivityClassifier、AppNameNormalizer

### PreviewRecord / ApplyRuleCoreResult (private records)
- 输入：构造
- 输出：内部聚合
- 副作用：无
- 步骤：预览前后分类对；应用核心结果四元组
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 注入 db/snapshots/rules/currentUser/logger；默认「其他」与 slate 色
2. PreviewRule：校验→加载记录与保护快照→前后分类差→计数/样本/摘要
3. ApplyRule / ApplySuggestion / WithSideEffect：预览后事务核心应用；建议标记 accepted
4. Recompute：范围审计 + EnsureClassifications
5. ApplyRuleCore：校验唯一名→可选回调→写规则与双审计→重载记录重算快照→Save
6. 范围 today/range 映射业务日；受保护源跳过重分类
7. InMemory 跳过事务与 ExecuteUpdate 分支

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs",
      "label": "ActivityClassificationRecomputeService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ClassificationRuleDraftService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities", "type": "depends_on" }
  ]
}
```
