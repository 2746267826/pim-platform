# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt

## 元信息
- 语言：Kotlin (JUnit)
- 程序集或包：client-android app test
- 职责：扫描活跃 v2 相关 Kotlin 源（ui/location/status），禁止出现 mojibake/替换字符标记。
- 主要依赖：java.io.File walkTopDown、UTF-8 readText
- 被谁使用：单元测试

## 函数级结构化伪代码

### AndroidV2TextEncodingTest
#### activeAndroidV2SourcesDoNotContainMojibakeMarkers
- 输入：无
- 输出：断言无 offenders
- 步骤：
  1. roots = repo 内 app ui / location / status 目录
  2. markers = U+FFFD 及一批已知乱码片段
  3. 遍历 .kt 文件，含任一 marker 则记 path
  4. assertFalse offenders 非空
- 调用：repoFile

#### repoFile(parts...)
- 从当前目录向上找存在的路径，找不到 error

## 近逐行中文伪代码

1. 定义三棵源码根目录。
2. 定义乱码 marker 列表。
3. walkTopDown 过滤 kt 并检测 marker。
4. 有违规路径则断言失败并打印列表。
5. repoFile 向上解析仓库相对路径。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt",
      "label": "AndroidV2TextEncodingTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2TextEncodingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status", "type": "tests" }
  ]
}
```
