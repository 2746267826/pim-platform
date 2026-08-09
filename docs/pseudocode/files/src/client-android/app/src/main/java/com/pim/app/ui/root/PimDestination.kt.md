# src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.ui.root
- 职责：底部主导航目的地枚举：中文 label + Material 图标。
- 主要依赖：Compose Material Icons、`ImageVector`
- 被谁使用：`PimRootScreen` / Scaffold 导航

## 函数级结构化伪代码

### PimDestination
#### enum class PimDestination(label, icon)
- 输入：无（编译期常量）
- 输出：Today/Tracks/Schedule/Status/Settings 五个目的地
- 副作用：无
- 步骤：
  1. 每个枚举项绑定中文标签与 `Icons.Filled.*`
  2. Today/Tracks 使用 LocationOn；Schedule 用 CheckCircle；Status 用 Security；Settings 用 Settings
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.ui.root`
2. [L10-13] 声明枚举，构造参数 label、icon
3. [L14] Today →「今日」+ LocationOn
4. [L15] Tracks →「轨迹」+ LocationOn
5. [L16] Schedule →「日程」+ CheckCircle
6. [L17] Status →「状态」+ Security
7. [L18] Settings →「设置」+ Settings

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt",
      "label": "PimDestination",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/root/PimDestination.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
