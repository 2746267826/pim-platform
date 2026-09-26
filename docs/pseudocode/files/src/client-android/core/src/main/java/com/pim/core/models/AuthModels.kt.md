# src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (core)
- 职责：核心 DTO：通用 `ApiResponse`、认证登录/注册/刷新、日历/事件/任务/搜索/ICS 导入请求响应模型（kotlinx.serialization）。
- 主要依赖：`kotlinx.serialization`（`@Serializable`、`@SerialName`、`@JsonNames`）
- 被谁使用：`ApiService`、登录协调器、日程映射、各 Android 网络层

## 函数级结构化伪代码

### 数据类（无行为方法，结构声明）
#### ApiResponse\<T\>
- 输入字段：`code`、`message`、`data?`、`timestamp`
- 输出：序列化信封
- 副作用：无
- 步骤：1. 统一 API 包装

#### AuthResponse / UserInfo / LoginRequest / RegisterRequest / RefreshRequest
- 步骤：`AuthResponse` 的 `userInfo` 兼容 JSON 名 `user` 与 `userInfo`；令牌与过期时间字符串

#### CalendarResponse / CreateCalendarRequest / EventResponse / CreateEventRequest
- 步骤：日历与事件 CRUD DTO；事件含 location/dtStart/dtEnd

#### TaskResponse / CreateTaskRequest / SearchResult / IcsImportResponse
- 步骤：任务优先级/状态；搜索 snippet/url；ICS 导入 data 为 Int 计数

## 近逐行中文伪代码

1. [L1-7] package 与 serialization 导入
2. [L8-14] `ApiResponse<T>` 通用信封
3. [L16-25] `AuthResponse`：access/refresh/expiresAt；user 字段别名
4. [L27-39] `UserInfo`、`LoginRequest`
5. [L41-52] `RegisterRequest`、`RefreshRequest`
6. [L54-69] 日历响应/创建请求
7. [L71-92] 事件响应/创建请求
8. [L94-112] 任务响应/创建请求
9. [L114-131] 搜索结果、ICS 导入响应

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt",
      "label": "AuthModels",
      "path": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
