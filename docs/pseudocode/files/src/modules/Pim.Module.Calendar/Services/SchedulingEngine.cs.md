# src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：调度引擎：加载用户任务与忙闲事件，按用户反馈权重依次运行贪心/CSP/遗传三种算法，汇总可行 `ScheduleSolution`。
- 主要依赖：`PimDbContext`、`TaskEntity`、`EventEntity`、`SchedulingFeedbackEntity`、`ISchedulingAlgorithm` 实现（Greedy/Csp/Genetic）
- 被谁使用：日历模块调度相关端点/服务

## 函数级结构化伪代码

### SchedulingEngine
#### SchedulingEngine(PimDbContext db)
- 输入：数据库上下文
- 输出：引擎实例，内置三种算法列表
- 副作用：构造算法实例列表
- 步骤：
  1. 保存 `_db`
  2. `_algorithms = [GreedyScheduler, CspScheduler, GeneticScheduler]`
- 分支与异常：无
- 调用：各 scheduler 构造函数

#### Task<List<ScheduleSolution>> GeneratePlansAsync(Guid userId, List<Guid> taskIds, CancellationToken ct)
- 输入：用户 Id、待排任务 Id 列表、取消令牌
- 输出：各算法成功产出的方案列表（可少于算法数）
- 副作用：只读查询任务、事件、反馈
- 步骤：
  1. 加载 `taskIds` 中且 `EstimatedDuration` 有值的 `TaskEntity`
  2. 加载该用户日历下全部 `EventEntity` 作为忙时
  3. 映射为 `TaskToSchedule`（缺省时长 1 小时、权重 1.0 等）
  4. 映射事件为 `BusySlot(DtStart, DtEnd)`
  5. 搜索窗口：`now = UtcNow` 到 `now+14 天`
  6. `GetUserWeightsAsync(userId)` 取目标权重
  7. 对每个算法 `SolveAsync(...)`；非 null 方案加入结果
  8. 返回 solutions
- 分支与异常：算法返回 null 则跳过；EF/取消可抛异常
- 调用：`_db.Set`、`GetUserWeightsAsync`、`ISchedulingAlgorithm.SolveAsync`

#### Task<Dictionary<string,double>> GetUserWeightsAsync(Guid userId) [private]
- 输入：用户 Id
- 输出：priority/coverage/compactness 权重字典
- 副作用：只读查询最近 50 条 `SchedulingFeedbackEntity`
- 步骤：
  1. 取该用户反馈按 `CreatedAt` 降序最多 50 条
  2. 若条数 < 5 → 默认权重 0.5 / 0.3 / 0.2
  3. 否则返回 0.6 / 0.25 / 0.15（当前未细粒度解析反馈内容）
- 分支与异常：无反馈或不足 5 条走默认
- 调用：`_db.Set<SchedulingFeedbackEntity>`

## 近逐行中文伪代码

1. 引用 EF Core、`PimDbContext`、日历实体
2. 命名空间 `Pim.Module.Calendar.Services`
3. 类 `SchedulingEngine`：字段 `_db` 与算法列表
4. 构造：注入 db，实例化 Greedy/Csp/Genetic 三种调度器
5. `GeneratePlansAsync`：
6.   查有预估时长的任务；查用户日历事件
7.   任务映射 `TaskToSchedule`；事件映射 `BusySlot`
8.   时间窗 now → +14 天；取用户权重
9.   循环算法 SolveAsync，收集非空方案并返回
10. `GetUserWeightsAsync`：最近 50 条反馈；不足 5 条用默认权重，否则用偏 priority 的固定权重

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs",
      "label": "SchedulingEngine",
      "path": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs", "to": "src/modules/Pim.Module.Calendar/Entities/TaskEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs", "to": "src/modules/Pim.Module.Calendar/Entities/EventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs", "to": "src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services/SchedulingEngine.cs", "to": "ISchedulingAlgorithm", "type": "calls" }
  ]
}
```
