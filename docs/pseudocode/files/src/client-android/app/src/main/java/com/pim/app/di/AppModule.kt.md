# src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：DI/模块 `AnonymousProbeClient`：提供依赖绑定。
- 主要依赖：`src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt`、`src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt`、`src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt`、`src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt`、`src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt`、`src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt`
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### AppModule
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L39 声明 `AppModule`
- 分支与异常：无
- 调用：无

### provideAppDatabase
#### provideAppDatabase(@ApplicationContext context: Context)
- 输入：@ApplicationContext context: Context
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideAppDatabase` 参数：@ApplicationContext context: Context
  2. 返回 Room.databaseBuilder(context, AppDatabase::class.java, "pim.db")
  3. 执行：.addMigrations(*PimDatabaseMigrations.ALL)
- 分支与异常：无显著分支
- 调用：provideAppDatabase、Room.databaseBuilder、addMigrations、build

### provideAppUsageDao
#### provideAppUsageDao(db: AppDatabase)
- 输入：db: AppDatabase
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideAppUsageDao` 参数：db: AppDatabase): AppUsageDao = db.appUsageDao(
- 分支与异常：无显著分支
- 调用：provideAppUsageDao、db.appUsageDao

### provideTrackingSharedPreferences
#### provideTrackingSharedPreferences(@ApplicationContext context: Context)
- 输入：@ApplicationContext context: Context
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideTrackingSharedPreferences` 参数：@ApplicationContext context: Context
  2. 返回 context.getSharedPreferences("pim_tracking", Context.MODE_PRIVATE)
- 分支与异常：无显著分支
- 调用：provideTrackingSharedPreferences、context.getSharedPreferences

### provideTrackingSettingsStore
#### provideTrackingSettingsStore(preferences: SharedPreferences)
- 输入：preferences: SharedPreferences
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideTrackingSettingsStore` 参数：preferences: SharedPreferences
  2. 返回 TrackingSettingsStore(preferences)
- 分支与异常：无显著分支
- 调用：provideTrackingSettingsStore、TrackingSettingsStore

### provideAnonymousProbeClient
#### provideAnonymousProbeClient(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideAnonymousProbeClient` 参数：无
  2. 返回 OkHttpClient.Builder()
  3. 执行：.applyPimApiTimeouts()
- 分支与异常：无显著分支
- 调用：provideAnonymousProbeClient、OkHttpClient.Builder、applyPimApiTimeouts、build

### provideAuthenticatedProbeClient
#### provideAuthenticatedProbeClient(client: OkHttpClient)
- 输入：client: OkHttpClient
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideAuthenticatedProbeClient` 参数：client: OkHttpClient
- 分支与异常：无显著分支
- 调用：provideAuthenticatedProbeClient

### provideProbeTokenSource
#### provideProbeTokenSource(tokenManager: TokenManager)
- 输入：tokenManager: TokenManager
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideProbeTokenSource` 参数：tokenManager: TokenManager
  2. 返回 ProbeTokenSource { serverUrl ->
  3. 执行：tokenManager.getAccessTokenForServer(serverUrl)
- 分支与异常：无显著分支
- 调用：provideProbeTokenSource、tokenManager.getAccessTokenForServer

## 近逐行中文伪代码

1. [L25] 注解 @Qualifier
2. [L26] 注解 @Retention
3. [L27] 执行：annotation class AnonymousProbeClient
4. [L29] 注解 @Qualifier
5. [L30] 注解 @Retention
6. [L31] 执行：annotation class AuthenticatedProbeClient
7. [L33] 注解 @Qualifier
8. [L34] 注解 @Retention
9. [L35] 执行：annotation class ConnectionProbePreferences
10. [L37] 注解 @Module
11. [L38] 注解 @InstallIn
12. [L39] 单例 object `AppModule`
13. [L41] 注解 @Provides
14. [L42] 注解 @Singleton
15. [L43] 函数 `provideAppDatabase` 参数：@ApplicationContext context: Context
16. [L44] 返回 Room.databaseBuilder(context, AppDatabase::class.java, "pim.db")
17. [L45] 执行：.addMigrations(*PimDatabaseMigrations.ALL)
18. [L49] 注解 @Provides
19. [L50] 注解 @Singleton
20. [L51] 函数 `provideAppUsageDao` 参数：db: AppDatabase): AppUsageDao = db.appUsageDao(
21. [L53] 注解 @Provides
22. [L54] 注解 @Singleton
23. [L55] 函数 `provideTrackingSharedPreferences` 参数：@ApplicationContext context: Context
24. [L56] 返回 context.getSharedPreferences("pim_tracking", Context.MODE_PRIVATE)
25. [L59] 注解 @Provides
26. [L60] 注解 @Singleton
27. [L61] 函数 `provideTrackingSettingsStore` 参数：preferences: SharedPreferences
28. [L62] 返回 TrackingSettingsStore(preferences)
29. [L65] 注解 @Provides
30. [L66] 注解 @Singleton
31. [L67] 注解 @AnonymousProbeClient
32. [L68] 函数 `provideAnonymousProbeClient` 参数：无
33. [L69] 返回 OkHttpClient.Builder()
34. [L70] 执行：.applyPimApiTimeouts()
35. [L74] 注解 @Provides
36. [L75] 注解 @Singleton
37. [L76] 注解 @AuthenticatedProbeClient
38. [L77] 函数 `provideAuthenticatedProbeClient` 参数：client: OkHttpClient
39. [L79] 注解 @Provides
40. [L80] 注解 @Singleton
41. [L81] 函数 `provideProbeTokenSource` 参数：tokenManager: TokenManager
42. [L82] 返回 ProbeTokenSource { serverUrl ->
43. [L83] 执行：tokenManager.getAccessTokenForServer(serverUrl)
44. [L87] 注解 @Provides
45. [L88] 注解 @Singleton
46. [L89] 注解 @ConnectionProbePreferences
47. [L90] 执行：fun provideConnectionProbePreferences(
48. [L91] 注解 @ApplicationContext
49. [L92] 执行：): SharedPreferences {
50. [L93] 返回 context.getSharedPreferences("pim_connection_probe", Context.MODE_PRIVATE)
51. [L96] 注解 @Provides
52. [L97] 注解 @Singleton
53. [L98] 执行：fun provideConnectionProbeStore(
54. [L99] 注解 @ConnectionProbePreferences
55. [L100] 执行：json: Json
56. [L101] 执行：): ConnectionProbeStore {
57. [L102] 返回 ConnectionProbeStore(preferences, json)
58. [L105] 注解 @Provides
59. [L106] 注解 @Singleton
60. [L107] 执行：fun provideConnectionProbeService(
61. [L108] 注解 @AnonymousProbeClient
62. [L109] 注解 @AuthenticatedProbeClient
63. [L110] 执行：tokenSource: ProbeTokenSource
64. [L111] 执行：): ConnectionProbeService {
65. [L112] 返回 ConnectionProbeService(anonymousClient, authenticatedClient, tokenSource)

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "label": "AnonymousProbeClient",
      "path": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt.md",
      "layer": "client-android",
      "kind": "entrypoint"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/PimDatabaseMigrations.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeService.kt",
      "type": "depends_on"
    },
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/di/AppModule.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt",
      "type": "depends_on"
    }
  ]
}
```
