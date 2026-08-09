# tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：验证模块程序集在核心模型构建后注册仍能进入 EF 模型缓存。
- 主要依赖：`PimDbContext.RegisterModuleAssembly`、IEntityTypeConfiguration 金丝雀实体
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ModelCache_UsesModuleAssembliesRegisteredAfterCoreModelIsBuilt
- 步骤：
  1. 先建 coreDb：找不到 ModelCacheCanaryEntity
  2. RegisterModuleAssembly(本测试程序集)
  3. 再建 moduleDb：能找到 Canary 实体类型

### ModelCacheCanaryEntity / Configuration
- 步骤：Guid Id；表 model_cache_canaries

## 近逐行中文伪代码

1. [L10-19] 注册前模型无 canary
2. [L21-28] 注册后模型有 canary
3. [L31-43] 内嵌实体与配置

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs",
      "label": "PimDbContextModelCacheTests",
      "path": "tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "tests/Pim.UnitTests/Operations/PimDbContextModelCacheTests.cs",
      "to": "src/Pim.Infrastructure/Data/PimDbContext.cs",
      "type": "tests"
    }
  ]
}
```
