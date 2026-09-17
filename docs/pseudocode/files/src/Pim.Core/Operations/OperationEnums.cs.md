# src/Pim.Core/Operations/OperationEnums.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：运维/操作域共享枚举——系统健康、组件类型、审计主体与结果、操作确认状态、风险等级、守护进程数据源状态。
- 主要依赖：`System.Text.Json.Serialization`（`DaemonSourceState` 的字符串枚举序列化）。
- 被谁使用：
  - `Pim.Infrastructure/Operations/SystemStatusService`、`OperationConfirmationService`
  - `Pim.Api/Today/TodaySectionProviders`（`PimHealthStatus` 映射）
  - Calendar 治理/Outlook 同步相关服务与测试（`OperationRiskLevel`、`OperationConfirmationStatus`）
  - PcTracker 质量服务与守护相关实体/测试（`PimHealthStatus`、`DaemonSourceState`）

## 函数级结构化伪代码

### PimHealthStatus
#### 枚举值语义（无方法）
- 输入：无
- 输出：整型支持的健康等级
- 副作用：无
- 步骤：
  1. `Unknown = 0`：未知/未探测。
  2. `Healthy = 1`：正常。
  3. `Warning = 2`：需关注。
  4. `Critical = 3`：故障。
- 分支与异常：无
- 调用：无

### StatusComponentKind
#### 枚举值语义（无方法）
- 输入：无
- 输出：系统状态组件类别（默认从 0 递增）
- 副作用：无
- 步骤：
  1. `Api` / `Database` / `Storage` / `TextExtraction` / `Daemon` / `ActivityWatch` / `KeyStats` / `BackgroundJobs` 标识各检查组件。
- 分支与异常：无
- 调用：无

### AuditActorType
#### 枚举值语义（无方法）
- 输入：无
- 输出：审计动作发起方类型
- 副作用：无
- 步骤：
  1. `User` / `Daemon` / `System` / `Job` / `Mcp`。
- 分支与异常：无
- 调用：无

### AuditResult
#### 枚举值语义（无方法）
- 输入：无
- 输出：审计结果状态
- 副作用：无
- 步骤：
  1. `Success` / `Failure` / `PendingConfirmation` / `Rejected`。
- 分支与异常：无
- 调用：无

### OperationConfirmationStatus
#### 枚举值语义（无方法）
- 输入：无
- 输出：高风险操作确认生命周期状态
- 副作用：无
- 步骤：
  1. `Pending` → 待确认；`Confirmed` → 已确认；`Rejected` → 已拒绝；`Expired` → 过期；`Executed` → 已执行。
- 分支与异常：无
- 调用：无

### OperationRiskLevel
#### 枚举值语义（无方法）
- 输入：无
- 输出：风险等级（含旧式 Low/Medium/High 与 L0–L4 治理分级）
- 副作用：无
- 步骤：
  1. 兼容档：`Low=0`、`Medium=1`、`High=2`。
  2. 治理档：`L0AutomaticArtifact=10`、`L1LowRiskAction=11`、`L2PimFactChange=12`、`L3ExternalSourceOrWriteback=13`、`L4BatchOrDestructiveGovernance=14`。
- 分支与异常：无
- 调用：无

### DaemonSourceState
#### 枚举值语义（无方法）
- 输入：无
- 输出：守护进程侧数据源可用状态；JSON 以字符串枚举写出（`JsonStringEnumConverter`）
- 副作用：无
- 步骤：
  1. `Unknown` / `Available` / `Unavailable` / `Paused`。
- 分支与异常：无
- 调用：序列化时由 `JsonStringEnumConverter` 处理

## 近逐行中文伪代码

1. 引入 `System.Text.Json.Serialization`。
2. 命名空间 `Pim.Core.Operations`。
3. 定义 `PimHealthStatus`：`Unknown=0`、`Healthy=1`、`Warning=2`、`Critical=3`。
4. 定义 `StatusComponentKind`：Api、Database、Storage、TextExtraction、Daemon、ActivityWatch、KeyStats、BackgroundJobs。
5. 定义 `AuditActorType`：User、Daemon、System、Job、Mcp。
6. 定义 `AuditResult`：Success、Failure、PendingConfirmation、Rejected。
7. 定义 `OperationConfirmationStatus`：Pending、Confirmed、Rejected、Expired、Executed。
8. 定义 `OperationRiskLevel`：Low/Medium/High（0–2）与 L0–L4（10–14）两套刻度并存。
9. 为 `DaemonSourceState` 标注 `[JsonConverter(typeof(JsonStringEnumConverter))]`。
10. 定义 `DaemonSourceState`：Unknown、Available、Unavailable、Paused。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Operations/OperationEnums.cs",
      "label": "OperationEnums",
      "path": "src/Pim.Core/Operations/OperationEnums.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Operations/OperationEnums.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Operations/SystemStatusService.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Operations/OperationConfirmationService.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Today/TodaySectionProviders.cs", "to": "src/Pim.Core/Operations/OperationEnums.cs", "type": "depends_on" }
  ]
}
```
