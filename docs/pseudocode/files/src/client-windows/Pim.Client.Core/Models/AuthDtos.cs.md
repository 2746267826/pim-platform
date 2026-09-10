# src/client-windows/Pim.Client.Core/Models/AuthDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：Windows 客户端 JSON 反序列化模型——通用 `ApiResponse<T>`、认证令牌与用户信息、日历/事件/任务响应、搜索结果、分页结果。
- 主要依赖：`System.Text.Json.Serialization`（JsonPropertyName）
- 被谁使用：`AuthService`、`ApiClient` 调用方、日历/任务/搜索相关客户端服务

## 函数级结构化伪代码

### ApiResponse\<T\>
#### 属性 Code / Message / Data
- 输入：JSON code/message/data
- 输出：包装响应
- 副作用：无
- 步骤：Data 可空
- 分支与异常：无
- 调用：无

### AuthResponse
#### 属性 AccessToken / RefreshToken / ExpiresAt / UserInfo
- 输入：登录/注册/刷新 JSON
- 输出：令牌与用户
- 副作用：无
- 步骤：UserInfo 可空
- 分支与异常：无
- 调用：无

### UserInfo
#### 属性 Id / Username / DisplayName / Role
- 输入：用户 JSON（Id 为 string）
- 输出：用户摘要
- 副作用：无
- 步骤：字符串默认 Empty
- 分支与异常：无
- 调用：无

### CalendarResponse / EventResponse / TaskResponse
#### 日历/事件/任务字段映射
- 输入：API camelCase JSON
- 输出：强类型属性（含 isDefault/eventCount、dtStart/dtEnd/rrule、priority/isInbox 等）
- 副作用：无
- 步骤：可选字段用可空类型
- 分支与异常：无
- 调用：无

### SearchResult
#### 属性 ModuleName / Type / Id / Title / Snippet / Url
- 输入：跨模块搜索 JSON
- 输出：搜索命中项
- 副作用：无
- 步骤：无
- 分支与异常：无
- 调用：无

### PagedResult\<T\>
#### 属性 Items / Page / PageSize / TotalCount / TotalPages
- 输入：分页 JSON
- 输出：泛型分页容器
- 副作用：无
- 步骤：Items 默认 new List
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 System.Text.Json.Serialization
2. 命名空间 `Pim.Client.Core.Models`
3. `ApiResponse<T>`：code/message/data
4. `AuthResponse`：accessToken/refreshToken/expiresAt/user
5. `UserInfo`：id/username/displayName/role（均为 string）
6. `CalendarResponse`：id/name/color/isDefault/eventCount
7. `EventResponse`：日历事件字段含 dtStart/dtEnd/rrule/status/source
8. `TaskResponse`：任务字段含 priority/estimatedDuration/due/isInbox/sortOrder
9. `SearchResult`：moduleName/type/id/title/snippet/url
10. `PagedResult<T>`：items 列表与分页元数据

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Models/AuthDtos.cs",
      "label": "AuthDtos",
      "path": "src/client-windows/Pim.Client.Core/Models/AuthDtos.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Models/AuthDtos.cs.md",
      "layer": "client-windows",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "to": "src/client-windows/Pim.Client.Core/Models/AuthDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Models/AuthDtos.cs", "type": "depends_on" }
  ]
}
```
