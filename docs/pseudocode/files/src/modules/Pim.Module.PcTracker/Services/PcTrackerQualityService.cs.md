# src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：在指定业务日范围内评估 PC 事实数据采集质量（AW 桶/事件、KeyStats 样本、守护程序心跳、解释时间线输入），汇总组件状态与问题列表。
- 主要依赖：`PimDbContext`、`AwBucketEntity`/`AwEventEntity`/`KeystatsSampleEntity`/`DaemonHeartbeatEntity`、`KeystatsDeltaCalculator`、`PcTrackerService`、`PimHealthStatus`、JSON
- 被谁使用：PcTracker 质量/健康查询 API

## 函数级结构化伪代码

### PcTrackerQualityService
#### PcTrackerQualityService(PimDbContext db)
- 输入：DB 上下文
- 输出：实例
- 副作用：保存 `_db`；常量 `StaleBucketAge = 24h`
- 步骤：赋值
- 分支与异常：无
- 调用：无

#### Task\<PcQualityResponse\> GetQualityAsync(DateTime? date, DateTime? dateFrom, DateTime? dateTo, CancellationToken ct)
- 输入：单日或起止日期（可空）
- 输出：总体状态、标签、消息、检查时间、组件列表、问题、下一步去重列表
- 副作用：只读查询多表
- 步骤：
  1. `checkedAt = UtcNow`；`GetRange` 得业务日起止
  2. 加载全部 AW 桶；范围内 AW 事件（按时间序）；范围内 KeyStats 样本（设备+时间序）；最新 windows 守护心跳
  3. 依次 `CheckBuckets`/`CheckEvents`/`CheckKeystats`/`CheckDaemon`/`CheckTimeline`，收集 issues 与 components
  4. overallStatus = 组件状态按严重度降序首项
  5. 组装 `PcQualityResponse`（含 issues.NextStep 去重）
- 分支与异常：DB 异常向上抛
- 调用：各 Check*、`GetSeverityRank`、`GetLabel`、`GetMessage`

#### (DateTimeOffset Start, DateTimeOffset End) GetRange(DateTime? date, DateTime? dateFrom, DateTime? dateTo)
- 输入：可选日期参数
- 输出：业务日查询闭开区间
- 副作用：无
- 步骤：from = dateFrom ?? date ?? Today；to = dateTo ?? date ?? from；若 to\<from 交换；`GetBusinessDayStartForQuery` 到 to+1 日
- 分支与异常：无
- 调用：`PcTrackerService.GetBusinessDayStartForQuery`

#### PcQualityComponentDto CheckBuckets(...)
- 输入：全部桶、检查时刻、全局 issues 列表
- 输出：组件 DTO（aw-buckets）
- 副作用：向 issues 追加组件问题
- 步骤：
  1. 缺 `currentwindow` → Critical「缺少窗口桶」
  2. 缺 `afkstatus` → Warning AFK 桶
  3. 缺 `web.tab.current` → Warning 网页桶
  4. SeenAt 超过 24h 计数 → Warning 陈旧桶
  5. details：bucketCount/staleBucketCount；`BuildComponent`
- 分支与异常：无
- 调用：`HasBucketType`、`BuildComponent`

#### PcQualityComponentDto CheckEvents(...)
- 输入：范围内事件、issues
- 输出：组件 DTO（aw-events）
- 副作用：追加 issues
- 步骤：
  1. 无事件 → Warning
  2. 有事件：无窗口/无 AFK → Warning；缺 SourceEventId / 无效 DataJson → MajoritySeverity
  3. details：event/window/afk 计数
- 分支与异常：无
- 调用：`IsWindowEvent`、`IsAfkEvent`、`IsValidJson`、`MajoritySeverity`

#### PcQualityComponentDto CheckKeystats(...)
- 输入：样本列表、issues
- 输出：组件 DTO（keystats-samples）
- 副作用：追加 issues
- 步骤：
  1. 无样本 → Critical
  2. 按设备分组逐样本 `KeystatsDeltaCalculator.Calculate` 累计 gap/reset
  3. gap/reset >0 → Warning
  4. details：sample/gap/reset 计数
- 分支与异常：无
- 调用：`KeystatsDeltaCalculator.Calculate`

#### PcQualityComponentDto CheckDaemon(...)
- 输入：最新心跳、检查时刻、issues
- 输出：组件 DTO（daemon-upload）
- 副作用：追加 issues
- 步骤：
  1. 无心跳 → Unknown + missing 详情
  2. 有心跳：记录 receivedAt/age/队列/AW 与 KeyStats 状态
  3. age≥60m Critical；≥10m Warning；LastError/队列>0/来源 Unavailable → Warning
- 分支与异常：无
- 调用：`IsSourceUnavailable`、`BuildComponent`

#### PcQualityComponentDto CheckTimeline(...)
- 输入：事件、样本、issues
- 输出：组件 DTO（interpreted-timeline）
- 副作用：追加 issues
- 步骤：缺 AW 或 KeyStats → Warning 输入不完整；有两者但每设备样本 <2 → Warning 无法算增量
- 分支与异常：无
- 调用：`BuildComponent`

#### 辅助：HasBucketType / IsWindowEvent / IsAfkEvent / MajoritySeverity / IsValidJson / IsSourceUnavailable / BuildComponent / ComponentMessage / GetLabel / GetMessage / GetSeverityRank
- 输入：桶类型、事件、计数比例、JSON 串、状态枚举等
- 输出：布尔、严重度、组件 DTO、中文标签/消息、排序秩
- 副作用：无
- 步骤：
  1. 类型比较忽略大小写
  2. 窗口/AFK 由 EventType 或 BucketType 判定
  3. 超半数 Critical 否则 Warning
  4. JsonDocument.Parse 校验
  5. 组件无问题 Healthy，否则取最严重 issue
  6. 状态映射中文文案与秩 0..3
- 分支与异常：`IsValidJson` 吞 JsonException
- 调用：`JsonDocument.Parse`

## 近逐行中文伪代码

1. 引入 JSON、EF、Core.Operations、Infrastructure 实体、PcTracker DTO/Entity
2. 命名空间 Services；密封类；陈旧桶阈值 24 小时
3. `GetQualityAsync`：算范围→查桶/事件/样本/心跳→五项检查→汇总总体状态
4. `GetRange`：业务日边界，支持单日或区间
5. `CheckBuckets`：窗口/AFK/网页桶与陈旧
6. `CheckEvents`：空集、窗口/AFK、SourceId、data_json
7. `CheckKeystats`：空集、gap、reset
8. `CheckDaemon`：心跳缺失/过期/错误/队列/来源不可用
9. `CheckTimeline`：双源与增量样本对
10. 辅助判定与 `BuildComponent`；中文标签消息；严重度排序

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs",
      "label": "PcTrackerQualityService",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwBucketEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/Pim.Infrastructure/Data/Entities/DaemonHeartbeatEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" }
  ]
}
```
