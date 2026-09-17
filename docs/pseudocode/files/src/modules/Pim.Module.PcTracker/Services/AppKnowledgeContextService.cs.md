# src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：应用知识上下文 CRUD：按 App 列表；按 (ProcessName, PatternType, PatternValue) 幂等 Upsert；删除；构建范围摘要。
- 主要依赖：`PimDbContext`、`AppKnowledgeContextEntity`、`AppSignatureEntity`、PcTracker DTOs
- 被谁使用：PcTracker 应用知识相关端点

## 函数级结构化伪代码

### AppKnowledgeContextService
#### 静态 AllowedPatternTypes
- 输入：无
- 输出：允许模式集合
- 副作用：无
- 步骤：app-default / domain / title / url-path / source-family（忽略大小写）
- 分支与异常：无
- 调用：无

#### 构造(PimDbContext db)
- 输入：db
- 输出：实例
- 副作用：无
- 步骤：捕获 `_db`
- 分支与异常：无
- 调用：无

#### Task\<List\<AppKnowledgeContextDto\>\> GetByAppAsync(appId, ct)
- 输入：AppSignature Id
- 输出：DTO 列表
- 副作用：只读查询
- 步骤：过滤 AppSignatureId → OrderBy UpdatedAt desc, PatternType, PatternValue → Select ToDto
- 分支与异常：无
- 调用：ToDto

#### Task\<AppKnowledgeContextDto\> SaveAsync(req, ct)
- 输入：SaveAppKnowledgeContextRequest
- 输出：保存后 DTO
- 副作用：Insert 或 Update + Save；并发插入冲突时 Detach 后重读再 Update
- 步骤：
  1. RequireTrimmed ProcessName/PatternType/PatternValue；PatternType 小写
  2. Confidence 默认 1.0；校验类型与 [0,1]
  3. 按三元组 FirstOrDefault；null 则 NewGuid 实体 Add（Affected* 归零）
  4. BuildScopeSummary；ApplyRequest
  5. Save；若 insert 遇 DbUpdateException → Detach → 再查 → 再 Apply + Save
  6. ToDto
- 分支与异常：ArgumentException 校验；二次仍 null 则 rethrow
- 调用：BuildScopeSummary、ApplyRequest

#### Task\<bool\> DeleteAsync(id, ct)
- 输入：上下文 Id
- 输出：是否删除成功
- 副作用：Remove + Save
- 步骤：Find → 不存在 false；否则 Remove Save true
- 分支与异常：无
- 调用：EF

#### static ToDto(entity)
- 输入：实体
- 输出：AppKnowledgeContextDto
- 副作用：无
- 步骤：字段一一映射含 Affected* 与 LastMatchedAt
- 分支与异常：无
- 调用：无

#### BuildScopeSummaryAsync(appId?, processName, patternType, patternValue, ct)
- 输入：可选 AppId 与模式键
- 输出：摘要串 `"{appName} · {标签}：{patternValue}"`
- 副作用：可能读 AppSignature
- 步骤：有 appId 则 Find，不存在抛错；DisplayName 优先否则 processName；拼 ToPatternLabel
- 分支与异常：App 不存在 ArgumentException
- 调用：ToPatternLabel、AppSignatureEntity

#### ToPatternLabel / ApplyRequest / RequireTrimmed / TrimToNull
- 输入：类型串或请求字段
- 输出：中文标签/写实体/非空 trim/可空 trim
- 副作用：ApplyRequest 改实体字段
- 步骤：
  1. 模式中文：App 默认/域名/窗口标题/网址路径/来源类型
  2. Apply：写 AppSignatureId、三元组、目标分类/项目、ScopeSummary、Source=user-confirmed、Confidence、Enabled 默认 true、UpdatedAt
  3. RequireTrimmed 空白抛错；TrimToNull 空白→null
- 分支与异常：Require 抛 ArgumentException
- 调用：无

## 近逐行中文伪代码

1. 允许五种 PatternType；注入 db
2. GetByApp：按 AppSignatureId 排序列表转 DTO
3. Save：校验三元组与置信度 → upsert → 摘要 → 保存；插入冲突则重读更新
4. Delete：Find 后删
5. 摘要用 App 展示名 + 模式中文标签；Source 固定 user-confirmed

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs",
      "label": "AppKnowledgeContextService",
      "path": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
