# src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt

## 元信息
- 语言：Kotlin/Compose
- 程序集或包：client-android (app)
- 职责：设置页 UI：API 地址保存/测连、登录登出、持续采集开关，以及省电档与权限说明；可见时循环刷新连接状态。
- 主要依赖：`SettingsViewModel`、`PimSection`、Compose Material3、Hilt `hiltViewModel`、lifecycle `collectAsStateWithLifecycle`
- 被谁使用：导航到 Settings 目的地时组合

## 函数级结构化伪代码

### SettingsScreen
#### SettingsScreen(modifier, viewModel)
- 输入：`modifier`；`viewModel` 默认 hiltViewModel
- 输出：Composable UI
- 副作用：周期调用 `refreshConnectionForVisibleScreen`；用户操作触发 ViewModel 登录/保存/采集开关
- 步骤：
  1. 收集 `viewModel.state`
  2. `rememberSaveable` 本地 `username`/`password`
  3. `LaunchedEffect(viewModel)`：while isActive → `refreshConnectionForVisibleScreen()`，delay 或 yield
  4. 纵向可滚动 Column 渲染各 `PimSection`
  5. API 地址：TextField、警告/错误/状态文案、保存/测试连接按钮
  6. 账号：登录状态、用户名密码、登录（busy 禁用）/退出
  7. 持续采集：Switch + 状态文案
  8. 省电档与权限：只读说明文本
- 分支与异常：`apiWarnings` 含 real-device-localhost 时显示 tertiary 提示；`apiError`/`apiStatus`/`loginStatus`/`collectionStatus` 可选展示
- 调用：`viewModel.updateApiAddress/saveApiAddress/testConnection/login/logout/setContinuousCollectionEnabled`、`refreshConnectionForVisibleScreen`

## 近逐行中文伪代码

1. [L34-38] Composable `SettingsScreen`，默认 Hilt ViewModel
2. [L39-41] 收集 state；可保存用户名密码本地状态
3. [L43-48] 可见屏循环刷新连接，按返回毫秒 delay
4. [L50-56] 全屏滚动 Column，间距 12.dp
5. [L57-85] 标题「设置」+ API 地址区：输入、真机 localhost 警告、错误/状态、保存与测连
6. [L86-115] 账号区：登录态、用户名/密码（密码遮罩）、登录/退出
7. [L116-124] 持续采集 Switch 与说明
8. [L125-135] 省电档参数说明与权限说明（只读）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt",
      "label": "SettingsScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsViewModel.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt", "type": "depends_on" }
  ]
}
```
