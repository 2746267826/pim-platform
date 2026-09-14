# src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端用量目标（按 Scope/包名/生活分类）的列表、upsert、删除
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`TimeProvider`、`MobileUserContext`、`MobileUsageGoalEntity`、`MobileUsageGoalDto`/`UpsertRequest`
- 被谁使用：Mobile 用量目标 API；分析概览可读取目标进度

## 函数级结构化伪代码

### MobileUsageGoalService
#### 构造函数
- 输入：db、currentUser、timeProvider
- 输出：实例
- 副作用：无
- 步骤：赋值 `_db`、`_currentUser`、`_timeProvider`
- 分支与异常：无
- 调用：无

#### `async Task<IReadOnlyList<MobileUsageGoalDto>> ListAsync(ct)`
- 输入：无
- 输出：当前用户目标列表
- 副作用：只读查询
- 步骤：
  1. RequireUserId
  2. AsNoTracking Where UserId
  3. OrderBy Scope → ThenBy LifeCategory → ThenBy PackageName
  4. Select ToDto → ToList
- 分支与异常：未登录
- 调用：EF、`ToDto`

#### `async Task<MobileUsageGoalDto> SaveAsync(MobileUsageGoalUpsertRequest request, ct)`
- 输入：Scope/PackageName/LifeCategory/Label/LimitSeconds/IsEnabled
- 输出：保存后的 Dto
- 副作用：插入或更新实体并 Save
- 步骤：
  1. userId；now = GetUtcNow
  2. scope = Normalize(Scope, `"total-daily"`)
  3. packageName/lifeCategory = NormalizeOptional（空白→null）
  4. 按 UserId+Scope+PackageName+LifeCategory 查单条
  5. 无则新建（UserId、Scope、PackageName、LifeCategory、CreatedAt）并 Add
  6. Label = Normalize(Label, `"每日手机总时长"`)
  7. LimitSeconds = Max(0, request.LimitSeconds)；IsEnabled；UpdatedAt=now
  8. Save；ToDto
- 分支与异常：未登录；Limit 负值钳到 0
- 调用：`Normalize`、`NormalizeOptional`、`ToDto`

#### `async Task<bool> DeleteAsync(string id, ct)`
- 输入：目标 Id 字符串
- 输出：是否删除成功
- 副作用：匹配则 Remove + Save
- 步骤：
  1. RequireUserId
  2. Guid.TryParse 失败 → false
  3. SingleOrDefault UserId+Id；无 → false
  4. Remove；Save；true
- 分支与异常：非法 Guid 或越权/不存在返回 false
- 调用：EF

#### `private static MobileUsageGoalDto ToDto(entity)`
- 输入：实体
- 输出：Dto（Id 为 `"D"` 格式字符串）
- 副作用：无
- 步骤：投影 Scope/PackageName/LifeCategory/Label/Limit/IsEnabled/时间戳
- 分支与异常：无
- 调用：无

#### `Normalize` / `NormalizeOptional`
- 输入：可空字符串与 fallback
- 输出：空白→fallback 或 null；否则 Trim
- 副作用：无
- 步骤：IsNullOrWhiteSpace 分支
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF、Auth、Data、DTOs、Entities
2. sealed 服务；注入 db、currentUser、TimeProvider
3. List：当前用户目标，Scope→LifeCategory→PackageName 排序后 ToDto
4. Save：规范化 scope（默认 total-daily）、可选包名/分类；按四元组 upsert
5. 新实体设 CreatedAt；Label 默认「每日手机总时长」；Limit≥0；写 IsEnabled/UpdatedAt；Save
6. Delete：解析 Guid 失败或找不到返回 false；否则删除并 true
7. ToDto：Id.ToString("D") 与字段映射
8. Normalize/NormalizeOptional：空白回退或 null，否则 Trim

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs",
      "label": "MobileUsageGoalService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageGoalService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageGoalEntity.cs", "type": "depends_on" }
  ]
}
```
