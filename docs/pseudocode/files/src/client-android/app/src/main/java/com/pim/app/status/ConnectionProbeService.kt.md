# src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.status
- 职责：分阶段探测服务器连通性（URL 校验 → version → 可选鉴权 status → Web 根页 → 能力判定），输出 `ConnectionProbeResult`。
- 主要依赖：OkHttpClient、PimServerEndpoints、ProbeTokenSource、ConnectionProbe 接口与模型
- 被谁使用：状态中心/设置页连接探测、ConnectionProbeServiceTest

## 函数级结构化伪代码

### ConnectionProbeService
#### probe(serverUrl: String): ConnectionProbeResult
- 输入：用户配置的 serverUrl
- 输出：ConnectionProbeResult（Reachable / Partial / Blocked + 延迟/能力/失败原因）
- 副作用：多次 HTTP 请求；记录各阶段延迟
- 步骤：
  1. 创建 `ProbeProgress`，记录检查时间
  2. `PimServerEndpoints.from(serverUrl)`；非法 URL → Blocked + InvalidUrl
  3. 匿名 GET versionUrl；失败或非兼容 JSON → Blocked
  4. 解析 capabilities；缺少 `mobileItemResultsV1` → Blocked IncompatibleVersion
  5. 若存在 access token：鉴权 GET statusSummary；失败 → Blocked
  6. 匿名 GET webOrigin；失败或非可用 HTML bootstrap → Partial
  7. 有 `androidEmbedV1` → Reachable，否则 Partial（未声明嵌入能力）
- 分支与异常：各阶段 transport/HTTP 映射为 Dns/Timeout/Tls/Connect/Unauthorized/WrongPath/Http
- 调用：execute、httpFailure、transportFailure、PimServerEndpoints.from

#### execute(client, request, stage, progress): StageAttempt
- 输入：OkHttp 客户端、请求、阶段枚举、进度
- 输出：Completed(response) 或 Failed(ProbeFailure)
- 副作用：网络 IO；记录 latency
- 步骤：
  1. 记时 → awaitCancellableResponse
  2. IOException → transportFailure
- 分支与异常：IO 捕获
- 调用：awaitCancellableResponse

#### anonymousRequest / requiredRequest
- 输入：HttpUrl
- 输出：Request（tag AuthMode.Anonymous 或 Required）
- 副作用：无
- 步骤：Builder GET + tag
- 调用：Request.Builder

#### httpFailure(response, optional)
- 输入：Response、是否可选阶段
- 输出：ProbeFailure?（成功 null）
- 步骤：401 Unauthorized；404 WrongPath；其它 Http；中文 safeMessage
- 调用：无

#### transportFailure(failure: IOException)
- 输入：IO 异常
- 输出：ProbeFailure（Dns/Timeout/Tls/Connect）
- 步骤：按异常类型映射
- 调用：无

#### Response.isUsableHtmlBootstrap
- 输入：Response
- 输出：Boolean
- 步骤：Content-Type 为 text/html；体含 `<html` 且 id=root 标记
- 调用：peekBody

#### StageAttempt.consume / ProbeProgress.*
- 输入：失败或响应回调；阶段完成/阻塞/部分结果
- 输出：ConnectionProbeResult 或 Unit
- 步骤：Failed 调 onFailure；Completed 用 response.use；组装 result 字段
- 调用：result / blocked / partial

### awaitCancellableResponse (Call 扩展)
- 输入：Call
- 输出：Response（可取消）
- 副作用：enqueue 异步回调
- 步骤：suspendCancellableCoroutine；取消时 call.cancel；onFailure resumeWithException；onResponse resume 并在取消时 close
- 调用：enqueue

## 近逐行中文伪代码

1. [L23-29] 构造注入匿名/鉴权 OkHttp、token 源、时钟。
2. [L30-42] probe：解析 endpoints，失败则 Blocked InvalidUrl。
3. [L43-46] 记录 serverIdentity 与 Url 阶段延迟并 complete。
4. [L47-67] 请求 version；解析 VersionDocument；失败/不兼容则 Blocked。
5. [L69-81] 构建 ServerCapabilities；缺 mobileItemResultsV1 则 Blocked。
6. [L83-98] 有 token 时探测 AuthenticatedStatus。
7. [L100-117] 探测 WebRoot；失败或非可用 HTML → Partial。
8. [L119-127] 按 androidEmbedV1 返回 Reachable 或 Partial。
9. [L130-145] execute：计时请求，IO 转 transportFailure。
10. [L147-161] 构建匿名/鉴权 Request。
11. [L163-193] HTTP/传输失败分类与中文文案。
12. [L195-200] HTML bootstrap 可用性检查。
13. [L202-276] StageAttempt 消费与 ProbeProgress 汇总结果。
14. [L278-291] VersionDocument 与常量（能力名、64KB、JSON、root 正则）。
15. [L294-313] Call.awaitCancellableResponse 可取消挂起封装。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "label": "ConnectionProbeService",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/AuthMode",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt",
      "type": "depends_on"
    }
  ]
}
```
