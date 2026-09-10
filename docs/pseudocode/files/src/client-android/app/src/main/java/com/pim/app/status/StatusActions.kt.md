# src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (app)
- 职责：状态中心动作执行与刷新信号——`StatusSyncActionRunner` 仅在 `TriggerSync` 时同步并刷新；`StatusRefreshSignal` 以版本号 StateFlow 广播刷新请求。
- 主要依赖：`StatusActionRoute`（同包 StatusIssue.kt）、kotlinx.coroutines.flow、javax.inject
- 被谁使用：状态中心 UI / ViewModel 注入 `StatusRefreshSignal`；动作路由后调用 runner

## 函数级结构化伪代码

### StatusSyncActionRunner
#### run(route: StatusActionRoute)
- 输入：`route` 状态动作路由
- 输出：无
- 副作用：可能触发 `syncNow` 与 `refresh`
- 步骤：
  1. 若 `route != TriggerSync` 则直接 return
  2. 挂起调用 `syncNow()`
  3. 调用 `refresh()`
- 分支与异常：非 TriggerSync 早退
- 调用：构造注入的 `syncNow`、`refresh` 闭包

### StatusRefreshSignal
#### requestRefresh()
- 输入：无
- 输出：无
- 副作用：`_version` 自增，订阅方收到新值
- 步骤：
  1. `_version.update { it + 1L }`
- 分支与异常：无
- 调用：`MutableStateFlow.update`

#### version: StateFlow<Long>（属性）
- 输入：无
- 输出：只读版本流
- 副作用：无
- 步骤：1. 暴露 `_version.asStateFlow()`
- 分支与异常：无
- 调用：`asStateFlow`

## 近逐行中文伪代码

1. [L1] package `com.pim.app.status`
2. [L10-13] 类 `StatusSyncActionRunner` 持有 `syncNow` 挂起闭包与 `refresh` 回调
3. [L14-18] `run`：仅 `TriggerSync` 时先 sync 再 refresh
4. [L21-24] `@Singleton` `StatusRefreshSignal`：内部 `MutableStateFlow(0L)`，对外 `version` StateFlow
5. [L26-28] `requestRefresh` 将 version +1

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt",
      "label": "StatusActions",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/status/StatusActions.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusIssue.kt", "type": "depends_on" }
  ]
}
```
