# src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：从活动分类建议生成/保存 App 知识上下文推荐（模式：域名/标题/app-default），并处理 App 签名查找与竞态创建。
- 主要依赖：`PimDbContext`、`AppKnowledgeContextService`、`AppSignatureService`、EF Core、`System.Text.Json`、PcTracker Entities/DTOs
- 被谁使用：活动分类建议确认/预览 API 流程

## 函数级结构化伪代码

### AppKnowledgeSuggestionService
#### AppKnowledgeSuggestionService(PimDbContext db, AppKnowledgeContextService contexts, AppSignatureService appSignatures)
- 输入：DB 与两个协作服务
- 输出：实例
- 副作用：保存依赖
- 步骤：赋值字段；常量 `SuggestionSource = "app-knowledge-suggestion"`
- 分支与异常：无
- 调用：无

#### Task\<AppKnowledgeSuggestionPreviewDto\> BuildRecommendedContextAsync(Guid suggestionId, SuggestionClassificationPreviewRequest request, ActivityClassificationPreviewDto? preview, CancellationToken ct)
- 输入：建议 Id、预览请求、可选影响预览、取消令牌
- 输出：推荐上下文 + 备选 + 影响预览 DTO
- 副作用：读 DB
- 步骤：
  1. 按 Id 加载 `ActivityClassificationSuggestionEntity`，无则 `KeyNotFoundException`
  2. 解析 `SanitizedContextJson` → Apps/Domains/Titles/Urls
  3. 域名候选：优先 Domains，否则从 Urls 抽 Host
  4. `ResolveProcessName`；`FindAppSignatureAsync` 可能校正 processName
  5. `BuildRecommendedPattern`（domain > title > app-default）→ `BuildContextDto` 为 recommended
  6. 备选：其余 domain、全部 title、以及 app-default；去重并去掉与 recommended 同模式
  7. 返回 PreviewDto（影响预览缺省时用建议样本数/时长估算）
- 分支与异常：建议不存在抛异常
- 调用：EF `FirstOrDefaultAsync`、`SanitizedSuggestionContext.Parse`、`BuildContextDto` 等

#### Task\<AppKnowledgeContextDto\> SaveRecommendedContextAsync(AppKnowledgeSuggestionPreviewDto suggestionPreview, CancellationToken ct)
- 输入：预览结果（含 RecommendedContext）
- 输出：保存后的上下文 DTO
- 副作用：写 App 签名（可能）、写知识上下文、回填 Source/SuggestionId/影响计数
- 步骤：
  1. 取 recommended；AppId 空则 `EnsureAppSignatureForContextAsync`
  2. `_contexts.SaveAsync(SaveAppKnowledgeContextRequest(...))`
  3. 再加载实体，写 Source、SourceSuggestionId、Affected*、UpdatedAt；`SaveChangesAsync`
  4. `AppKnowledgeContextService.ToDto`
- 分支与异常：保存后找不到实体抛 KeyNotFound
- 调用：`EnsureAppSignatureForContextAsync`、`_contexts.SaveAsync`、`ToDto`

#### Task\<Guid?\> EnsureAppSignatureForContextAsync(AppKnowledgeContextDto context, CancellationToken ct)
- 输入：上下文中的 ProcessName
- 输出：App 签名 Id 或 null
- 副作用：可能插入 `AppSignatureEntity`
- 步骤：
  1. processName 空 → null
  2. 已存在 → 返回 Id
  3. 若 ChangeTracker 已有未提交增删改 → `InvalidOperationException`（禁止与其他写入混批）
  4. 新建 learned 签名（置信度 0.6）并 Save
  5. `DbUpdateException`：Detach 后重查竞态结果；仍无则重抛
- 分支与异常：见上
- 调用：`FindByProcessNameAsync`、`SaveChangesAsync`

#### (string PatternType, string PatternValue) BuildRecommendedPattern(string processName, IReadOnlyList\<string\> domains, IReadOnlyList\<string\> titles)
- 输入：进程名、域名列表、标题列表
- 输出：推荐模式对
- 副作用：无
- 步骤：有 domain 取首；否则 title 首；否则 app-default+processName
- 分支与异常：无
- 调用：无

#### Task\<AppSignatureDto?\> FindAppSignatureAsync(string processName, IReadOnlyList\<string\> appCandidates, CancellationToken ct)
- 输入：进程名与上下文应用候选
- 输出：匹配的 App 签名 DTO 或 null
- 副作用：读 DB
- 步骤：
  1. 候选 = processName + apps，扩展 `.exe` 变体，去重忽略大小写
  2. 逐个 `FindByProcessNameAsync`
  3. 否则按 DisplayName 小写匹配首条 → `ToDto`
- 分支与异常：无匹配 null
- 调用：`AddExeVariant`、`AppSignatureService`

#### IEnumerable\<string\> AddExeVariant(string value)
- 输入：进程名候选
- 输出：原值 + 必要时 `.exe` 后缀
- 副作用：无
- 步骤：yield 原值；无 exe 后缀则再 yield 加后缀
- 分支与异常：无
- 调用：无

#### AppKnowledgeContextDto BuildContextDto(...)
- 输入：建议实体、可选 App、进程名、模式、请求、可选预览
- 输出：未持久化的上下文 DTO（Id=Empty）
- 副作用：无
- 步骤：规范化模式；类别/项目标签优先请求再建议；影响计数用预览或建议样本；标签文案 `appLabel · 模式：值`；Source 常量、置信度 0.9、Enabled=true
- 分支与异常：无
- 调用：`TrimToNull`、`ToPatternLabel`

#### string ResolveProcessName(string clusterKey, SanitizedSuggestionContext context)
- 输入：聚类键与清洗上下文
- 输出：进程名字符串
- 副作用：无
- 步骤：优先 context.Apps 首项；否则解析 clusterKey 值段；再否则 clusterKey 或 `"unknown-app"`
- 分支与异常：无
- 调用：`ParseClusterKey`

#### (string Kind, string Value) ParseClusterKey(string clusterKey)
- 输入：聚类键
- 输出：Kind/Value 对
- 副作用：无
- 步骤：空白→空；`kind:value` 冒号切；否则 `|` 前为 Value；否则整串为 Value
- 分支与异常：无
- 调用：无

#### ActivityClassificationPreviewDto BuildSuggestionImpactPreview(ActivityClassificationSuggestionEntity suggestion)
- 输入：建议实体
- 输出：基于样本数/总时长的估算预览
- 副作用：无
- 步骤：构造空字典/空样本列表、HasImpact=SampleCount>0、中文说明
- 分支与异常：无
- 调用：无

#### List\<AppKnowledgeContextDto\> Deduplicate / bool IsSamePattern
- 输入：上下文集合或左右对
- 输出：按 PatternType+Value 去重后列表；是否同模式
- 副作用：无
- 步骤：GroupBy 忽略大小写；比较两字段
- 分支与异常：无
- 调用：无

#### string? ExtractDomainFromUrl / string ToPatternLabel / string? TrimToNull
- 输入：URL 或模式类型或字符串
- 输出：Host / 中文标签 / 空白转 null
- 副作用：无
- 步骤：Uri 解析；switch 模式标签；Trim
- 分支与异常：URL 无效 null
- 调用：`Uri.TryCreate`

### SanitizedSuggestionContext（私有 record）
#### static SanitizedSuggestionContext Parse(string? json)
- 输入：建议清洗上下文 JSON
- 输出：Apps/Domains/Titles/Urls 列表
- 副作用：无
- 步骤：空→Empty；Parse 对象；多别名读属性；JsonException→Empty
- 分支与异常：吞 JSON 异常
- 调用：`ReadValues`/`TryGetProperty`/`AddValues`

#### ReadValues / TryGetProperty / AddValues
- 输入：JsonElement 与属性名
- 输出：去重字符串列表；递归展开数组/标量
- 副作用：无
- 步骤：忽略大小写找属性；数组递归；字符串/数字/布尔转串
- 分支与异常：无
- 调用：`TrimToNull`、`WhereNotNull`

### AppKnowledgeSuggestionEnumerableExtensions
#### IEnumerable\<T\> WhereNotNull\<T\>(IEnumerable\<T?\> source)
- 输入：可空引用序列
- 输出：非 null 元素
- 副作用：无
- 步骤：foreach 过滤
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 JSON、EF、Infrastructure Data、PcTracker DTO/Entity
2. 命名空间 Services；密封类注入 db/contexts/appSignatures
3. `BuildRecommendedContextAsync`：加载建议→解析清洗上下文→域名/进程/签名→推荐模式与备选→返回预览
4. `SaveRecommendedContextAsync`：确保 AppId→Save 上下文→回填 Source 与影响字段
5. `EnsureAppSignatureForContextAsync`：查找或创建 learned 签名，处理并发唯一冲突
6. `BuildRecommendedPattern` / `FindAppSignatureAsync` / `AddExeVariant`
7. `BuildContextDto` 组装展示标签与 0.9 置信度
8. `ResolveProcessName` / `ParseClusterKey` 解析聚类键
9. 影响预览估算、去重、域名提取、模式中文标签、TrimToNull
10. 内部 `SanitizedSuggestionContext.Parse` 多键名读 JSON
11. 内部 `WhereNotNull` 扩展

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs",
      "label": "AppKnowledgeSuggestionService",
      "path": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeSuggestionService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs", "type": "depends_on" }
  ]
}
```
