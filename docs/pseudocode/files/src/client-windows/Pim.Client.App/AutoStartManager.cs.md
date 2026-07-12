# src/client-windows/Pim.Client.App/AutoStartManager.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：通过 HKCU\Run 注册表项管理 Windows 开机自启（值名 `PIM`）。
- 主要依赖：`Microsoft.Win32.Registry`、`Logger`、`Environment.ProcessPath`
- 被谁使用：`StatusWindow` 自启勾选、应用启动配置同步

## 函数级结构化伪代码

### AutoStartManager
#### bool IsRegistered { get }
- 输入：无
- 输出：当前可执行路径是否已注册为自启
- 副作用：读注册表
- 步骤：
  1. 打开 `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`
  2. 读取值 `PIM` 为字符串
  3. 非空且与 `ExecutablePath` 忽略大小写相等 → true，否则 false
  4. 任意异常 → false
- 分支与异常：吞掉异常返回 false
- 调用：`Registry.CurrentUser.OpenSubKey`、`ExecutablePath`

#### void Set(bool enabled)
- 输入：是否启用自启
- 输出：无
- 副作用：写/删注册表 Run 值；失败时 `Logger.Warn`
- 步骤：
  1. 以可写方式打开 Run 键
  2. enabled=true → `SetValue("PIM", ExecutablePath)`
  3. enabled=false → `DeleteValue("PIM", throwOnMissingValue: false)`
  4. 捕获异常并记录 Warn
- 分支与异常：key 为 null 时静默跳过写入
- 调用：`Logger.Warn`、`ExecutablePath`

#### string ExecutablePath { get }
- 输入：无
- 输出：当前进程路径（含空格时加引号）
- 副作用：无
- 步骤：
  1. `Environment.ProcessPath` 或空串
  2. 含空格则包裹双引号
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 `Microsoft.Win32`、`Pim.Client.App.Services`
2. 静态类 `AutoStartManager`
3. 常量：`RegistryKeyPath` = Run 路径；`ValueName` = `"PIM"`
4. `IsRegistered`：try 打开 Run → 取 `PIM` 字符串 → 与 `ExecutablePath` 忽略大小写比较
5. catch：返回 false
6. `Set(enabled)`：try 可写打开 Run
7. 若 enabled：写入 `PIM` = `ExecutablePath`
8. 否则：删除 `PIM`（缺失不抛）
9. catch：`Logger.Warn` 说明启用/禁用失败与消息
10. `ExecutablePath`：取 `ProcessPath`，含空格则 `"path"`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/AutoStartManager.cs",
      "label": "AutoStartManager",
      "path": "src/client-windows/Pim.Client.App/AutoStartManager.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/AutoStartManager.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/AutoStartManager.cs", "to": "Microsoft.Win32.Registry", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/AutoStartManager.cs", "to": "src/client-windows/Pim.Client.App/Services/Logger.cs", "type": "calls" }
  ]
}
```
