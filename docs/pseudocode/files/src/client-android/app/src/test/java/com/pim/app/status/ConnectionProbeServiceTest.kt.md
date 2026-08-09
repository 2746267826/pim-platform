# src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android tests / com.pim.app.status
- 职责：用 MockWebServer/Robolectric 覆盖 ConnectionProbeService 多阶段探测、失败分类、能力兼容、token 绑定、证据持久化与安全消息脱敏。
- 主要依赖：ConnectionProbeService、ConnectionProbeStore、MockWebServer、OkHttp、ProbeTokenSource、PimServerEndpoints
- 被谁使用：测试运行器

## 函数级结构化伪代码

### ConnectionProbeServiceTest
#### setUp / tearDown
- 启动 MockWebServer；构造匿名/鉴权 OkHttpClient（记录 AuthMode tag）；固定 wallClock/monotonic；shutdown server

#### 成功路径
- version+status+webroot 全成功 → Reachable、三阶段路径与鉴权头顺序、capabilities 解析

#### 传输失败
- DNS/Connect/Timeout/TLS 异常 → Blocked + 稳定 failureKind；safeMessage 不含 token/Authorization

#### HTTP/路径/能力
- version 404 → WrongPath；503 body 不泄露密钥
- 缺 mobile capability → IncompatibleVersion
- 仅 mobileItemResultsV1 → Partial + IncompatibleVersion
- webroot 404/非 HTML → Partial

#### 鉴权与 token 绑定
- 401 → Unauthorized Blocked
- 无 token 或 token 绑定其他 server → 跳过 AuthenticatedStatus

#### URL / 时钟 / Store
- 含 userinfo 的非法 URL → InvalidUrl、无网络、不回显 secret
- 墙钟回拨不影响单调阶段延迟
- Store 持久化与 5 分钟新鲜度边界；损坏 JSON → null 不崩溃

#### 辅助
- enqueueVersion/Json/Html、serviceFor、FakeProbeTokenSource（按 trustedOrigin 绑定）

## 近逐行中文伪代码

1. Robolectric sdk=34；MockWebServer 与双 Client 记录 AuthMode。
2. 成功探测断言顺序 `/api/version` → `/api/v1/status/summary` → `/` 与 Bearer 仅第二段。
3. 四类传输异常映射 Dns/Connect/Timeout/Tls 且脱敏。
4. 404/503/能力缺失/Partial 场景断言 outcome 与 lastCompletedStage。
5. 无 token 或跨 server token 跳过鉴权阶段。
6. 非法 URL 零请求；Store 5 分钟 isFresh 边界与坏数据容错。
7. FakeProbeTokenSource 用 PimServerEndpoints.trustedOrigin 比对。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt",
      "label": "ConnectionProbeServiceTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/status/ConnectionProbeServiceTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt", "type": "depends_on" }
  ]
}
```
