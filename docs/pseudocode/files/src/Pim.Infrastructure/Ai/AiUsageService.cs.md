# src/Pim.Infrastructure/Ai/AiUsageService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：实现 `IAiUsageService`：从 `AiRequestLogs` / `AiProviderSettings` 与 `AiOptions` 组装 AI 状态、分页请求列表、请求详情与用量汇总。
- 主要依赖：`PimDbContext`、`IOptions<AiOptions>`、`Microsoft.EntityFrameworkCore`、`Pim.Core.Ai`、`Pim.Core.Common.PagedResult`
- 被谁使用：DI 注册为 `IAiUsageService`；`AiEndpoints` 调用状态/列表/详情/汇总接口

## 函数级结构化伪代码

### AiUsageService
#### Task<AiStatusDto> GetStatusAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：`AiStatusDto`（启用开关、Provider、BaseUrl、DefaultModel、最近健康检查、最近错误、最近成功时间）
- 副作用：只读查询数据库
- 步骤：
  1. 读取 `options.Value` 配置
  2. 查询 `AiRequestLogs` 中 Status=`"succeeded"` 的最近 `StartedAt`
  3. 查询 `AiProviderSettings` 中 Provider=`"litellm"` 的单行（可空）
  4. 组装并返回 `AiStatusDto`
- 分支与异常：无 settings 行时健康字段为 null；EF 异常向上抛
- 调用：`db.AiRequestLogs`、`db.AiProviderSettings`、`FirstOrDefaultAsync`、`SingleOrDefaultAsync`

#### Task<PagedResult<AiRequestLogListItemDto>> ListRequestsAsync(AiRequestLogFilter filter, CancellationToken ct = default)
- 输入：筛选与分页条件；取消令牌
- 输出：分页的 `AiRequestLogListItemDto` 列表
- 副作用：只读查询
- 步骤：
  1. `AsNoTracking` + `ApplyFilter`
  2. `CountAsync` 得 total
  3. page=`Max(1, filter.Page)`；pageSize=`Clamp(1..200)`
  4. 按 `StartedAt` 降序 Skip/Take，投影为列表项 DTO（Status 经 `FromStorageStatus`）
  5. 计算 totalPages 并返回 `PagedResult`
- 分支与异常：total=0 时 totalPages=0
- 调用：`ApplyFilter`、`FromStorageStatus`

#### Task<AiRequestLogDetailDto?> GetRequestDetailAsync(Guid id, CancellationToken ct = default)
- 输入：日志主键；取消令牌
- 输出：详情 DTO 或 null
- 副作用：只读查询
- 步骤：
  1. 按 Id `SingleOrDefaultAsync`
  2. 不存在 → null
  3. 存在 → 映射全部字段为 `AiRequestLogDetailDto`（含 `AiTokenUsage`、存储状态转枚举）
- 分支与异常：未找到返回 null
- 调用：`FromStorageStatus`

#### Task<AiUsageSummaryDto> GetUsageSummaryAsync(DateTimeOffset? from, DateTimeOffset? to, CancellationToken ct = default)
- 输入：可选时间范围；取消令牌
- 输出：`AiUsageSummaryDto`（总数、成功/失败、token/费用合计及按 Module/Purpose/Model/Status 分组）
- 副作用：只读查询后内存聚合
- 步骤：
  1. 构造仅含 from/to 的 `AiRequestLogFilter`
  2. `ApplyFilter` 后投影为内部 `AiUsageSummaryRow` 列表
  3. 统计 count / success / failed / token 与 cost 合计
  4. 对 Module/Purpose/Model/Status 调用 `Group`
  5. 返回汇总 DTO
- 分支与异常：无数据时各计数为 0、分组为空列表
- 调用：`ApplyFilter`、`IsSuccess`、`Group`

#### IQueryable<AiRequestLogEntity> ApplyFilter(IQueryable<AiRequestLogEntity> query, AiRequestLogFilter filter)
- 输入：可查询序列；筛选条件
- 输出：叠加条件后的查询
- 副作用：无
- 步骤：依次对 From/To/Module/Purpose/SourceObjectType/SourceObjectId/Model/Status/UserId 非空时 `Where`
- 分支与异常：Status 经 `ToStorageStatus` 转存储字符串
- 调用：`ToStorageStatus`

#### IReadOnlyList<AiUsageGroupDto> Group(IEnumerable<AiUsageSummaryRow> logs, Func<AiUsageSummaryRow, string> keySelector)
- 输入：行集合；分组键选择器
- 输出：按数量降序的分组 DTO 列表
- 副作用：无
- 步骤：GroupBy → OrderByDescending Count → 投影 `AiUsageGroupDto`（含成功/失败/token/费用）
- 分支与异常：无
- 调用：`IsSuccess(AiUsageSummaryRow)`

#### bool IsSuccess(AiRequestLogEntity log) / bool IsSuccess(AiUsageSummaryRow log)
- 输入：实体或汇总行
- 输出：Status 是否等于 `"succeeded"`
- 副作用：无
- 步骤：字符串比较
- 分支与异常：无
- 调用：无

#### string ToStorageStatus(AiRequestStatus status)
- 输入：领域枚举
- 输出：存储用小写/下划线字符串
- 副作用：无
- 步骤：Succeeded/Failed/Blocked/TimedOut/FailedValidation 映射；默认 `"failed"`
- 分支与异常：未知枚举 → failed
- 调用：无

#### AiRequestStatus FromStorageStatus(string status)
- 输入：存储字符串
- 输出：领域枚举
- 副作用：无
- 步骤：succeeded/blocked/timed_out/failed_validation 映射；默认 Failed
- 分支与异常：未知字符串 → Failed
- 调用：无

### AiUsageSummaryRow（私有 record）
- 输入：Module/Purpose/Model/Status 与 token/cost 字段
- 输出：内存聚合用行
- 副作用：无
- 步骤：纯数据载体
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 EF Core、Options、`Pim.Core.Ai`、`Pim.Core.Common`、`Pim.Infrastructure.Data` 与 Entities
2. 命名空间 `Pim.Infrastructure.Ai`
3. 密封类 `AiUsageService` 主构造注入 `PimDbContext` 与 `IOptions<AiOptions>`，实现 `IAiUsageService`
4. `GetStatusAsync`：取 options；查最近成功请求时间；查 litellm 的 provider settings；返回 `AiStatusDto`
5. `ListRequestsAsync`：无跟踪查询 + 过滤；计总数；规范化 page/pageSize；降序分页投影列表项；返回 `PagedResult`
6. `GetRequestDetailAsync`：按 Id 查单条；空则 null；否则映射完整详情含 `AiTokenUsage`
7. `GetUsageSummaryAsync`：仅时间过滤；投影内部行；合计与四维 Group；返回 `AiUsageSummaryDto`
8. `ApplyFilter`：对 From/To/Module/Purpose/SourceObjectType/SourceObjectId/Model/Status/UserId 条件叠加 Where
9. 私有 record `AiUsageSummaryRow` 承载聚合字段
10. `Group`：按键分组、按条数降序、生成 `AiUsageGroupDto`
11. 两个 `IsSuccess` 重载：Status == `"succeeded"`
12. `ToStorageStatus`：枚举 → 存储字符串（含 timed_out / failed_validation）
13. `FromStorageStatus`：存储字符串 → 枚举；未知归 Failed

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Ai/AiUsageService.cs",
      "label": "AiUsageService",
      "path": "src/Pim.Infrastructure/Ai/AiUsageService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Ai/AiUsageService.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Ai/IAiUsageService.cs", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Infrastructure/Ai/AiOptions.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "type": "calls" }
  ]
}
```
