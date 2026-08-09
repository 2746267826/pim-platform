# src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：键鼠日快照中单键计数明细实体（表 `pc_keystats_key_counts`），外键关联 `KeystatsDailyEntity`。
- 主要依赖：
  - `KeystatsDailyEntity`（导航 + FK）
- 被谁使用：Keystats 聚合/查询服务、`PimDbContext`

## 函数级结构化伪代码

### KeystatsKeyCountEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. 主键 `Id` long；`DailySnapshotId` long FK。
  2. `KeyName` MaxLength 128 默认空串；`Count` int。
  3. 导航 `DailySnapshot` → `KeystatsDailyEntity`（ForeignKey 标注）。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 表 `pc_keystats_key_counts`。
2. id、daily_snapshot_id、key_name、count。
3. 外键导航到日快照实体。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs",
      "label": "KeystatsKeyCountEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs", "type": "depends_on" }
  ]
}
```
