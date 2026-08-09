# src/client-windows/Pim.Client.App/TrayIcon.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：系统托盘图标与菜单：状态中心、立即同步、AW 回填、打开 Web 工作台、登录、退出。
- 主要依赖：`NotifyIcon`、`DaemonConfig`、`ApiClient`、`AwCollectorService`、`KeyStatsCollectorService`、`StatusWindow`、`LoginWindow`、`ClientDefaults`
- 被谁使用：Windows 客户端启动路径（App/Startup）

## 函数级结构化伪代码

### TrayIcon
#### Show()
- 输入：无
- 输出：无
- 副作用：创建/显示托盘与右键菜单
- 步骤：
  1. 若已 Visible 则 return。
  2. Dispose 旧实例；新建 `NotifyIcon`（图标、提示、菜单）。
  3. 菜单项：打开状态中心 / 分隔 / 立即同步 / 回填 14 天 AW / 打开 Web 工作台 / 分隔 / 登录 / 退出。
  4. 双击 → `ShowStatusWindow`。
- 分支与异常：无
- 调用：`LoadIcon`、各处理函数

#### LoadIcon() [static private]
- 输入：无
- 输出：`Icon`
- 副作用：读 WPF 资源流
- 步骤：尝试 `pack://.../app.ico`；失败或无流 → `SystemIcons.Application`
- 分支与异常：`IOException` 吞掉
- 调用：`Application.GetResourceStream`

#### ShowStatusWindow() [static private]
- 输入：无
- 输出：无
- 副作用：激活已有或新建 `StatusWindow`
- 步骤：查 `Application.Current.Windows` 中 `StatusWindow`；有则 Activate；否则 `new().Show()`
- 分支与异常：无
- 调用：`StatusWindow`

#### OpenWebWorkbench() [static private]
- 输入：无
- 输出：无
- 副作用：用默认浏览器打开 `{root}/today`
- 步骤：
  1. `DaemonConfig.Load` 取 ServerUrl；空白则默认；`ApiClient.NormalizeServerUrl`。
  2. 去掉尾 `/`；若以 `/api/v1` 结尾则裁掉。
  3. 再空白则回退默认；`Process.Start` 打开 `{root}/today`。
  4. 异常 MessageBox。
- 分支与异常：catch 弹错误框
- 调用：`DaemonConfig.Load`、`ApiClient.NormalizeServerUrl`

#### ShowLogin() [static private]
- 输入：无
- 输出：无
- 副作用：模态 `LoginWindow`
- 步骤：`new LoginWindow().ShowDialog()`
- 分支与异常：无
- 调用：`LoginWindow`

#### TriggerSyncAsync() [static private]
- 输入：无
- 输出：Task
- 副作用：并行触发 AW 与 KeyStats `SyncNowAsync`；弹成功/警告/失败
- 步骤：
  1. DI 取两采集器；`Task.WhenAll(SyncNowAsync)`。
  2. `BuildUploadErrorMessage`；有错误警告框，否则“同步完成”。
  3. catch 失败框。
- 分支与异常：catch Exception
- 调用：`AwCollectorService`、`KeyStatsCollectorService`

#### TriggerAwBackfillAsync() [static private]
- 输入：无
- 输出：Task
- 副作用：`BackfillAsync(now-14d, now)`；弹结果
- 步骤：DI 取 AW；回填；看 `LastUploadError` 警告或成功；catch 失败框
- 分支与异常：catch Exception
- 调用：`AwCollectorService.BackfillAsync`

#### BuildUploadErrorMessage(string? awError, string? keyStatsError) [static private]
- 输入：两路上传错误
- 输出：合并消息或 null
- 副作用：无
- 步骤：非空错误加前缀列表；空则 null，否则 `\n` 拼接
- 分支与异常：无
- 调用：无

#### ConfirmAndExit() [private]
- 输入：无
- 输出：无
- 副作用：确认后 Dispose 托盘并 `Shutdown`
- 步骤：YesNo 确认；Yes → Dispose + Application.Current.Shutdown
- 分支与异常：无
- 调用：`Dispose`

#### Dispose()
- 输入：无
- 输出：无
- 副作用：释放 NotifyIcon
- 步骤：`_notifyIcon?.Dispose()`；置 null
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 字段 `_notifyIcon`；`Show` 幂等创建托盘与中文菜单。
2. 图标优先 pack 资源，失败用系统图标。
3. 状态窗单例激活；登录模态；退出二次确认后关应用。
4. 打开工作台：从配置规范化服务根 URL，拼 `/today` 用 shell 打开。
5. 立即同步：AW+KeyStats 并行；汇总 LastUploadError。
6. 回填：UTC 最近 14 天 `BackfillAsync`。
7. `Dispose` 清理托盘。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/TrayIcon.cs",
      "label": "TrayIcon",
      "path": "src/client-windows/Pim.Client.App/TrayIcon.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/TrayIcon.cs.md",
      "layer": "client-windows",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.App/DaemonConfig.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App", "to": "src/client-windows/Pim.Client.App/TrayIcon.cs", "type": "depends_on" }
  ]
}
```
