# src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt

## 元信息
- 语言：Kotlin (Dagger Hilt)
- 程序集或包：client-android-core / com.pim.core.di
- 职责：Singleton 组件中提供 Token/会话、登录传输、JSON、刷新操作与协调器、带超时与 Auth 拦截的 OkHttp、动态 ApiService。
- 主要依赖：`TokenManager`、`ApiClientProvider`、`AuthInterceptor`、`AuthRefreshCoordinator`、`applyPimApiTimeouts`、Hilt
- 被谁使用：Hilt 注入图；App 各层通过 DI 获取网络与认证依赖

## 函数级结构化伪代码

### CoreModule
#### provideTokenManager(context): TokenManager
- 输入：Application Context
- 输出：TokenManager 单例
- 步骤：`TokenManager(context)`
- 副作用：无（构造可能触存储）
- 调用：无

#### provideAuthSessionStore(tokenManager): AuthSessionStore
- 输入：TokenManager
- 输出：AuthSessionStore（同一实例）
- 步骤：直接返回 tokenManager
- 调用：无

#### provideServerBoundLoginTransport(apiClientProvider): ServerBoundLoginTransport
- 输入：Lazy&lt;ApiClientProvider&gt;
- 输出：按 serverIdentity 绑定 login 的传输
- 步骤：lambda 内 refreshApiServiceForServer(serverIdentity).login(request)
- 调用：`ApiClientProvider.refreshApiServiceForServer`

#### provideJson(): Json
- 步骤：`Json { ignoreUnknownKeys = true }`

#### provideAuthRefreshOperation(apiClientProvider): AuthRefreshOperation
- 步骤：RetrofitAuthRefreshOperation，refreshCall 按 serverIdentity 调 refresh
- 调用：`refreshApiServiceForServer`、`ApiService.refresh`

#### provideAuthRefreshCoordinator(sessionStore, refreshOperation): AuthRefreshCoordinator
- 步骤：`AuthRefreshCoordinator(sessionStore, refreshOperation)`

#### provideOkHttpClient(sessionStore, refreshCoordinator): OkHttpClient
- 步骤：Builder → applyPimApiTimeouts → AuthInterceptor → build
- 调用：`applyPimApiTimeouts`、`AuthInterceptor`

#### provideApiService(apiClientProvider): ApiService
- 步骤：`apiClientProvider.dynamicApiService()`

## 近逐行中文伪代码

1. [L1] 包 com.pim.core.di
2. [L23-L25] @Module @InstallIn(SingletonComponent) object CoreModule
3. [L27-L31] provideTokenManager
4. [L33-L37] provideAuthSessionStore = tokenManager
5. [L39-L49] ServerBoundLoginTransport：按捕获服务器 login
6. [L51-L53] Json ignoreUnknownKeys
7. [L55-L67] RetrofitAuthRefreshOperation 绑定 refresh
8. [L69-L76] AuthRefreshCoordinator
9. [L78-L88] OkHttp：超时 + AuthInterceptor
10. [L90-L94] dynamicApiService 作为 ApiService

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "label": "CoreModule",
      "path": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt.md",
      "layer": "client-android",
      "kind": "other"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/auth/TokenManager.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiClientProvider.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthInterceptor.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/AuthRefreshCoordinator.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/core/src/main/java/com/pim/core/di/CoreModule.kt",
      "to": "src/client-android/core/src/main/java/com/pim/core/network/OkHttpTimeouts.kt",
      "type": "calls"
    }
  ]
}
```
