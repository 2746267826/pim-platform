# src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：对 `PcDetailRecord` 提供活动分类记录键相关字段的薄门面，统一委托 `PcActivityRecordKeyService.Build`。
- 主要依赖：`Pim.Module.PcTracker.DTOs.PcDetailRecord`、`PcActivityRecordKeyService`
- 被谁使用：活动分类持久化/建议流程需要 RecordKey、源事件/桶 JSON、版本与稳定性时

## 函数级结构化伪代码

### ActivityClassificationRecordKey
#### string FromRecord(PcDetailRecord record)
- 输入：明细记录
- 输出：`RecordKey` 字符串
- 副作用：无
- 步骤：`PcActivityRecordKeyService.Build(record).RecordKey`
- 分支与异常：委托服务异常向上抛
- 调用：`PcActivityRecordKeyService.Build`

#### string SourceEventIdsJson(PcDetailRecord record)
- 输入：明细记录
- 输出：源事件 Id 的 JSON 字符串
- 副作用：无
- 步骤：Build 后取 `SourceEventIdsJson`
- 分支与异常：同上
- 调用：`PcActivityRecordKeyService.Build`

#### string SourceBucketIdsJson(PcDetailRecord record)
- 输入：明细记录
- 输出：源桶 Id 的 JSON 字符串
- 副作用：无
- 步骤：Build 后取 `SourceBucketIdsJson`
- 分支与异常：同上
- 调用：`PcActivityRecordKeyService.Build`

#### string KeyVersion(PcDetailRecord record)
- 输入：明细记录
- 输出：键版本字符串
- 副作用：无
- 步骤：Build 后取 `KeyVersion`
- 分支与异常：同上
- 调用：`PcActivityRecordKeyService.Build`

#### string KeyStability(PcDetailRecord record)
- 输入：明细记录
- 输出：稳定性标签字符串
- 副作用：无
- 步骤：Build 后取 `Stability`
- 分支与异常：同上
- 调用：`PcActivityRecordKeyService.Build`

## 近逐行中文伪代码

1. 引入 PcTracker DTOs
2. 命名空间 `Pim.Module.PcTracker.Services`
3. 静态类 `ActivityClassificationRecordKey`
4. `FromRecord` → Build.RecordKey
5. `SourceEventIdsJson` → Build.SourceEventIdsJson
6. `SourceBucketIdsJson` → Build.SourceBucketIdsJson
7. `KeyVersion` → Build.KeyVersion
8. `KeyStability` → Build.Stability
9. （无本地算法，全部委托）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs",
      "label": "ActivityClassificationRecordKey",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRecordKey.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs/PcTrackerDtos.cs", "type": "depends_on" }
  ]
}
```
