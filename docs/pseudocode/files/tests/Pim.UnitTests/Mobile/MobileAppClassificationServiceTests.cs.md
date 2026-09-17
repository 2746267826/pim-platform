# tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖移动应用分类优先级：用户覆盖 > 规则 > 元数据/内置 > fallback。
- 主要依赖：`MobileAppClassificationService`、`MobileTestHelpers`
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Override 胜出 user-override
2. 规则 exact/prefix/keyword 源标签
3. 元数据/内置包/未知 fallback
4. 数字 Android 类别映射 Theory
5. 系统噪声包 IsSystemNoise
6. DisplayName：最新 catalog → 内置友好名 → package

## 近逐行中文伪代码

1. [L1-L51] override 优先
2. [L53-L78] 规则阶梯
3. [L80-L100] 元数据/内置/fallback
4. [L102-L161] Theory 与显示名
5. [L163-L201] helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs",
      "label": "MobileAppClassificationServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileAppClassificationServiceTests.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "type": "tests" }
  ]
}
```
