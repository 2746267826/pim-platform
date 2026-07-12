# tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：PC 事实数据质量：桶缺失、守护进程心跳、keystats 样本/间隙/复位、AFK、legacy 行、健康路径与模块端点源码契约。
- 主要依赖：`PcTrackerQualityService`、`AwBucketEntity`、`AwEventEntity`、`KeystatsSampleEntity`、`DaemonHeartbeatEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### PcTrackerQualityServiceTests
#### GetQualityAsync_ReturnsCritical_WhenWindowBucketIsMissing()
- 输入：有 afk/web 桶无 window 桶
- 输出：无
- 副作用：写 fixture
- 步骤：Overall Critical；issue `missing-aw-window-bucket`；中文 message/nextStep；component aw-buckets Critical
- 分支与异常：无
- 调用：`PcTrackerQualityService.GetQualityAsync`

#### GetQualityAsync_ReturnsWarning_WhenOnlyWebBucketIsMissing()
- 输入：缺 web 桶
- 输出：无
- 副作用：写 fixture
- 步骤：Warning；`missing-aw-web-bucket`；无 window 缺失 issue
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsUnknownIssue_WhenDaemonHeartbeatIsMissing()
- 输入：无心跳
- 输出：无
- 副作用：写 fixture
- 步骤：`missing-windows-daemon-heartbeat` Severity Unknown；daemon-upload Unknown
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_UsesCurrentBuckets_WhenQueryingPastRange()
- 输入：当前 SeenAt 桶 + 历史 keystats
- 输出：无
- 副作用：写 fixture
- 步骤：查历史日不报缺桶；aw-buckets 非 Critical
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsWarning_WhenOnlyOneKeyStatsSampleCannotBuildInputTimeline()
- 输入：单样本
- 输出：无
- 副作用：写 fixture
- 步骤：`keystats-insufficient-samples`；interpreted-timeline Warning
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsWarning_WhenAfkEventsAreMissing()
- 输入：有 afk 桶无 afk 事件
- 输出：无
- 副作用：写 fixture
- 步骤：`missing-aw-afk-events`；aw-events Warning
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsWarning_ForKeyStatsGapAndReset()
- 输入：间隔>2min 与计数回落
- 输出：无
- 副作用：写 fixture
- 步骤：含 `keystats-sample-gap` 与 `keystats-counter-reset`
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsCompletenessIssue_ForLegacyAwRows()
- 输入：SourceEventId null、DataJson 空
- 输出：无
- 副作用：写 fixture
- 步骤：`aw-events-missing-source-id` 与 `aw-events-invalid-data-json`
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsCritical_WhenDaemonHeartbeatIsStale()
- 输入：心跳 2 小时前
- 输出：无
- 副作用：写 fixture
- 步骤：Critical；`stale-windows-daemon-heartbeat`
- 分支与异常：无
- 调用：同上

#### GetQualityAsync_ReturnsHealthy_WhenFactsAreComplete()
- 输入：桶+window+afk+双 keystats+近心跳
- 输出：无
- 副作用：写 fixture
- 步骤：Healthy；Issues/NextSteps 空；timeline Healthy
- 分支与异常：无
- 调用：同上

#### PcTrackerModule_ExposesQualityEndpointInSource()
- 输入：无
- 输出：无
- 副作用：读 PcTrackerModule.cs
- 步骤：含 MapGet `/quality` 与服务类型名
- 分支与异常：无
- 调用：`File.ReadAllText`

#### 辅助 Add* / CreateDbContext
- 输入：可选时间/键数
- 输出：无 / DbContext
- 副作用：注册模块、插入实体
- 步骤：心跳 DESKTOP；桶/事件/采样工厂
- 分支与异常：无
- 调用：EF Set.Add

## 近逐行中文伪代码

1. 缺 window 桶 Critical
2. 仅缺 web Warning
3. 无守护进程心跳 Unknown
4. 查历史用当前桶
5. 单 keystats 不足 Warning
6. 无 AFK 事件 Warning
7. gap+reset Warning
8. legacy 行完整性 issue
9. 过期心跳 Critical
10. 完整事实 Healthy
11. 模块源码暴露 /quality
12. fixture 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs",
      "label": "PcTrackerQualityServiceTests",
      "path": "tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerQualityService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/PcTrackerQualityServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "type": "tests" }
  ]
}
```
