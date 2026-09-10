# tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：数据中心全局查询覆盖全部批准对象类型。
- 主要依赖：`DataCenterQueryService`、完整 fixture 种子
- 被谁使用：dotnet test

## 函数级结构化伪代码

### GlobalSearchCoversAllApprovedObjectTypes
- 步骤：SeedFullDataCenterFixture；Query pageSize=200；断言含 task/event/task-segment/habit/reminder/report/confirmation/sync-batch/sync-conflict/audit-version/recycle-bin

### SeedFullDataCenterFixture
- 步骤：写入日历事件/任务/片段/习惯/提醒/报告/确认/同步批与冲突/审计版本/回收站项

## 近逐行中文伪代码

1. [L18-47] 类型覆盖断言
2. [L49-191] CreateDb 与 Seed

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs",
      "label": "DataCenterCoverageTests",
      "path": "tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Calendar/DataCenterCoverageTests.cs", "to": "src/Pim.Module.Calendar/Services/DataCenterQueryService.cs", "type": "tests" }
  ]
}
```
