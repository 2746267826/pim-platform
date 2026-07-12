# tests/Pim.UnitTests/Services/SchedulingAlgorithmsTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖空闲槽计算与 greedy/genetic/csp 调度算法的基本可解性与边界。
- 主要依赖：`SchedulingHelpers`、`GreedyScheduler`、`GeneticScheduler`、`CspScheduler`
- 被谁使用：xUnit

## 函数级结构化伪代码

### SchedulingHelpersTests
#### ComputeFreeSlots_*
- 输入：busy 列表与 [start,end]
- 输出：无
- 副作用：无
- 步骤：
  1. 无 busy → 整段空闲
  2. 中间一段 busy → 两段 free
  3. 范围外 busy 不影响
  4. 重叠 busy 合并后两段 free
- 分支与异常：无
- 调用：`SchedulingHelpers.ComputeFreeSlots`

### GreedySchedulerTests
#### SolveAsync_NoTasks / SingleTask / MultipleTasks
- 输入：任务列表、busy、时间窗
- 输出：无
- 副作用：无
- 步骤：
  1. 无任务 → AlgorithmName=`greedy`、Slots 空
  2. 单任务 → 首槽 Title 匹配
  3. 多任务按优先级，高优先级先排
- 分支与异常：无
- 调用：`GreedyScheduler.SolveAsync`

### GeneticSchedulerTests
#### SolveAsync_SingleTask / NoFreeTime
- 输入：任务与 busy
- 输出：无
- 副作用：无
- 步骤：
  1. 有空闲 → AlgorithmName=`genetic`、Slots 非空
  2. 整段 busy → Slots 空
- 分支与异常：无
- 调用：`GeneticScheduler.SolveAsync`

### CspSchedulerTests
#### SolveAsync_AssignsTasksWithConstraints / RelaxesConstraintsForOversized
- 输入：多任务或超时长任务
- 输出：无
- 副作用：无
- 步骤：
  1. 正常约束 → AlgorithmName=`csp`
  2. 时长大于窗口但带 minimum segment/deadline → 仍产出 slots（松弛）
- 分支与异常：无
- 调用：`CspScheduler.SolveAsync`

## 近逐行中文伪代码

1. ComputeFreeSlots：空/中间/外置/重叠 busy 四场景
2. Greedy：空解、单任务、按 priority 排序排程
3. Genetic：有解与无空闲
4. Csp：约束分配与 oversized 松弛

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/SchedulingAlgorithmsTests.cs",
      "label": "SchedulingAlgorithmsTests",
      "path": "tests/Pim.UnitTests/Services/SchedulingAlgorithmsTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/SchedulingAlgorithmsTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/SchedulingAlgorithmsTests.cs", "to": "src/modules/Pim.Module.Calendar/Services/SchedulingAlgorithms.cs", "type": "tests" }
  ]
}
```
