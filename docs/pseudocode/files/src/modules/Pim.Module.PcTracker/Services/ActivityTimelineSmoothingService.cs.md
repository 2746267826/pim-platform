# src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：对活动时间线条目做短时低置信度片段平滑——在两侧高置信同分类片段之间合并中间噪声段。
- 主要依赖：`Pim.Module.PcTracker.DTOs.TimelineItem`
- 被谁使用：PcTracker 时间线/分析服务

## 函数级结构化伪代码

### ActivityTimelineSmoothingService
#### Smooth(IReadOnlyList<TimelineItem> items, int recommendedMinimumMinutes)
- 输入：时间线条目列表、推荐最小分钟数
- 输出：平滑后的 `TimelineItem` 列表
- 副作用：无（纯函数式处理，不改入参）
- 步骤：
  1. 若条目数 < 3 或 `recommendedMinimumMinutes <= 1`：直接 `ToList` 返回副本。
  2. 按 Start 再 End 升序排序得到 `ordered`。
  3. 顺序追加到 `smoothed`；每当长度 ≥ 3 时尝试对末尾三元组合并：
     - 取 previous/current/next；`CanMerge` 失败则 break。
     - 成功则移除末尾 3 项并 `Merge` 压回一项，继续 while 检查新三元组。
  4. 返回 `smoothed`。
- 分支与异常：时间字符串解析失败时 `DateTimeOffset.Parse` 抛异常
- 调用：`CanMerge`、`Merge`、`ParseTime`

#### CanMerge(previous, current, next, recommendedMinimumMinutes) [private static]
- 输入：连续三片段与最小分钟
- 输出：bool 是否可合并
- 副作用：无
- 步骤：
  1. current 时长 &lt; 推荐最小分钟。
  2. current 分类置信度 &lt; 0.5 且来源为 `"fallback"`（忽略大小写）。
  3. current 无 ProjectTag（空白）。
  4. previous-current 与 current-next 时间相邻（`AreContiguous`，容差 1 秒）。
  5. previous 与 next 的 CategoryName 完全相等；ProjectTag 忽略大小写相等。
  6. previous 与 next 置信度均 ≥ 0.7。
- 分支与异常：无
- 调用：`AreContiguous`

#### AreContiguous(left, right) [private static]
- 输入：左右时间片
- 输出：|right.Start - left.End| ≤ 1 秒
- 副作用：无
- 步骤：解析时间后取 Duration 比较 `ContiguousTolerance`。
- 分支与异常：Parse 失败抛异常
- 调用：`ParseTime`

#### Merge(previous, current, next) [private static]
- 输入：可合并三元组
- 输出：合并后的 `TimelineItem`（基于 previous 的 with 拷贝）
- 副作用：无
- 步骤：
  1. start=previous.Start，end=next.End（解析为 UTC）。
  2. 保留 previous 的 AppName/WindowTitle/ClassificationSource。
  3. DurationMinutes = max(0, end-start 分钟数)。
  4. 置信度取 previous/next 的较小值。
  5. 追加说明：短低置信活动已平滑并入两侧匹配项目上下文。
- 分支与异常：Parse 失败抛异常
- 调用：`ParseTime`、`FormatUtc`

#### ParseTime / FormatUtc [private static]
- 输入：ISO 时间字符串 / DateTimeOffset
- 输出：UTC DateTimeOffset / `"O"` 格式 UTC 字符串
- 副作用：无
- 步骤：Parse 后 ToUniversalTime；Format 用 UtcDateTime.ToString("O")。
- 分支与异常：非法字符串抛 FormatException
- 调用：BCL 解析格式化

## 近逐行中文伪代码

1. 引入 PcTracker DTOs；命名空间 Services。
2. 类 `ActivityTimelineSmoothingService`；连续容差 1 秒。
3. Smooth：条目少于 3 或最小分钟 ≤1 则原样列表返回。
4. 按开始/结束排序后逐项追加；维护 smoothed 末尾三元组合并循环。
5. CanMerge：中间段短、低置信 fallback、无项目标签、与两侧相邻，且两侧同分类高置信。
6. AreContiguous：结束与下一开始差值绝对值 ≤1 秒。
7. Merge：从 previous 扩展时间窗到 next.End，保留主上下文并写平滑说明。
8. ParseTime 转 UTC；FormatUtc 输出 ISO O 格式。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs",
      "label": "ActivityTimelineSmoothingService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityTimelineSmoothingService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
