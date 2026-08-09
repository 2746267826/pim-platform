# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.v2
- 职责：静态扫描主源码屏幕文件，断言信息架构文案标签齐全（今日/轨迹/日程策略/状态中心/设置）。
- 主要依赖：JUnit、java.io.File
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AndroidV2ScreenContentTest
#### screensExposeApprovedInformationArchitecture
- 对五个 Screen.kt 路径 assertContains 中文/数值标签列表

#### assertContains(path, labels)
- repoFile 定位 `src/main/java/com/pim/app/...` 读全文
- 每个 label 必须 contains

#### repoFile(parts...)
- 从 cwd 向上找首个存在的路径；找不到 error

## 近逐行中文伪代码

1. 断言 Today/Tracks/SchedulePolicy/StatusCenter/Settings 屏幕源文件包含约定文案。
2. assertContains 读文件并逐标签 assertTrue。
3. repoFile 向上遍历父目录解析仓库相对路径。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt",
      "label": "AndroidV2ScreenContentTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/schedule/SchedulePolicyScreen.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/status/StatusCenterScreen.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ScreenContentTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/settings/SettingsScreen.kt", "type": "tests" }
  ]
}
```
