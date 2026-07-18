# Android 客户端阶段 2：今日与轨迹

**最终目标：** 今日成为默认标签并显示真实地图、轨迹、手机使用和本地传输事实；轨迹页面提供筛选、片段详情和原始点分页。两页复用当前服务器的 React WebUI，Android 只负责可信鉴权、传输状态、加载错误和导航边界。

## 实现概要

```
React App
  ├─ 普通路由 → AuthProvider + AppLayout（保持现有桌面登录）
  └─ /embed/android/* → AndroidEmbedLayout（无侧栏、无 localStorage 鉴权）
                              ↕ 当前服务器 origin 的 WebMessage 通道
Android PimWebViewScreen → 原生 access token / refresh / 本地传输事实
```

- `/embed/android/today` 和 `/embed/android/tracks` 是 React 客户端路由，继续由现有 SPA fallback 返回 `index.html`。
- access token 只保存在 React 模块内存；refresh token 始终留在 Android 原生认证仓库。
- 地图、筛选和详情留在 Web 端，今后更换地图 SDK 或样式只改 WebUI。

## 当前状态（2026-07-18）

- Task 1-6 的代码和焦点测试已经完成，并已同步到 `origin/master` 的 fused location 最新实现。
- 已通过：Web embed 测试、Today 测试、Stage 2 变更文件 lint（0 error）、Web 生产构建、后端 1170 项测试、Android app/core 917 项测试，以及 debug / androidTest APK 构建。
- 全仓 Web lint 当前被 `origin/master` 既有的 18 个无关 error 阻塞；本阶段文件只有 2 个 hook dependency warning，不把基线失败写成通过，也不在本 PR 扩大修复范围。
- 10 个并行只读专项审查和一次汇总复核未发现未处理的 Critical / Important 问题；审查确认的共享错误边界、安全区和子资源失败日志缺口已补齐。
- 当前 `adb devices -l` 没有可用设备；`connectedDebugAndroidTest` 与本阶段人工场景仍待模拟器或真机验收。PR #34 已创建，首轮 Actions 的 Android、Web、API 与汇总检查均通过；Windows 和 release 因本次路径/非发布条件跳过。

## 前置依赖

- Stage 1 已完成服务器绑定认证、同步结果流、状态事实和连接检查。
- Task 2 已提供 `src/Pim.Api/Endpoints/VersionEndpoints.cs` 与 `GET /api/version`。
- `src/Pim.Api/Program.cs` 已配置 `MapFallbackToFile("index.html")`。

---

## 1. React 嵌入入口与能力声明

**目的：** 建立两个无桌面侧栏的 SPA 入口，并让 Android 先通过 `androidEmbedV1` 判断当前服务器是否支持。

**文件：**
- `src/client-web/src/App.tsx`
- `src/client-web/src/layout/AndroidEmbedLayout.tsx`（新建）
- `src/Pim.Api/Endpoints/VersionEndpoints.cs`
- `tests/Pim.UnitTests/Api/VersionEndpointTests.cs`
- `tests/client-web/androidEmbedRoutes.test.tsx`（新建）
- `src/client-web/package.json`

**复用：** 普通桌面路由继续使用 `AuthProvider` 与 `AppLayout`；嵌入入口复用现有 React Query、Today/mobile API 和页面组件。`Program.cs` 的 SPA fallback 不改。

**完成方式：**
1. 在 `App.tsx` 最外层先区分 `/embed/android/*` 与普通路由。只有普通路由创建 `AuthProvider`，避免嵌入页初始化时调用 `loadTokens()` 读取桌面 localStorage。
2. `AndroidEmbedLayout` 只提供安全区域、内容滚动和共享错误边界，不渲染 `Sidebar`、`InboxPanel`、快捷笔记或桌面登录页。
3. 增加 `/embed/android/today` 与 `/embed/android/tracks` 路由。能力不支持时 Android 显示“服务器版本不支持嵌入页面”，不得回退到现有写死页面。
4. 在现有 `VersionEndpoints` 增加 `AndroidEmbedV1` 常量并加入 capability 列表；更新现有 endpoint test，不新建 settings capability 或 HTML endpoint。
5. 增加 `test:android-embed` 脚本统一运行本阶段 Web 测试。

**自动验证：** 路由测试确认两个入口可直接刷新、无桌面 chrome、不会初始化桌面 token；API 测试确认 capability 同时保留 `mobileItemResultsV1` 和 `androidEmbedV1`。

**人工验收：** 桌面浏览器打开两个 embed URL 时只出现内容区；普通 `/today` 仍走原桌面布局和登录流程。

---

## 2. 当前服务器限定的内存鉴权桥

**目的：** 只让当前配置服务器的文档异步取得短期 access token 和原生状态；401 时由原生层刷新一次，WebView 永远接触不到 refresh token。

**文件：**
- `src/client-android/app/build.gradle.kts`
- `src/client-android/app/src/main/java/com/pim/app/ui/shell/AndroidWebMessageBridge.kt`（新建）
- `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`
- `src/client-web/src/embed/androidBridge.ts`（新建）
- `src/client-web/src/api/client.ts`
- `tests/client-web/androidEmbedAuth.test.ts`（新建）
- `src/client-android/app/src/test/java/com/pim/app/ui/shell/AndroidWebMessageBridgeTest.kt`（新建）

**复用：** Stage 1 的 `TokenManager`、唯一 refresh 入口、`ServerSettingsStore` 和 `PimServerEndpoints`；不在 bridge 内重复实现 `/auth/refresh` 请求。

**完成方式：**
1. 引入 AndroidX WebKit，使用 `WebViewCompat.addWebMessageListener` 创建一个消息通道，`allowedOriginRules` 只包含规范化后的当前服务器 origin。IP、局域网域名、HTTP 和非标准端口都按实际 origin 精确匹配。
2. 启动前检查 `WebViewFeature.WEB_MESSAGE_LISTENER`；设备 WebView 不支持时显示原生“不支持安全嵌入，请更新 Android System WebView”错误，不回退到无 origin 限制的 `JavascriptInterface`。
3. Web 页发送带 request id 的 `token.request`；原生读取当前 access token 后异步回复。收到 `token.refresh` 时，原生调用 Stage 1 认证仓库的单 Mutex refresh，最多一次，再回复新 token 或登录已过期。
4. `androidBridge.ts` 把回复匹配到 Promise；`api/client.ts` 的 embed 模式只使用模块内存 access token。首次请求等待握手；401 请求一次 refresh 并只重放原请求一次。
5. embed 路径不调用 `loadTokens()`、`setTokens()` 或桌面 refresh 逻辑。普通桌面模式可继续使用当前 localStorage 登录，两种模式的状态入口明确分开。
6. 通道同时支持非敏感消息：原生向页面提供采集/传输摘要，页面向原生报告 `hasServerData`、`generatedAt` 和页面错误，供今日原生状态条组合事实。
7. 外部主框架导航在进入 WebView 前被拦截并交给系统浏览器；因此外部文档从未获得消息通道。通道在原受信任页面销毁时移除。地图瓦片/CDN 子资源正常加载，但请求不附加 Android Authorization header。

**自动验证：** 覆盖初始 token、401 refresh 一次、refresh 失败、并发请求共用一次 refresh、embed localStorage 始终无 auth token、外部 origin 消息被拒绝、服务器切换后旧 origin 失效，以及 WebMessage 不可用时安全失败。

**人工验收：** 登录后两页无需再次登录；令牌过期可恢复一次；登出或切换服务器后旧页面立即失去鉴权；地图资源仍能加载。

---

## 3. 通用 WebView 的加载、错误与导航状态

**目的：** 所有嵌入页共享稳定的加载、HTTP 警告、详细错误和重试体验，不出现白屏。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- `src/client-android/app/src/test/java/com/pim/app/ui/shell/PimWebViewStateTest.kt`（新建）
- `src/client-android/app/src/androidTest/java/com/pim/app/ui/shell/PimWebViewScreenTest.kt`（按需；仅当 JVM 无法覆盖时增加，本阶段由设备验收覆盖）

**复用：** 现有 `buildPimWebUrl()` 和原生导航壳；服务器 origin 统一由 Stage 1 resolver 产生。

**完成方式：**
1. `PimWebViewScreen` 管理 loading、content、main-frame error、login-expired 四类状态；尺寸稳定，状态变化不推动底部导航跳动。
2. 仅主 frame 的 `onReceivedError` / `onReceivedHttpError` 替换为原生错误页；子资源失败保留页面并记录详情。SSL 错误始终取消加载并显示原因，不提供忽略证书按钮。
3. HTTP 页面允许加载但持续显示“连接未加密”警告；重试重新加载同一可信 URL并立即回到 loading 状态。
4. 主框架只允许当前 origin 下的两个 embed 路径及其查询参数；同源其他页面和外部链接交给系统浏览器。普通子资源请求不被错误拦截。
5. Composable 销毁时移除 listener、加载 `about:blank` 并销毁 WebView；登出或切换服务器时再清当前 origin 的站点数据。普通 Tab 切换不反复清全局缓存。

**自动验证：** 状态机、main-frame 与子资源错误区分、HTTP/HTTPS 警告、可信路径判断、重试、销毁和服务器切换清理均有焦点测试。

**人工验收：** 慢网有加载反馈；断网、500 和证书失败显示具体原因及重试；外部链接不留在受信任 WebView。

---

## 4. 今日页面与本地传输事实

**目的：** 默认标签展示今日真实位置、手机使用、采集策略和数据时间；原生状态条明确区分无数据、待上传、未开始和错误。

**文件：**
- `src/client-web/src/pages/AndroidTodayEmbedPage.tsx`（新建）
- `src/client-web/src/components/mobile/AndroidTodaySummary.tsx`（新建，只有组合复杂度需要时）
- `src/client-web/src/api/mobile.ts`
- `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/today/TodayViewModel.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- `tests/client-web/androidTodayEmbed.test.tsx`（新建）
- `src/client-android/app/src/test/java/com/pim/app/ui/today/TodayViewModelTest.kt`（新建）

**复用：** `LocationHistoryMap`、`LocationMetricStrip`、移动轨迹 API、`MobileInsightStrip`、`MobileAppRanking`、手机 analytics API，以及普通 `TodayPage` 已有的 Today section API；不假定当前“日程任务工作台”已经包含地图。

**完成方式：**
1. `/embed/android/today` 渲染专用轻量页面，查询当天位置 overview/tracks 和手机 usage overview，显示真实轨迹线、停留点、距离、完整度、前台使用时长与 Top 应用。
2. 页面从 bridge 的非敏感状态读取当前采集模式、触发原因和下次定位时间；展示服务端各查询的 `generatedAt`，有旧数据时明确标注“生成于/可能过期”。
3. 页面把是否有服务端数据和生成时间回报给原生。`TodayViewModel` 与 Stage 1 本地 pending、WorkInfo、采集意图组合出：未开始、待上传、服务端确认无数据、加载/同步错误四类状态。
4. 原生状态条持续显示 pending / uploading / confirmed / rejected、上次成功和下一次尝试；“立即同步”沿用 Stage 1 动作和反馈，不在 Web 页再实现第二个同步入口。
5. `PimDestination.Today` 仍是默认目的地。服务器不支持 embed 时显示可操作升级提示，不展示假地图或旧占位文案。

**自动验证：** Web 测试覆盖真实数据、服务端空和错误；Android 测试覆盖四类组合状态、服务端时间、同步反馈和 capability 不支持。

**人工验收：** 有数据时地图和 Web 端一致；待上传时状态条不把服务端空误判为无采集；旧数据有明确时间；手动同步后状态立即变化。

---

## 5. 轨迹筛选、详情与原始点分页

**目的：** 复用历史位置 WebUI，补齐窄屏布局、明确状态和真正的原始点分页。

**文件：**
- `src/client-web/src/pages/HistoricalLocationPage.tsx`
- `src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx`
- `src/client-web/src/components/mobile/LocationHistoryMap.tsx`
- `src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx`
- `src/client-web/src/components/mobile/LocationSegmentDetail.tsx`
- `src/client-web/src/components/mobile/LocationRawPointTable.tsx`
- `src/client-web/src/api/mobile.ts`
- `src/client-android/app/src/main/java/com/pim/app/ui/tracks/TracksScreen.kt`
- `tests/client-web/androidTracksEmbed.test.tsx`（新建）

**复用：** 现有日期/设备/精度筛选、Leaflet 轨迹图、停留/移动片段和 segment-points API；不接入原生地图 SDK。

**完成方式：**
1. `/embed/android/tracks` 直接使用 `HistoricalLocationPage` 的数据流，embed 模式只调整密度和窄屏结构，不复制查询逻辑。
2. 时间范围、设备、精度和是否包含 rejected 点写入 URL query；整页 reload 后筛选仍保持。
3. 地图展示轨迹线和选中点；时间线可展开，片段详情与地图选择联动。
4. 原始点查询增加明确的 page/pageSize 状态和上一页/下一页；换筛选或片段时回到第一页。页大小沿用 API 上限，不在计划里另造固定协议。
5. 无轨迹、筛选后无结果、片段无原始点和请求失败分别显示清晰状态，错误保留当前筛选并可重试。

**自动验证：** 覆盖筛选到 API/URL 的映射、片段展开与详情、页码切换和重置、空状态及失败重试。

**人工验收：** 7/30 天与自定义范围都能更新地图；片段详情可读；原始点可翻页；断网后不是白屏。

---

## 6. 同步后刷新与阶段验收

**目的：** 同步产生新确认数据后刷新当前 WebUI，并完成 API/Web/Android 一次整体验证。

**文件：**
- `src/client-android/app/src/main/java/com/pim/app/ui/root/PimRootScreen.kt`
- `src/client-android/app/src/main/java/com/pim/app/ui/shell/PimWebViewScreen.kt`
- `src/client-web/src/embed/androidBridge.ts`
- 本阶段新增测试文件

**复用：** Stage 1 的同步结果流与 confirmed/rejected 计数。

**完成方式：**
1. 只在同步成功或部分成功且 confirmed 数增加时使 Today/Tracks 失效；失败或无变化不伪造刷新成功。
2. 当前可见页 reload 当前 URL；隐藏页只记录 dirty 标记，在下次打开时加载一次，避免保留两个后台 WebView。Tracks 查询参数随 URL 保留。
3. reload 后页面重新报告服务端数据时间，原生状态条同步刷新；登录过期仍走 Task 2 的一次 refresh 规则。

**自动验证：**
- `dotnet test Pim.sln`
- `npm --prefix src/client-web run lint`
- `npm --prefix src/client-web run build`
- `npm --prefix src/client-web run test:today`
- `npm --prefix src/client-web run test:android-embed`
- 在 `src/client-android` 运行 `./gradlew :core:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug`
- `git diff --check`

**人工验收：** 登录后今日真实数据、轨迹筛选/详情/分页、HTTP 警告、IP/LAN 地址、断网错误与重试、401 刷新一次、外部 origin 隔离、同步完成后页面和时间戳更新。

最后进行一次整体代码审查，重点核对 origin 规则、token 生命周期、主 frame 错误处理和 React/Android 状态是否一致。

---

## 本阶段明确不做

- 返回专用 HTML 的 API endpoint 或新的 WebView token 发放 endpoint
- refresh token 进入 WebView、embed token 写入 localStorage、OAuth/PKCE、证书固定
- `JavascriptInterface` 同步网络刷新、多个 bridge 框架或 WebView 预加载池
- 原生地图 SDK、Today/Tracks 原生重写、Service Worker 离线缓存
- 日程页面与策略实现（Stage 3）
- 逐任务双重审查或大型证据矩阵

## 完成标准

1. 本阶段相关 API、Web 和 Android 测试与构建零失败；若全仓门禁存在 `origin/master` 已有失败，必须证明本阶段变更文件无新增 error 并记录基线。
2. 模拟器一次通过本阶段全部人工验收场景。
3. 一次整体审查无未处理的关键安全、数据状态或交互问题。
