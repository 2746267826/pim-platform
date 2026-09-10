# src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core tests / com.pim.core.settings
- 职责：覆盖 ServerUrlValidator 空白、公网 IP/域名规范化、拒绝非法路径/query/fragment/userinfo、localhost 警告、默认空 Base URL。
- 主要依赖：ServerUrlValidator、ServerSettingsStore、JUnit
- 被谁使用：测试运行器

## 函数级结构化伪代码

### ServerUrlValidatorTest
#### blankUrlIsNotConfigured
- validate("") → invalid + missing + normalized 空

#### publicIpIsAcceptedAndGetsTrailingSlash
- `http://203.0.113.8:5858/api/v1` → 末尾补 `/`

#### publicDomainIsAccepted
- https 域名 /api/v1/ 保持有效

#### rejectsAnythingTheEndpointResolverCannotUse
- 非 /api/v1、多余路径、query、fragment、user:pass → isValid=false

#### realDeviceLocalhostReceivesWarning
- 127.0.0.1 有效且含 real-device-localhost

#### serverSettingsDefaultIsBlankForRealPhones
- DEFAULT_BASE_URL 与 normalizeBaseUrl("") 均为空串

## 近逐行中文伪代码

1. 空串 missing。
2. 文档 IP 与域名 URL 规范化通过。
3. 五类非法 URL 全部拒绝。
4. localhost 警告码存在。
5. 真机默认 Base URL 为空。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt",
      "label": "ServerUrlValidatorTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt", "type": "tests" },
    { "from": "src/client-android/core/src/test/java/com/pim/core/settings/ServerUrlValidatorTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt", "type": "depends_on" }
  ]
}
```
