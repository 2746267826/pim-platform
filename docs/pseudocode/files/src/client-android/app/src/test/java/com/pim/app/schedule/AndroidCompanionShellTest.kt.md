# src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.schedule
- 职责：源码契约测试：Manifest/Shell/WebView/PermissionCenter 声明通知权限、入口与关键 Web 路由。
- 主要依赖：JUnit、`File` 向上查找仓库文件
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AndroidCompanionShellTest
#### shellSourcesDeclareWebViewPermissionCenterAndRoutes
- 输入：无
- 输出：断言通过/失败
- 副作用：读磁盘源文件
- 步骤：
  1. 读取 AndroidManifest、PimShellActivity、PimWebViewScreen、PermissionCenterScreen 文本
  2. 断言 Manifest 含 `POST_NOTIFICATIONS`、`.ui.shell.PimShellActivity`、`NotificationActionReceiver`
  3. 断言 shell 引用 PermissionCenterScreen 与 PimWebViewScreen
  4. 断言 permissions 含 `collection quality`
  5. 对路由列表 `/today`…`/confirmations` 断言出现在 shell 或 webView 源码中
- 分支与异常：找不到文件则 `error`
- 调用：`repoFile`

#### repoFile(vararg parts): File
- 输入：相对路径片段
- 输出：存在的 File
- 副作用：向上遍历 canonical 父目录
- 步骤：从当前目录起 fold resolve；存在则返回；否则 parent；全失败 error
- 分支与异常：未找到抛 error
- 调用：`File.resolve`、`exists`

## 近逐行中文伪代码

1. [L7-9] 测试方法开始
2. [L10-13] 读四个源文件
3. [L15-21] 断言 Manifest/Shell/Permissions 关键字
4. [L22-24] 循环断言 Web 路由字符串
5. [L27-36] `repoFile` 向上查找

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt",
      "label": "AndroidCompanionShellTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidCompanionShellTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt",
      "type": "tests"
    }
  ]
}
```
