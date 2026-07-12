# src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：管理本机 `KeyStats` 进程：枚举、收敛（单实例当前会话）、启动/停止/重启。
- 主要依赖：
  - `System.Diagnostics.Process`
  - `KeyStatsProcessInfo`（Models）
- 被谁使用：守护进程健康检查/确保采集进程存活逻辑

## 函数级结构化伪代码

### KeyStatsConvergencePlan（record）
#### 主构造
- 输入：KeepProcessId?、ProcessIdsToStop、ShouldStart
- 输出：不可变收敛计划
- 副作用：无

### KeyStatsProcessManager
#### 常量
- `ProcessName` = "KeyStats"；`ExeFileName` = "KeyStats.exe"

#### `BuildConvergencePlan(processes, currentSessionId)` static
- 输入：进程信息列表、当前会话 ID
- 输出：`KeyStatsConvergencePlan`
- 副作用：无
- 步骤：
  1. 筛当前用户且 Session 匹配的进程，按 PID 升序 → current。
  2. 其余 PID → foreign。
  3. current 为空 → Keep=null，Stop=foreign，ShouldStart=true。
  4. 否则 Keep=current[0]；Stop=foreign + current 多余实例；ShouldStart=false。
- 分支与异常：无
- 调用：无

#### `ListProcesses(currentSessionId)`
- 输入：当前会话 ID
- 输出：`KeyStatsProcessInfo` 列表
- 副作用：枚举系统进程并 Dispose
- 步骤：
  1. `GetProcessesByName(KeyStats)`。
  2. 读 SessionId，标记是否当前会话；退出中进程 catch 忽略。
  3. finally Dispose 每个 Process。
- 分支与异常：访问失败跳过
- 调用：`Process.GetProcessesByName`

#### `EnsureRunning(keyStatsExePath, currentSessionId)`
- 输入：exe 路径、会话 ID
- 输出：执行后的收敛计划
- 副作用：Kill 多余进程；必要时启动
- 步骤：
  1. List + BuildConvergencePlan。
  2. 对 plan 中每个 PID `TryStop`。
  3. ShouldStart → `StartInCurrentSession`。
  4. 返回 plan。
- 分支与异常：启动失败抛 FileNotFoundException
- 调用：`ListProcesses`、`BuildConvergencePlan`、`TryStop`、`StartInCurrentSession`

#### `Restart(keyStatsExePath, currentSessionId)`
- 输入：exe 路径、会话 ID
- 输出：void
- 副作用：停当前会话相关全部 KeyStats 后重启
- 步骤：
  1. 枚举后全部 TryStop。
  2. StartInCurrentSession。
- 分支与异常：同启动
- 调用：`ListProcesses`、`TryStop`、`StartInCurrentSession`

#### `TryStop(processId)` private static
- 输入：PID
- 输出：void
- 副作用：Kill 进程树，最多等 3s
- 步骤：GetProcessById → Kill(entireProcessTree) → WaitForExit(3000)；失败吞掉
- 分支与异常：best effort
- 调用：`Process.GetProcessById`

#### `StartInCurrentSession(keyStatsExePath)` private static
- 输入：exe 完整路径
- 输出：void
- 副作用：启动新进程
- 步骤：
  1. 文件不存在 → FileNotFoundException。
  2. Process.Start：UseShellExecute=true，工作目录为 exe 所在目录。
- 分支与异常：路径无效抛异常
- 调用：`Process.Start`

## 近逐行中文伪代码

1. 收敛计划：保留一个当前会话实例，杀掉外会话与多余实例，必要时启动。
2. 枚举 KeyStats 进程，按会话标记。
3. EnsureRunning 执行计划；Restart 先全停再启。
4. TryStop 杀进程树；Start 校验 exe 存在后 Shell 启动。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs",
      "label": "KeyStatsProcessManager",
      "path": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs", "to": "src/client-windows/Pim.Client.Core/Models", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs", "to": "System.Diagnostics.Process", "type": "calls" }
  ]
}
```
