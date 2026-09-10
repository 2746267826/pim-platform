# src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.status
- 职责：定义连接探测阶段、失败类型、结果 DTO、能力位，以及探测/Token 源函数接口。
- 主要依赖：`kotlinx.serialization`
- 被谁使用：`ConnectionProbeService`、`ConnectionProbeStore`、设置页/状态中心

## 函数级结构化伪代码

### ConnectionProbeStage
#### enum class ConnectionProbeStage
- 输入：无
- 输出：Url | Version | AuthenticatedStatus | WebRoot
- 副作用：无
- 步骤：1. 声明探测流水线各阶段
- 分支与异常：无
- 调用：无

### ConnectionFailureKind
#### enum class ConnectionFailureKind
- 输入：无
- 输出：InvalidUrl/Dns/Connect/Timeout/Tls/Http/Unauthorized/WrongPath/IncompatibleVersion
- 副作用：无
- 步骤：1. 声明可序列化失败分类
- 分支与异常：无
- 调用：无

### ConnectionProbeOutcome
#### enum class ConnectionProbeOutcome
- 输入：无
- 输出：Reachable | Partial | Blocked
- 副作用：无
- 步骤：1. 声明探测总结果
- 分支与异常：无
- 调用：无

### ServerCapabilities
#### data class ServerCapabilities(mobileItemResultsV1, androidEmbedV1)
- 输入：两个能力布尔
- 输出：能力快照
- 副作用：无
- 步骤：1. 记录服务端是否支持移动结果与 Android 嵌入
- 分支与异常：无
- 调用：无

### ConnectionProbeResult
#### data class ConnectionProbeResult(...)
- 输入：outcome、时间戳、serverIdentity、阶段、延迟 map、capabilities、可选 failure/http/message
- 输出：完整探测结果
- 副作用：无
- 步骤：1. 聚合一次探测的成功/失败信息
- 分支与异常：无
- 调用：无

### ProbeTokenSource
#### fun interface currentAccessToken(serverUrl): String?
- 输入：serverUrl
- 输出：当前 access token 或 null
- 副作用：读会话存储（实现侧）
- 步骤：1. 由实现按服务器提供 token
- 分支与异常：无
- 调用：实现方

### ConnectionProbe
#### fun interface probe(serverUrl): ConnectionProbeResult (suspend)
- 输入：serverUrl
- 输出：`ConnectionProbeResult`
- 副作用：网络探测（实现侧）
- 步骤：1. 抽象探测入口
- 分支与异常：无
- 调用：实现方

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.status`
2. [L5-6] `@Serializable` 枚举 `ConnectionProbeStage` 四阶段
3. [L8-9] 枚举 `ConnectionFailureKind` 九种失败
4. [L11-12] 枚举 `ConnectionProbeOutcome` 三种总结果
5. [L14-18] 数据类 `ServerCapabilities`
6. [L20-31] 数据类 `ConnectionProbeResult` 全字段
7. [L33-35] 函数接口 `ProbeTokenSource`
8. [L37-39] 函数接口 `ConnectionProbe.probe` 挂起

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt",
      "label": "ConnectionProbeResult",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
