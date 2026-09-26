# src/client-windows/Pim.Client.App/StatusWindow.xaml.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 客户端状态中心窗口——探测 API/ActivityWatch/KeyStats、展示队列与上传错误、配置服务器 URL/自启、手动同步与诊断复制。
- 主要依赖：`ApiClient`、`AuthService`、`AwCollectorService`、`KeyStatsCollectorService`、`KeyStatsProcessManager`、`DaemonConfig`、`AutoStartManager`、`StatusCenterEvaluator`、`HttpClient`、WPF
- 被谁使用：托盘/主壳打开状态窗口

## 函数级结构化伪代码

### StatusWindow
#### StatusWindow()
- 输入：无
- 输出：实例
- 副作用：InitializeComponent；DI 解析服务；填充 ServerUrl/AutoStart；RefreshAll
- 步骤：
  1. 从 `App.Services` 取 Api/Auth/Aw/KeyStats/ProcessManager
  2. `DaemonConfig.Load` → ServerUrlBox、AutoStartCheckBox
  3. `RefreshAll`
- 调用：DI、`DaemonConfig`、`RefreshAll`

#### void RefreshAll() / void QueueRefreshStatus() / void RefreshAuth()
- 输入：无
- 输出：无
- 副作用：更新账号 UI；异步刷新状态（失败写 Debug）
- 步骤：RefreshAuth + fire-and-forget RefreshStatusAsync；已登录显示用户名与服务器，否则显示登录按钮
- 调用：`RefreshStatusAsync`、`_authService`

#### async Task RefreshStatusAsync()
- 输入：无
- 输出：无
- 副作用：更新全部状态绑定字段与诊断报告缓存
- 步骤：
  1. 时间戳与 sessionId
  2. `BuildApiProbeAsync` → ApiConnectivityText、`_apiOk`
  3. 探测 `http://127.0.0.1:5600/api/0/buckets/` → AW Available/Unavailable 文案
  4. `ListProcesses` + KeyStats `LastHealth` → 摘要/详情/`_ksState`/`_ksSkipReason`
  5. AW 队列计数、KeyStats 上传行、跳过原因、最近错误列表
  6. `StatusCenterEvaluator.Rate` → 整体健康中文
  7. `BuildDiagnosticsReport` 缓存
- 调用：`ProbeEndpointAsync`、`FormatProcesses`、`FormatUploadLine`、`StatusCenterEvaluator`

#### async Task\<(...)\> BuildApiProbeAsync() / static ProbeEndpointAsync(url)
- 输入：无 / URL
- 输出：Ok、Summary/StatusLine/Message 元组
- 副作用：HTTP GET（3s 超时静态 HttpClient）
- 步骤：从 CurrentBaseUrl 去掉 `/api/v1` 拼 `/health`；失败用 DaemonConfig.ServerUrl；GET 成功判 IsSuccessStatusCode
- 分支：异常 → Exception 状态
- 调用：`Http.GetAsync`、`DaemonConfig.Load`

#### FormatProcesses / FormatUploadLine / FormatAgo / BuildDiagnosticsReport
- 输入：进程列表、上传时间/错误、诊断上下文
- 输出：格式化字符串
- 副作用：无
- 步骤：拼接 pid/session；「最近上传」+ 相对时间；多行诊断文本
- 调用：无

#### UI 事件
##### OnRefresh / OnSaveServerUrl / OnLogin / OnManualSync / OnRestartKeyStats / OnOpenInstallDir / OnCopyDiagnostics / OnOpenWebBrowser / OnViewLogs / OnClose / OnAutoStartToggled
- 输入：RoutedEventArgs
- 输出：无
- 副作用：配置持久化、打开窗口/进程、剪贴板、MessageBox、同步采集
- 步骤要点：
  1. 保存 URL：Normalize → Uri 校验 → SetBaseUrl/Auth/DaemonConfig.Save
  2. 登录：`LoginWindow.ShowDialog` 成功则刷新
  3. 手动同步：`WhenAll(Aw.SyncNow, KeyStats.SyncNow)` 后刷新并报告上传错误
  4. 重启 KeyStats：路径下 exe 存在则 `Restart`
  5. 打开安装目录 explorer；复制诊断；打开 `{root}/today` Web
  6. 日志 notepad 或显示路径；关闭窗口
  7. 自启：`AutoStartManager.Set` + config.AutoStart 保存
- 调用：`ApiClient.NormalizeServerUrl`、`AutoStartManager`、`LoginWindow`、采集服务

#### static BuildUploadErrorMessage(awError, keyStatsError)
- 输入：两路上传错误
- 输出：合并消息或 null
- 步骤：非空白行加入列表，空则 null

## 近逐行中文伪代码

1. 静态 HttpClient 超时 3 秒；字段保存服务与 AW/KS/API 状态缓存
2. 构造：DI + 配置 UI + RefreshAll
3. RefreshAuth：登录态切换摘要与 Login 按钮可见性
4. QueueRefreshStatus：ContinueWith 仅故障时 Debug 异常
5. RefreshStatusAsync：API 探测 → AW 5600 → KeyStats 健康/进程 → 队列/上传/错误 → Evaluator 整体 → 诊断报告
6. BuildApiProbeAsync：剥 `/api/v1` 拼 health
7. ProbeEndpointAsync：GET，返回状态码行或异常类型
8. 格式化辅助：进程、上传时间相对、诊断多行
9. OnSaveServerUrl：校验 http(s) 绝对 URI 后持久化
10. OnManualSync：双采集 SyncNow，有错误 Warning
11. OnRestartKeyStats：BaseDirectory + ExeFileName
12. OnOpenWebBrowser：归一化根 URL + `/today`
13. OnAutoStartToggled：注册表 + DaemonConfig
14. OnCopyDiagnostics：剪贴板；空则提示先刷新

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs",
      "label": "StatusWindow",
      "path": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/StatusWindow.xaml.cs.md",
      "layer": "client-windows",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/AutoStartManager.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "http://127.0.0.1:5600", "type": "http" }
  ]
}
```
