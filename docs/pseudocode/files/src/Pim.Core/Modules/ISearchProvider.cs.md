# src/Pim.Core/Modules/ISearchProvider.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义跨模块全局搜索提供者契约及统一搜索结果 DTO，供各业务模块注册并被 API 聚合查询。
- 主要依赖：无（仅 BCL 与本文件内 `SearchResult`）
- 被谁使用：`Pim.Module.Calendar.Search.CalendarSearchProvider` 实现并在 `CalendarModule` 注册；`Pim.Api.Search.SearchEndpoints` 注入 `IEnumerable<ISearchProvider>` 聚合调用

## 函数级结构化伪代码

### ISearchProvider
#### string ModuleName { get }
- 输入：无
- 输出：模块名称字符串（标识结果来源模块）
- 副作用：无
- 步骤：
  1. 由实现返回固定或配置的模块名
- 分支与异常：无
- 调用：聚合搜索时可按模块过滤或标注

#### Task<IReadOnlyList<SearchResult>> SearchAsync(string query, int limit, CancellationToken ct)
- 输入：`query` 查询文本；`limit` 最大返回条数；`ct` 取消令牌
- 输出：只读 `SearchResult` 列表
- 副作用：无（契约层）；实现侧通常只读查询存储
- 步骤：
  1. 在本模块数据范围内按 `query` 检索
  2. 截断至 `limit` 条
  3. 映射为 `SearchResult` 并返回
- 分支与异常：空查询/无命中返回空列表；实现可因取消或存储失败抛出
- 调用：被 `SearchEndpoints` 对每个已注册提供者并行或顺序调用

### SearchResult
#### record SearchResult(string ModuleName, string Type, string Id, string Title, string Snippet, string Url)
- 输入：模块名、结果类型、实体 Id、标题、摘要片段、跳转 URL
- 输出：不可变搜索结果 DTO
- 副作用：无
- 步骤：
  1. 以位置参数绑定全部字段为只读属性
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间：`Pim.Core.Modules`
2. 声明公开接口 `ISearchProvider`
3. 只读属性 `ModuleName`：返回 string，标识模块
4. 方法 `SearchAsync`：
5.   - 参数 `query`（string）、`limit`（int）、`ct`（CancellationToken）
6.   - 返回 `Task<IReadOnlyList<SearchResult>>`
7. 声明公开记录 `SearchResult`，位置参数依次为：
8.   - `ModuleName`：来源模块名
9.   - `Type`：结果类型（如事件、文件等）
10.   - `Id`：实体标识
11.   - `Title`：展示标题
12.   - `Snippet`：匹配摘要
13.   - `Url`：前端或 API 跳转路径
14. 接口无默认实现；`SearchResult` 为数据载体无行为

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Modules/ISearchProvider.cs",
      "label": "ISearchProvider",
      "path": "src/Pim.Core/Modules/ISearchProvider.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Modules/ISearchProvider.cs.md",
      "layer": "core",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "to": "src/Pim.Core/Modules/ISearchProvider.cs", "type": "implements" },
    { "from": "src/Pim.Api/Search/SearchEndpoints.cs", "to": "src/Pim.Core/Modules/ISearchProvider.cs", "type": "calls" }
  ]
}
```
