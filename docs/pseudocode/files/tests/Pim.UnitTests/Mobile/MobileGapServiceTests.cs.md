# tests/Pim.UnitTests/Mobile/MobileGapServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：移动回填缺口：14 天钳制、缺失/仅 fallback/当日尾窗、空完成批视为覆盖。
- 主要依赖：`MobileGapService`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. 请求 45 天钳到 14 天
2. 有事件日不报缺口；fallback-only 与 missing-day
3. 当日仅早期覆盖 → missing tail
4. 完成空批视为已覆盖

## 近逐行中文伪代码

1. 四 Fact 种子事件/摘要/批后断言 Windows 列表

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileGapServiceTests.cs",
      "label": "MobileGapServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileGapServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileGapServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileGapServiceTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileGapService.cs", "type": "tests" }
  ]
}
```
