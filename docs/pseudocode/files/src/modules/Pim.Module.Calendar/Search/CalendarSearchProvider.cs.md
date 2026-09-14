# src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日历模块全局搜索提供者：按当前用户在事件与任务标题/描述中做不区分大小写模糊匹配，返回统一 `SearchResult`。
- 主要依赖：`IServiceScopeFactory`、`PimDbContext`、`ICurrentUserService`、`EventEntity`、`TaskEntity`、`ISearchProvider`
- 被谁使用：`CalendarModule` 注册为 `ISearchProvider`；`SearchEndpoints` 聚合调用

## 函数级结构化伪代码

### CalendarSearchProvider
#### string ModuleName { get }
- 输入：无
- 输出：固定 `"calendar"`
- 副作用：无
- 步骤：
  1. 返回模块名常量
- 分支与异常：无
- 调用：无

#### CalendarSearchProvider(IServiceScopeFactory scopeFactory)
- 输入：作用域工厂（避免长生命周期持有 Scoped DbContext）
- 输出：构造完成的实例
- 副作用：保存 `_scopeFactory`
- 步骤：
  1. 赋值 `_scopeFactory = scopeFactory`
- 分支与异常：无
- 调用：无

#### Task<IReadOnlyList<SearchResult>> SearchAsync(string query, int limit, CancellationToken ct)
- 输入：`query` 关键词；`limit` 最大条数；`ct` 取消令牌
- 输出：事件与任务的 `SearchResult` 列表（合计不超过 `limit`）
- 副作用：创建 DI 作用域；只读查询数据库
- 步骤：
  1. `CreateScope` 解析 `PimDbContext` 与 `ICurrentUserService`
  2. 若 `UserId` 为 null → 返回空数组
  3. 查询 `EventEntity`：日历归属当前用户，且标题或描述 `ILike %query%`
  4. 标题匹配优先排序，取 `limit` 条，投影 Id/Title/Description
  5. 映射为 `SearchResult(module=calendar, type=event, url=/calendar/event/{id})`，snippet 截断 200
  6. 若仍有剩余配额，同样模式查询 `TaskEntity`（`UserId` 过滤），type=task，url=/calendar/task/{id}
  7. 返回合并列表
- 分支与异常：未登录空结果；描述为 null 时仅用标题作 snippet 回退；取消令牌可中断 EF 查询
- 调用：`IServiceScopeFactory.CreateScope`、`GetRequiredService`、`EF.Functions.ILike`、`Truncate`

#### string Truncate(string value, int maxLength) [private static]
- 输入：字符串与最大长度
- 输出：原串或截断后加 `"..."` 的串
- 副作用：无
- 步骤：
  1. 长度 ≤ maxLength → 原样返回
  2. 否则取前 `maxLength-3` 字符并追加 `...`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 EF Core、DI、`Pim.Core.Modules`、`ICurrentUserService`、`PimDbContext`、日历实体
2. 命名空间 `Pim.Module.Calendar.Search`
3. 类 `CalendarSearchProvider` 实现 `ISearchProvider`
4. `ModuleName` 返回 `"calendar"`；字段保存 `IServiceScopeFactory`
5. `SearchAsync`：建 scope，取 db 与 currentUser
6. `userId` 为空则返回空数组
7. 查事件：日历用户匹配 + 标题/描述 ILike，标题命中优先，Take(limit)
8. 映射为 event 型 SearchResult，描述截断 200
9. 剩余额度 > 0 时查任务：用户匹配 + 标题/描述 ILike，映射 task 型结果
10. 返回 results
11. 私有 `Truncate`：超长则截断并加省略号

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs",
      "label": "CalendarSearchProvider",
      "path": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "to": "src/Pim.Core/Modules/ISearchProvider.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/modules/Pim.Module.Calendar/Search/CalendarSearchProvider.cs", "type": "depends_on" }
  ]
}
```
