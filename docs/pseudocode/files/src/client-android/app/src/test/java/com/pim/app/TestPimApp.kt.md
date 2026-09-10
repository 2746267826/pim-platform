# src/client-android/app/src/test/java/com/pim/app/TestPimApp.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android test / com.pim.app
- 职责：测试用极简 `Application` 子类，避免单元/仪表测试拉起真实 Hilt `PimApp`。
- 主要依赖：`android.app.Application`
- 被谁使用：Android 测试 manifest / Robolectric 配置

## 函数级结构化伪代码

### TestPimApp
- 空实现，继承 `Application`，无 onCreate 业务逻辑

## 近逐行中文伪代码

1. 声明 `class TestPimApp : Application()`。
2. 供测试环境替换生产 Application。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/TestPimApp.kt",
      "label": "TestPimApp",
      "path": "src/client-android/app/src/test/java/com/pim/app/TestPimApp.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/TestPimApp.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": []
}
```
