# src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Stats
- 职责：Stats 模块上传用 DTO：`AppUsageEntry`（单条应用使用区间）与 `UploadBatch`（设备批量条目）。
- 主要依赖：无外部项目类型
- 被谁使用：Stats 模块摄取/端点

## 函数级结构化伪代码

### AppUsageEntry
#### record 构造（位置参数）
- 输入：`PackageName`、`StartTime`、`EndTime`、`DurationMs`、`LastTimeUsed`
- 输出：不可变记录实例
- 副作用：无
- 步骤：
  1. 描述单包名在一段时间内的前台使用区间与时长。
- 分支与异常：无
- 调用：无

### UploadBatch
#### record 构造（位置参数）
- 输入：`DeviceId`、`Entries`（`List<AppUsageEntry>`）
- 输出：不可变记录实例
- 副作用：无
- 步骤：
  1. 将设备标识与多条 `AppUsageEntry` 组成一次上传批次。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Stats.DTOs`。
2. `AppUsageEntry`：包名、起止时间戳、时长毫秒、最后使用时间。
3. `UploadBatch`：设备 Id + 条目列表。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs",
      "label": "StatsDtos",
      "path": "src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs.md",
      "layer": "module.stats",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Stats", "to": "src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs", "type": "depends_on" }
  ]
}
```
