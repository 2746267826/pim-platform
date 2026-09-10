# src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui
- 职责：Android 主壳 Compose UI（状态/使用/定位/设置四 Tab）及 `LocationCaptureViewModel`、`MobileStatusViewModel`；展示同步队列、登录与手动定位。
- 主要依赖：`LocationCaptureRepository`、`MobileSyncCoordinator`/`Scheduler`、`TokenManager`、`ServerBoundLoginCoordinator`、`ServerSettingsStore`、`AppDatabase`/`MobileDataDao`、`StructuredLogRepository`、`LocationSubmissionPolicy`
- 被谁使用：MainActivity / 壳层入口

## 函数级结构化伪代码

### PimAppScaffold
- 输入：可选两个 Hilt ViewModel
- 输出：Material3 Scaffold + Tab 内容
- 副作用：权限请求启动器回调 `startCapture`
- 步骤：
  1. 收集 location/status 状态流
  2. Tab：Status / Usage / Location / Settings
  3. Location 授权 launcher 成功后 startCapture

### StatusTab / UsageTab / LocationTab / SettingsTab
- 状态：版本、服务器、登录、权限、同步阶段/窗口/批次/队列/日志
- 使用：打开 Usage Access 设置与刷新探测
- 定位：精度策略文案、快照行、授权/开始/停止/手动提交
- 设置：保存 URL、登录/清除登录

### Section / StatusRow
- 通用卡片分区与标签-值行

### LocationCaptureViewModel
- 代理 repository 的 start/stop/submit；`onCleared` 停采集

### MobileUiState / MobileLogLine / PendingQueueCounts
- UI 状态与待传计数聚合（uploadable 四类之和）

### MobileStatusViewModel
#### init
- `refresh`；订阅 sync state；combine 多类 pending Flow 更新队列

#### refresh / saveServerUrl / syncNow / login / clearLogin
- 刷新：持久 sync + 队列 + 日志 + 登录态
- 保存 URL：校验 → setBaseUrl → 重载状态；失败回滚文案
- syncNow：`startSync` → scheduler.enqueueNow
- login：校验用户名密码与 URL → coordinator.login 分支 Success/Stale/SessionSaveFailed/Failure → 成功则 startSync
- clearLogin：tokenManager.clear

#### 私有
- `hasCurrentServerSession`：当前 baseUrl 下 access token 非空
- `pendingQueueCounts`：suspend first() 各 Flow
- `copyFromSync`：同步字段映射
- `toLine` / `appVersionDisplay` / packageInfo / versionCode

### 工具函数
- `phaseLabel` / `localizedProgress`：中英同步阶段文案
- `windowProgress` / `currentWindowLabel` / `batchLabel` / `logDisplayMessage`
- `hasUsageAccess` / `hasFineLocationPermission`
- `formatDuration` / `formatTime`

## 近逐行中文伪代码

1. 顶栏「PIM Android」+ TabRow 四页。
2. Status：运行态、同步传输指标、本机最近日志。
3. Usage：探测 usage stats 非空即授权；跳转系统设置。
4. Location：`LocationSubmissionPolicy.decide` 控制手动提交可用性与原因。
5. Settings：URL 校验保存；登录走 ServerBound；清除令牌。
6. MobileStatusViewModel 合并同步状态与 Room pending 计数。
7. 登录成功自动 enqueue 同步；失败写 structured log。
8. 权限/时长/时间格式本地化显示。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "label": "PimAppScaffold",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationCaptureRepository.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/LocationSubmissionPolicy.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncCoordinator.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync/MobileSyncScheduler.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/ServerBoundLoginCoordinator.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/PimAppScaffold.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt",
      "type": "depends_on"
    }
  ]
}
```
