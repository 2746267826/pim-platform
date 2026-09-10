# src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：Stage0 诊断后台任务：执行时写一条 Information 日志并立即完成。
- 主要依赖：`ILogger<Stage0DiagnosticJob>`、`Microsoft.Extensions.Logging`
- 被谁使用：后台作业调度/DI 注册的 Stage0 诊断触发点

## 函数级结构化伪代码

### Stage0DiagnosticJob
#### 构造函数 Stage0DiagnosticJob(ILogger<Stage0DiagnosticJob> logger)
- 输入：日志器
- 输出：实例
- 副作用：保存 `_logger` 字段
- 步骤：赋值 `_logger = logger`
- 分支与异常：无
- 调用：无

#### Task RunAsync()
- 输入：无
- 输出：已完成的 Task
- 副作用：写 Information 日志（含 UTC 执行时间）
- 步骤：
  1. `LogInformation("Stage0 diagnostic job executed at {ExecutedAt}", DateTimeOffset.UtcNow)`
  2. 返回 `Task.CompletedTask`
- 分支与异常：无业务分支
- 调用：`ILogger.LogInformation`

## 近逐行中文伪代码

1. 引入 `Microsoft.Extensions.Logging`
2. 命名空间 `Pim.Infrastructure.Operations`
3. 密封类 `Stage0DiagnosticJob`
4. 私有只读 `_logger`
5. 构造函数注入并保存 logger
6. `RunAsync`：记录 Information 级别诊断执行时间（UtcNow），返回已完成任务

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs",
      "label": "Stage0DiagnosticJob",
      "path": "src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/Stage0DiagnosticJob.cs", "to": "Microsoft.Extensions.Logging", "type": "depends_on" }
  ]
}
```
