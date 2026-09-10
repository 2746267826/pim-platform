# src/client-android/core/src/test/java/com/pim/core/models/AuthResponseSerializationTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core test / com.pim.core.models
- 职责：验证 `AuthResponse` 反序列化兼容服务端 `user`、旧版 `userInfo`、以及缺用户信息仍保留令牌。
- 主要依赖：kotlinx.serialization `Json`、`ApiResponse`、`AuthResponse`
- 被谁使用：测试运行器

## 函数级结构化伪代码

### authResponseAcceptsServerUserField
- JSON 含 `user`；断言 access/refresh 与 `userInfo.username=alice`

### authResponseAcceptsLegacyUserInfoField
- JSON 含 `userInfo`；断言 username

### authResponseKeepsTokensWhenUserIsMissing
- 无用户字段；accessToken 仍解析，userInfo 为 null

## 近逐行中文伪代码

1. `Json { ignoreUnknownKeys = true }`。
2. 解码 `ApiResponse<AuthResponse>` 包装。
3. 三种载荷覆盖命名别名与可选用户。
4. 确保登录管线不因字段名差异失败。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/models/AuthResponseSerializationTest.kt",
      "label": "AuthResponseSerializationTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/models/AuthResponseSerializationTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/models/AuthResponseSerializationTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/test/java/com/pim/core/models/AuthResponseSerializationTest.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt",
      "type": "tests"
    }
  ]
}
```
