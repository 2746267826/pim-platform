# src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：按业务日与时间块聚合 PC 活动明细，输出强度、活跃秒、待分类数、切换次数、分类与应用 Top 列表。
- 主要依赖：`PcTrackerService`、`Pim.Module.PcTracker.DTOs`
- 被谁使用：PcTracker 分析相关端点/服务

## 函数级结构化伪代码

### PcActivityAnalysisService
#### 构造 `PcActivityAnalysisService(PcTrackerService tracker)`
- 输入：跟踪查询服务
- 输出：分析服务实例
- 副作用：保存 `_tracker`
- 调用：无

#### `Task<PcActivityAnalysisResponse> GetDailyAnalysisAsync(DateTime date, int blockMinutes, CancellationToken ct)`
- 输入：业务日、块分钟数 15–240、取消令牌
- 输出：当日各时间块分析响应
- 副作用：调用 tracker 查询明细（只读）
- 步骤：
  1. blockMinutes 不在 [15,240] → ArgumentException（中文消息）。
  2. dateText = yyyy-MM-dd；`QueryCompleteDetailAsync` 同日 interpreted 视图，排序 date asc，页 1 大小 2000。
  3. dayStart = `GetBusinessDayStartForQuery(date)`；blockCount = ceil(1440/blockMinutes)。
  4. 对每个块 i：
     - start/end = dayStart + i*block、+block
     - 取 DurationSeconds>0 且 Start 可解析且落在 [start,end) 的记录，按 Start 排序
     - activeSeconds = 时长和
     - categories：按 CategoryName（默认 Other）分组，颜色取首个非空或 `#64748b`，按时长降序
     - apps：web-page 用 Domain/BrowserAppName/web，否则 AppName/DisplayName/unknown；按时长 Top5
     - 块 DTO：起止 O 格式、ToIntensity、activeSeconds、待分类计数、应用切换数、分类切换数、categories、apps
  5. 返回 (dateText, blockMinutes, blocks)
- 分支与异常：块大小非法；Start 解析失败的记录被过滤
- 调用：`_tracker.QueryCompleteDetailAsync`、`PcTrackerService.GetBusinessDayStartForQuery`、ToIntensity、IsPendingClassification、CountSwitches

#### `static bool IsPendingClassification(PcDetailRecord)`
- fallback 源或 confidence < 0.5 为待分类

#### `static int ToIntensity(activeSeconds, blockMinutes)`
- ratio = active / (block*60)；0 / ≤0.2→1 / ≤0.45→2 / ≤0.7→3 / 否则 4

#### `static int CountSwitches(IEnumerable<string> values)`
- 跳过空白；与前一个非空值忽略大小写不同则 +1

## 近逐行中文伪代码

1. 注入 PcTrackerService。
2. GetDailyAnalysisAsync：校验块分钟 → 查当日 interpreted 明细（最多 2000）。
3. 业务日起点切块；每块筛落在区间内的正时长记录。
4. 汇总活跃秒、分类时长色、应用 Top5。
5. 强度档、待分类数、应用/分类切换次数写入块 DTO。
6. IsPendingClassification / ToIntensity / CountSwitches 为纯函数辅助。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs",
      "label": "PcActivityAnalysisService",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcActivityAnalysisService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" }
  ]
}
```
