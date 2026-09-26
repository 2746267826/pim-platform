# src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.network
- 职责：Retrofit API 接口定义：认证、日历/任务、搜索、ICS、Stats、Mobile 同步/定位分析、Daemon 心跳、Endpoint 通知动作。
- 主要依赖：`com.pim.core.models.*`、Retrofit 注解、`RequestBody`
- 被谁使用：`ApiClientProvider` 生成实现；各 Repository/Coordinator 调用

## 函数级结构化伪代码

### Auth
- `login` / `register` / `refresh`（refresh 返回 `Response<ApiResponse<AuthResponse>>`）

### Calendar / Events / Tasks
- CRUD：calendars、events（时间窗）、tasks（inbox 可选）、delete 返回消息

### Search / ICS / Outlook
- `search(q,type)`、`importIcs`、`exportIcs`、`syncOutlook`

### Stats
- `uploadStats(UploadBatch)`

### Mobile
- 设备注册、gaps、usage 上传、location 点上传
- summary / timeline / quality
- location history + analytics overview/tracks/segment points（分页 cursor）

### Daemon / Endpoint
- `sendHeartbeat`、`sendEndpointNotificationAction(deviceId, body)`

## 近逐行中文伪代码

1. 声明 Retrofit `interface ApiService`。
2. 路径相对 api base（如 `/api/v1` 由客户端拼接）。
3. Auth 三段：login/register/refresh。
4. 日历域：日历列表与创建；事件按 start/end；任务 inbox 过滤。
5. 搜索、ICS 导入导出、Outlook 同步。
6. Stats 批上传。
7. Mobile 采集闭环：register → gaps → usage/location 上传 → 查询摘要/时间线/质量/历史与分析。
8. Daemon 心跳与端点通知动作上报。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "label": "ApiService",
      "path": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/DaemonModels.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt",
      "type": "depends_on"
    }
  ]
}
```
