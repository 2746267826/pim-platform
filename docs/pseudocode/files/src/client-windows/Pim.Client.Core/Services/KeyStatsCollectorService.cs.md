# src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：后台周期从本机 KeyStats（默认 127.0.0.1:18080）拉快照，经健康探测决定是否上传；双通道 POST 样本与遗留接口；暴露上次上传/健康/跳过原因；支持立即同步与释放。
- 主要依赖：`HttpClient`、`ApiClient`、`KeyStatsProcessManager`、`KeyStatsHealthProbe`、`KeyStatsCounterSnapshot`、`ApiResponse`、`PeriodicTimer`
- 被谁使用：`Startup` DI 单例；`App.xaml.cs` 启动；`MainShellWindow`/`StatusWindow`/`TrayIcon` 展示与手动同步

## 函数级结构化伪代码

### KeyStatsCollectorService
#### 静态 KeyStatsBase / ShouldUpload(health)
- 输入：环境变量 `KEYSTATS_BASE_URL` 或默认 URL；health 结果
- 输出：基址字符串；是否 `health.CanUpload`
- 副作用：无
- 步骤：环境变量优先
- 分支与异常：无
- 调用：无

#### 属性 LastUploadTime / LastUploadError / LastHealth / LastSkipReason / Log
- 输入：无
- 输出：锁保护的状态；Log 可注入 Action
- 副作用：无
- 步骤：lock LockObj 读字段
- 分支与异常：无
- 调用：无

#### KeyStatsCollectorService(api, processManager?)
- 输入：ApiClient；可选进程管理器
- 输出：实例
- 副作用：创建指向 KeyStatsBase 的 HttpClient（5s 超时）
- 步骤：processManager 默认 new；_cts 新建
- 分支与异常：无
- 调用：`KeyStatsProcessManager`

#### void Start()
- 输入：无
- 输出：无
- 副作用：后台 Task 每分钟 CollectAndUpload；取消吞掉
- 步骤：立即执行一次；PeriodicTimer 1 分钟循环
- 分支与异常：OperationCanceledException 忽略
- 调用：`CollectAndUploadAsync`

#### Task SyncNowAsync()
- 输入：无
- 输出：无
- 副作用：手动触发一次采集上传
- 步骤：try Collect；catch 记 Log
- 分支与异常：异常仅日志
- 调用：`CollectAndUploadAsync`

#### CollectAndUploadAsync()（私有）
- 输入：无
- 输出：无
- 副作用：读 KeyStats API；更新健康与 previousSnapshot；可能双 POST；更新错误/时间
- 步骤：
  1. GET `/api/stats/` → KeyStatsSnapshot；null 则 return
  2. 构造 CounterSnapshot；取 previous；当前 SessionId 与进程列表
  3. HealthProbe.Evaluate；写 previous/health/skip
  4. !ShouldUpload → 记 SummaryZh 并 return
  5. 构造 Sample payload；TryPost samples 与 legacy upload
  6. 任一成功更新 LastUploadTime；BuildUploadHealthMessage
- 分支与异常：Http/Timeout 与其它 Exception 写 LastUploadError
- 调用：`_keyStats.GetFromJsonAsync`、`_processManager`、`KeyStatsHealthProbe`、`TryPostAsync`

#### TryPostAsync(endpoint, payload)
- 输入：相对 API 路径与 body
- 输出：ApiResponse\<string\>?（失败 null）
- 副作用：可能网络 POST
- 步骤：`_api.PostAsync`；Http/Timeout catch 日志返回 null
- 分支与异常：网络类吞掉
- 调用：`ApiClient.PostAsync`

#### FormatUploadResult / BuildUploadHealthMessage
- 输入：结果可空；sampleOk/legacyOk 布尔
- 输出：`failed`/`ok`；健康消息或 null（双成功）
- 副作用：无
- 步骤：四路 switch 文案
- 分支与异常：无
- 调用：无

#### void Dispose()
- 输入：无
- 输出：无
- 副作用：Cancel CTS；Dispose HttpClient
- 步骤：取消后台循环
- 分支与异常：无
- 调用：`CancellationTokenSource`、`HttpClient`

### 嵌套类型 KeyStatsSnapshot / KeyStatsAppStats / KeystatsSampleUploadPayload
- 输入：KeyStats JSON 字段映射；样本上传 record 含 peakKPS/peakCPS 等
- 输出：反序列化/序列化模型
- 副作用：无
- 步骤：DeviceId 默认 MachineName；Date 默认 Now O 格式
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 Diagnostics、Http.Json、Json.Serialization、Models
2. 基址 KEYSTATS_BASE_URL 或 18080；字段含 api、processManager、cts、锁、上传/健康状态、previousSnapshot
3. 构造：HttpClient BaseAddress+5s 超时
4. Start：Task.Run + PeriodicTimer 每分钟 Collect
5. SyncNowAsync：手动 Collect，异常打 Log
6. Collect：GET stats；构 CounterSnapshot；进程列表 + HealthProbe
7. 不可上传则 SkipReason/SummaryZh 返回
8. 样本 POST `/pc/keystats/samples` 与遗留 `/pc/keystats/upload`
9. 更新 LastUploadTime/Error；Format/Build 消息
10. TryPost 捕获网络异常返回 null
11. Dispose 取消与释放
12. 嵌套 Snapshot/AppStats/SamplePayload 定义 JSON 形状

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs",
      "label": "KeyStatsCollectorService",
      "path": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Models/AuthDtos.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "http://127.0.0.1:18080/api/stats/", "type": "http" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "/pc/keystats/samples", "type": "http" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "/pc/keystats/upload", "type": "http" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "type": "calls" }
  ]
}
```
