# src/modules/Pim.Module.Stats/Entities/AppUsageEntityConfiguration.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Stats
- 职责：`AppUsageEntity` 的 EF 索引配置（设备、包名、创建时间）。
- 主要依赖：`Microsoft.EntityFrameworkCore`、`AppUsageEntity`
- 被谁使用：Stats 模块 DbContext 模型配置

## 函数级结构化伪代码

### AppUsageEntityConfiguration
#### `void Configure(EntityTypeBuilder<AppUsageEntity> builder)`
- 输入：`AppUsageEntity` builder
- 输出：无
- 副作用：为三列建立单列索引
- 步骤：
  1. `HasIndex(DeviceId)`。
  2. `HasIndex(PackageName)`。
  3. `HasIndex(CreatedAt)`。
- 分支与异常：无
- 调用：EF Fluent API

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Stats.Entities`。
2. 类实现 `IEntityTypeConfiguration<AppUsageEntity>`。
3. Configure 仅为 `DeviceId`、`PackageName`、`CreatedAt` 各建索引，无默认值/关系配置。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Stats/Entities/AppUsageEntityConfiguration.cs",
      "label": "AppUsageEntityConfiguration",
      "path": "src/modules/Pim.Module.Stats/Entities/AppUsageEntityConfiguration.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Stats/Entities/AppUsageEntityConfiguration.cs.md",
      "layer": "module.stats",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Stats/Entities/AppUsageEntityConfiguration.cs", "to": "src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Stats/Entities/AppUsageEntityConfiguration.cs", "type": "depends_on" }
  ]
}
```
