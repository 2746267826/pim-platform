# src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui.status
- 职责：状态中心 ViewModel：暴露 StatusCenterState、问题动作、立即同步、连接探测刷新。
- 主要依赖：StatusCenterRepository、MobileSyncScheduler、ServerSettingsStore、ConnectionProbeService/Store、PimServerEndpoints
- 被谁使用：StatusCenterScreen

## 函数级结构化伪代码

### StatusCenterViewModel
#### 构造 / state
- repository.observe() → stateIn(WhileSubscribed 5s, empty)
- init 调用 refresh()

#### onIssueAction(issue)
- 输入：StatusIssue
- 输出：StatusActionTarget
- 步骤：requestRefresh；返回 issue.target

#### syncNow()
- 协程：mobileSyncScheduler.enqueueNow + requestRefresh

#### refresh()
- requestRefresh；协程 refreshConnectionForVisibleScreen

#### refreshConnectionForVisibleScreen()
- 输入：无
- 输出：下次可探测的建议等待毫秒（0 或剩余 freshness 或 PROBE_RETRY_MILLIS=30s）
- 步骤：
  1. 读 baseUrl；解析 serverIdentity（apiBaseUrl 字符串）
  2. 若有 identity 且 store 有 fresh 结果则用缓存
  3. 否则 probe(serverUrl)，若 URL 未变则 save
  4. 失败 → PROBE_RETRY_MILLIS
  5. 成功后对照 store 当前结果 identity/age，返回 freshness 剩余时间
- 调用：ServerSettingsStore、PimServerEndpoints、ConnectionProbeStore、ConnectionProbeService、repository.requestRefresh

## 近逐行中文伪代码

1. HiltViewModel 注入仓库、同步调度器、设置、探测服务与缓存。
2. state 来自 repository.observe 的 StateFlow。
3. 初始化即 refresh。
4. 问题动作：刷新快照并返回导航 target。
5. syncNow：enqueueNow 后刷新。
6. refresh：先 requestRefresh，再协程探测连接。
7. 解析服务器 identity；有新鲜缓存则跳过 probe。
8. 否则 probe；仅当 baseUrl 未变才写入 store。
9. 失败返回 30s 重试间隔；成功按 freshness 计算剩余等待。
10. 全程 requestRefresh 驱动 UI 重算 issue。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "label": "StatusCenterViewModel",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterViewModel.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "type": "depends_on"
    }
  ]
}
```
