# src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.ui.settings（client-android app）
- 职责：设置页 ViewModel：API 地址、连接探测、登录/退出、持续采集开关与权限门禁。
- 主要依赖：`ServerSettingsStore`、`TokenManager`、`ServerBoundLoginCoordinator`、`TrackingSettingsStore`、`ForegroundLocationController`、`PermissionStatusRepository`、`ConnectionProbeService`、`ConnectionProbeStore`、`ServerUrlValidator`、`PimServerEndpoints`
- 被谁使用：设置 UI 界面

## 函数级结构化伪代码

### SettingsUiState
#### 数据类字段
- 输入：无
- 输出：UI 状态快照
- 副作用：无
- 步骤：持有 apiAddress/warnings/error/status、登录态、持续采集开关与文案、isBusy
- 分支与异常：无
- 调用：无

### SettingsViewModel
#### init / refresh()
- 输入：无
- 输出：Unit（更新 state）
- 副作用：读持久化配置并可能跑探测
- 步骤：
  1. 读 baseUrl 并校验
  2. 更新地址/警告/错误/登录/采集开关
  3. `runConnectionProbe(force=false)`
- 分支与异常：协程内执行
- 调用：`ServerUrlValidator`、`hasCurrentServerSession`、`persistedCollectionEnabled`

#### updateApiAddress(value)
- 输入：用户输入地址
- 输出：Unit
- 副作用：仅更新内存 state
- 步骤：校验 → 写 apiAddress/warnings/error，清空 apiStatus
- 分支与异常：无效时写 reasonCode
- 调用：`ServerUrlValidator.validate`

#### saveApiAddress(): Boolean
- 输入：当前 state.apiAddress
- 输出：是否保存成功
- 副作用：写 `ServerSettingsStore`
- 步骤：
  1. 无效 → 错误文案，false
  2. `setBaseUrl(normalized)`，失败则 reload 并 false
  3. 成功 reload 并 true
- 分支与异常：`runCatching` 捕获写盘失败
- 调用：`reloadPersistedServerState`

#### testConnection()
- 输入：无
- 输出：Unit
- 副作用：保存地址后强制探测
- 步骤：校验 → save → isBusy + 探测文案 → `runConnectionProbe(force=true, finishBusy=true)`
- 分支与异常：无效地址直接返回
- 调用：`saveApiAddress`、`runConnectionProbe`

#### login(username, password)
- 输入：用户名密码
- 输出：Unit
- 副作用：登录与 token 持久化
- 步骤：
  1. 空账号密码 / 无效 URL / 保存失败则提示返回
  2. 协程：`serverBoundLoginCoordinator.login`
  3. Success / StaleServer / SessionSaveFailed / Failure 分支
  4. 成功更新登录态；失败写 loginStatus；重抛 CancellationException
- 分支与异常：fold onSuccess/onFailure
- 调用：`hasCurrentServerSession`、`persistedCollectionEnabled`

#### logout()
- 输入：无
- 输出：Unit
- 副作用：清 token；采集意图保留
- 步骤：`tokenManager.clear` 失败则提示；成功 isLoggedIn=false，采集设置不变
- 分支与异常：clear 失败
- 调用：`persistedCollectionEnabled`

#### setContinuousCollectionEnabled(enabled)
- 输入：开关
- 输出：Unit
- 副作用：写 tracking 设置、启停前台定位
- 步骤：
  1. 关闭：写 false、stop、文案
  2. 开启：校验 URL → 保存 baseUrl → 需会话 → 检查权限 → 写 true 并 start
  3. start 失败则回滚开关
- 分支与异常：URL/会话/权限门禁；`runCatching` 启动失败
- 调用：`missingCollectionPermissions`、`keepCollectionOff`、`showCollectionBlocked`

#### missingCollectionPermissions(): List\<String\>
- 输入：无
- 输出：缺失权限中文标签列表
- 副作用：读权限快照
- 步骤：检查通知、精确定位、后台定位、活动识别
- 分支与异常：无
- 调用：`permissionStatusRepository.snapshot`

#### keepCollectionOff / showCollectionBlocked
- 输入：消息与可选 extra state
- 输出：Unit
- 副作用：关采集或仅提示
- 步骤：`keepCollectionOff` 写 false+stop；`showCollectionBlocked` 保持持久化意图并写文案
- 分支与异常：无
- 调用：tracking store / controller

#### refreshConnectionForVisibleScreen(): Long
- 输入：无
- 输出：下次刷新间隔毫秒
- 副作用：可能探测
- 步骤：探测成功 → `millisUntilRefresh`；失败 → 30s
- 分支与异常：无
- 调用：`runConnectionProbe`、`millisUntilRefresh`

#### runConnectionProbe(force, finishBusyState): Boolean
- 输入：是否强制、是否结束 busy
- 输出：是否探测成功
- 副作用：probe 与 store 缓存
- 步骤：
  1. 记录 targetUrl
  2. force：直接 probe 并在 URL 未变时 save
  3. 非 force：先读 fresh 缓存，否则 probe+save
  4. 成功/失败按当前 URL 是否仍匹配更新 apiStatus 与 isBusy
- 分支与异常：`runCatching`；URL 变更时丢弃结果
- 调用：`connectionProbeService.probe`、`connectionProbeStore`、`PimServerEndpoints.from`

#### millisUntilRefresh / persistedCollectionEnabled / reloadPersistedServerState / hasCurrentServerSession
- 输入：见签名
- 输出：剩余新鲜度 / 布尔 / Unit / 布尔
- 副作用：读 store 与 token
- 步骤：按 serverIdentity 与 FRESHNESS 算剩余；读 continuous 标志；reload 地址与登录；检查 access token
- 分支与异常：解析 endpoints 失败回退
- 调用：`PimServerEndpoints`、`TokenManager`、`TrackingSettingsStore`

### ConnectionProbeResult.statusMessage（扩展）
#### statusMessage(): String
- 输入：outcome
- 输出：中文状态文案
- 副作用：无
- 步骤：Reachable/Partial/Blocked 映射文案，Partial/Blocked 优先 safeMessage
- 分支与异常：when
- 调用：无

## 近逐行中文伪代码

1. [L1-25] 包、ViewModel/Hilt/协程与依赖导入
2. [L26-36] `SettingsUiState` 字段
3. [L38-50] `SettingsViewModel` 注入与 `_state`/`state`
4. [L52-71] init→refresh：加载地址、校验、登录与采集，再探测
5. [L73-112] 更新/保存 API 地址
6. [L114-130] 测试连接（强制探测）
7. [L132-180] 登录流程与结果分支
8. [L182-201] 退出登录
9. [L203-270] 持续采集开关与权限/会话门禁
10. [L272-305] 权限列表与关采集/阻塞提示
11. [L307-382] 可见屏刷新与连接探测缓存逻辑
12. [L384-410] 持久化重载与会话判断
13. [L413-423] PROBE_RETRY_MILLIS 与 statusMessage 扩展

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt",
      "label": "SettingsViewModel",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/app/src/main/java/com/pim/app/permissions/PermissionStatusRepository.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt", "type": "calls" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt", "type": "depends_on" }
  ]
}
```
