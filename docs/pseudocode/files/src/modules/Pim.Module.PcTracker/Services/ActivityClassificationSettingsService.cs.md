# src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：活动分类推荐最短时长设置的读取与保存；预设分钟钳制；并发插入冲突重试
- 主要依赖：`PimDbContext`、`ActivityClassificationSettingsEntity`、`ActivityClassificationSettingsDto`
- 被谁使用：PcTracker 活动分类设置 API / UI

## 函数级结构化伪代码

### ActivityClassificationSettingsService
#### 常量
- DefaultSettingsKey = `"default"`
- DefaultRecommendedMinimumMinutes = 5
- SupportedRecommendedMinimumDurations = [1, 3, 5, 10, 15]

#### 构造函数
- 输入：`PimDbContext db`
- 输出：实例
- 副作用：无
- 步骤：保存 `_db`
- 分支与异常：无
- 调用：无

#### `async Task<ActivityClassificationSettingsDto> GetSettingsAsync(ct)`
- 输入：取消令牌
- 输出：设置 Dto（有行则 ToDto，否则 DefaultDto）
- 副作用：只读
- 步骤：GetSettingsEntityAsync → 有实体 ToDto 否则 DefaultDto
- 分支与异常：无
- 调用：`GetSettingsEntityAsync`、`ToDto`、`DefaultDto`

#### `async Task<ActivityClassificationSettingsDto> SaveSettingsAsync(int requestedMinutes, ct)`
- 输入：请求的推荐最短分钟数
- 输出：保存后 Dto
- 副作用：插入或更新设置行
- 步骤：
  1. 取实体或 `CreateDefaultSettingsEntity`
  2. RecommendedMinimum… = ClampToSupportedPreset(requestedMinutes)
  3. UpdatedAt = UtcNow
  4. 若 Entry 为 Detached 则 Add
  5. SaveChanges
  6. 若 DbUpdateException 且状态为 Added：Clear tracker；重新加载默认行（不存在则抛 InvalidOperationException 中文消息）；再次钳制与 UpdatedAt；Save
  7. 返回 ToDto
- 分支与异常：并发插入冲突重试；重试后仍无默认行则抛
- 调用：`GetSettingsEntityAsync`、`CreateDefaultSettingsEntity`、`ClampToSupportedPreset`、`ToDto`

#### `private async GetSettingsEntityAsync`
- 输入：ct
- 输出：SettingsKey==default 的首行或 null
- 副作用：只读
- 步骤：FirstOrDefaultAsync
- 分支与异常：无
- 调用：EF

#### `private static CreateDefaultSettingsEntity`
- 输入：无
- 输出：新实体（新 Guid、key=default、默认 5 分钟、Created/Updated=now）
- 副作用：无
- 步骤：构造未跟踪实体
- 分支与异常：无
- 调用：无

#### `private static int ClampToSupportedPreset(int requestedMinutes)`
- 输入：任意分钟数
- 输出：预设列表中与请求最接近者；并列取较小 duration
- 副作用：无
- 步骤：OrderBy Abs(diff) ThenBy duration → First
- 分支与异常：无
- 调用：无

#### `ToDto` / `DefaultDto`
- 输入：实体或无
- 输出：`ActivityClassificationSettingsDto(当前分钟, 支持预设数组副本)`
- 副作用：无
- 步骤：ToArray 支持列表；默认分钟用 DefaultRecommendedMinimumMinutes
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF、Data、DTOs、Entities
2. 默认 key、默认 5 分钟、支持 [1,3,5,10,15]
3. 构造注入 PimDbContext
4. GetSettings：查 default 行 → ToDto 或 DefaultDto
5. Save：取或建默认实体；钳制分钟；UpdatedAt；Detached 则 Add
6. Save 遇 DbUpdateException 且 Added：Clear → 重载行 → 再写 → Save
7. 重载失败抛「保存设置时发生冲突，且默认设置行不存在。」
8. GetSettingsEntity：SettingsKey==default
9. CreateDefault：新 Guid + 默认字段
10. Clamp：最近预设，并列取更小值
11. ToDto/DefaultDto：分钟 + 预设数组

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs",
      "label": "ActivityClassificationSettingsService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityClassificationSettingsEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
