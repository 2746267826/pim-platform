# src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 键鼠日统计主表实体 `pc_keystats_daily`，聚合按键/点击/移动/滚动峰值，并导航到按键计数与应用分解。
- 主要依赖：`KeystatsKeyCountEntity`、`KeystatsAppBreakdownEntity`（导航集合）
- 被谁使用：PcTracker keystats 摄取/查询服务、`PimDbContext`

## 函数级结构化伪代码

### KeystatsDailyEntity
#### 属性与导航（无自定义方法）
- 输入：无（POCO）
- 输出：字段与子集合
- 副作用：无
- 步骤：
  1. 表 `pc_keystats_daily`；`Id` long 主键。
  2. `DeviceId`(64)；`SnapshotDate` 列类型 date。
  3. `KeyPresses`；左/右/中/侧后/侧前点击计数。
  4. `MouseDistance`/`ScrollDistance` double；`PeakKps`/`PeakCps`。
  5. `CreatedAt` 默认 UtcNow。
  6. 导航 `KeyCounts`、`AppBreakdowns` 初始化空 List。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.Entities`；表 `pc_keystats_daily`。
2. Id、DeviceId、SnapshotDate。
3. 按键与多类鼠标点击；移动/滚动距离与峰值 KPS/CPS。
4. CreatedAt；KeyCounts 与 AppBreakdowns 集合。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs",
      "label": "KeystatsDailyEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsKeyCountEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs", "type": "depends_on" }
  ]
}
```
