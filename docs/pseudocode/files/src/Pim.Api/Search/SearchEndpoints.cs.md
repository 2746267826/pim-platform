# src/Pim.Api/Search/SearchEndpoints.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：统一搜索入口 `/api/v1/search`，聚合全部 `ISearchProvider` 结果，支持类型过滤与标题命中优先排序。
- 主要依赖：`ISearchProvider`、`SearchResult`、`PagedResult`、`ApiResponse`
- 被谁使用：API 启动时 `MapSearchEndpoints` 映射

## 函数级结构化伪代码

### SearchEndpoints
#### void MapSearchEndpoints(this IEndpointRouteBuilder endpoints)
- 输入：路由构建器
- 输出：无
- 副作用：注册 GET `/api/v1/search/`（需授权）
- 步骤：
  1. 建组 `/api/v1/search` 并 `RequireAuthorization`
  2. 处理查询参数 `q`、`type`、`limit` 与注入的 `IEnumerable<ISearchProvider>`
  3. 若 `q` 空白：返回空 `PagedResult`（page=1, pageSize=20, total=0）
  4. `maxLimit = Min(limit ?? 20, 100)`
  5. 将 `type` 按逗号拆分、trim、小写为 HashSet（可空）
  6. 对每个 provider 调用 `SearchAsync(q, maxLimit, ct)` 并 `Task.WhenAll`
  7. `SelectMany` 合并；若 typeFilter 非空则按 `r.Type` 过滤
  8. 按标题是否包含 `q`（忽略大小写）降序排序
  9. `Take(maxLimit)`；构造 `PagedResult`（page 固定 1，totalPages 按 total/maxLimit 上取整）
  10. `ApiResponse.Ok` 返回
- 分支与异常：空查询短路；type 过滤可选
- 调用：各 `ISearchProvider.SearchAsync`

## 近逐行中文伪代码

1. 引入 `Pim.Core.Common`、`Pim.Core.Modules`
2. 命名空间 `Pim.Api.Search`，静态类 `SearchEndpoints`
3. `MapSearchEndpoints`：组 `/api/v1/search` + 授权
4. GET `/`：参数 q/type/limit、providers、ct
5. q 空白 → 空分页结果 Ok
6. maxLimit 上限 100，默认 20
7. type 拆成小写集合
8. 并行调用所有 SearchProvider
9. 合并结果；可选 type 过滤
10. 标题包含查询词的排前
11. 截断 maxLimit，计算 totalCount/totalPages
12. 包装 `PagedResult<SearchResult>` 返回 Ok

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/Search/SearchEndpoints.cs",
      "label": "SearchEndpoints",
      "path": "src/Pim.Api/Search/SearchEndpoints.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/Search/SearchEndpoints.cs.md",
      "layer": "api",
      "kind": "endpoint"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/Search/SearchEndpoints.cs", "to": "src/Pim.Core/Modules", "type": "depends_on" },
    { "from": "src/Pim.Api/Search/SearchEndpoints.cs", "to": "src/Pim.Core/Common", "type": "depends_on" },
    { "from": "src/Pim.Api/Search/SearchEndpoints.cs", "to": "ISearchProvider", "type": "calls" }
  ]
}
```
