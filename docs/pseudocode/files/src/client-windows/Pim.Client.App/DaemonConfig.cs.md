# src/client-windows/Pim.Client.App/DaemonConfig.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 守护进程本地配置（ServerUrl、AutoStart）的读写，落盘 `%LocalAppData%/PIM/config.json`。
- 主要依赖：`System.Text.Json`、`ClientDefaults.DefaultServerUrl`
- 被谁使用：`TrayIcon`、启动/登录相关流程

## 函数级结构化伪代码

### DaemonConfig
#### 属性 ServerUrl / AutoStart
- 输入/输出：`ServerUrl` 默认 `ClientDefaults.DefaultServerUrl`；`AutoStart` 默认 true
- 副作用：无
- 步骤：纯属性默认值
- 分支与异常：无
- 调用：无

#### Load() [static]
- 输入：无（读固定路径）
- 输出：`DaemonConfig` 实例
- 副作用：读本地文件
- 步骤：
  1. 若 `config.json` 存在：读全文 → `JsonSerializer.Deserialize`；null 则 `new()`。
  2. 任意异常吞掉，返回 `new()`。
  3. 文件不存在也返回 `new()`。
- 分支与异常：catch 空处理
- 调用：`File.Exists`/`ReadAllText`/`JsonSerializer.Deserialize`

#### Save()
- 输入：当前实例
- 输出：无
- 副作用：创建目录并写 `config.json`
- 步骤：
  1. `Directory.CreateDirectory(Dir)`。
  2. 序列化 this 写 `FilePath`。
  3. 异常吞掉。
- 分支与异常：catch 空处理
- 调用：`CreateDirectory`/`WriteAllText`/`Serialize`

## 近逐行中文伪代码

1. 类字段：`Dir=%LocalAppData%/PIM`，`FilePath=Dir/config.json`。
2. `ServerUrl`/`AutoStart` 带默认值。
3. `Load`：存在则反序列化，失败或缺失则新实例。
4. `Save`：确保目录后序列化写入，失败静默。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/DaemonConfig.cs",
      "label": "DaemonConfig",
      "path": "src/client-windows/Pim.Client.App/DaemonConfig.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/DaemonConfig.cs.md",
      "layer": "client-windows",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/DaemonConfig.cs", "to": "src/client-windows/Pim.Client.Core/ClientDefaults.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.App/DaemonConfig.cs", "type": "calls" }
  ]
}
```
