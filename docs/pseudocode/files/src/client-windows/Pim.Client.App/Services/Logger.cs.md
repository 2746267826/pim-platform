# src/client-windows/Pim.Client.App/Services/Logger.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 守护进程静态日志门面：Serilog 按日滚动 Compact JSON 文件 + Debug 输出。
- 主要依赖：Serilog、`System.Diagnostics`、本地 `%LocalAppData%\PIM\logs`
- 被谁使用：Client.App 各服务/窗口/启动路径调用 Info/Warn/Error/Trace

## 函数级结构化伪代码

### Logger
#### void Initialize()
- 输入：无
- 输出：无
- 副作用：创建日志目录；配置并持有全局 `_serilog`
- 步骤：
  1. `Directory.CreateDirectory(LogDir)`
  2. 日志文件模板 `pim-daemon-.jsonl`（Serilog 按日滚动插入日期）
  3. `LoggerConfiguration`：MinimumLevel.Debug；Enrich Service=`pim-daemon`；WriteTo.Debug；WriteTo.File(CompactJsonFormatter, 保留 30 天)
  4. `CreateLogger` 赋给 `_serilog`
- 分支与异常：目录/IO 异常向上
- 调用：Serilog API

#### void Info / Warn / Error(message, ex?) / Trace(message)
- 输入：消息；Error 可选异常
- 输出：无
- 副作用：写日志（Trace 仅 DEBUG 编译）
- 步骤：委托 `Write` 对应级别；Trace 包在 `#if DEBUG`
- 分支与异常：无
- 调用：`Write`

#### void Write(LogEventLevel level, string message, Exception? ex)
- 输入：级别、消息、异常
- 输出：无
- 副作用：Serilog 写入或 Debug 回退
- 步骤：
  1. 若 `_serilog` 非空：`Write(level, ex, message)`
  2. 否则：`Debug.WriteLine` 级别与消息；有异常再写异常字符串
- 分支与异常：初始化前走 Debug 回退
- 调用：`ILogger.Write` 或 `Debug.WriteLine`

#### string LogFilePath
- 输入：无
- 输出：当日日志文件路径 `pim-daemon-yyyyMMdd.jsonl`
- 副作用：懒缓存 `_logFilePath`
- 步骤：首次访问时用 `DateTime.Now` 拼路径并缓存
- 分支与异常：无
- 调用：`Path.Combine`

## 近逐行中文伪代码

1. 引入 Diagnostics、IO、Serilog 及 Compact 格式
2. 静态类 `Logger`；LogDir=`LocalApplicationData/PIM/logs`
3. 可空 `_serilog`
4. `Initialize`：建目录；配置 Debug 级、Service 属性、Debug sink + 日滚文件 30 份
5. Info/Warn/Error 映射级别调用 Write
6. Trace 仅 DEBUG 写 Verbose
7. Write：有 Serilog 则写；否则 Debug 回退
8. LogFilePath 懒生成当日 jsonl 路径

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/Services/Logger.cs",
      "label": "Logger",
      "path": "src/client-windows/Pim.Client.App/Services/Logger.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/Services/Logger.cs.md",
      "layer": "client-windows",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/Services/Logger.cs", "to": "Serilog", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/Services/Logger.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.App/Services/Logger.cs", "type": "calls" }
  ]
}
```
