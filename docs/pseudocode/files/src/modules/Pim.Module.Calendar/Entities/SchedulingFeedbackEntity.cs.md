# src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Calendar
- 职责：记录用户对排程方案选项的选择反馈，映射表 `scheduling_feedback`。
- 主要依赖：DataAnnotations/Schema（jsonb 列）
- 被谁使用：排程反馈写入/查询服务与 EF 映射

## 函数级结构化伪代码

### SchedulingFeedbackEntity
#### 属性与默认值（无自定义方法）
- 输入：无
- 输出：字段读写
- 副作用：无
- 步骤：
  1. 表 `scheduling_feedback`
  2. `Id` 默认 NewGuid
  3. `UserId` 归属用户
  4. `PlanOptions` jsonb 默认 `[]`（方案列表）
  5. `SelectedIndex` 用户选中下标
  6. `Context` 可选 jsonb 上下文
  7. `CreatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema
2. 命名空间 `Pim.Module.Calendar.Entities`
3. `[Table("scheduling_feedback")]` 定义实体
4. 主键 id；user_id；plan_options jsonb；selected_index；context jsonb；created_at

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs",
      "label": "SchedulingFeedbackEntity",
      "path": "src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs.md",
      "layer": "module.calendar",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Calendar/Entities/SchedulingFeedbackEntity.cs", "type": "depends_on" }
  ]
}
```
