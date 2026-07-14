# KeyStats 一键修复设计

Date: 2026-07-14  
Status: Approved for planning  
Branch: `codex/keystats-one-click-fix`

## 目标

Windows 客户端「PIM 状态中心 → 数据源」页在 KeyStats 异常时，不仅展示诊断，还要：

1. 按诊断原因给出**中文修复建议**
2. 提供主按钮**一键修复**，自动执行可恢复路径
3. 多数情况**普通权限**完成；仅在清理 Session 0 / 跨会话实例访问被拒绝时，经用户确认后**只对独立脚本提权**
4. 修复后做**两阶段复检**并在页面展示结果

成功标准（用户已确认）：

- 阶段 1：进程收敛（无跨会话残留、当前会话有 KeyStats）+ 本地 API 可达
- 阶段 2：短等后探测计数；有增长则标成功；仍为 0 则黄提示「请敲几下键盘后点刷新」，不算硬失败

## 背景与根因

已有运维结论（见 `docs/operations/windows-keystats-session-fix.md`）：

- 本地 KeyStats API 可返回 HTTP 200，但计数全 0
- 常见原因：Session 0 僵尸实例占用端口，用户输入在 Session 1，钩子挂在错误会话
- 状态中心已能诊断 `ApiOkButStaleZero` / `stale-zero` / `HasForeignSession`，并有「重启 KeyStats」
- 缺口：缺少面向用户的修复说明，以及覆盖「普通 kill 失败 → 提权清理」的完整编排与结果反馈

截图典型状态：

- Summary：`KeyStats API 可达但计数全 0，且存在非当前会话实例`
- `DetailState: ApiOkButStaleZero`，`SkipReason: stale-zero`
- 进程：`session=0` + `session=1` 并存

## 方案选择

采用 **方案 A：进程内修复 + 按需提权脚本**。

| 方案 | 说明 | 结论 |
|------|------|------|
| A | 普通权限先收敛/重启；Session 0 杀不掉再确认后 `runas` 独立脚本 | **采用** |
| B | 一键修复始终 UAC | 打扰大，否决 |
| C | 仅文案增强 | 不能真正清 Session 0，否决 |

约束：

- 不提权整个 WPF 客户端
- 不改 KeyStats 上游本体（Session 0 拒绝启动的修复在 keyStats 仓库）
- 不改服务端心跳协议
- 保留「重启 KeyStats」作为简单回退

## UI 设计

位置：`StatusWindow` 数据源 Tab · KeyStats 区块。

### 布局顺序

1. 标题 `KeyStats`
2. 摘要 `KeyStatsSummaryText`（现有）
3. 诊断详情 `KeyStatsDetailText`（现有）
4. **修复建议** 区域（新增，异常时高亮；完全正常可显示「运行正常」或折叠为空）
5. 按钮行（从左到右）：
   - **一键修复**（主按钮）
   - 重启 KeyStats（次要，行为保持现状）
   - 打开安装目录
   - 复制诊断
6. **修复结果** 区域（新增；无操作时可为占位或隐藏，有结果时展示两阶段文案）

### 修复建议文案映射

由纯函数根据最近健康结果映射（便于单测），输入建议：`DetailState`、`SkipReason`、`HasForeignSessionProcess`、`CanUpload`。

| 条件 | 建议摘要（中文） |
|------|------------------|
| `stale-zero` + 跨会话 | 检测到非当前会话（常为 Session 0）实例可能占用本地 API。建议使用「一键修复」：结束非当前会话实例 → 在当前会话重启 KeyStats → 自动复检。 |
| `stale-zero` 无跨会话 | API 可达但计数全 0 或不增长。建议一键修复重启后，操作键鼠再刷新；若仍为 0，复制诊断。 |
| `missing-process` | KeyStats 进程未运行。一键修复将在当前会话启动 KeyStats。 |
| `api-unreachable` | KeyStats API 不可达。一键修复将收敛进程并重启；若仍失败，请复制诊断。 |
| 可用但有跨会话 | KeyStats 可用，但存在额外会话实例。一键修复可收敛为当前会话单实例。 |
| 完全正常 | 运行正常，无需修复。 |

### 修复结果文案（示例）

- 阶段 1 成功：`已结束 N 个非当前会话进程；当前会话 KeyStats 已运行；API 可达。`
- 阶段 1 需提权：`普通权限无法结束 Session 0 进程。已请求管理员权限运行修复脚本…`
- 用户取消 UAC：`已取消管理员授权，未完成跨会话清理。`
- 阶段 2 成功：`等待输入后计数开始增长 — 已恢复可用。`
- 阶段 2 黄提示：`进程与 API 已正常，但计数仍为 0。请敲几下键盘/移动鼠标后点「刷新」。`

## 修复编排

### 组件

| 组件 | 职责 |
|------|------|
| `KeyStatsFixAdvisor`（Core，新建） | 建议文案映射；是否需要 elevate 的决策辅助（基于 kill 失败标记） |
| `KeyStatsOneClickFixService`（Core，新建） | 编排：普通收敛 → 可选提权脚本 → 两阶段复检 → 结构化结果 |
| `KeyStatsProcessManager`（现有） | `ListProcesses` / `EnsureRunning` / `Restart` / `TryStop`；必要时暴露 stop 失败信息 |
| `fix-keystats-session.ps1`（新建脚本） | 提权路径：强制结束全部 KeyStats → 启动指定 exe → 打印进程 Session 摘要 |
| `StatusWindow`（App） | 按钮、建议/结果 UI、调用编排、禁用重复点击 |

### 一键修复主流程

```
OnOneClickFix:
  disable button, result = "修复中…"
  sessionId = current process SessionId
  exe = BaseDirectory/KeyStats.exe
  script = BaseDirectory/fix-keystats-session.ps1  (or scripts path packaged next to exe)

  if exe missing -> error dialog, abort

  // Phase 0: normal-privilege converge
  plan = BuildConvergencePlan(ListProcesses, sessionId)
  stopFailures = []
  for each pid in plan.ProcessIdsToStop:
    if !TryStop(pid): stopFailures.Add(pid)
  if plan.ShouldStart or current session missing after stops:
    StartInCurrentSession(exe)

  // Elevate branch
  if stopFailures not empty OR still HasForeignSession after re-list:
    confirm dialog (中文说明为何需要管理员、只提权脚本)
    if user cancels:
      write result cancel message; refresh; re-enable; return
    run elevated script with -KeyStatsExe <exe>
    if UAC cancel or non-zero exit:
      write failure + script output snippet; refresh; re-enable; return

  // Phase 1 recheck (~1-2s settle)
  recheck processes + HTTP GET KeyStats API
  write phase-1 result

  // Phase 2 counter recheck (~8s)
  snapshot1, wait ~8s, snapshot2
  if grew or non-zero: success green
  else: yellow "请输入后刷新"

  RefreshAll(); re-enable button
```

### 普通权限 vs 提权

- **默认**：复用 `KeyStatsProcessManager` 收敛逻辑（结束非当前会话 + 多余当前会话实例；必要时启动）
- **提权触发条件**（满足其一）：
  1. 普通 `Kill` 对目标 PID 失败（访问被拒绝等）
  2. 收敛后仍检测到非当前会话 KeyStats 进程
- **提权方式**：`ProcessStartInfo` 启动  
  `powershell.exe -NoProfile -ExecutionPolicy Bypass -File fix-keystats-session.ps1 -KeyStatsExe <path>`  
  且 `Verb = "runas"`，`UseShellExecute = true`
- **不**对 `Pim.Client.App.exe` 请求管理员

### 独立脚本契约

路径（源码）：`scripts/fix-keystats-session.ps1`  
发布：复制到 Windows 客户端安装目录（与 `KeyStats.exe` 同级），保证运行时 `BaseDirectory` 可找到。

参数：

- `-KeyStatsExe`（必填）：`KeyStats.exe` 绝对路径

行为：

1. `taskkill /F /IM KeyStats.exe /T`（best effort，记录输出）
2. `Start-Process` 启动 `-KeyStatsExe`（当前提权令牌下的交互桌面；脚本以 UAC 提升运行，目标是清掉 Session 0 后由用户态客户端或脚本再拉起当前会话实例）
3. 打印：结束结果、启动结果、`Get-Process KeyStats | Id,SessionId`
4. 退出码：0 = 至少当前可见进程列表可接受（无残留或仅当前会话）；非 0 = 无法启动 exe 或仍异常

实现注意：

- 脚本必须可重复执行
- 不依赖 PIM API
- 输出 UTF-8 友好，便于客户端截取展示

补充：若提权后 `Start-Process` 落在提升会话而非用户会话，则以「杀光全部 KeyStats」为脚本主责，启动当前会话实例由客户端在脚本返回后用**普通权限**再 `StartInCurrentSession` 一次（推荐默认）。这样脚本提权面最小：**只负责强制清理**，启动仍走用户会话。

**修订后的脚本主责（最终）：**

1. 强制结束所有 `KeyStats.exe`
2. 打印清理后进程列表
3. 退出 0 表示清理命令已执行且清理后无 KeyStats 残留（或仅记录无法结束的 PID）

客户端在脚本成功返回后：普通权限 `StartInCurrentSession(exe)`，再进入两阶段复检。

### 「重启 KeyStats」保持不变

- 调用现有 `Restart`：尽量结束可见实例 + 当前会话启动
- 不做建议文案编排、不做两阶段复检、不主动弹 UAC 脚本
- 作为高级/快速回退

## 错误处理

| 情况 | 处理 |
|------|------|
| 缺少 `KeyStats.exe` | 错误框 + 安装目录路径 |
| 缺少修复脚本且需要 elevate | 错误框提示脚本缺失，建议重装/复制诊断 |
| 普通 kill 部分失败 | 进入提权确认，不静默忽略 |
| 用户取消 UAC | 结果区明确「已取消」 |
| 脚本非 0 | 展示退出码 + 输出摘要 + 引导复制诊断 |
| 阶段 2 仍 0 | 黄提示，不阻断；引导输入后刷新 |
| 修复进行中 | 禁用一键修复（及可选禁用重启）防重入；`finally` 恢复 |

## 数据流

```
StatusWindow
  -> KeyStatsFixAdvisor.BuildSuggestion(health) -> 建议 UI
  -> KeyStatsOneClickFixService.RunAsync(...)
       -> KeyStatsProcessManager (normal converge)
       -> optional elevated fix-keystats-session.ps1
       -> process re-list + HTTP stats probe (phase 1/2)
       -> KeyStatsFixResult (structured)
  -> 结果 UI + RefreshStatusAsync
```

`KeyStatsFixResult` 建议字段：

- `Succeeded` / `Partial` / `Failed` / `Cancelled`
- `Phase1MessageZh` / `Phase2MessageZh`
- `StoppedProcessIds` / `FailedStopProcessIds`
- `ElevatedUsed` / `ScriptExitCode` / `ScriptOutputExcerpt`
- `ApiReachable` / `CountersGrew`

## 测试计划

### 单元测试

- `KeyStatsFixAdvisor`：上表各条件文案与「是否显示建议」
- 提权决策：`FailedStops` 或仍有 foreign → `NeedsElevation == true`
- 现有 `KeyStatsProcessManager` / `KeyStatsHealthProbe` 回归

### UI 契约

- `StatusWindow.xaml` 含「一键修复」「修复建议」相关元素名或文案
- `StatusWindow.xaml.cs` 含一键修复处理与脚本文件名引用
- 扩展 `WindowsStatusCenterTests`

### 手动验证

1. 复现 Session 0 + Session 1，状态为 stale-zero
2. 点一键修复：若普通权限可杀 Session 0 → 无 UAC，收敛后计数可增长
3. 若普通权限杀不掉 → 确认框 → UAC → 脚本清理 → 客户端重启用户态实例 → 阶段 2 通过
4. 取消 UAC → 结果区说明未完成
5. 正常状态 → 建议为运行正常；一键修复应可安全 no-op 或轻量收敛

## 发布与打包

- 将 `scripts/fix-keystats-session.ps1` 纳入 Windows 客户端发布产物（与 `KeyStats.exe` 同目录）
- 更新 `docs/operations/windows-keystats-session-fix.md`：指向状态中心一键修复与脚本路径
- 不修改 `.github/workflows/*`，除非现有打包脚本必须列出新文件（若 publish 脚本用通配符复制 `scripts/` 则只需确认路径）

## 范围外

- 修改 KeyStats 上游仓库代码
- 服务端/Web 状态中心对等功能
- 自动静默 UAC（无用户确认）
- 将整个客户端改为 requireAdministrator

## 验收

- [ ] 异常态显示中文修复建议
- [ ] 存在主按钮「一键修复」
- [ ] 普通权限路径可收敛并可两阶段反馈
- [ ] Session 0 杀失败时确认后仅脚本提权
- [ ] 取消 UAC / 脚本失败有明确中文结果
- [ ] 「重启 KeyStats」行为未破坏
- [ ] 相关单测与 UI 契约测试通过
