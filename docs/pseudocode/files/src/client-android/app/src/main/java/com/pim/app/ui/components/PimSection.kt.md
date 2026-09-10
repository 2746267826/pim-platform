# src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt

## 元信息
- 语言：Kotlin / Jetpack Compose
- 程序集或包：client-android / com.pim.app.ui.components
- 职责：通用分区卡片：圆角边框 Surface + 标题 + 垂直间距内容槽。
- 主要依赖：Material3 Surface/Text、Compose layout
- 被谁使用：StatusCenterScreen 等 UI 页面

## 函数级结构化伪代码

### PimSection
#### PimSection(title, modifier, content)
- 输入：标题字符串、可选 Modifier、ColumnScope 内容 lambda
- 输出：Composable UI
- 副作用：无
- 步骤：
  1. Surface：fillMaxWidth、8.dp 圆角、outlineVariant 1.dp 边框、surface 色
  2. 内层 Column：padding 16.dp、项间距 10.dp
  3. Text 标题 titleMedium + SemiBold
  4. 调用 content() 渲染子内容
- 分支与异常：无
- 调用：MaterialTheme、Surface、Column、Text

## 近逐行中文伪代码

1. 导入 Compose foundation/layout 与 Material3。
2. @Composable fun PimSection(title, modifier, content)。
3. Surface 铺满宽度并加边框圆角。
4. Column 内先画标题再 content()。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt",
      "label": "PimSection",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/components/PimSection.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
