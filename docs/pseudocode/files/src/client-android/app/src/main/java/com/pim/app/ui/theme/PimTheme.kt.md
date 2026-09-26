# src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui.theme
- 职责：Compose Material3 浅色主题封装，统一 PIM Android UI 色板。
- 主要依赖：`MaterialTheme`、`lightColorScheme`
- 被谁使用：Activity/根 Composable 包裹应用内容

## 函数级结构化伪代码

### PimLightColors
- `lightColorScheme`：primary 蓝、secondary 青绿、tertiary 琥珀、error 红、浅灰背景与描边

### PimTheme(content)
- 输入：子 Composable
- 输出：无（渲染）
- 副作用：提供 MaterialTheme 作用域
- 步骤：`MaterialTheme(colorScheme=PimLightColors, typography=默认, content)`

## 近逐行中文伪代码

1. 定义私有 `PimLightColors` 各角色色值。
2. `PimTheme` 用该色板 + 默认 typography 包裹 `content`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt",
      "label": "PimTheme",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/theme/PimTheme.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
