# src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 应用名/模式到分类的映射规则实体（含颜色、优先级、内置标记）。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`System.ComponentModel.DataAnnotations.Schema`
- 被谁使用：PcTracker 分类/时间线服务、EF 映射与迁移

## 函数级结构化伪代码

### AppCategoryEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务或种子数据赋值后由 EF 持久化
- 输出：应用分类规则表一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 主键 `Id`（Guid）。
  2. `AppPattern` 最长 128：匹配应用名/模式。
  3. `CategoryName` 最长 64：目标分类名。
  4. `Color` 最长 7，默认 `#6B5EE4`。
  5. `Priority`：匹配优先级。
  6. `IsBuiltin`：是否内置规则。
  7. `CreatedAt` 默认 `DateTime.UtcNow`。
- 分支与异常：本类型无校验逻辑
- 调用：被 PcTracker 服务读写

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema。
2. 命名空间 `Pim.Module.PcTracker.Entities`；类 `AppCategoryEntity`。
3. `Id` 主键列 id。
4. `AppPattern` MaxLength 128；`CategoryName` MaxLength 64。
5. `Color` MaxLength 7 默认紫色；`Priority`；`IsBuiltin`。
6. `CreatedAt` 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs",
      "label": "AppCategoryEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppCategoryEntity.cs", "type": "depends_on" }
  ]
}
```
