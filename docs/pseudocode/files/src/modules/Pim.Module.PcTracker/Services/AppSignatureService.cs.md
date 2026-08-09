# src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 应用签名（process→展示名/分类路径）的查询、模糊匹配、知识库应用列表、upsert 与删除（禁止删 builtin）。
- 主要依赖：`PimDbContext`、`AppSignatureEntity`、`AppKnowledgeContextEntity`、相关 DTO
- 被谁使用：PcTracker 模块 API / 知识库界面

## 函数级结构化伪代码

### AppSignatureService
#### 构造(PimDbContext db)
- 步骤：保存 `_db`

#### Task\<List\<AppSignatureDto\>\> GetAllAsync(search, ct)
- 输入：可选搜索串
- 输出：签名 DTO 列表
- 步骤：可选 ProcessName/DisplayName 包含过滤；按 LastSeenAt 降序、ProcessName 升序；ToDto
- 调用：EF

#### Task\<List\<AppKnowledgeAppDto\>\> GetKnowledgeAppsAsync(search, ct)
- 输入：可选搜索（含 SearchKeywords）
- 输出：应用知识卡片 DTO（含上下文数与受影响时长合计）
- 步骤：查签名；按 AppSignatureId 聚合 ContextCount 与 AffectedDurationSeconds；组装 DTO（中间字段 0 占位）
- 调用：`AppKnowledgeContextEntity` GroupBy

#### LookupByProcessNameAsync / FindByProcessNameAsync
- 输入：processName
- 输出：匹配 DTO 或 null
- 副作用：Lookup 命中时更新 LastSeenAt 并 Save
- 步骤：`FindMatchingEntityByProcessNameAsync`；Lookup 写 LastSeenAt
- 调用：FindMatching...

#### SaveAsync(req, ct)
- 输入：SaveAppSignatureRequest
- 输出：保存后 DTO
- 副作用：按 ProcessName upsert；新建 Source=manual、Confidence=1
- 步骤：存在则更新 DisplayName/CategoryPath/Productivity/Description；否则 Add；Save
- 调用：EF

#### DeleteAsync(id, ct)
- 输入：Guid
- 输出：是否删除成功
- 副作用：删除非 builtin 行
- 分支：不存在或 Source==builtin → false

#### GetCountAsync
- 输出：签名总数

#### FindMatchingEntityByProcessNameAsync（private）
- 输入：processName
- 输出：实体或 null
- 步骤：
  1. 小写精确匹配。
  2. 无 `.exe` 时再试 `name.exe`。
  3. 加载全部，对含 `*`/`?` 的 ProcessName 转通配正则匹配 name 与 name.exe。
- 调用：`Regex.Escape` / `IsMatch`

#### static ToDto
- 输入：实体
- 输出：AppSignatureDto 字段投影

## 近逐行中文伪代码

1. 构造注入 Db。
2. GetAll：可选搜索，LastSeenAt 排序。
3. GetKnowledgeApps：搜索含 keywords；聚合知识上下文统计。
4. Lookup：匹配并刷新 LastSeenAt；Find：只读匹配。
5. Save：ProcessName upsert，manual 来源。
6. Delete：禁止 builtin；Count 统计。
7. 匹配：精确 → .exe → 通配 glob 正则。
8. ToDto 映射公共字段。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs",
      "label": "AppSignatureService",
      "path": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppKnowledgeContextEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppSignatureService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
