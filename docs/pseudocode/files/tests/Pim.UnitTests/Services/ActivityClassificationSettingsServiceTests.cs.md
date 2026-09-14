# tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：活动分类最短时长设置默认、钳制预设与更新同一行。
- 主要依赖：`ActivityClassificationSettingsService`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Get 默认 5 分钟且不落库
2. Save 7→钳到 5 并持久化 default 键
3. Save 10 更新已有行 Id 不变

## 近逐行中文伪代码

1. [L1-L22] 默认不持久化
2. [L24-L38] 钳制
3. [L40-L63] 更新
4. [L65-L73] CreateDb

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs",
      "label": "ActivityClassificationSettingsServiceTests",
      "path": "tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/ActivityClassificationSettingsServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSettingsService.cs", "type": "tests" }
  ]
}
```
