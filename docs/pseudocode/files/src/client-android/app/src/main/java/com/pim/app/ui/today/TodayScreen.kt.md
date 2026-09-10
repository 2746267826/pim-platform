# src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui.today
- 职责：Compose「今日概览」屏：展示采集/API 状态 chip 与轨迹、指标、使用、策略占位区块。
- 主要依赖：`TodayViewModel`、`PimSection`、Material3、Hilt navigation compose
- 被谁使用：根导航 Today 目的地

## 函数级结构化伪代码

### TodayScreen
#### @Composable fun TodayScreen(modifier, viewModel = hiltViewModel())
- 输入：可选 Modifier；默认 Hilt `TodayViewModel`
- 输出：UI（Unit）
- 副作用：订阅 `viewModel.state` 生命周期状态
- 步骤：
  1. `collectAsStateWithLifecycle` 取得 state
  2. 可滚动 Column：标题「今日概览」
  3. 两个 `AssistChip` 显示 `collectionStatusLabel` / `apiStatusLabel`（onClick 空）
  4. `PimSection` 四块：今日轨迹、位置指标、手机使用、当前策略（多为占位文案）
- 分支与异常：无业务分支
- 调用：`hiltViewModel`、`collectAsStateWithLifecycle`、`PimSection`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.ui.today`
2. [L22-26] 声明 `TodayScreen` Composable，默认 Hilt ViewModel
3. [L27] 收集 state
4. [L28-34] 全屏可滚动 Column + padding/spacing
5. [L35] 标题 Text
6. [L36-39] 状态 AssistChip 行
7. [L40-43] 区块「今日轨迹」占位
8. [L44-48] 区块「位置指标」占位
9. [L49-52] 区块「手机使用」占位
10. [L53-57] 区块「当前策略」占位

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt",
      "label": "TodayScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt",
      "type": "depends_on"
    }
  ]
}
```
