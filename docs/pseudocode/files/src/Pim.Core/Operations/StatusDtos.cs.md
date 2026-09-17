# src/Pim.Core/Operations/StatusDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义系统健康状态摘要/详情 DTO 以及 `ISystemStatusService` 查询契约。
- 主要依赖：同命名空间 `PimHealthStatus`、`StatusComponentKind`（`OperationEnums.cs`）
- 被谁使用：`SystemStatusService` 实现；`StatusEndpoints`；`OperationsHealthTodaySectionProvider`；单元测试假实现

## 函数级结构化伪代码

### SystemStatusSummaryDto
#### 记录构造 `SystemStatusSummaryDto(Status, Label, Message, CheckedAt)`
- 输入：整体 `PimHealthStatus`、展示标签、说明文案、检查时间
- 输出：不可变摘要记录
- 副作用：无
- 步骤：
  1. 以位置参数保存四字段
- 分支与异常：无
- 调用：无

### StatusComponentDto
#### 记录构造 `StatusComponentDto(Key, Name, Kind, Status, Message, CheckedAt, Details)`
- 输入：组件键、显示名、`StatusComponentKind`、健康状态、消息、检查时间、明细字典
- 输出：单组件状态记录
- 副作用：无
- 步骤：
  1. 保存组件身份与健康结果及 `Details` 字典
- 分支与异常：无
- 调用：无

### SystemStatusDetailDto
#### 记录构造 `SystemStatusDetailDto(Summary, Components, NextSteps)`
- 输入：摘要、组件列表、后续建议步骤列表
- 输出：详情聚合记录
- 副作用：无
- 步骤：
  1. 组合 `Summary` + `Components` + `NextSteps`
- 分支与异常：无
- 调用：无

### ISystemStatusService
#### `Task<SystemStatusSummaryDto> GetSummaryAsync(CancellationToken ct = default)`
- 输入：可选取消令牌
- 输出：系统级健康摘要
- 副作用：实现侧可能探测 DB/守护进程等（本文件仅契约）
- 步骤：
  1. 实现方异步聚合后返回 `SystemStatusSummaryDto`
- 分支与异常：由实现定义
- 调用：实现 → 基础设施探测

#### `Task<SystemStatusDetailDto> GetDetailAsync(CancellationToken ct = default)`
- 输入：可选取消令牌
- 输出：含组件列表与 `NextSteps` 的详情
- 副作用：同摘要，通常更重
- 步骤：
  1. 实现方收集各 `StatusComponentDto` 与建议步骤
  2. 组装 `SystemStatusDetailDto` 返回
- 分支与异常：由实现定义
- 调用：实现 → 各组件探测服务

## 近逐行中文伪代码

1. 命名空间 `Pim.Core.Operations`
2. 声明密封记录 `SystemStatusSummaryDto`：字段 `Status`、`Label`、`Message`、`CheckedAt`
3. 声明密封记录 `StatusComponentDto`：字段 `Key`、`Name`、`Kind`、`Status`、`Message`、`CheckedAt`、`Details`
4. 声明密封记录 `SystemStatusDetailDto`：字段 `Summary`、`Components`、`NextSteps`
5. 声明接口 `ISystemStatusService`
6. 方法 `GetSummaryAsync`：异步返回摘要 DTO，可选 `CancellationToken`
7. 方法 `GetDetailAsync`：异步返回详情 DTO，可选 `CancellationToken`
8. （文件结束；无实现体）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Operations/StatusDtos.cs",
      "label": "StatusDtos",
      "path": "src/Pim.Core/Operations/StatusDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Operations/StatusDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Operations/StatusDtos.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/StatusEndpoints.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "calls" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "calls" },
    { "from": "tests/Pim.UnitTests/Operations/SystemStatusServiceTests.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Today/TodaySectionProviderTests.cs", "to": "src/Pim.Core/Operations/StatusDtos.cs", "type": "tests" }
  ]
}
```
