# tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：规则/建议预览与应用重算：优先级、手工保护、业务日、重复拒绝、建议二次应用。
- 主要依赖：ActivityClassificationRecomputeService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### PreviewRuleAsync / ApplyRuleAsync / RecomputeAsync
### 优先级与手工快照保护 / 域名与 bucket 规则
### Preview/ApplySuggestion / 非法 range 与 conditions

## 近逐行中文伪代码

1. 预览不落库
2. 应用写规则+审计
3. 优先级/CreatedAt/manual 保护
4. 建议应用与二次拒绝
5. 非法 range/JSON

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs",
      "label": "ActivityClassificationRecomputeServiceTests.cs",
      "path": "tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Services/ActivityClassificationRecomputeServiceTests.cs","to":"src/Pim.Module.PcTracker/Services/ActivityClassificationRecomputeService.cs","type":"tests"}]
}
```