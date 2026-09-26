# src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android core / com.pim.core.network
- 职责：按 ServerSettingsStore 的 baseUrl 缓存并创建 Retrofit `ApiService`；提供 refresh 专用客户端与动态代理。
- 主要依赖：OkHttpClient、Json、ServerSettingsStore、PimServerEndpoints、Retrofit、kotlinx.serialization
- 被谁使用：DI/网络层注入 ApiService

## 函数级结构化伪代码

### ApiClientProvider
#### apiService() / refreshApiService()
- 返回 clients() 中对应实例（业务客户端 vs 无拦截 refresh 客户端）

#### refreshApiServiceForServer(serverIdentity)
- 由 trusted origin 推导 apiBaseUrl，用 refreshOkHttpClient 新建 ApiService

#### dynamicApiService()
- Proxy：Any 方法本地处理；业务方法 invokeApiMethod 转发到当前 apiService()

#### clients() [private]
- 读 baseUrl；缓存命中且 URL 相同则返回
- synchronized 双重检查后 createApiService 两套（okHttpClient / refreshOkHttpClient）

#### createApiService(baseUrl, client)
- PimServerEndpoints.from；失败 IllegalStateException「API address is not configured or invalid」
- Retrofit baseUrl=apiBaseUrl + JSON converter + client → ApiService

#### invokeApiMethod / invokeAnyMethod
- Method.invoke 解包 InvocationTargetException；equals/hashCode/toString 语义

### Clients [private data]
- baseUrl + apiService + refreshApiService

### companion
- JSON_MEDIA_TYPE；refreshOkHttpClient 应用 applyPimApiTimeouts

## 近逐行中文伪代码

1. 单例持有 okHttp、Json、settingsStore 与 volatile 缓存。
2. apiService/refreshApiService 走缓存 clients。
3. 指定 serverIdentity 时单独建 refresh 服务。
4. dynamicApiService 代理每次转发到最新 apiService。
5. baseUrl 变化时重建两个 Retrofit 客户端。
6. 无效地址抛 IllegalStateException。
7. 反射调用剥离 InvocationTargetException。
8. refresh 客户端无共享拦截器，仅超时配置。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "label": "ApiClientProvider",
      "path": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/ServerSettingsStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/settings/PimServerEndpoints.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/OkHttpTimeouts.kt",
      "type": "depends_on"
    }
  ]
}
```
