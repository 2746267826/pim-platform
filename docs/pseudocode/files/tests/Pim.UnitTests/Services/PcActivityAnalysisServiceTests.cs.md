# tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：日活动分块分析：聚合时长/强度/待分类/切换；非法 blockMinutes 拒绝。
- 主要依赖：`PcActivityAnalysisService`、`PcTrackerService`、`AwEventEntity`
- 被谁使用：xUnit

## 函数级结构化伪代码

### PcActivityAnalysisServiceTests
#### GetDailyAnalysisAsync_GroupsRecordsAndFlagsPendingClassification()
- 输入：两窗口事件（Code + Mystery）、block=60
- 输出：无
- 副作用：InMemory 写 AW 事件
- 步骤：
  1. 构造 PcTrackerService + PcActivityAnalysisService
  2. GetDailyAnalysisAsync(2026-07-05, 60)
  3. 断言 Date/BlockMinutes；有活动块 ActiveDurationSeconds=900；强度/待分类/切换/应用/分类计数
- 分支与异常：无
- 调用：`PcActivityAnalysisService.GetDailyAnalysisAsync`

#### GetDailyAnalysisAsync_RejectsUnsupportedBlockSize(int blockMinutes)
- 输入：Theory 10 与 241
- 输出：无
- 副作用：无
- 步骤：ArgumentException
- 分支与异常：期望抛 ArgumentException
- 调用：同上

#### CreateDb / WindowEvent
- 输入：时间/时长/app/title
- 输出：DbContext / AwEventEntity
- 副作用：注册模块
- 步骤：归一 AppNameNormalized；EventType=window
- 分支与异常：无
- 调用：`AppNameNormalizer.Normalize`

## 近逐行中文伪代码

1. 注入两条 window 事件，分析日块
2. 校验聚合指标与 pending 分类
3. 非法 block 大小抛错
4. 工厂 CreateDb、WindowEvent

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs",
      "label": "PcActivityAnalysisServiceTests",
      "path": "tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/PcActivityAnalysisServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "tests" }
  ]
}
```
