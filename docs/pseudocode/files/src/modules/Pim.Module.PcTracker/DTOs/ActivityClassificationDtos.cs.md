# src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：活动分类、规则预览/应用、建议、设置、重算、活动分析块，以及 App 知识库（签名/上下文/建议）的请求与响应 DTO 集合。
- 主要依赖：无外部程序集（纯 record）；引用同模块 `PcDetailRecord` 等类型于预览样本
- 被谁使用：`PcTrackerModule` 端点、`ActivityClassification*` / `AppKnowledge*` / `ActivitySuggestion*` 服务

## 函数级结构化伪代码

### ActivityClassificationResult
#### `record ActivityClassificationResult(CategoryName, CategoryColor, ProjectTag?, Confidence, Source, Explanation, SourceRuleId?)`
- 输入：分类结果字段
- 输出：不可变结果
- 副作用：无
- 步骤：承载分类输出
- 分支与异常：无
- 调用：无

#### `static Fallback()`
- 输入：无
- 输出：类别「其他」、色 `#64748b`、Confidence 0.2、Source `fallback`、说明无匹配
- 副作用：无
- 步骤：构造默认结果
- 分支与异常：无
- 调用：构造函数

### 规则与设置
#### `ActivityClassificationRuleDto` / `SaveActivityClassificationRuleRequest`
- 输入/输出：规则 Id、名称、Scope、类别/项目标签、Color、Priority、Source/Status、ConditionsJson、Confidence、Explanation
- 副作用：无
- 步骤：列表与保存载荷
- 分支与异常：无
- 调用：无

#### `ActivityClassificationSuggestionDto` / `AcceptActivityClassificationSuggestionRequest`
- 输入/输出：聚类建议字段（ClusterKey、样本、时长、SanitizedContext、当前/建议类别、LLM JSON、Status、App 展示等）；Accept 载荷同规则保存形状
- 副作用：无
- 步骤：建议展示与接受建规则
- 分支与异常：无
- 调用：无

#### `ActivityClassificationSettingsDto` / `SaveActivityClassificationSettingsRequest`
- 输入/输出：推荐最小分类时长分钟数 + 支持时长列表 / 保存分钟数
- 副作用：无
- 步骤：设置读写
- 分支与异常：无
- 调用：无

### 范围、预览与应用
#### `ActivityClassificationApplyRangeRequest(Mode, DateFrom?, DateTo?)`
- 输入：应用模式与日期
- 输出：范围请求
- 副作用：无
- 步骤：供 preview/apply/recompute
- 分支与异常：无
- 调用：无

#### `ActivityClassificationPreviewRequest` / `ApplyActivityClassificationRuleRequest`
- 输入：Rule + Range
- 输出：预览或应用请求
- 副作用：无
- 步骤：包装规则与范围
- 分支与异常：无
- 调用：无

#### `ActivityClassificationPreviewDto`
- 输入：影响条数/时长、新旧类别计数、Samples(`PcDetailRecord`)、RequiresConfirmation、Summary
- 输出：预览结果
- 副作用：无
- 步骤：展示重算影响面
- 分支与异常：无
- 调用：无

#### 建议预览/应用请求与响应
- `SuggestionClassificationPreviewRequest` / `SuggestionClassificationApplyRequest`：可选类别/项目 + Range
- `ActivityClassificationSuggestionPreviewDto`：生成 Rule + Preview
- `ActivityClassificationSuggestionApplyDto`：Rule + Preview + AuditId + SuggestionStatus
- 副作用：无
- 步骤：端点契约
- 分支与异常：无
- 调用：无

#### `ActivityClassificationRecomputeRequest` / `ActivityClassificationRecomputeDto`
- 输入：Range / 输出重算条数、时长、AuditId、Summary
- 副作用：无
- 步骤：全量重算 API
- 分支与异常：无
- 调用：无

### 活动分析
#### `PcActivityAnalysisResponse` / `PcActivityAnalysisBlockDto` / CategoryDto / AppDto
- 输入：日期、块分钟、块列表（起止、强度、活跃时长、待分类/切换/类别变更计数、类别与 App 时长）
- 输出：日分析响应
- 副作用：无
- 步骤：时间线块聚合展示
- 分支与异常：无
- 调用：无

### App 知识库
#### `AppSignatureDto` / `SaveAppSignatureRequest`
- 输入/输出：进程名、展示名、CategoryPath、Productivity、Description、Source、Confidence、Icon、LastSeenAt、CreatedAt
- 副作用：无
- 步骤：应用签名读写
- 分支与异常：无
- 调用：无

#### `AppKnowledgeAppDto` / `AppKnowledgeContextDto` / `SaveAppKnowledgeContextRequest`
- 输入/输出：签名扩展（ContextCount、Pending、RecentAffectedDuration）；上下文模式 PatternType/Value、目标类别、ScopeSummary、影响统计；保存请求含 Confidence/Enabled 可选
- 副作用：无
- 步骤：知识库列表与上下文 CRUD 契约
- 分支与异常：无
- 调用：无

#### `AppKnowledgeSuggestionPreviewDto` / `AppKnowledgeSuggestionApplyDto`
- 输入/输出：SuggestionId、RecommendedContext、Alternatives、Preview；应用后 SavedContext、AuditId、Status、Message
- 副作用：无
- 步骤：建议预览与落库响应
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.DTOs`
2. ActivityClassificationResult + Fallback 默认「其他」
3. RuleDto / SaveRule / SuggestionDto / AcceptSuggestion
4. Settings 读写；ApplyRange；Preview/Apply 规则请求
5. PreviewDto 影响面与样本；Suggestion Preview/Apply DTO
6. Recompute 请求与结果
7. PcActivityAnalysis 日块/类别/App
8. AppSignature 与 Save；AppKnowledgeApp/Context/Save
9. AppKnowledge 建议预览与应用 DTO

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs",
      "label": "ActivityClassificationDtos",
      "path": "src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs.md",
      "layer": "module.pctracker",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services", "to": "src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs", "type": "depends_on" }
  ]
}
```
