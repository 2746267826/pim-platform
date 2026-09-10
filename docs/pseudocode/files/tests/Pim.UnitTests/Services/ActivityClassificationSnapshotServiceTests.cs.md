# tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：分类快照确定性写入、键版本、手动保护、重复键、无效时长与 RecordKey 工具。
- 主要依赖：`ActivityClassificationSnapshotService`、`ActivityClassificationRecordKey`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 创建快照不改原 record 对象
2. 持久化 keyVersion/source/stability/buckets
3. 同 key 更新 + auditId
4. 重复 key 每条返回分类但库 1 行
5. 保留 manual；可更新 source 元数据
6. 规则上下文用 bucketType
7. 无效 duration/时间戳原样返回不落库
8. FromRecord 开端记录 end=start；SourceEventIdsJson 排序偏 web

## 近逐行中文伪代码

1. 多个 EnsureClassificationsAsync Fact/Theory
2. NewRecord/NewRule/CreateDb helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs",
      "label": "ActivityClassificationSnapshotServiceTests",
      "path": "tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityClassificationSnapshotServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "type": "tests" }
  ]
}
```
