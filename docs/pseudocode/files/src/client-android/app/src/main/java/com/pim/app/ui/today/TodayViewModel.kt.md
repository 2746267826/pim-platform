# src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui.today
- 职责：Today 页 ViewModel：将 StatusCenter 快照映射为 API/采集状态文案 StateFlow。
- 主要依赖：StatusCenterRepository、StatusCenterState、Hilt
- 被谁使用：TodayScreen

## 函数级结构化伪代码

### TodayUiState
#### data class
- 输入：默认「API：待连接」「持续采集：未开启」
- 输出：UI 状态数据
- 副作用：无
- 步骤：持有 apiStatusLabel、collectionStatusLabel
- 调用：无

### TodayStatusMapper
#### fromStatus(state: StatusCenterState): TodayUiState
- 输入：状态中心状态
- 输出：TodayUiState
- 副作用：无
- 步骤：
  1. api：无效地址→待连接；无 accessToken→待登录；过期→登录过期；否则已连接
  2. collection：continuousCollectionEnabled 真→已开启，否则未开启
- 分支与异常：when 链式条件
- 调用：无

### TodayViewModel
#### constructor(statusCenterRepository)
- 输入：StatusCenterRepository（Hilt 注入）
- 输出：ViewModel
- 副作用：订阅 observe 流
- 步骤：
  1. observe → map(fromStatus) → stateIn(WhileSubscribed 5s, 初始 TodayUiState())
- 分支与异常：无
- 调用：statusCenterRepository.observe、TodayStatusMapper.fromStatus

## 近逐行中文伪代码

1. [L14-17] 定义 TodayUiState 默认文案。
2. [L19-38] TodayStatusMapper 从 snapshot.api/auth/service 生成标签。
3. [L40-50] HiltViewModel 将仓库 Flow 映射为 state。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt",
      "label": "TodayViewModel",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/StatusCenterRepository.kt",
      "type": "depends_on"
    }
  ]
}
```
