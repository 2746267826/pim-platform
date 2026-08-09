# src/Pim.Core/Common/PagedResult.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：通用分页结果 DTO，封装一页数据项与分页元数据。
- 主要依赖：无项目内 using（仅 `System` 集合类型）。
- 被谁使用：
  - `Pim.Infrastructure/Ai/AiUsageService`（AI 请求日志列表）
  - `Pim.Api/Endpoints/AiEndpoints`、`SearchEndpoints`
  - `Pim.Module.QuickNotes`、`Pim.Module.Calendar`、`Pim.Module.Files` 等列表 API
  - `Pim.Core/Ai/IAiUsageService` 接口签名
  - 前端 `client-web` 对应 TypeScript `PagedResult<T>`（结构镜像，非本类型直接引用）

## 函数级结构化伪代码

### PagedResult\<T\>
#### 记录主构造 `PagedResult(Items, Page, PageSize, TotalCount, TotalPages)`
- 输入：
  - `Items`：`IReadOnlyList<T>`，当前页元素只读列表
  - `Page`：`int`，当前页码（调用方约定，通常从 1 起）
  - `PageSize`：`int`，每页条数
  - `TotalCount`：`int`，符合条件的总记录数
  - `TotalPages`：`int`，总页数（由调用方计算后传入）
- 输出：不可变记录实例（位置参数 + 自动属性）
- 副作用：无
- 步骤：
  1. 以 record 主构造绑定五个字段，生成值相等语义与解构支持。
- 分支与异常：无运行时逻辑；空列表与 0 计数由调用方合法构造。
- 调用：无

## 近逐行中文伪代码

1. 命名空间设为 `Pim.Core.Common`。
2. 声明公开泛型记录 `PagedResult<T>`，主构造参数依次为：
3. `Items`：当前页只读列表。
4. `Page`：当前页码。
5. `PageSize`：页大小。
6. `TotalCount`：总条数。
7. `TotalPages`：总页数。
8. 记录体结束；无额外成员或方法。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Common/PagedResult.cs",
      "label": "PagedResult<T>",
      "path": "src/Pim.Core/Common/PagedResult.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Common/PagedResult.cs.md",
      "layer": "core",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Ai/AiUsageService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/Pim.Core/Ai/IAiUsageService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Endpoints/AiEndpoints.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Search/SearchEndpoints.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/CalendarRecycleBinService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/Pim.Core/Common/PagedResult.cs", "type": "depends_on" }
  ]
}
```
