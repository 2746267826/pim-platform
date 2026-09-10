# tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：位置点提交精度/坐标校验与历史过滤。
- 主要依赖：`MobileLocationService`、DomainException 6201/6202
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Submit：<50m usable；=50 抛 6202；>50 落库 rejected；非法坐标 6201；空海拔可接受
### GetHistory：排除 rejected 与 ≥50 精度点

## 近逐行中文伪代码

1. [L12-74] 提交校验
2. [L76-96] 空海拔
3. [L98-136] 历史过滤
4. [L138+] Request 工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs",
      "label": "MobileLocationServiceTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileLocationServiceTests.cs", "to": "src/Pim.Module.Mobile/Services/MobileLocationService.cs", "type": "tests" }
  ]
}
```
