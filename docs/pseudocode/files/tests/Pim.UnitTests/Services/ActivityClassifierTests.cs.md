# tests/Pim.UnitTests/Services/ActivityClassifierTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：活动分类规则持久化与 `ActivityClassifier.Classify` 规则/启发式优先级。
- 主要依赖：`PcTrackerService`、`ActivityClassifier`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. SaveActivityClassificationRule 持久化并列表
2. 用户规则胜启发式；GitHub→编程+项目标签；docs→学习
3. 未知 fallback；非 active 规则忽略
4. 终端/办公/文件启发式
5. 空白分类规则回落；低置信内置浏览器不赢 docs；project scope 不挡 activity 启发式；app scope 兼容；docs.github→学习

## 近逐行中文伪代码

1. Save 规则服务路径
2. 多个 Classify 静态场景 + CreateContext helper

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityClassifierTests.cs",
      "label": "ActivityClassifierTests",
      "path": "tests/Pim.UnitTests/Services/ActivityClassifierTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityClassifierTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityClassifierTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/ActivityClassifierTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "tests" }
  ]
}
```
