# src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：日程排程算法库：空闲槽计算、贪心/CSP 松弛/遗传算法调度接口与实现，以及相关记录类型。
- 主要依赖：BCL（LINQ、Task、Random）；无外部 DI
- 被谁使用：规划/排程上层服务；`SchedulingAlgorithmsTests`（Helpers/Greedy/Csp/Genetic）

## 函数级结构化伪代码

### 记录类型
#### TimeSlot / TaskToSchedule / BusySlot / ScheduleSolution / ScheduledSlot
- 输入：构造字段
- 输出：不可变值对象
- 副作用：无
- 步骤：承载起止、任务元数据（优先级/时长/最小片段/截止/偏好权重）、忙碌区间、解与指标
- 分支与异常：无
- 调用：无

### SchedulingHelpers
#### static List<TimeSlot> ComputeFreeSlots(List<BusySlot> busy, DateTimeOffset start, DateTimeOffset end)
- 输入：忙碌列表、搜索窗口
- 输出：空闲 `TimeSlot` 列表
- 副作用：无
- 步骤：
  1. 忙碌按 Start 排序；cursor = start
  2. 对每个 busy：若 End≤cursor 跳过；若 Start>cursor 产出 [cursor, Start)；cursor 推进到 max(cursor, End)
  3. cursor < end 时追加尾部空闲
- 分支与异常：重叠忙碌被 cursor 合并
- 调用：OrderBy

### ISchedulingAlgorithm
#### string Name；Task<ScheduleSolution?> SolveAsync(tasks, busySlots, searchStart, searchEnd, userWeights, ct)
- 输入：待排任务、忙碌、窗口、用户权重字典、取消令牌
- 输出：可选 `ScheduleSolution`
- 副作用：实现相关（纯计算为主）
- 步骤：接口契约
- 分支与异常：实现定义
- 调用：实现类

### GreedyScheduler : ISchedulingAlgorithm
#### Task<ScheduleSolution?> SolveAsync(...)
- 输入：同接口
- 输出：名为 `greedy` 的解
- 副作用：无
- 步骤：
  1. 任务按 Priority 降序、Deadline 升序
  2. `ComputeFreeSlots`
  3. 对每任务在空闲槽上顺序切分分配直到 Duration 耗尽（可跨多槽）
  4. Metrics：tasks_scheduled、total_tasks
- 分支与异常：槽不足则部分/未排完仍返回解
- 调用：`SchedulingHelpers.ComputeFreeSlots`

### CspScheduler : ISchedulingAlgorithm
#### Task<ScheduleSolution?> SolveAsync(...)
- 输入：同接口
- 输出：名为 `csp` 的解
- 副作用：无（字段 `_timeout` 未在本方法使用）
- 步骤：
  1. 计算空闲；维护 assignedFreeSlots 剩余时长与当前 Start
  2. Phase1：优先级排序后尝试整段 Duration 装入剩余≥Duration 的槽，更新剩余
  3. Phase2：未排任务用 MinSegment 或 15 分钟松弛再装
  4. Metrics 含 constraint_relaxations
- 分支与异常：装不下进入 unscheduled 再松弛
- 调用：`ComputeFreeSlots`

### GeneticScheduler : ISchedulingAlgorithm
#### Task<ScheduleSolution?> SolveAsync(...)
- 输入：同接口
- 输出：名为 `genetic` 的最优个体
- 副作用：使用 `Random.Shared`；响应 `ct` 提前结束进化
- 步骤：
  1. 初始化 PopulationSize=50 的随机日程
  2. Generations=100：适应度、精英前 5、轮盘赌交叉、MutationRate=0.1
  3. 取最高适应度个体返回
- 分支与异常：取消则提前停止代数循环
- 调用：`RandomSchedule`/`Fitness`/`SelectParent`/`Crossover`/`Mutate`

#### private List<ScheduledSlot> RandomSchedule(tasks, freeSlots, rng)
- 输入：任务与空闲
- 输出：随机可行安排（整段装入）
- 副作用：无
- 步骤：Fisher-Yates 打乱任务；随机选足够大的剩余槽分配并缩减
- 分支与异常：无候选槽则跳过该任务
- 调用：rng

#### private double Fitness(slots, tasks, weights)
- 输入：安排、任务、权重
- 输出：标量适应度
- 副作用：无
- 步骤：coverage = 已排任务比例；priorityScore = 已排优先级和/总优先级；加权 priority+coverage（默认各 0.5）
- 分支与异常：totalPriority=0 时 priorityScore=0
- 调用：无

#### private List<ScheduledSlot> SelectParent(pop, fitnesses, rng)
- 输入：种群与适应度
- 输出：轮盘赌选中的个体
- 副作用：无
- 步骤：按累计适应度采样
- 分支与异常：兜底 `pop.Last()`
- 调用：无

#### private List<ScheduledSlot> Crossover(a, b, rng)
- 输入：双亲
- 输出：子代列表
- 副作用：无
- 步骤：随机 split 点：a.Take + b.Skip
- 分支与异常：split 受 min 长度限制
- 调用：无

#### private void Mutate(schedule, freeSlots, rng)
- 输入：日程与空闲（空闲未用）
- 输出：无
- 副作用：可能删除随机一个槽
- 步骤：若 count>0 随机 RemoveAt
- 分支与异常：空日程不操作
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Calendar.Services`
2. 定义 TimeSlot、TaskToSchedule、BusySlot、ScheduleSolution、ScheduledSlot 记录
3. `SchedulingHelpers.ComputeFreeSlots`：排序忙碌、cursor 扫窗产出空闲
4. 接口 `ISchedulingAlgorithm`：Name + SolveAsync
5. `GreedyScheduler`：高优先级优先，跨槽填满 Duration
6. `CspScheduler`：整段优先，失败则 MinSegment/15min 松弛
7. `GeneticScheduler`：种群 50×100 代，精英 5，交叉突变，适应度=优先级覆盖加权
8. 辅助：随机排程、轮盘赌、单点交叉、删除式突变

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs",
      "label": "SchedulingAlgorithms",
      "path": "src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs", "to": "ISchedulingAlgorithm", "type": "implements" },
    { "from": "tests/Pim.UnitTests/Services/SchedulingAlgorithmsTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs", "type": "tests" }
  ]
}
```
