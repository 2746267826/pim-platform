# src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt

## 元信息
- 语言：Kotlin (Jetpack Compose)
- 程序集或包：client-android / com.pim.app.ui.schedule
- 职责：日程低频定位策略说明 UI 占位屏（当前日程、策略影响、即将到来、策略切换说明）。
- 主要依赖：Compose Material3、`PimSection`
- 被谁使用：导航到日程策略目的地时组合渲染

## 函数级结构化伪代码

### SchedulePolicyScreen
#### SchedulePolicyScreen(modifier: Modifier = Modifier)
- 输入：modifier
- 输出：Composable UI
- 副作用：无业务 IO；仅布局与静态文案
- 步骤：
  1. 全屏可滚动 Column，padding 16dp，间距 12dp
  2. 标题「日程低频策略」
  3. `PimSection("当前日程")`：无带位置日程说明 + 进入后 15 分钟间隔
  4. `PimSection("策略影响")`：15 分钟 / 100m 恢复阈值 / 运动或位移后 1 分钟观察
  5. `PimSection("即将到来")`：后续带地点日程占位说明
  6. `PimSection("策略切换")`：切换记录占位说明
- 分支与异常：无动态分支
- 调用：`PimSection`、MaterialTheme.typography

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.ui.schedule`
2. [L3-L14] 导入 Compose 布局、Material3、PimSection
3. [L16-L17] 声明 `@Composable fun SchedulePolicyScreen(modifier)`
4. [L18-L24] Column：fillMaxSize + verticalScroll + padding + spacedBy
5. [L25] 标题 Text headlineSmall
6. [L26-L29] 区块「当前日程」两段说明
7. [L30-L34] 区块「策略影响」三行：间隔、阈值、恢复条件
8. [L35-L37] 区块「即将到来」占位
9. [L38-L40] 区块「策略切换」占位
10. [L41-L42] 结束 Column / 函数

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt",
      "label": "SchedulePolicyScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt",
      "type": "depends_on"
    }
  ]
}
```
