# src/Pim.Core/Today/TodayDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义「今日」聚合页的状态常量、链接关系常量、查询/区块/注册表 DTO，以及区块提供者契约 `ITodaySectionProvider`
- 主要依赖：无外部程序集引用（仅 BCL：`DateOnly`、`DateTimeOffset`、`IReadOnlyList`、`Task`、`CancellationToken`）
- 被谁使用：`TodaySectionService`、`TodaySectionProviders` 内各 Provider、`TodayEndpoints`、`Program` 中 DI 注册

## 函数级结构化伪代码

### TodaySectionStatuses
#### static class TodaySectionStatuses（常量集）
- 输入：无
- 输出：区块状态字符串常量
- 副作用：无
- 步骤：
  1. 提供 `Available` / `Normal` / `Empty` / `Warning` / `Critical` / `Unavailable` 六个状态字面量
- 分支与异常：无
- 调用：注册表项与各 Provider 填 `Status` 时使用

### TodayLinkRels
#### static class TodayLinkRels（常量集）
- 输入：无
- 输出：链接关系类型字符串常量
- 副作用：无
- 步骤：
  1. 提供 `Self` / `Details` / `Api` 三种 `rel` 字面量
- 分支与异常：无
- 调用：构造 `TodayLinkDto` 时使用

### TodayQuery
#### record TodayQuery(DateOnly Date, DateOnly PcBusinessDate)
- 输入：`Date` 今日日期；`PcBusinessDate` PC 业务日
- 输出：不可变查询上下文 DTO
- 副作用：无
- 步骤：
  1. 绑定两个日期字段
- 分支与异常：无
- 调用：由 `TodaySectionService.BuildQuery` 构造，传入各 Provider

### TodayLinkDto
#### record TodayLinkDto(string Rel, string Href)
- 输入：`Rel` 关系类型；`Href` 目标路径/URL
- 输出：超媒体链接 DTO
- 副作用：无
- 步骤：
  1. 绑定关系与地址
- 分支与异常：无
- 调用：注册表项与区块结果的 `Links` 列表

### TodaySectionErrorDto
#### record TodaySectionErrorDto(string Code, string Message)
- 输入：`Code` 错误码；`Message` 可读消息
- 输出：区块错误 DTO
- 副作用：无
- 步骤：
  1. 绑定错误码与消息
- 分支与异常：无
- 调用：Provider 失败或不可用时写入 `TodaySectionDto.Error`

### TodaySectionRegistryItemDto
#### record TodaySectionRegistryItemDto(...)
- 输入：`Id`、`Kind`、`Status`、`Links`
- 输出：注册表中单区块摘要项
- 副作用：无
- 步骤：
  1. 绑定区块标识、类型、状态与链接列表
- 分支与异常：无
- 调用：`GetRegistryAsync` 聚合时构造

### TodaySectionRegistryDto
#### record TodaySectionRegistryDto(...)
- 输入：`Date`、`PcBusinessDate`、`GeneratedAt`、`Sections`
- 输出：今日区块注册表整体 DTO
- 副作用：无
- 步骤：
  1. 绑定日期上下文、生成时间与区块项列表
- 分支与异常：无
- 调用：`TodayEndpoints` 返回注册表 API 响应

### TodaySectionDto
#### record TodaySectionDto(...)
- 输入：`Id`、`Kind`、`Status`、`GeneratedAt`、`Data`、`Links`、可选 `Error`
- 输出：单个今日区块完整载荷
- 副作用：无
- 步骤：
  1. 绑定区块元数据、动态 `Data` 对象、链接与可选错误
- 分支与异常：无
- 调用：各 Provider `BuildAsync` 返回；详情 API 直接输出

### ITodaySectionProvider
#### string SectionId { get }
- 输入：无
- 输出：区块唯一 Id
- 副作用：无
- 步骤：
  1. 返回本 Provider 负责的 section 标识
- 分支与异常：无
- 调用：服务按 Id 查找 Provider、构建注册表

#### string Kind { get }
- 输入：无
- 输出：区块类型/种类字符串
- 副作用：无
- 步骤：
  1. 返回区块 kind 分类
- 分支与异常：无
- 调用：注册表项与区块 DTO 填 `Kind`

#### Task<TodaySectionDto> BuildAsync(TodayQuery query, CancellationToken ct)
- 输入：`query` 今日查询上下文；`ct` 取消令牌
- 输出：构建完成的 `TodaySectionDto`
- 副作用：实现侧可能读库/调下游服务
- 步骤：
  1. 按 `query` 拉取本区块数据
  2. 计算 `Status` 与 `Links`
  3. 组装并返回 `TodaySectionDto`
- 分支与异常：契约不规定异常；实现可在失败时由上层捕获并映射为 `Unavailable`
- 调用：`TodaySectionService.GetSectionAsync` 等

## 近逐行中文伪代码

1. 声明命名空间 `Pim.Core.Today`
2. 定义静态类 `TodaySectionStatuses`
3.   - 常量 `Available` = `"available"`
4.   - 常量 `Normal` = `"normal"`
5.   - 常量 `Empty` = `"empty"`
6.   - 常量 `Warning` = `"warning"`
7.   - 常量 `Critical` = `"critical"`
8.   - 常量 `Unavailable` = `"unavailable"`
9. 定义静态类 `TodayLinkRels`
10.   - 常量 `Self` = `"self"`
11.   - 常量 `Details` = `"details"`
12.   - 常量 `Api` = `"api"`
13. 定义密封记录 `TodayQuery(Date, PcBusinessDate)`：两个 `DateOnly`
14. 定义密封记录 `TodayLinkDto(Rel, Href)`
15. 定义密封记录 `TodaySectionErrorDto(Code, Message)`
16. 定义密封记录 `TodaySectionRegistryItemDto(Id, Kind, Status, Links)`
17. 定义密封记录 `TodaySectionRegistryDto(Date, PcBusinessDate, GeneratedAt, Sections)`
18. 定义密封记录 `TodaySectionDto(Id, Kind, Status, GeneratedAt, Data, Links, Error?)`
19. 声明接口 `ITodaySectionProvider`
20.   - 只读属性 `SectionId`
21.   - 只读属性 `Kind`
22.   - 方法 `BuildAsync(query, ct)` 返回 `Task<TodaySectionDto>`
23. 文件结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Today/TodayDtos.cs",
      "label": "TodayDtos",
      "path": "src/Pim.Core/Today/TodayDtos.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Today/TodayDtos.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Today/TodaySectionService.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "implements" },
    { "from": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" }
  ]
}
```
