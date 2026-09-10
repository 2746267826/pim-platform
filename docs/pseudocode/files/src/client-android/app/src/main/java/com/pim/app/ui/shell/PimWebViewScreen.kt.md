# src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt

## 元信息
- 语言：Kotlin / Jetpack Compose
- 程序集或包：client-android / com.pim.app.ui.shell
- 职责：Compose 内嵌 WebView 加载服务端 SPA 路由；页面完成后可选注入 accessToken 到 localStorage。
- 主要依赖：WebView、AndroidView、ServerSettingsStore.DEFAULT_BASE_URL
- 被谁使用：壳层导航需要嵌入 Web 的页面

## 函数级结构化伪代码

### PimWebViewScreen(route, modifier, serverUrl, authToken)
- 输入：route、serverUrl（默认 DEFAULT_BASE_URL）、authToken
- 输出：Composable UI
- 副作用：创建 WebView、loadUrl、evaluateJavascript
- 步骤：
  1. buildPimWebUrl(serverUrl, route)
  2. AndroidView factory：
     - WebViewClient.onPageFinished：authToken 非空则 localStorage.setItem('accessToken', ...)
     - 启用 JS / DOM storage / database
     - loadUrl(targetUrl)
  3. update：url 变化时重新 loadUrl

### buildPimWebUrl(serverUrl, route) → String
- 去尾 `/`；route 空则 `/today`；拼 `$root/$normalizedRoute`

### String.toJsString()
- 转义 `\` 与 `"` 后包双引号

## 近逐行中文伪代码

1. 抑制 SetJavaScriptEnabled lint。
2. 计算 targetUrl = server + route。
3. AndroidView 工厂创建 WebView 并设 WebViewClient。
4. 页面完成时若有 token，evaluateJavascript 写入 localStorage.accessToken。
5. 开启 JS、DOM 与 database 存储后 loadUrl。
6. update 阶段 URL 不一致则重新加载。
7. buildPimWebUrl 规范化根与路由（默认 today）。
8. toJsString 做 JS 字符串转义。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt",
      "label": "PimWebViewScreen",
      "path": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "type": "depends_on" }
  ]
}
```
