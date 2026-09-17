# src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：纯函数评估 KeyStats 采集健康：进程是否存在、API 是否可达、计数是否活跃，输出是否可上传与跳过原因。
- 主要依赖：`KeyStatsProcessInfo`、`KeyStatsCounterSnapshot`、`KeyStatsHealthResult`、`KeyStatsDetailState`（Models）
- 被谁使用：KeyStats 采集/心跳/状态上报路径

## 函数级结构化伪代码

### KeyStatsHealthProbe
#### Evaluate(processes, currentSessionId, snapshot, previousSnapshot, apiError)
- 输入：进程列表、当前会话 Id、当前/上一快照、API 错误串
- 输出：`KeyStatsHealthResult`（DetailState、高层 State 文案、CanUpload、SkipReason、进程数、是否有外会话、快照、说明）
- 副作用：无
- 步骤：
  1. `processCount = processes.Count`。
  2. `hasForeign`：任一根进程非当前用户会话或 `SessionId != currentSessionId`。
  3. 若无进程 → `MissingProcess` / Unavailable / 不可上传 / skip `missing-process`。
  4. 若 `apiError` 非空白或 `snapshot` 为 null → `ApiUnreachable` / skip `api-unreachable`。
  5. `available = snapshot.HasAnyActivity || snapshot.GrewFrom(previousSnapshot)`。
  6. 若不可用 → `ApiOkButStaleZero` / skip `stale-zero`；文案依 `hasForeign` 区分。
  7. 否则 → `Available` / Available / 可上传 / SkipReason null；文案提示是否有额外会话实例。
- 分支与异常：无抛出；分支按进程→API→活跃度递进
- 调用：`KeyStatsCounterSnapshot.HasAnyActivity`、`GrewFrom`

## 近逐行中文伪代码

1. 静态类；引入 Models。
2. Evaluate：数进程；检测外会话实例。
3. 零进程 → MissingProcess，不可上传。
4. API 错误或空快照 → ApiUnreachable。
5. 快照无活动且相对上期未增长 → ApiOkButStaleZero。
6. 否则 Available 可上传；有外会话时消息提示额外实例。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs",
      "label": "KeyStatsHealthProbe",
      "path": "src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/KeyStatsHealthProbe.cs", "to": "src/client-windows/Pim.Client.Core/Models", "type": "depends_on" }
  ]
}
```
