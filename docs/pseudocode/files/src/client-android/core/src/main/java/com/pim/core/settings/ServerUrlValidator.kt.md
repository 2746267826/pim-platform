# src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.settings
- 职责：校验并规范化 API Base URL；空值/非法解析失败；对 localhost 与 cleartext HTTP 给出警告码。
- 主要依赖：PimServerEndpoints
- 被谁使用：设置页保存服务器地址；ServerUrlValidatorTest

## 函数级结构化伪代码

### ServerUrlValidationResult
- 字段：input、normalizedUrl、isValid、reasonCode、warnings

### ServerUrlValidator (object)
#### validate(value: String?) → ServerUrlValidationResult
- 输入：原始字符串
- 输出：校验结果
- 副作用：无
- 步骤：
  1. trim；空白 → isValid=false, reasonCode=missing
  2. PimServerEndpoints.from(input) 失败 → invalid(..., invalid-api-url)
  3. 取 scheme/host
  4. warnings：localhost/127.0.0.1/::1 → real-device-localhost；http → cleartext-http
  5. 返回 isValid=true、normalizedUrl=apiBaseUrl.toString()

#### invalid(input, reasonCode)
- isValid=false，normalizedUrl=input

## 近逐行中文伪代码

1. 定义结果 data class。
2. validate：trim 后空串直接 missing。
3. runCatching 解析 PimServerEndpoints，失败 invalid-api-url。
4. 主机为环回地址加 real-device-localhost 警告。
5. scheme 为 http 加 cleartext-http 警告。
6. 成功返回规范化 apiBaseUrl 字符串。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt",
      "label": "ServerUrlValidator",
      "path": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/main/java/com/pim/core/settings/ServerUrlValidator.kt", "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt", "type": "depends_on" }
  ]
}
```
