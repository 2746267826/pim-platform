# src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：KeyStats 采集健康诊断模型——明细状态枚举、进程信息、计数快照、健康结果。
- 主要依赖：无外部程序集
- 被谁使用：`KeyStatsCollectorService`、`KeyStatsProcessManager`、状态 UI/上报逻辑

## 函数级结构化伪代码

### KeyStatsDetailState
#### 枚举值
- 输入：无
- 输出：状态标签
- 副作用：无
- 步骤：定义 `MissingProcess` | `ApiUnreachable` | `ApiOkButStaleZero` | `Available`
- 分支与异常：无
- 调用：被 `KeyStatsHealthResult` 引用

### KeyStatsProcessInfo
#### record 字段
- 输入：构造时赋值
- 输出：`ProcessId`、`SessionId`、`IsCurrentUserSession`
- 步骤：不可变记录进程与会话归属

### KeyStatsCounterSnapshot
#### 属性与计算成员
- 输入：各点击/距离计数
- 输出：快照 + 派生量
- 副作用：无
- 步骤：
  1. 存储 KeyPresses 与左/右/中/侧键点击及鼠标/滚动距离。
  2. `TotalClicks`：五类点击求和。
  3. `HasAnyActivity`：按键、总点击或距离任一 > 0。
  4. `GrewFrom(previous)`：previous 为 null 则 false；否则任一项严格大于 previous 对应值。
- 分支与异常：`GrewFrom` 对 null 返回 false
- 调用：健康检查比较前后快照

### KeyStatsHealthResult
#### record 字段
- 输入：诊断流水线组装
- 输出：`DetailState`、`DaemonSourceState`、`CanUpload`、`SkipReason`、`ProcessCount`、`HasForeignSessionProcess`、可选 `Snapshot`、`SummaryZh`
- 步骤：一次性承载上传可否与中文摘要

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.Core.Models`。
2. 枚举四态：缺进程、API 不可达、API 通但陈旧全零、可用。
3. `KeyStatsProcessInfo`：进程 Id、会话 Id、是否当前用户会话。
4. `KeyStatsCounterSnapshot`：按键与点击、距离；TotalClicks/HasAnyActivity/GrewFrom 辅助。
5. `GrewFrom`：无 previous→false；否则比较按键、总点击、鼠标/滚动距离是否增长。
6. `KeyStatsHealthResult`：聚合状态、守护来源、可否上传、跳过原因、进程数、外会话进程、快照、中文摘要。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs",
      "label": "KeyStatsHealthModels",
      "path": "src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs.md",
      "layer": "client-windows",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsProcessManager.cs", "to": "src/client-windows/Pim.Client.Core/Models/KeyStatsHealthModels.cs", "type": "depends_on" }
  ]
}
```
