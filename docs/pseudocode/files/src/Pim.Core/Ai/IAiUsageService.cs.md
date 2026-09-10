# src/Pim.Core/Ai/IAiUsageService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义 AI 用量/请求日志查询契约，供 API 与基础设施实现解耦。
- 主要依赖：`Pim.Core.Common.PagedResult<T>`；`AiStatusDto` / `AiRequestLogFilter` / `AiRequestLogListItemDto` / `AiRequestLogDetailDto` / `AiUsageSummaryDto`（同层 `AiDtos.cs`）
- 被谁使用：`Pim.Infrastructure.Ai.AiUsageService` 实现；`Pim.Api.Endpoints.AiEndpoints` 注入调用；DI 在 `ServiceCollectionExtensions` 注册

## 函数级结构化伪代码

### IAiUsageService
#### Task<AiStatusDto> GetStatusAsync(CancellationToken ct = default)
- 输入：`ct` 取消令牌
- 输出：`AiStatusDto`（AI 功能开关、配置摘要与健康相关状态）
- 副作用：无（契约层）；实现侧只读查询
- 步骤：
  1. 由实现读取 AI 配置与健康信息
  2. 组装并返回 `AiStatusDto`
- 分支与异常：契约不规定异常；实现可因存储/配置失败抛出
- 调用：被 `AiEndpoints` 的 `/status` 与 `/health-check` 路径调用

#### Task<PagedResult<AiRequestLogListItemDto>> ListRequestsAsync(AiRequestLogFilter filter, CancellationToken ct = default)
- 输入：`filter` 时间/模块/用途/模型/状态等筛选与分页；`ct`
- 输出：分页的请求日志列表项
- 副作用：无（契约层）；实现侧只读查询
- 步骤：
  1. 按 `filter` 条件过滤 AI 请求日志
  2. 分页投影为 `AiRequestLogListItemDto`
  3. 包装为 `PagedResult` 返回
- 分支与异常：空结果返回空页；无效筛选由实现或调用方处理
- 调用：被 `AiEndpoints` 列表接口调用

#### Task<AiRequestLogDetailDto?> GetRequestDetailAsync(Guid id, CancellationToken ct = default)
- 输入：请求日志 `id`；`ct`
- 输出：详情 DTO，或 `null` 表示不存在
- 副作用：无（契约层）
- 步骤：
  1. 按主键查找单条请求日志
  2. 存在则映射为 `AiRequestLogDetailDto`，否则返回 `null`
- 分支与异常：未找到 → `null`（API 层再映射为 404）
- 调用：被 `AiEndpoints` 的 `/requests/{id}` 调用

#### Task<AiUsageSummaryDto> GetUsageSummaryAsync(DateTimeOffset? from, DateTimeOffset? to, CancellationToken ct = default)
- 输入：可选时间范围 `from`/`to`；`ct`
- 输出：用量汇总 `AiUsageSummaryDto`
- 副作用：无（契约层）
- 步骤：
  1. 在时间窗口内聚合请求次数、token、费用等指标
  2. 返回汇总 DTO
- 分支与异常：无数据时返回零值汇总（实现约定）
- 调用：被 `AiEndpoints` 的 `/usage/summary` 调用

## 近逐行中文伪代码

1. 引入公共分页类型 `PagedResult`
2. 命名空间：`Pim.Core.Ai`
3. 声明公开接口 `IAiUsageService`
4. 方法 `GetStatusAsync`：异步返回 AI 状态 DTO，支持取消
5. 方法 `ListRequestsAsync`：按过滤器异步返回分页请求日志列表
6. 方法 `GetRequestDetailAsync`：按 GUID 异步返回详情或空
7. 方法 `GetUsageSummaryAsync`：按可选起止时间异步返回用量汇总
8. 接口无默认实现体，全部由基础设施层提供

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Ai/IAiUsageService.cs",
      "label": "IAiUsageService",
      "path": "src/Pim.Core/Ai/IAiUsageService.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Ai/IAiUsageService.cs.md",
      "layer": "core",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Ai/IAiUsageService.cs", "to": "src/Pim.Core/Ai/AiDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Core/Ai/IAiUsageService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Ai/IAiUsageService.cs", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Ai/IAiUsageService.cs", "type": "calls" }
  ]
}
```
