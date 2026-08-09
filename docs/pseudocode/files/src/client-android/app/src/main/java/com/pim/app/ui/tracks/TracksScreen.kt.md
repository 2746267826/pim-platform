# src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt

## 元信息
- 语言：Kotlin (Jetpack Compose)
- 程序集或包：client-android / com.pim.app.ui.tracks
- 职责：轨迹历史 UI 占位屏（时间范围芯片、质量过滤、片段/详情/原始点说明）。
- 主要依赖：Compose Material3、`PimSection`、`AssistChip`
- 被谁使用：导航到轨迹历史目的地时组合渲染

## 函数级结构化伪代码

### TracksScreen
#### TracksScreen(modifier: Modifier = Modifier)
- 输入：modifier
- 输出：Composable UI
- 副作用：无业务 IO；芯片 onClick 为空
- 步骤：
  1. 全屏可滚动 Column，padding 16dp，间距 12dp
  2. 标题「轨迹历史」
  3. `PimSection("时间范围")`：今日 / 7 天 / 30 天 AssistChip（暂无点击逻辑）
  4. `PimSection("质量过滤")`：默认 &lt;50m；低质量点见状态中心
  5. `PimSection("轨迹片段")`：移动/停留/缺口/低置信占位
  6. `PimSection("片段详情")`：时长距离速度精度占位
  7. `PimSection("原始点")`：选中片段后原始点占位
- 分支与异常：无动态数据分支
- 调用：`PimSection`、`AssistChip`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.ui.tracks`
2. [L3-L16] 导入 Compose 布局、AssistChip、PimSection
3. [L18-L19] `@Composable fun TracksScreen(modifier)`
4. [L20-L26] Column 滚动布局
5. [L27] 标题「轨迹历史」
6. [L28-L34] 时间范围三个 AssistChip，onClick 空
7. [L35-L38] 质量过滤说明
8. [L39-L41] 轨迹片段占位
9. [L42-L44] 片段详情占位
10. [L45-L47] 原始点占位
11. [L48-L49] 结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt",
      "label": "TracksScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt",
      "type": "depends_on"
    }
  ]
}
```
