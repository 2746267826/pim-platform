# src/modules/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：根据变更字段与来源，将日程/事实操作分类为 L1–L4 风险，并给出二级确认/严格确认标志。
- 主要依赖：`Pim.Core.Operations.OperationRiskLevel`
- 被谁使用：Calendar 写操作、数据中心批量、Outlook 写回前的确认决策

## 函数级结构化伪代码

### ScheduleFactConfirmationPolicy
#### 静态集合 `DestructiveFields` / `CoreFactFields`
- 输入：无
- 输出：字段名集合（忽略大小写）
- 副作用：无
- 步骤：
  1. 破坏性：stop-sync、batch-delete、bulk-writeback、recurrence-wide-delete、book-with-children
  2. 核心事实：title/name/时间字段/due/location/status/project/book/owner/recurrence/rrule/delete/restore/task-segment/habit-rule 等
- 分支与异常：无
- 调用：无

#### `static ScheduleFactConfirmationDecision Classify(source, changedFields, externalWriteback = false)`
- 输入：来源字符串、变更字段列表、是否外部写回
- 输出：`ScheduleFactConfirmationDecision(RiskLevel, RequiresSecondLevelConfirmation, RequiresStrictConfirmation)`
- 副作用：无
- 步骤：
  1. 任一字段 ∈ Destructive → L4，Strict=true，Second=false
  2. 否则 externalWriteback 或 source 等于 outlook（忽略大小写）→ L3，Second=true，Strict=false
  3. 否则任一字段 ∈ CoreFact → L2，两者确认 false
  4. 否则 L1，两者确认 false
- 分支与异常：无异常；集合匹配短路
- 调用：`HashSet.Contains`、`string.Equals`

### ScheduleFactConfirmationDecision
#### `record ScheduleFactConfirmationDecision(OperationRiskLevel RiskLevel, bool RequiresSecondLevelConfirmation, bool RequiresStrictConfirmation)`
- 输入：风险等级与确认标志
- 输出：决策值
- 副作用：无
- 步骤：承载分类结果
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 `Pim.Core.Operations`；静态策略类
2. DestructiveFields 五个破坏性操作名
3. CoreFactFields 标题/时间/状态/归属/复发/删除恢复/段与习惯等
4. Classify：先扫破坏性 → L4 严格确认
5. 再判断外部写回或 outlook 源 → L3 二级确认
6. 再扫核心事实 → L2
7. 默认 L1
8. 记录类型打包 RiskLevel 与两个 bool

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs",
      "label": "ScheduleFactConfirmationPolicy",
      "path": "src/modules/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs.md",
      "layer": "module.calendar",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Calendar/Services", "to": "src/modules/Pim.Module.Calendar/Services/ScheduleFactConfirmationPolicy.cs", "type": "calls" }
  ]
}
```
