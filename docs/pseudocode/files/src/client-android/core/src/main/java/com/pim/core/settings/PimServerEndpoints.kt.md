# src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.core.settings（client-android core）
- 职责：由配置的 API URL 推导 API 基址、Web 源、可信 Origin 与健康/版本/状态/嵌入页 URL。
- 主要依赖：okhttp3.HttpUrl
- 被谁使用：网络客户端、设置/探测、嵌入 Web 入口

## 函数级结构化伪代码

### PimServerEndpoints
#### 数据类字段
- 输入：构造参数
- 输出：endpoints 包
- 副作用：无
- 步骤：apiBaseUrl、webOrigin、trustedOrigin、health/version/statusSummary、today/tracks embed
- 分支与异常：无
- 调用：无

#### from(configuredApiUrl): PimServerEndpoints
- 输入：配置的 API URL 字符串
- 输出：规范化 endpoints
- 副作用：无（纯解析）
- 步骤：
  1. `toHttpUrl` 解析
  2. require scheme http/https、host 非空、无凭据、path 恰为 `/api/v1`、无 query/fragment
  3. apiBaseUrl 路径 `/api/v1/`；webOrigin 路径 `/`
  4. trustedOrigin = trustedOriginOf(webOrigin)
  5. 派生 health、version、status summary、today/tracks embed
- 分支与异常：require 失败抛 IllegalArgumentException
- 调用：`trustedOriginOf`、`HttpUrl.resolve`

#### trustedOriginOf(url): String
- 输入：HttpUrl
- 输出：scheme://host[:port]
- 副作用：无
- 步骤：IPv6 host 加方括号；非默认端口附加 port
- 分支与异常：scheme 限制
- 调用：无

#### normalizeTrustedOrigin(value): String
- 输入：origin 字符串
- 输出：规范化 origin
- 副作用：无
- 步骤：解析后禁止凭据；path 必须 `/` 且无 query/fragment；再 `trustedOriginOf`
- 分支与异常：require
- 调用：`trustedOriginOf`

#### apiBaseUrlForTrustedOrigin(value): HttpUrl
- 输入：trusted origin 字符串
- 输出：`/api/v1/` 基址
- 副作用：无
- 步骤：normalize → resolve API_PATH/
- 分支与异常：resolve 非空断言
- 调用：`normalizeTrustedOrigin`

## 近逐行中文伪代码

1. [L1-5] 包与 OkHttp HttpUrl
2. [L6-15] 数据类持有全部派生 URL
3. [L17-55] `from`：严格校验配置 API URL 并派生 endpoints
4. [L57-72] `trustedOriginOf` 构造 origin 串
5. [L74-83] `normalizeTrustedOrigin` 校验纯 origin
6. [L85-90] 由 origin 得 API base；API_PATH=`/api/v1`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "label": "PimServerEndpoints",
      "path": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt.md",
      "layer": "client-android",
      "kind": "other"
    }
  ],
  "edges": []
}
```
