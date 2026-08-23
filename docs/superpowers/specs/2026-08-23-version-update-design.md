# PIM 完整版本号与更新系统设计（方案 B）

> 状态：已通过头脑风暴 5 节评审，待实现计划  
> 日期：2026-08-23  
> 分支建议：`opencode-linux/version-update`  
> 覆盖 6 个版本源：`Pim.Api` / `client-web` / `Pim.Client.App` (Windows Daemon) / `Pim.Shell.App` (Windows Shell) / `Android Shell` / `Android Daemon`（后两者同 APK）

## 1. 背景与目标

- **现状**：CI 已在 `scripts/ci/resolve-version.sh` 产出 `CalVer YYYY.MM.N`（`N = GITHUB_RUN_NUMBER` 单调递增）并注入 `Pim.Api`/`Daemon`/`Android`，但 `Pim.Shell.App` 硬编码 `0.1.0`（`src/client-shell-windows/Pim.Shell.App/ShellWindow.xaml.cs:34`），`client-web` 的 `__APP_VERSION__`（`src/client-web/vite.config.ts:22`）未被消费，`Dockerfile` 二次构建覆盖导致容器内 `InformationalVersion` 恒为 `0.0.0-local`，`/api/client/shell/latest`（`src/Pim.Api/Modules/ClientShell/ClientShellModule.cs:17`）依赖手工 `ClientShellOptions` 未与 GitHub Releases 关联，Web/API/客户端均无版本 UI，`UpdateChecker.cs:9` 字符串比较对 `2026.08.10 vs 2026.08.9` 误判且静默 `catch {}`。
- **目标（C 全栈可见，服务端仅提示）**：6 端各自烘焙并就地展示当前版本且可一键复制；统一走 `PIM API` 获取最新版本（服务端自拉 GitHub），失败透传真实错误；版本比对仅比末段 `N`；Windows 本期保留打开链接（`a`）+ 进度条占位预留一键覆盖（`C`），Android 仅打开链接，Web/API 仅提示无 changelog。

## 2. 架构

- **真相源**：`resolve-version.sh` 的 `YYYY.MM.N` 唯一；`is_release` 仅 `refs/heads/master` 为真。
- **写入层**：5 个 CI 流水线各自把 `version` 烘焙进产物，`artifact_slug` 仅用于文件名与 Release tag `vYYYY.MM.N`。
- **服务层**：`Pim.Api` 新增 `GitHubReleaseService`（`Singleton + IHostedService` 定时），对外扩展两个只读端点：`GET /api/version` 与 `GET /api/client/shell/latest`，均返回 `checkedAt/error` 并透传失败。
- **消费层**：Web 页脚读本地烘焙，设置页聚合 6 源；Windows Shell/Daemon 各自读自身 `InformationalVersion`；Android 读 `PackageInfo`；三端按 启动+每6h+手动按钮 请求 `latest`，用 `IsNewer`（只比 N）决定横幅。
- **隔离**：版本与更新与业务模块零耦合；`GitHubReleaseService` 仅依赖 `HttpClient + IMemoryCache`，不写 DB。

## 3. 组件与接口契约

### 3.1 CI/构建修复

- `build-docker.yml` / `src/Pim.Api/Dockerfile`：`Dockerfile` 增加 `ARG PIM_VERSION`，`server-build` 阶段 `RUN dotnet publish ... -p:InformationalVersion=$PIM_VERSION -p:Version=$ASSEMBLY_VERSION -p:FileVersion=$ASSEMBLY_VERSION`；`build-docker.yml` 将 `version/assembly_version/git_sha_short` 透传为 `build-args`。
- `build-windows.yml`：拆为两步分别 `dotnet publish Pim.Client.App` 与 `Pim.Shell.App` 均带 `-p:InformationalVersion`，产物合并进同一 `pim-windows-v*.zip`，保留 `publish/VERSION`。
- `build-web.yml`：补充 `VITE_GIT_SHA` 注入；`vite.config.ts` 保留 `__APP_VERSION__` 兼容，新增 `define.__GIT_SHA__`。
- `Directory.Build.props` 保持 `0.0.0-local` 兜底不变。

### 3.2 服务端

- `GitHubReleaseService`：
  - `HttpClient` 复用，`GET https://api.github.com/repos/2746267826/pim-platform/releases/latest`，带 `Authorization: Bearer ${GITHUB_TOKEN}`（无 token 则匿名）、`If-None-Match: etag`、`User-Agent: pim-platform`。
  - 解析 `tag_name` 去 `v` 得 `latestVersion`，遍历 `assets` 按 `pim-windows-*.zip` / `pim-android-*.apk` 提取 `browser_download_url`。
  - 6h 周期 + 启动立即拉取；304 仅更新 `checkedAt`；失败置 `error` 并 `Logger.Warning`。
  - 白名单校验：`windowsUrl/androidUrl` 必须 `https://github.com/2746267826/pim-platform/releases/download/...`，否则置 `error`。
- `VersionEndpoints.cs`：`ApiVersionResponse` 扩展为 `record ApiVersionResponse(string Version, IReadOnlyList<string> Capabilities, string? LatestVersion, DateTimeOffset? CheckedAt, string? Error)`。
- `ClientShellModule.cs`：`GET /api/client/shell/latest` 返回 `new { windowsVersion, windowsUrl, androidVersion, androidUrl, checkedAt, error }`，`error != null` 时版本可空；保持 `AllowAnonymous`。
- `UpdateChecker.cs`：重写 `IsNewer(current, remote)` 为取末段 `N` 数值比较（`version.Split('.').Last().Split(new[]{'-','+'})[0]` 转 `int`），`remoteN > currentN` 即新版；空 `remote` 返回 `false`；空 `current` 返回 `true`；非法格式回退 `Ordinal` 并 `Logger.Warn`。

### 3.3 Web

- `useVersionInfo` hook：读 `__APP_VERSION__`/`import.meta.env.VITE_APP_VERSION` 得 `localVersion`，`GET /api/version` 得 `serverVersion/latestVersion/checkedAt/error`，计算 `hasUpdate = ParseN(latestVersion) > ParseN(serverVersion)`。
- `AppLayout` 页脚：`v{local} · API v{server} · {hasUpdate ? "有新版 v{latest}" : ""}`，`checkedAt` tooltip。
- `SettingsPage` 新增“关于 PIM”卡片：6 行（Web/API/Win Daemon/Win Shell/Android），每行 `version + gitSha` + 复制按钮，顶部横幅“服务端有新版 vX”仅提示无 changelog，`error` 时显“检查失败：{error}”。

### 3.4 Windows

- Shell `ShellWindow.xaml.cs`：`current = Assembly.GetExecutingAssembly().GetCustomAttribute<AssemblyInformationalVersionAttribute>().InformationalVersion`；`HttpClient` 5s 超时；三触发（`Loaded` 延迟3s / `PeriodicTimer 6h` / 设置“检查更新”按钮）；`UpdateBar` 保留 `Process.Start(url)`，新增 `ProgressBar Visibility.Collapsed` 占位 C。
- Daemon `App.xaml.cs`：托盘菜单新增“关于”“检查更新”（复用同一 `IsNewer` 与日志）。

### 3.5 Android

- `PimAppScaffold` 设置页新增“关于”行 `versionName (versionCode)` + 复制；`SettingsViewModel` 三触发请求 `latest`，有新版 `Snackbar`“发现新版 vX，去下载” → `Intent(Intent.ACTION_VIEW, Uri.parse(url))`。

## 4. 数据流与时序

- **构建时**：`push master` → `ci.yml:resolve-version` 产出 `version=YYYY.MM.N` → 5 个 `build-*` 并行烘焙 → `release` 以 `tag vYYYY.MM.N` 创建 Release 并上传 5 资产。
- **服务端运行时**：启动立即拉取 → 缓存 `{ latestVersion, windowsUrl, androidUrl, etag, checkedAt, error }` → 每6h 轮询（304 仅刷新 `checkedAt`）；`GET /api/version|/api/client/shell/latest` 仅读缓存不触发外网。
- **客户端运行时**：启动读本地 Current → 延迟3s 请求 Latest → `IsNewer` 比 N → 横幅/Snackbar/托盘气泡；每6h 再查；设置页按钮立即触发并显 `checkedAt/error`；点击“去下载”打开链接；服务端被 `docker pull` 更新后下次轮询自动刷新。
- **失败分支**：GitHub 限流/无外网 → 缓存 `error`，`latest` 返回 `{error, checkedAt}`，客户端不提示更新仅显错误；客户端超时 → `Logger.Warn` + “检查失败”；非法版本 → 回退比较并 Warn。

## 5. 错误处理与可观测性

- 失败透传不吞错，`error` 原样返回，纠正 `ShellWindow.xaml.cs:39` 的 `catch {}`。
- 客户端必打日志：Windows `Logger.Warn`、Android `Timber.w`、Web `console.warn`。
- 限流与缓存：`GITHUB_TOKEN` + `If-None-Match`，304 不计限流；服务端缓存6h，客户端“检查更新”节流30s。
- 安全：仅两个 `GET` 暴露版本，不暴露 token；URL 白名单 `github.com/2746267826/pim-platform/releases/download`。
- 可观测：日志字段 `checkedAt/latestVersion/error/etag/durationMs` 结构化；设置页展示 `checkedAt`。

## 6. 测试与验收

- **单测（TDD）**：`UpdateCheckerTests` 扩充 N 比较、后缀忽略、空/非法输入；`GitHubReleaseServiceTests` 覆盖 ETag、200 解析、限流/超时/JSON 异常、白名单；`ClientShellModuleTests` 覆盖正常/带 error/匿名；`useVersionInfo` 前端单测。
- **集成**：`WebApplicationFactory` 契约测试 `GET /api/version|/api/client/shell/latest`；Android 设置页 UI 测试。
- **手工/E2E**：`dotnet test Pim.sln --no-restore` 全绿；`npm --prefix src/client-web run build` 验证注入；Windows 断网显“检查失败”联通显横幅；`docker run ... && curl /api/version` 非 `0.0.0-local`。
- **验收**：6 源各自可见且可复制；`latest` 失败透传；`IsNewer` 只比 N；无外网不误报；日志必有 `checkedAt/error`。

## 7. 非目标

- Windows 一键静默覆盖（C）与 Android 应用内下载安装（`DownloadManager + REQUEST_INSTALL_PACKAGES`）本期仅预留接口不实现。
- Web/API 不展示 Release Notes，不提供 `compose` 更新脚本。
- 不引入强制更新标记。
- 不持久化版本历史到 DB。

## 8. 风险与缓解

- Docker 二次构建覆盖：通过 `ARG` 透传解决，已验证 `ci.yml` 已有 `version` 输出。
- GitHub 限流：`GITHUB_TOKEN + ETag` 缓解，失败可接受为“检查失败”不阻塞业务。
- 字符串版本误判：`IsNewer` 仅比 N，兼容 `+patch/-pr` 后缀。

## 9. 实施顺序（供 writing-plans 细化）

1. 修复 3 个漏注入（Docker/Shell/Web）并补 `UpdateChecker` 单测
2. 实现 `GitHubReleaseService` + 扩展两个端点
3. Web 页脚与设置“关于”卡片（含复制）
4. Windows Shell/Daemon 关于与检查更新（含日志与占位进度条）
5. Android 关于与 Snackbar
6. 全量回归与文档
