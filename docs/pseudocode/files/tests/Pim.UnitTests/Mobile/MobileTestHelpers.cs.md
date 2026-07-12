# tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Mobile 单元测试共享：模块注册、InMemory Db、固定用户与时间。
- 主要依赖：`MobileModule`、`PimDbContext`、`ICurrentUserService`
- 被谁使用：各 Mobile*Tests

## 函数级结构化伪代码

### MobileTestHelpers
- UserId 常量
- RegisterMobileModule：new MobileModule().RegisterServices
- CreateDb：注册后 InMemory
- CurrentUser / Time：Stub 与 FixedTimeProvider

## 近逐行中文伪代码

1. [L1-L22] UserId/Register/CreateDb
2. [L24-L51] CurrentUser/Time 与私有实现

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs",
      "label": "MobileTestHelpers",
      "path": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs", "to": "src/modules/Pim.Module.Mobile/MobileModule.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Mobile/MobileTestHelpers.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" }
  ]
}
```
