# src/client-android/app/src/main/java/com/pim/app/location/PimServerUrls.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：PIM 服务 URL 规范化与 /api/v1 路由拼接；默认 127.0.0.1:5858。
- 主要依赖：无
- 被谁使用：HTTP 客户端/同步

## 函数级结构化伪代码

### DEFAULT_PIM_SERVER_URL
- http://127.0.0.1:5858

### normalizePimServerUrl
- trim 去尾斜杠；空则默认；无 scheme 补 http://

### buildPimApiUrl
- 确保 api/v1 前缀 + route

## 近逐行中文伪代码

1. 与本地 API 默认端口对齐。
2. 拼接 REST 路径。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/PimServerUrls.kt",
      "label": "PimServerUrls",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/PimServerUrls.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/PimServerUrls.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
`
