# src/Pim.Infrastructure/Audit/AuditVersionService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：对象版本审计服务——记录变更快照、查询时间线、生成恢复预览、按时间范围导出 JSON。
- 主要依赖：`PimDbContext`、`AuditVersionEntity`、`Pim.Core.Audit` DTO、EF Core、`System.Text.Json`
- 被谁使用：`ServiceCollectionExtensions` 注册为 Scoped；`DataCenterGovernanceService`、Calendar 等模块调用

## 函数级结构化伪代码

### AuditVersionService
#### 构造函数 AuditVersionService(PimDbContext db)
- 输入：`PimDbContext`
- 输出：服务实例
- 副作用：无
- 步骤：
  1. 保存 `_db` 引用
- 分支与异常：无
- 调用：无

#### Task\<AuditVersionDto\> RecordAsync(string objectType, Guid objectId, object before, object after, IReadOnlyList\<string\> changedFields, Guid? confirmationId, string source, CancellationToken ct = default)
- 输入：对象类型/Id、变更前后对象、变更字段列表、可选确认 Id、来源、取消令牌
- 输出：新写入的 `AuditVersionDto`
- 副作用：插入 `audit_versions` 并 `SaveChangesAsync`
- 步骤：
  1. 构造 `AuditVersionEntity`：序列化 before/after/changedFields 为 JSON；Actor 固定 `"system"`；CreatedAt=UtcNow
  2. `_db.AuditVersions.Add(entity)`
  3. `SaveChangesAsync`
  4. `Map(entity)` 返回 DTO
- 分支与异常：序列化/EF 异常向上抛出
- 调用：`JsonSerializer.Serialize`、`Map`、EF SaveChanges

#### Task\<AuditTimelineResponse\> GetTimelineAsync(string objectType, Guid objectId, CancellationToken ct = default)
- 输入：对象类型与 Id；取消令牌
- 输出：按时间升序的版本列表响应
- 副作用：只读查询
- 步骤：
  1. `AsNoTracking` 过滤 ObjectType+ObjectId
  2. `OrderBy CreatedAt ThenBy Id`
  3. `Select Map` → `ToListAsync`
  4. 包装为 `AuditTimelineResponse`
- 分支与异常：无匹配时返回空列表
- 调用：EF 查询、`Map`

#### Task\<RestorePreviewResponse\> PreviewRestoreAsync(Guid auditVersionId, CancellationToken ct = default)
- 输入：审计版本 Id；取消令牌
- 输出：恢复预览（类型/Id/摘要/需确认/变更字段）
- 副作用：只读查询
- 步骤：
  1. `AsNoTracking` + `SingleAsync` 按 Id 取实体
  2. 反序列化 `ChangedFieldsJson`（失败则空数组）
  3. 构造 `RestorePreviewResponse`：Summary 含类型/对象/版本 Id；`RequiresConfirmation=true`
- 分支与异常：不存在 → EF `SingleAsync` 抛异常
- 调用：`JsonSerializer.Deserialize`、`SingleAsync`

#### Task\<AuditExportResponse\> ExportAsync(DateTimeOffset start, DateTimeOffset end, CancellationToken ct = default)
- 输入：起止时间（含边界）；取消令牌
- 输出：`AuditExportResponse`（文件名 `audit-export.json`、content-type、序列化后的 items JSON）
- 副作用：只读查询
- 步骤：
  1. 过滤 `CreatedAt` 在 [start, end]
  2. 排序 CreatedAt/Id；Map 为列表
  3. 序列化列表为 JSON 字符串放入响应
- 分支与异常：无
- 调用：EF 查询、`Map`、`JsonSerializer.Serialize`

#### static AuditVersionDto Map(AuditVersionEntity entity)
- 输入：实体
- 输出：DTO（字段一一对应，含 BeforeJson/AfterJson/ChangedFieldsJson 原文字符串）
- 副作用：无
- 步骤：位置参数构造 `AuditVersionDto`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 `System.Text.Json`、EF Core、`Pim.Core.Audit`、`Pim.Infrastructure.Data`
2. 命名空间 `Pim.Infrastructure.Audit`
3. 密封类 `AuditVersionService`，字段 `_db`
4. 构造：注入 `PimDbContext`
5. `RecordAsync`：new 实体，ObjectType/ObjectId/ConfirmationId/Source 赋值
6. Actor=`"system"`；Before/After/ChangedFields 序列化为 JSON；CreatedAt=UtcNow
7. Add 到 `AuditVersions`；SaveChanges；返回 Map
8. `GetTimelineAsync`：无跟踪查询 ObjectType+ObjectId，按 CreatedAt/Id 升序，Select Map，ToList
9. 返回 `new AuditTimelineResponse(items)`
10. `PreviewRestoreAsync`：按 Id SingleAsync 取实体
11. 反序列化 ChangedFieldsJson，null 则 `Array.Empty`
12. 返回 RestorePreviewResponse（摘要字符串、RequiresConfirmation=true、changedFields）
13. `ExportAsync`：CreatedAt 区间过滤，排序 Map 列表
14. 返回文件名/MIME/序列化 JSON 的 `AuditExportResponse`
15. 私有静态 `Map`：实体字段映射到 `AuditVersionDto`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Audit/AuditVersionService.cs",
      "label": "AuditVersionService",
      "path": "src/Pim.Infrastructure/Audit/AuditVersionService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Audit/AuditVersionService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "to": "src/Pim.Core/Audit/AuditVersionDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Audit/AuditVersionService.cs", "type": "depends_on" }
  ]
}
```
