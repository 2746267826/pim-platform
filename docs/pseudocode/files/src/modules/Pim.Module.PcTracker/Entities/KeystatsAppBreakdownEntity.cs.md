# src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：键盘/鼠标统计按应用拆分明细，映射表 `pc_keystats_app_breakdown`，归属日快照
- 主要依赖：DataAnnotations；导航 `KeystatsDailyEntity`
- 被谁使用：Keystats 聚合/查询服务；EF `PimDbContext`

## 函数级结构化伪代码

### KeystatsAppBreakdownEntity
#### 属性映射（无业务方法）
- 输入：无
- 输出：应用级键鼠统计行
- 副作用：无
- 步骤：
  1. `Id` long 主键
  2. `DailySnapshotId` → FK 到 `KeystatsDailyEntity`
  3. `AppName`（≤256）、`DisplayName`（≤512）
  4. 计数：KeyPresses、Left/Right/Middle/SideBack/SideForward Clicks
  5. `ScrollDistance` double
  6. 导航属性 `DailySnapshot` 非空引用
- 分支与异常：无
- 调用：`KeystatsDailyEntity`

## 近逐行中文伪代码

1. 引入 DataAnnotations
2. 命名空间 `Pim.Module.PcTracker.Entities`
3. 表 `pc_keystats_app_breakdown`
4. id long；daily_snapshot_id long
5. app_name MaxLength 256；display_name MaxLength 512
6. key_presses 与各按钮点击 int
7. scroll_distance double
8. ForeignKey 导航到 KeystatsDailyEntity

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs",
      "label": "KeystatsAppBreakdownEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsDailyEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsAppBreakdownEntity.cs", "type": "depends_on" }
  ]
}
```
