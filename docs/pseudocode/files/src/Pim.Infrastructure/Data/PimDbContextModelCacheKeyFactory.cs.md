# src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：自定义 EF `IModelCacheKeyFactory`，使 `PimDbContext` 模型缓存键包含模块程序集签名，模块集变化时重建模型。
- 主要依赖：`Microsoft.EntityFrameworkCore`、`Microsoft.EntityFrameworkCore.Infrastructure`；`PimDbContext.ModuleAssemblySignature`
- 被谁使用：`PimDbContext.OnConfiguring` 中 `ReplaceService<IModelCacheKeyFactory, PimDbContextModelCacheKeyFactory>()`

## 函数级结构化伪代码

### PimDbContextModelCacheKeyFactory
#### `object Create(DbContext context, bool designTime)`
- 输入：当前 `DbContext` 实例；是否设计时
- 输出：用作模型缓存键的元组对象
- 副作用：无
- 步骤：
  1. 若 `context is PimDbContext`：返回 `(context.GetType(), designTime, PimDbContext.ModuleAssemblySignature)`
  2. 否则：返回 `(context.GetType(), designTime)`（默认语义）
- 分支与异常：非 Pim 上下文走短键；无显式抛错
- 调用：`context.GetType()`；读静态 `ModuleAssemblySignature`

## 近逐行中文伪代码

1. 引入 EF Core 与 Infrastructure
2. 命名空间 `Pim.Infrastructure.Data`
3. 密封类实现 `IModelCacheKeyFactory`
4. 方法 `Create`：
5.   若上下文为 `PimDbContext`，缓存键三元组含模块程序集签名
6.   否则缓存键仅为类型 + designTime
7. 返回该 object 供 EF 模型缓存使用

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs",
      "label": "PimDbContextModelCacheKeyFactory",
      "path": "src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs", "to": "Microsoft.EntityFrameworkCore.Infrastructure.IModelCacheKeyFactory", "type": "implements" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/PimDbContextModelCacheKeyFactory.cs", "type": "depends_on" }
  ]
}
```
