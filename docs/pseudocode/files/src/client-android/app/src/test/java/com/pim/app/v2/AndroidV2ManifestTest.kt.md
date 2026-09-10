# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (app test)
- 职责：静态读取 `AndroidManifest.xml`，断言 V2 权限、前台定位服务类型、MainActivity 启动器；禁止 Web shell 作为 LAUNCHER。
- 主要依赖：`java.io.File`、JUnit
- 被谁使用：单元测试运行器

## 函数级结构化伪代码

### AndroidV2ManifestTest
#### manifestDeclaresNativeLauncherAndLocationForegroundService()
- 输入：无
- 输出：无（断言）
- 副作用：读仓库文件
- 步骤：
  1. `repoFile("src","main","AndroidManifest.xml").readText()`
  2. 断言含后台定位、活动识别、FGS location 权限与服务声明
  3. 断言含 MainActivity
  4. 断言 launcher 块不含 `PimShellActivity`
- 分支与异常：文件找不到由 repoFile error
- 调用：`repoFile`、`launcherBlock`、assertTrue/False

#### launcherBlock(manifest: String): String
- 步骤：定位 LAUNCHER category，回退到最近 `<activity` 截到 `</activity>`

#### repoFile(vararg parts): File
- 步骤：从当前目录向上找存在的候选路径，否则 error

## 近逐行中文伪代码

1. [L8-9] 测试类与用例
2. [L11] 读取 manifest 全文
3. [L13-18] 断言权限/服务/MainActivity 字符串存在
4. [L19] launcher 块不得含 PimShellActivity
5. [L22-28] launcherBlock 切片逻辑
6. [L30-38] 向上查找 repo 文件

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt",
      "label": "AndroidV2ManifestTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2ManifestTest.kt", "to": "src/client-android/app/src/main/AndroidManifest.xml", "type": "tests" }
  ]
}
```
