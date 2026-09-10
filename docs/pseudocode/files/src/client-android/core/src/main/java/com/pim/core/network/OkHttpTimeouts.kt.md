# src/client-android/core/src/main/java/com/pim/core/network/OkHttpTimeouts.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android-core / com.pim.core.network
- 职责：为 OkHttpClient.Builder 统一应用 PIM API 超时（连接/读/写/整次调用）。
- 主要依赖：`okhttp3.OkHttpClient`、`TimeUnit`
- 被谁使用：`CoreModule.provideOkHttpClient` 等构建 HTTP 客户端处

## 函数级结构化伪代码

### applyPimApiTimeouts
#### OkHttpClient.Builder.applyPimApiTimeouts(): OkHttpClient.Builder
- 输入：Builder 接收者
- 输出：同一 Builder（链式）
- 副作用：修改 builder 超时字段
- 步骤：
  1. connectTimeout 15s
  2. readTimeout 60s
  3. writeTimeout 60s
  4. callTimeout 90s
  5. 返回 this
- 分支与异常：无
- 调用：OkHttp timeout setters

## 近逐行中文伪代码

1. [L1] 包 com.pim.core.network
2. [L3-L4] 导入 OkHttpClient、TimeUnit
3. [L6] 扩展函数 applyPimApiTimeouts
4. [L7] connectTimeout 15 秒
5. [L8] readTimeout 60 秒
6. [L9] writeTimeout 60 秒
7. [L10] callTimeout 90 秒
8. [L11] 结束函数

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/network/OkHttpTimeouts.kt",
      "label": "applyPimApiTimeouts",
      "path": "src/client-android/core/src/main/java/com/pim/core/network/OkHttpTimeouts.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/network/OkHttpTimeouts.kt.md",
      "layer": "client-android",
      "kind": "other"
    }
  ],
  "edges": []
}
```
