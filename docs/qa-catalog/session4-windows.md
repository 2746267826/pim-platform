# Session 4 Windows 代码静查汇总

> 时间：2026-08-24
> 范围：`src/client-windows` (Pim.Client.App / Pim.Client.Core / Pim.Client.Infrastructure) + `src/client-shell-windows` (Pim.Shell.App / Pim.Shell.Tests)
> 方式：不启动 UI，仅静态审计 + `dotnet build` 编译检查（Linux `EnableWindowsTargeting=true`）
> 约束：不改业务代码、不修复、不启动 WPF

## 环境准备

| 项目 | 命令 | 结果 |
|---|---|---|
| client-windows Core | `dotnet build Pim.Client.Core.csproj` | Build succeeded 0 Warning 0 Error |
| client-windows Infra | `dotnet build Pim.Client.Infrastructure.csproj` | Build succeeded 0 Warning 0 Error (空工程，仅 Sqlite 引用) |
| client-windows App | `dotnet build Pim.Client.App.csproj -p:EnableWindowsTargeting=true` | Build succeeded 0 Warning 0 Error |
| client-shell App | `dotnet build Pim.Shell.App.csproj -p:EnableWindowsTargeting=true` | Build succeeded 0 Warning 0 Error |
| client-shell Tests | `dotnet build Pim.Shell.Tests.csproj -p:EnableWindowsTargeting=true` | Build succeeded 0 Warning 0 Error |
| slnx | `dotnet build Pim.Client.Windows.slnx` / `Pim.Shell.Windows.slnx` | MSB4068 失败（`dotnet 8.0.424` 不支持 VS slnx XML 格式，非代码问题，见 `evidence/windows-build.log`） |

详细记录见 `evidence/windows-build.log`（含 5 条独立 csproj 构建成功，slnx 失败原因为工具链版本）。

## 审计项总览

| # | 审计类别 | 证据方式 | 发现 | 证据文件 |
|---|---|---|---|---|
| 1 | Service 异常处理 | `grep -rn catch` + 手工审读 `ApiClient`/`AuthService`/`DaemonHeartbeatReporter` | 3 项 | WIN-001~003 |
| 2 | 数据同步逻辑（死循环/无限重试/内存泄漏） | `grep -rn while/for` + `limit=-1` + 定时器审读 | 3 项 | WIN-004~006 |
| 3 | 配置读取（硬编码/缺失默认值） | `grep -rn 127.0.0.1/DefaultServerUrl` + `DaemonConfig`/`ShellConfig` 审读 | 3 项 | WIN-007~009 |
| 4 | API 调用（超时/重试/错误码） | `grep -rn Timeout/HttpClient/EnsureSuccessStatusCode` | 3 项 | WIN-010~012 |
| 5 | 文件 IO（未关闭流/未释放资源） | `grep -rn File./FileStream/StreamReader` + `HttpResponseMessage` 审读 | 3 项 | WIN-013~015 |
| 6 | 安全（明文密码/token） | `grep -rn token/password/localStorage -i` | 3 项 | WIN-016~018 |

共 18 个 WIN 证据，覆盖全部 6 类；无 PASS（均有发现）。未启动 UI，未改业务代码。

## 各类审计详情

### 1. Service 类异常处理

grep 证据（`grep -rn "catch" src/client-windows/Pim.Client.Core/Services`）：
```
AuthService.cs:95        catch
AuthService.cs:117       catch
KeyStatsCollectorService.cs:57            catch (OperationCanceledException)
AwCollectorService.cs:41        catch (Exception ex)
AwCollectorService.cs:104  catch (TaskCanceledException)
ApiClient.cs: 无 catch
DaemonHeartbeatReporter.cs: 无 catch
PlannedOfflineReporter.cs: 无 catch
```

- **WIN-001** `AuthService` 登录/刷新无 catch，`RefreshAsync` 经 `OnUnauthorized` 回调传播异常。
- **WIN-002** `ApiClient.SendWithAuthRetryAsync:134` 对 `request()` 与 `EnsureSuccessStatusCode()` 无 try/catch，非 401 异常直接抛。
- **WIN-003** `DaemonHeartbeatReporter:16` 与 `PlannedOfflineReporter:14` 透传无独立 catch。

### 2. 数据同步逻辑

grep 证据（`grep -rn "while\|for (" src/client-windows --include="*.cs"`）：
```
AwCollectorService.cs:97   while (!_cts.Token.IsCancellationRequested)
App.xaml.cs:180            while (await _heartbeatTimer.WaitForNextTickAsync(ct))
KeyStatsCollectorService.cs:52 while (await timer.WaitForNextTickAsync(_cts.Token))
AwCollectorService.cs:379  limit=-1 (ActivityWatchUnboundedLimit)
```

- **WIN-004** 固定 30s 重试无退避/熔断，游标仅成功时 `CommitFetched`，失败永久重传。
- **WIN-005** `FetchNewEvents:360` 同步阻塞 `GetAwaiter().GetResult()` 且无界 `limit=-1`，内存膨胀与死锁风险。
- **WIN-006** `KeyStatsCollector` 1分钟 `PeriodicTimer` 无执行超时；`AwCollector` 回填与定时共用 `_collectionGate`，回填饥饿实时采集。

### 3. 配置读取

grep 证据（`grep -rn "127.0.0.1\|DefaultServerUrl" src/client-windows`）：
```
ClientDefaults.cs:5  DefaultServerUrl = "http://127.0.0.1:5858"
AwCollectorService.cs:20  AW_BASE_URL ?? "http://127.0.0.1:5600"
KeyStatsCollectorService.cs:11  KEYSTATS_BASE_URL ?? "http://127.0.0.1:18080"
ApiClient.cs:65  localhost -> 127.0.0.1
ServerAddress.cs:17  trimmed = "https://" + trimmed
DaemonConfig.cs:9  ServerUrl = ClientDefaults.DefaultServerUrl
ShellConfig.cs:8  ServerUrl = ""
```

- **WIN-007** 硬编码散落 + 归一化不一致（`ApiClient` 转 127.0.0.1 vs `ServerAddress` 强制 https://）。
- **WIN-008** `ShellConfig` 默认空、无原子写入、损坏静默回空。
- **WIN-009** `DaemonConfig` 损坏静默回默认、无日志、非原子写入。

### 4. API 调用

grep 证据（`grep -rn "Timeout\|EnsureSuccessStatusCode" src/client-windows`）：
```
ApiClient.cs:25  HttpClient(handler) // 无 Timeout
AwCollectorService.cs:31  HttpClient { BaseAddress } // 无 Timeout
KeyStatsCollectorService.cs:39  Timeout = 5s
StatusWindow.xaml.cs:15  Timeout = 3s
ApiClient.cs:165  EnsureSuccessStatusCode()
ServerHealthClient.cs:20  IsSuccessStatusCode
```

- **WIN-010** `ApiClient` 无 Timeout（默认 100s）、仅 401 重试一次、无 transient 重试，HTTP vs `Code 0/200` 语义割裂。
- **WIN-011** `AwCollector._aw` 无 Timeout；`StatusWindow` 硬编码 5600 探活与实际 `AW_BASE_URL` 不一致。
- **WIN-012** `ShellWindow:32` 升级检查 fire-and-forget 且 `UpdateChecker.IsNewer` 字符序比对版本号。

### 5. 文件 IO

grep 证据（`grep -rn "File\.\|FileStream" src/client-windows`）：
```
AuthService.cs:75  ReadAllTextAsync
AuthService.cs:115  WriteAllText
DaemonConfig.cs:22  ReadAllText / WriteAllText
KeyStatsOneClickFixService.cs:354  FileStream + StreamReader (正确)
AwCollectorService.cs:360  GetAsync + ReadFromJsonAsync 未 Dispose response
TrayIcon.cs:48  new Icon(stream) 未 Dispose stream
```

- **WIN-013** `FetchNewEvents:360` 未释放 `HttpResponseMessage`，每 30s 泄漏句柄。
- **WIN-014** `TrayIcon/TrayManager.LoadIcon` 未释放 ResourceStream；`Logger.LogFilePath` 惰性 `DateTime.Now` 与 `Initialize` 路径不一致。
- **WIN-015** 非原子 `WriteAllText` 多进程并发半截 JSON，`ReadAllText` 无 `FileShare` 互斥。

### 6. 安全

grep 证据（`grep -rn "token\|localStorage" src/client-windows -i`）：
```
AuthService.cs:13  TokenDir = .../PIM  TokenPath = .../token.json
AuthService.cs:115  WriteAllText(JsonSerialize) // 明文
EmbeddedWebViewHost.cs:72  tokenJson = Serialize(CurrentAccessToken)
EmbeddedWebViewHost.cs:73  localStorage.setItem('accessToken', ...)
EmbeddedWebViewHost.cs:75  AddScriptToExecuteOnDocumentCreatedAsync
ClientDefaults.cs:5  http://127.0.0.1:5858 (明文 http)
ServerAddress.cs:26  IsInsecure http:
```

- **WIN-016** 明文 `token.json` 落盘，未加密/DPAPI，违 `AGENTS.md B5`。
- **WIN-017** `EmbeddedWebViewHost:65` 持久注入 `localStorage`，XSS 可窃，与 `CATALOG PIM-038` 同源。
- **WIN-018** 默认 http 明文 + DevTools 常开 `AreDevToolsEnabled=true`。

## 证据清单

| 文件 | 对应审计 |
|---|---|
| `evidence/windows/WIN-001.md` | Service 异常：AuthService 无 catch |
| `evidence/windows/WIN-002.md` | Service 异常：ApiClient 未包裹网络异常 |
| `evidence/windows/WIN-003.md` | Service 异常：Heartbeat/Offline 无独立 catch |
| `evidence/windows/WIN-004.md` | 同步：无限重试无退避 |
| `evidence/windows/WIN-005.md` | 同步：阻塞 GetResult + 无界拉取 |
| `evidence/windows/WIN-006.md` | 同步：周期任务无超时/饥饿 |
| `evidence/windows/WIN-007.md` | 配置：硬编码散落与归一化不一致 |
| `evidence/windows/WIN-008.md` | 配置：ShellConfig 空默认与静默吞错 |
| `evidence/windows/WIN-009.md` | 配置：DaemonConfig 静默回退 |
| `evidence/windows/WIN-010.md` | API：缺超时/重试/错误码不完整 |
| `evidence/windows/WIN-011.md` | API：部分 Client 无 Timeout + 硬编码探活 |
| `evidence/windows/WIN-012.md` | API：升级检查吞错 + 字符序版本 |
| `evidence/windows/WIN-013.md` | IO：HttpResponseMessage 未释放 |
| `evidence/windows/WIN-014.md` | IO：Icon Stream 未释放 + 日志路径不一致 |
| `evidence/windows/WIN-015.md` | IO：非原子写入并发损坏 |
| `evidence/windows/WIN-016.md` | 安全：明文 token.json |
| `evidence/windows/WIN-017.md` | 安全：localStorage 注入 |
| `evidence/windows/WIN-018.md` | 安全：默认 http + DevTools 常开 |
| `evidence/windows-build.log` | 构建记录（5 csproj 成功，slnx MSB4068） |

无 `PASS-*.md`（6 类均有发现）。

## 复查命令

```bash
dotnet build src/client-windows/Pim.Client.Core/Pim.Client.Core.csproj -c Debug
dotnet build src/client-windows/Pim.Client.Infrastructure/Pim.Client.Infrastructure.csproj -c Debug
dotnet build src/client-windows/Pim.Client.App/Pim.Client.App.csproj -p:EnableWindowsTargeting=true -c Debug
dotnet build src/client-shell-windows/Pim.Shell.App/Pim.Shell.App.csproj -p:EnableWindowsTargeting=true -c Debug
dotnet build src/client-shell-windows/Pim.Shell.Tests/Pim.Shell.Tests.csproj -p:EnableWindowsTargeting=true -c Debug
grep -rn "catch" src/client-windows/Pim.Client.Core/Services --include="*.cs"
grep -rn "127.0.0.1" src/client-windows --include="*.cs"
grep -rn "Timeout\|EnsureSuccessStatusCode" src/client-windows --include="*.cs"
grep -rn "File\.\|FileStream" src/client-windows --include="*.cs"
grep -rn "token\|localStorage" src/client-windows --include="*.cs" -i
```

## 不做事项确认

- 未启动 Windows UI（WPF 需 Windows 宿主，Linux 仅 `EnableWindowsTargeting` 编译）
- 未改业务代码
- 未修复 bug，仅静查与证据落盘
