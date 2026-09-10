# src/client-android/core/src/test/java/com/pim/core/network/ApiServiceConverterTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core tests / com.pim.core.network
- 职责：验证 `LoginRequest` kotlinx.serialization 与 Retrofit converter 写出 JSON 字段。
- 主要依赖：`LoginRequest`、Retrofit、kotlinx.serialization converter、JUnit
- 被谁使用：测试运行器

## 函数级结构化伪代码

### ApiServiceConverterTest
#### loginRequestSerializerWritesJson
- 输入：无
- 输出：断言
- 副作用：无
- 步骤：`Json.encodeToString(LoginRequest("alice","secret"))` 含 username/password 字段
- 分支与异常：无
- 调用：`Json.encodeToString`

#### loginRequestBodyConverterWritesJson
- 输入：无
- 输出：断言
- 副作用：构建 Retrofit
- 步骤：
  1. Retrofit + kotlinx converter baseUrl
  2. `requestBodyConverter<LoginRequest>` 转换请求体
  3. writeTo Buffer，读 UTF-8，断言含 alice/secret 字段
- 分支与异常：converter null → error
- 调用：`Retrofit.Builder`、`convert`、`Buffer`

## 近逐行中文伪代码

1. [L13-20] 直接序列化 LoginRequest 断言 JSON
2. [L22-28] 建 Retrofit converter factory
3. [L29-41] 请求体转换并断言内容

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/network/ApiServiceConverterTest.kt",
      "label": "ApiServiceConverterTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/network/ApiServiceConverterTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/network/ApiServiceConverterTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/network/ApiServiceConverterTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/LoginRequest.kt",
      "type": "tests"
    }
  ]
}
```
