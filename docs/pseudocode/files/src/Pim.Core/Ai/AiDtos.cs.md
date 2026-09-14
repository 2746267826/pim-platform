# src/Pim.Core/Ai/AiDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义 AI 网关请求/响应、Schema、状态、请求日志与用量汇总等核心 DTO（`record`）
- 主要依赖：`Pim.Core.Common`（命名空间引用）；`AiMessageRole`、`AiRequestStatus`（AI 枚举）
- 被谁使用：AI 网关服务、API 控制器、日志查询与用量统计等上层模块

## 函数级结构化伪代码

### AiMessage
#### record AiMessage(AiMessageRole Role, string Content)
- 输入：`Role` 消息角色；`Content` 文本内容
- 输出：不可变消息 DTO
- 副作用：无
- 步骤：
  1. 以位置参数构造消息记录
- 分支与异常：无
- 调用：无

### AiGatewayRequest
#### record AiGatewayRequest(...)
- 输入：模块/用途/源对象、消息列表、可选模型与 Schema、token/重试上限、元数据
- 输出：网关调用请求 DTO
- 副作用：无
- 步骤：
  1. 绑定全部位置/可选参数为只读属性
- 分支与异常：无
- 调用：无

#### int EffectiveMaxAttempts { get }
- 输入：可选 `MaxAttempts`
- 输出：钳制后的有效重试次数（1..2）
- 副作用：无
- 步骤：
  1. 若 `MaxAttempts` 为空则取 1，否则取其值
  2. 用 `Math.Clamp` 限制在闭区间 [1, 2]
- 分支与异常：无
- 调用：`Math.Clamp`

### AiTokenUsage
#### record AiTokenUsage(...)
- 输入：prompt/completion/total token、估算费用与币种（均可空）
- 输出：用量 DTO
- 副作用：无
- 步骤：
  1. 构造 token 与费用快照
- 分支与异常：无
- 调用：无

### AiResult
#### record AiResult(...)
- 输入：状态、响应文本、解析 JSON、Schema 校验错误、用量、日志 Id、面向用户错误
- 输出：一次 AI 调用结果 DTO
- 副作用：无
- 步骤：
  1. 绑定结果字段
- 分支与异常：无
- 调用：无

#### static AiResult FailedValidation(Guid? logId, IReadOnlyList<string> errors)
- 输入：`logId` 可选日志标识；`errors` Schema 校验错误列表
- 输出：状态为 `FailedValidation` 的 `AiResult`
- 副作用：无
- 步骤：
  1. 状态设为 `AiRequestStatus.FailedValidation`
  2. 响应文本与解析 JSON 置空
  3. 写入 `errors` 为 `SchemaValidationErrors`
  4. 用量填全空 `AiTokenUsage`
  5. 写入 `logId` 与固定中文用户提示
- 分支与异常：无
- 调用：`AiTokenUsage` 构造

### AiSchemaDefinition
#### record AiSchemaDefinition(...)
- 输入：名称、版本、JSON Schema 文本、描述
- 输出：Schema 定义 DTO
- 副作用：无
- 步骤：
  1. 绑定 Schema 元数据
- 分支与异常：无
- 调用：无

### AiStatusDto
#### record AiStatusDto(...)
- 输入：是否启用、提供商、BaseUrl、默认模型、最近健康检查/成功调用时间、最近错误
- 输出：AI 服务状态 DTO
- 副作用：无
- 步骤：
  1. 绑定运行时状态字段
- 分支与异常：无
- 调用：无

### AiRequestLogFilter
#### record AiRequestLogFilter(...)
- 输入：时间范围、模块/用途/源对象、模型、状态、用户、分页（默认 Page=1, PageSize=50）
- 输出：请求日志查询过滤条件
- 副作用：无
- 步骤：
  1. 绑定可选过滤字段与分页默认值
- 分支与异常：无
- 调用：无

### AiRequestLogListItemDto
#### record AiRequestLogListItemDto(...)
- 输入：日志摘要字段（Id、时间、模块、模型、状态、token、费用、耗时、源对象、错误摘要）
- 输出：列表项 DTO
- 副作用：无
- 步骤：
  1. 绑定列表展示所需字段
- 分支与异常：无
- 调用：无

### AiRequestLogDetailDto
#### record AiRequestLogDetailDto(...)
- 输入：日志完整明细（用户、请求/响应 JSON、Schema 快照、校验错误、用量、错误码等）
- 输出：详情 DTO
- 副作用：无
- 步骤：
  1. 绑定审计与排障所需全部字段
- 分支与异常：无
- 调用：无

### AiUsageGroupDto
#### record AiUsageGroupDto(...)
- 输入：分组键与请求/成功/失败计数、token 与估算费用
- 输出：用量分组聚合 DTO
- 副作用：无
- 步骤：
  1. 绑定单组聚合指标
- 分支与异常：无
- 调用：无

### AiUsageSummaryDto
#### record AiUsageSummaryDto(...)
- 输入：全局汇总指标 + 按 Module/Purpose/Model/Status 的分组列表
- 输出：用量总览 DTO
- 副作用：无
- 步骤：
  1. 绑定汇总计数与四类分组
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用命名空间 `Pim.Core.Common`
2. 声明命名空间 `Pim.Core.Ai`
3. 定义密封记录 `AiMessage`：角色 `Role`（`AiMessageRole`）与内容 `Content`（string）
4. 定义密封记录 `AiGatewayRequest`，参数依次为：
5.   - `Module`：调用方模块名
6.   - `Purpose`：调用用途
7.   - `SourceObjectType`：源对象类型
8.   - `SourceObjectId`：源对象标识
9.   - `Messages`：只读消息列表
10.   - 可选 `Model`、`SchemaName`、`SchemaVersion`
11.   - 可选 `MaxOutputTokens`、`MaxAttempts`
12.   - 可选 `Metadata` 字符串字典
13. 计算属性 `EffectiveMaxAttempts`：
14.   - 取 `MaxAttempts`，空则用 1
15.   - 用 `Math.Clamp` 限制到 1 到 2 之间并返回
16. 定义密封记录 `AiTokenUsage`：Prompt/Completion/Total Tokens、EstimatedCost、Currency（均可空）
17. 定义密封记录 `AiResult`：Status、ResponseText、ParsedOutputJson、SchemaValidationErrors、Usage、LogId、UserFacingError
18. 静态工厂 `FailedValidation(logId, errors)`：
19.   - 构造新 `AiResult`
20.   - Status = `AiRequestStatus.FailedValidation`
21.   - ResponseText / ParsedOutputJson = null
22.   - SchemaValidationErrors = errors
23.   - Usage = 全 null 的 `AiTokenUsage`
24.   - LogId = logId
25.   - UserFacingError = 「AI 响应不符合要求的格式，未生成建议。」
26. 定义密封记录 `AiSchemaDefinition`：Name、Version、JsonSchema、Description
27. 定义密封记录 `AiStatusDto`：Enabled、Provider、BaseUrl、DefaultModel、LastHealthCheckAt、LastError、RecentSuccessfulCallAt
28. 定义密封记录 `AiRequestLogFilter`：From、To、Module、Purpose、SourceObjectType、SourceObjectId、Model、Status、UserId；Page 默认 1，PageSize 默认 50
29. 定义密封记录 `AiRequestLogListItemDto`：Id、StartedAt、Module、Purpose、Model、Status、TotalTokens、EstimatedCost、DurationMs、SourceObjectType、SourceObjectId、ErrorSummary
30. 定义密封记录 `AiRequestLogDetailDto`：完整日志明细字段（用户、提供商、LiteLLM 请求 Id、关联 Id、尝试次数、起止时间、请求/响应 JSON、Schema 快照与校验错误、Usage、错误码/消息、MetadataJson 等）
31. 定义密封记录 `AiUsageGroupDto`：GroupKey 与请求/成功/失败计数及 token、费用
32. 定义密封记录 `AiUsageSummaryDto`：全局汇总 + ByModule / ByPurpose / ByModel / ByStatus 四组 `AiUsageGroupDto` 列表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Ai/AiDtos.cs",
      "label": "AiDtos",
      "path": "src/Pim.Core/Ai/AiDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Ai/AiDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Core/Ai/AiDtos.cs", "to": "src/Pim.Core/Ai/AiEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Core/Ai/AiDtos.cs", "to": "Pim.Core.Common", "type": "depends_on" }
  ]
}
```
