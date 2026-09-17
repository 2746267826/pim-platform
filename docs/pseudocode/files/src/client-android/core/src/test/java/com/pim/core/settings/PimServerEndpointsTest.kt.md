# src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.core.settings（client-android core test）
- 职责：验证 `PimServerEndpoints.from` 从 API base 推导 api/web/health/version/status/embed URL，规范化斜杠、默认端口省略、IPv6，并拒绝非法路径/查询/scheme/凭据。
- 主要依赖：`PimServerEndpoints`、JUnit
- 被谁使用：JUnit 测试运行器

## 函数级结构化伪代码

### PimServerEndpointsTest
#### derivesApiAndWebEndpointsFromConfiguredApiBase()
- 输入：`http://127.0.0.1:5858/api/v1/`
- 输出：断言 apiBase、webOrigin、trustedOrigin、health、version、statusSummary、today/tracks embed
- 副作用：无
- 步骤：`from` 后逐项 assertEquals
- 分支与异常：无
- 调用：`PimServerEndpoints.from`

#### preservesHttpsAndPortAndNormalizesExactlyOneTrailingSlash()
- 输入：无尾斜杠与多余尾斜杠的 https URL
- 输出：apiBase 均为恰好一个尾斜杠；webOrigin/trustedOrigin 正确
- 副作用：无
- 步骤：两次 from 比较 apiBase；再验 web/trusted
- 分支与异常：无
- 调用：`from`

#### trustedOriginOmitsDefaultPorts()
- 输入：:80 / :443 URL
- 输出：trustedOrigin 不含默认端口
- 副作用：无
- 步骤：assertEquals host-only origin
- 分支与异常：无
- 调用：`from`

#### preservesIpv6HostWithBrackets()
- 输入：`http://[2001:db8::1]:5858/api/v1/`
- 输出：webOrigin/trusted/health 保留方括号 IPv6
- 副作用：无
- 步骤：from 后断言
- 分支与异常：无
- 调用：`from`

#### rejectsWrongPathQueryFragmentSchemeHostAndCredentials()
- 输入：错误路径/查询/fragment/ftp/空 host/userinfo 等 URL 列表
- 输出：每个 from 抛 IllegalArgumentException
- 副作用：无
- 步骤：forEach assertThrows
- 分支与异常：期望 IllegalArgumentException
- 调用：`from`

## 近逐行中文伪代码

1. [L1-6] 包与 JUnit 导入
2. [L8-20] 从 127.0.0.1:5858/api/v1 推导全套端点
3. [L22-36] HTTPS 端口与尾斜杠规范化
4. [L38-48] 默认端口从 trustedOrigin 省略
5. [L50-57] IPv6 括号保留
6. [L59-76] 非法 URL 全部拒绝

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt",
      "label": "PimServerEndpointsTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/test/java/com/pim/core/settings/PimServerEndpointsTest.kt", "to": "com.pim.core.settings.PimServerEndpoints", "type": "tests" }
  ]
}
```
