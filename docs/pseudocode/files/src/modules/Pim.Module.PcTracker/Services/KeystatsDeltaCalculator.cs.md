# src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：由相邻 `KeystatsSampleEntity` 计算分钟级增量（按键/点击/鼠标与滚动距离），并标记 gap/reset。
- 主要依赖：
  - `KeystatsSampleEntity`
- 被谁使用：Keystats 入库/分钟聚合流水线

## 函数级结构化伪代码

### KeystatsMinuteDelta（record）
#### 主构造
- 输入：DeviceId、MinuteStartUtc、KeyPresses、TotalClicks、MouseDistance、ScrollDistance、IsGap、IsReset
- 输出：不可变分钟增量
- 副作用：无

### KeystatsDeltaCalculator（static）
#### `Calculate(previous, current)`
- 输入：上一采样（可 null）、当前采样
- 输出：`KeystatsMinuteDelta`
- 副作用：无
- 步骤：
  1. previous 为 null 或 StatsDate 不同 → 用 current 绝对值；IsGap=true，IsReset=false。
  2. 否则各计数/距离做差（五类点击、键按、鼠标距、滚动距）。
  3. 任一差 < 0 → 全 0；IsReset=true；IsGap = 采样间隔 > 2 分钟。
  4. 否则正常增量；TotalClicks=五类点击和；IsGap = 间隔 > 2 分钟。
- 分支与异常：无
- 调用：`TotalClicks(sample)`

#### `TotalClicks(sample)` private
- Left+Right+Middle+SideBack+SideForward

## 近逐行中文伪代码

1. 定义 `KeystatsMinuteDelta` 记录结构。
2. 无前序或跨日：把当前累计当本分钟增量，标 gap。
3. 有前序：差分；负值视为计数器重置，输出 0 并标 reset。
4. 采样间隔超过 2 分钟标 gap。
5. 点击汇总为五键之和。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs",
      "label": "KeystatsDeltaCalculator",
      "path": "src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/KeystatsDeltaCalculator.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/KeystatsSampleEntity.cs", "type": "depends_on" }
  ]
}
```
