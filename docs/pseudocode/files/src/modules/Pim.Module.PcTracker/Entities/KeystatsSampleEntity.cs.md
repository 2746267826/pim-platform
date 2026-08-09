# src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 键鼠统计采样实体（按设备与业务日记录按键/点击/移动/滚动及 JSON 明细）。
- 主要依赖：DataAnnotations、Schema
- 被谁使用：PcTracker 入库/查询服务、EF 映射与迁移

## 函数级结构化伪代码

### KeystatsSampleEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：采集/同步层赋值后由 EF 持久化
- 输出：`pc_keystats_samples` 表一行
- 副作用：JSON 与时间默认值
- 步骤：
  1. 主键 `Id`（long）；`PimDeviceId` 最长 64。
  2. 时间：`SampledAtUtc`；`StatsDate`（date）；`StatsTimezoneOffsetMinutes`。
  3. 计数：KeyPresses、Left/Right/Middle/SideBack/SideForward Clicks。
  4. 距离：`MouseDistance`、`ScrollDistance`；峰值 `PeakKps`/`PeakCps`。
  5. 格式化展示串：FormattedMouseDistance、FormattedScrollDistance 可空。
  6. JSON：`KeyCountsJson`/`AppStatsJson`/`RawJson` 默认 `"{}"`。
  7. `CreatedAt` 默认 UtcNow。
- 分支与异常：无
- 调用：被 PcTracker 服务读写

## 近逐行中文伪代码

1. 表 `pc_keystats_samples`；Id long、PimDeviceId。
2. SampledAtUtc、StatsDate、时区偏移分钟。
3. 按键与各类点击计数。
4. 鼠标/滚动距离与峰值 KPS/CPS。
5. 可选格式化距离字符串。
6. KeyCountsJson/AppStatsJson/RawJson 默认 {}；CreatedAt 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs",
      "label": "KeystatsSampleEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs", "type": "depends_on" }
  ]
}
```
