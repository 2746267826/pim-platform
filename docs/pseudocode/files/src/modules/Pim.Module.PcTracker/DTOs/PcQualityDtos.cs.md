# src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC Tracker 数据质量/健康检查响应 DTO：总览、组件明细、问题列表、下一步建议。
- 主要依赖：`Pim.Core.Operations.PimHealthStatus`
- 被谁使用：PcTracker 质量查询服务与对应 HTTP 端点

## 函数级结构化伪代码

### PcQualityResponse
#### record 字段
- 输入：构造赋值
- 输出：不可变响应
- 副作用：无
- 步骤：
  1. `OverallStatus`：整体健康
  2. `Label`/`Message`：展示文案
  3. `CheckedAt`：检查时刻
  4. `Components`：组件状态列表
  5. `Issues`：问题列表
  6. `NextSteps`：建议步骤字符串列表
- 分支与异常：无
- 调用：无

### PcQualityComponentDto
#### record 字段
- 输入：构造赋值
- 输出：组件 DTO
- 副作用：无
- 步骤：`Key`/`Name`/`Status`/`Message`/`Details` 字典
- 分支与异常：无
- 调用：无

### PcQualityIssueDto
#### record 字段
- 输入：构造赋值
- 输出：问题 DTO
- 副作用：无
- 步骤：`Code`/`Severity`/`ComponentKey`/`Message`/可选 `NextStep`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.DTOs`；using Operations
2. `PcQualityResponse`：总状态+标签消息+检查时间+组件+问题+下一步
3. `PcQualityComponentDto`：键名状态消息与 Details 字典
4. `PcQualityIssueDto`：代码严重度组件键消息与可选下一步

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs",
      "label": "PcQualityDtos",
      "path": "src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs.md",
      "layer": "module.pctracker",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs", "to": "src/Pim.Core/Operations", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services", "to": "src/modules/Pim.Module.PcTracker/DTOs/PcQualityDtos.cs", "type": "depends_on" }
  ]
}
```
