# src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：将移动端 usage session 与 fallback summary 归并为时间线块（按 lifeCategory + 5 分钟间隙合并）；分页/cursor 查询块、块内会话、会话关联事件。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`TimeProvider`、可选 `MobileAppClassificationService`、`MobileAnalyticsQueryService`、`MobileUsageQueryService`、Mobile 实体/DTO
- 被谁使用：`MobileModule` 时间线相关 HTTP 端点

## 函数级结构化伪代码

### MobileTimelineBlockService
#### 构造注入
- 输入：db、currentUser、timeProvider、可选 classificationService
- 输出：实例
- 副作用：无
- 步骤：捕获依赖；静态 BlockMergeGap=5min；Web 默认 JsonOptions
- 分支与异常：无
- 调用：无

#### GetBlocksAsync(MobileAnalyticsQueryRequest)
- 输入：分析查询请求（范围/设备/包/源/分类过滤/分页/cursor）
- 输出：`MobileTimelineBlockPageDto`（页项、nextCursor、hasMore、page 元数据）
- 副作用：只读查询
- 步骤：
  1. Normalize 请求；BuildBlocks 后按 StartUtc 降序、Id 升序。
  2. 有 Cursor：ApplyCursor 后取 pageSize+1 判 hasMore；无 cursor：Skip/Take 分页。
  3. hasMore 时用末项 (StartUtc,Id) 编码 nextCursor。
  4. 映射 block.Dto 列表返回。
- 分支与异常：未登录（深层）
- 调用：BuildBlocksAsync、EncodePayload

#### GetSessionsForBlockAsync(blockId, request)
- 输入：块 id、查询上下文
- 输出：会话 DTO 列表
- 副作用：只读
- 步骤：
  1. 空 blockId → []。
  2. 在当前 BuildBlocks 中找同 Id 块；若无，Decode BlockIdPayload，用 ItemIds 从 timeline items 回填。
  3. 按时间/Id 排序后 ToSessionDto。
- 分支与异常：payload 无效返回空
- 调用：BuildBlocksAsync、BuildTimelineItemsAsync

#### GetSessionEventsAsync(sessionId)
- 输入：session GUID 字符串
- 输出：`MobileSessionEventDto` 列表
- 副作用：只读
- 步骤：
  1. 非 GUID → []；加载用户 session，不存在 []。
  2. EffectiveSessionEnd 校正 end>=start。
  3. 查同 user/device/package 且时间落在 [start,end] 的 MobileUsageEvent，投影 DTO。
- 分支与异常：未登录
- 调用：EF、EffectiveSessionEnd

#### BuildBlocksAsync / BuildTimelineItemsAsync
- 输入：Normalized 查询上下文
- 输出：ComputedBlock 列表 / TimelineItem 列表
- 副作用：只读
- 步骤：
  1. 解析时区；items 按 start/end/id 排序。
  2. BlockBuilder：同 lifeCategory 且 item.StartUtc <= EndUtc+5min 则合并，否则新块。
  3. TimelineItems：按 Source 取 sessions 与/或 fallback summaries；加载分类；裁剪到查询范围；最小时长过滤；session confidence=1，fallback=0.6。
- 分支与异常：分类服务可选路径
- 调用：QuerySessions、QueryFallbackSummaries、LoadClassifications*

#### LoadClassificationsAsync / LoadClassificationsFromServiceAsync
- 输入：userId、deviceId、package 集合
- 输出：package → AppClassification
- 副作用：只读
- 步骤：
  1. 无 package → 空字典；catalog 按 package 取最新 UpdatedAt。
  2. 有 classificationService：逐包 ClassifyAsync。
  3. 否则合并 override + enabled rules + catalog：DisplayName/LifeCategory/IsSystemNoise 优先级 override>rule>catalog/默认。
- 分支与异常：无
- 调用：MobileAppClassificationService 或规则匹配

#### 辅助：Normalize、EffectiveSessionEnd、分页 cursor、JSON payload、SourceMatches、RuleMatches、时长/裁剪/质量标志
- 输入：见签名
- 输出：上下文、时间、bool、秒数等
- 副作用：无（QualityFlags 反序列化失败吞掉）
- 步骤：
  1. EffectiveSessionEnd：EndUtc 或 Start+DurationMs 或 now。
  2. Cursor/BlockId 用 base64url JSON；非法 Format/Json → default。
  3. RuleMatches：package-prefix/contains、display-name-contains、category-exact、默认 exact。
  4. ProratedSeconds：按重叠比例折算 fallback 可见时长。
- 分支与异常：JSON/Base64 错误返回空/默认
- 调用：MobileAnalyticsQueryService.Normalize

#### 内部类型 TimelineItem / AppClassification / ComputedBlock / PageCursor / BlockIdPayload / BlockBuilder
- 输入：构建参数
- 输出：合并块 DTO（topApps 前 5、sourceMix、qualityFlags、含本地时间字符串）
- 副作用：无
- 步骤：BlockBuilder.Build 编码稳定 id、按时长选主 lifeCategory、汇总秒数/会话数/包数
- 分支与异常：无
- 调用：EncodePayload、时区 FormatLocal

## 近逐行中文伪代码

1. 合并间隙 5 分钟；构造注入 db/用户/时间/可选分类服务。
2. GetBlocks：建块→排序→cursor 或 page 分页→nextCursor。
3. GetSessionsForBlock：按块 id 或 payload ItemIds 回填会话。
4. GetSessionEvents：session 窗内 usage events。
5. BuildTimelineItems：events/fallback 源、分类过滤、时长裁剪；BuildBlocks 按类别+间隙合并。
6. 分类：服务路径或 override/rule/catalog；cursor/payload base64url；BlockBuilder 汇总 TopApps 与 sourceMix。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs",
      "label": "MobileTimelineBlockService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "to": "src/Pim.Infrastructure/Auth", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAnalyticsQueryService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileUsageQueryService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileTimelineBlockService.cs", "to": "src/modules/Pim.Module.Mobile/Entities", "type": "depends_on" }
  ]
}
```
