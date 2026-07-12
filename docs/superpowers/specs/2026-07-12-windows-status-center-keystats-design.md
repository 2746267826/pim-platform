# Windows Status Center And KeyStats Reliability Design

> 状态：用户已批准完整设计（B+D）。  
> 实现分支建议：`codex/windows-status-center-keystats`（从最新 master + worktree）

## Purpose

把 Windows 客户端收回「本机守护程序 + 精简状态中心」产品形态，并修复 KeyStats 本机 API 持续返回全 0、守护程序无法真实反映采集健康的问题。

业务 UI 继续由 Web 承担，通过系统浏览器打开。Companion Shell / WebView2 代码本轮保留，但不再作为启动主路径或托盘主入口。

## Decisions Locked With User

- 产品形态：方案 **B + D**
  - B：守护程序 + 精简状态中心
  - D：优先修复 KeyStats 采集与诊断
- WebView2 / MainShell：代码先隐藏保留，不删除；默认不启动、不挂托盘主菜单
- 全 0 或持续不增长的 KeyStats 快照：**不上传** samples / legacy
- 如会话/单实例策略仍不足以恢复钩子计数，允许修改 KeyStats 源码仓库：`https://github.com/2746267826/keyStats`
- 实现前从 `master` 建立 `codex/` 分支与 worktree

## Goals

1. 启动后只出现托盘；主操作路径是状态中心，而不是内嵌 Web 大壳。
2. 状态中心能区分并展示：
   - KeyStats 进程不存在
   - KeyStats API 不可达
   - KeyStats API 可达但计数全 0 / 不增长
   - KeyStats 可用且计数在增长
   - 数据源正常但上传失败
3. KeyStats 在当前用户交互会话中可靠运行；键鼠活动后 1–2 分钟内本机 `/api/stats/` 累计可增长。
4. 健康的非全 0 样本可上传到 PIM；全 0 / 不增长样本不污染服务端。
5. 守护程序心跳写入真实 ActivityWatch / KeyStats / 上传状态，不再写死 `Unknown`。

## Non-Goals

- 重做完整 Windows 桌面业务 UI
- 把 Web 工作台重新做成默认内嵌主界面
- 改服务端 PC 业务语义（除非为诊断所必需的只读字段映射）
- Android 客户端
- 伪造历史 KeyStats 分钟数据

## Current Problems

### Product surface

当前 Windows 客户端在 `a42b1b0f feat: add windows companion shell` 后变成：

- 启动自动打开 `MainShellWindow`
- 托盘双击打开内嵌 Web 壳
- 托盘菜单堆了大量 Web 路由入口（今日/任务/日历/报告/Outlook/Data Center/审计/通知）
- 真正有用的 `StatusWindow`（PIM 守护程序状态）被边缘化

这些新加内容对「本机采集守护」帮助很小，且与「Web 是业务 UI」的既有方向冲突。

### KeyStats reliability

本机 2026-07-12 实测：

- `GET http://127.0.0.1:18080/api/stats/` 返回 200
- 响应字段全部为 0，短间隔复测无增长
- 存在两个 `KeyStats` 进程：
  - Session 0 实例
  - Session 1 用户会话实例
- 计划任务 `PimKeyStats` 存在，目标为 `C:\ProgramLocal\PIM\KeyStats.exe`
- PIM 守护程序会调用 `EnsureKeyStatsRunning()`
- `DaemonHeartbeatReporter.BuildHeartbeat(...)` 将 `ActivityWatchState` 与 `KeyStatsState` 写死为 `"Unknown"`

结论：问题不只是「API 没通」，而是「API 通但采集无效 / 会话隔离 / 多实例 / 健康状态失真」。

## Product Shape

### Primary UX

```text
托盘图标
├─ 双击 → 状态中心
├─ 打开状态中心
├─ 立即同步
├─ 回填最近 14 天 ActivityWatch
├─ 在浏览器打开 Web 工作台
├─ 登录...
└─ 退出
```

### Startup

- 守护程序启动后：
  - 显示托盘
  - 恢复登录或提示登录
  - 启动 AW / KeyStats 采集
  - 启动心跳
- **不**自动打开 `MainShellWindow`
- **不**自动打开浏览器

### Companion Shell retention policy

- 保留 `MainShellWindow` / `EmbeddedWebViewHost` 源码与项目依赖，避免本轮大删
- 不在托盘主菜单暴露
- 不在启动路径调用
- 相关测试改为断言「代码可存在，但不是启动/托盘主路径」

## Status Center Design

将现有 `StatusWindow` 升级为精简状态中心，分区如下。

### 1. 概览

显示：

- 账户状态（已登录用户名 / 未登录）
- PIM API 连通性
- 今日采集健康总评：`正常` / `部分异常` / `不可用`
- 一键「立即同步」

总评规则：

- AW 与 KeyStats 都 Available，且最近上传无错误 → 正常
- 任一数据源 StaleZero / 警告，或队列积压 → 部分异常
- 关键数据源 Missing / ApiUnreachable，或账户未登录导致无法上传 → 不可用或偏严重警告

### 2. 数据源

#### ActivityWatch

- 进程/API：`http://127.0.0.1:5600/api/0/buckets/`
- 最近上传时间 / 错误
- 队列数量

#### KeyStats

- 进程数量
- 各进程 SessionId（至少识别当前用户会话 vs Session 0）
- API：`http://127.0.0.1:18080/api/stats/`
- 今日 `keyPresses` / 总点击 / 是否在观察窗口内增长
- 诊断分级与建议动作

建议动作：

- 重启 KeyStats（当前用户会话）
- 打开 KeyStats 安装目录
- 复制诊断文本

### 3. 上传

- ActivityWatch 队列长度
- KeyStats 最近成功上传时间
- KeyStats 最近跳过原因（例如 `stale-zero`）
- AW / KeyStats 最近错误
- 最近一次心跳结果摘要

### 4. 设置

- 服务器 URL
- 开机自启动
- 查看日志
- 在浏览器打开 Web 工作台
- 登录 / 账户状态

文案默认简体中文；协议字段、API 名、日志键保持英文。

## KeyStats Reliability Design

### Health states

KeyStats 诊断分级：

| State | Meaning |
| --- | --- |
| `MissingProcess` | 无 KeyStats 进程 |
| `ApiUnreachable` | 进程可能存在，但本地 API 不可达 |
| `ApiOkButStaleZero` | API 200，但计数全 0 或观察窗口内不增长 |
| `Available` | API 可达，且出现非零活动或计数增长 |

上传失败不覆盖数据源状态，单独记在上传区。

### Process and session policy

1. 目标只保留 **当前用户交互会话** 中的一个 KeyStats 实例。
2. 发现 Session 0 或其他会话实例时：
   - 状态中心标记风险
   - 优先停止非目标实例
   - 确保当前用户会话实例存活
3. `EnsureKeyStatsRunning` / 重启动作优先使用当前用户上下文启动，不再依赖可能落入 Session 0 的路径作为唯一手段。
4. 计划任务 `PimKeyStats`：
   - 若继续使用，必须保证在用户登录会话中运行
   - 若无法保证，则降级为文档化的用户态启动，并在状态中心暴露原因

### Probe policy

状态中心与采集器共享同一套健康判定：

1. 检查进程与 Session
2. 请求 `/api/stats/`
3. 记录 counters：`keyPresses`、各点击、`mouseDistance` 等
4. 在短观察窗口内（实现计划可选 30–60 秒，或两次分钟采样对比）判断是否增长
5. 输出 health state + 人类可读建议

### Upload policy

- `Available`：允许上传 `/pc/keystats/samples` 与 legacy `/pc/keystats/upload`
- `ApiOkButStaleZero` / `MissingProcess` / `ApiUnreachable`：**不上传**
- 跳过上传时记录：
  - 本地 last error / skip reason
  - 日志
  - 状态中心「上传」区
- 修复后一旦出现真实非零/增长，恢复上传

说明：用户已确认「修好之后不存在长期全 0 样本」；因此跳过全 0 是保护措施，不是产品上接受假数据。

### Optional KeyStats source changes

若仅靠 PIM 侧启动/会话/单实例仍无法让计数增长，则修改 KeyStats 仓库，优先做：

1. 单实例互斥（named mutex），第二个实例退出或把焦点交给已有实例
2. 钩子安装失败时写入可见状态，而不是静默提供全 0 API
3. API 增加 health/hook 字段，例如：
   - `hookActive`
   - `sessionId` / `interactive`
   - `lastInputAt`
4. 避免在 Session 0 无交互环境中伪装成健康服务

PIM 侧应兼容旧 API；新 health 字段存在时优先使用。

## Heartbeat Design

`DaemonHeartbeatReporter.BuildHeartbeat` 不再写死：

```text
ActivityWatchState = Unknown
KeyStatsState = Unknown
```

改为由守护程序汇总真实状态：

- ActivityWatch：Available / Unavailable / Unknown
- KeyStats：映射到服务端已有 `DaemonSourceState` 时：
  - `Available` → Available
  - `MissingProcess` / `ApiUnreachable` / `ApiOkButStaleZero` → Unavailable
  - 尚未探测 → Unknown
- `lastSuccessfulUploadAt` / `lastAttemptedUploadAt` / `lastError`
- `statusJson` 可包含更细的本机诊断：
  - keyStatsDetailState
  - keyStatsProcessCount
  - keyStatsSessions
  - keyStatsSkipReason
  - awQueueCount

服务端若只消费粗粒度 source state，细字段放 `statusJson`，避免本轮强制大改 API 契约。

## Architecture

```text
TrayIcon
  └─ StatusCenterWindow (upgraded StatusWindow)
       ├─ Overview
       ├─ Sources (AW probe + KeyStats probe)
       ├─ Upload (collector + heartbeat facts)
       └─ Settings

Collectors
  ├─ AwCollectorService
  └─ KeyStatsCollectorService
       ├─ local API fetch
       ├─ health gate
       └─ conditional upload

DaemonHeartbeatReporter
  └─ real AW/KeyStats/upload summary → API

Optional retained
  └─ MainShellWindow + EmbeddedWebViewHost (not primary path)
```

### Suggested code boundaries

- `KeyStatsHealthProbe`：进程/会话/API/增长判定
- `KeyStatsProcessManager`：确保单实例、用户会话启动/重启
- `KeyStatsCollectorService`：只在健康门禁通过后上传
- `DaemonHeartbeatReporter`：接收真实状态快照
- `StatusWindow`：展示 4 区与动作
- `TrayIcon` / `App.xaml.cs`：主路径切换

保持与现有 daemon 风格一致：WPF 状态窗 + WinForms 托盘 + Core 服务。

## Data Flow

```text
KeyStats.exe (user session)
  → GET /api/stats/
  → HealthProbe
      ├─ Available → Collector upload → PIM API
      └─ not available → skip upload + local diagnosis

ActivityWatch
  → AwCollector → queue/upload → PIM API

Probes + collector facts
  → Status Center UI
  → Heartbeat → PIM API
```

## Error Handling

| Condition | User-facing summary | Action |
| --- | --- | --- |
| 无 KeyStats 进程 | 未运行 | 启动/重建用户态启动 |
| API 不通 | 未连接 | 检查端口、僵死进程、重启 |
| API 通但全 0 / 不增长 | 钩子或会话异常 | 结束 Session0 实例，重启用户会话实例；必要时改 KeyStats |
| 多实例 | 存在冲突实例 | 收敛到单实例 |
| 未登录 | 无法上传到 PIM | 引导登录 |
| 源正常上传失败 | 上传错误 | 保留源状态，显示 API 错误，支持手动同步 |
| AW 不可用 | ActivityWatch 未连接 | 提示启动 AW |

## Testing Strategy

### Unit / component

- KeyStats 健康分级：
  - missing process
  - api unreachable
  - stale zero
  - available on growth/non-zero
- Collector：
  - stale zero 不上传
  - available 上传
- Heartbeat：
  - 真实 source state 组装
  - 不再固定 Unknown
- App / Tray：
  - 启动不打开 MainShell
  - 双击打开状态中心
  - 托盘菜单精简
- Companion shell tests：
  - 允许 WebView2 代码存在
  - 断言其不是启动/托盘主路径

### Local acceptance

1. 启动守护程序：只有托盘，无大壳
2. 打开状态中心：4 区可用
3. 确认 KeyStats 仅当前用户会话单实例
4. 进行键鼠操作后 1–2 分钟：
   - `/api/stats/` 计数增长
   - 状态中心显示 Available
   - PIM 出现新的 keystats sample
5. 人为制造全 0 / 停进程：
   - 不上传
   - 状态中心分级正确
6. 浏览器打开 Web 工作台可用

### Verification commands

```powershell
dotnet test Pim.sln --filter "FullyQualifiedName~ClientWindows"
dotnet publish src/client-windows/Pim.Client.App/Pim.Client.App.csproj -c Release -o publish/PimDaemon -r win-x64 --self-contained true
```

若修改 KeyStats 源码，另按其仓库构建，并把产物纳入 Windows 发布/本地安装验证。

## Implementation Sequence

1. 从最新 `master` 创建实现分支与 worktree：
   - 建议分支名：`codex/windows-status-center-keystats`
2. 先写失败测试：健康分级、跳过全 0 上传、启动/托盘主路径
3. 实现 KeyStats process manager + health probe
4. 改造 collector 上传门禁与心跳真实状态
5. 升级 StatusWindow 为 4 区状态中心
6. 收敛 Tray / App 启动路径；保留 WebView2 代码但移出主路径
7. 本机验证 KeyStats 增长；不足则改 keyStats 仓库并回归
8. 跑 ClientWindows 测试与 publish
9. 开 PR，等待 GitHub Actions

## Success Criteria

- 用户感知上，Windows 客户端重新成为「守护 + 诊断」，而不是无用 Web 套壳
- KeyStats 本机计数在真实键鼠后可增长
- 全 0 数据不会上传污染服务端
- 状态中心能解释问题并提供重启/诊断动作
- 心跳与状态中心对 AW/KeyStats 状态一致且真实
- WebView2 代码仍在仓库中，但不干扰主路径

## Open Implementation Notes

- `DaemonSourceState` 是否扩展细粒度枚举：本设计默认不扩展服务端枚举，细状态进 `statusJson`；若实现中发现 Web 状态页强依赖更细枚举，再做最小增量
- KeyStats 源码修改触发条件：用户会话单实例收敛后，若本机键鼠仍不能让 `/api/stats/` 增长，则必须进入 keyStats 仓库修复
- 状态中心 UI 可用 Tab 或单页分区实现；优先少翻页、信息密度高、中文文案清晰

## Spec Self-Review

- Placeholder scan：无 TBD/TODO
- Internal consistency：B+D、跳过全 0、保留 WebView2、可改 keyStats 源码一致
- Scope：单实现计划可覆盖（PIM Windows + 条件性 keyStats）
- Ambiguity：健康观察窗口长度留给实现计划选 30–60s 或跨分钟对比；服务端枚举默认不扩展
