# src/Pim.Api/Endpoints/TodayEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：注册「今日」模块 HTTP 端点：分区列表与单分区详情；统一校验 `date` 查询参数格式。
- 主要依赖：`TodaySectionService`、`Pim.Core.Today` DTO、`ApiResponse`、`Pim.Api.Today`
- 被谁使用：API 启动时 `MapTodayEndpoints` 映射路由

## 函数级结构化伪代码

### TodayEndpointPaths
#### const Sections
- 输入：无
- 输出：路径常量 `"/api/v1/today/sections"`
- 副作用：无
- 步骤：声明路由前缀
- 分支与异常：无
- 调用：无

#### string Section(string sectionId)
- 输入：分区 ID
- 输出：`/api/v1/today/sections/{escapedSectionId}`
- 副作用：无
- 步骤：对 `sectionId` 做 `Uri.EscapeDataString` 后拼接
- 分支与异常：无
- 调用：`Uri.EscapeDataString`

### TodayEndpoints
#### void MapTodayEndpoints(this IEndpointRouteBuilder endpoints)
- 输入：路由构建器
- 输出：无
- 副作用：注册需授权的路由组与两个 GET
- 步骤：
  1. `MapGroup("/api/v1/today").RequireAuthorization()`
  2. `GET /sections`：可选 `date` → `today.GetRegistryAsync` → `ApiResponse<TodaySectionRegistryDto>.Ok`
  3. `GET /sections/{sectionId}`：`GetSectionAsync`；null 则 404「今日模块不存在。」否则 200
  4. 两处均经 `RunWithDateValidationAsync` 包装
- 分支与异常：日期 `FormatException` 由包装器转 400
- 调用：`TodaySectionService.GetRegistryAsync` / `GetSectionAsync`、`Results.Ok` / `NotFound`

#### IResult ToInvalidDateResult()
- 输入：无
- 输出：400 BadRequest，`ApiResponse` 错误文案要求 YYYY-MM-DD 或可解析日期
- 副作用：无
- 步骤：构造 `Results.BadRequest(...)`
- 分支与异常：无
- 调用：`ApiResponse<string>.Error`

#### Task\<IResult\> RunWithDateValidationAsync(Func\<Task\<IResult\>\> action)
- 输入：异步动作
- 输出：`IResult`
- 副作用：依赖 action
- 步骤：
  1. try 执行 `action()`
  2. catch `FormatException` → `ToInvalidDateResult()`
- 分支与异常：仅捕获格式异常
- 调用：`action`、`ToInvalidDateResult`

## 近逐行中文伪代码

1. 引入 `Pim.Api.Today`、`Pim.Core.Common`、`Pim.Core.Today`
2. 命名空间 `Pim.Api.Endpoints`
3. 静态类 `TodayEndpointPaths`：常量 `Sections`；`Section` 转义 sectionId 拼路径
4. 静态类 `TodayEndpoints`
5. `MapTodayEndpoints`：组路径 `/api/v1/today`，强制授权
6. GET `/sections`：注入 `date?`、`TodaySectionService`、`ct`；在日期校验包装内取注册表并 200
7. GET `/sections/{sectionId}`：取单分区；不存在 404 中文错误，存在 200
8. `ToInvalidDateResult`：返回 400 英文日期格式说明
9. `RunWithDateValidationAsync`：执行 action，捕获 `FormatException` 返回无效日期结果

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Endpoints/TodayEndpoints.cs",
      "label": "TodayEndpoints",
      "path": "src/Pim.Api/Endpoints/TodayEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Endpoints/TodayEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "to": "Pim.Api.Today.TodaySectionService", "type": "calls" },
    { "from": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "to": "src/Pim.Core/Today/TodayDtos.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/TodayEndpoints.cs", "to": "src/Pim.Core/Common", "type": "depends_on" }
  ]
}
```
