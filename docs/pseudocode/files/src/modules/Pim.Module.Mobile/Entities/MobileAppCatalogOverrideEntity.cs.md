# src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：用户对移动应用目录的覆盖配置（显示名、生活分类、系统噪声、短事件隐藏、备注）。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`System.ComponentModel.DataAnnotations.Schema`、`Pim.Module.Mobile.DTOs.MobileLifeCategories`
- 被谁使用：Mobile 模块应用目录/分析服务、EF 映射与迁移

## 函数级结构化伪代码

### MobileAppCatalogOverrideEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：服务层按用户与包名写入后由 EF 持久化
- 输出：`mobile_app_catalog_overrides` 表一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 表名 `mobile_app_catalog_overrides`；主键 `Id` 默认 `Guid.NewGuid()`。
  2. 归属：`UserId`。
  3. 目标应用：`PackageName`（最长 256，默认空串）。
  4. 覆盖字段：`DisplayNameOverride`（可空，最长 256）。
  5. 生活分类：`LifeCategory` 默认 `MobileLifeCategories.Uncategorized`（最长 128）。
  6. 行为开关：`IsSystemNoise`、`HideShortEvents` 默认 false。
  7. 备注：`Notes` 可空，最长 1024。
  8. 审计：`CreatedAt`/`UpdatedAt` 默认 `DateTimeOffset.UtcNow`。
- 分支与异常：本类型无校验逻辑
- 调用：被 Mobile 服务读写；依赖 DTO 常量 `MobileLifeCategories`

## 近逐行中文伪代码

1. 引入 DataAnnotations、Schema 与 Mobile DTOs。
2. 命名空间 `Pim.Module.Mobile.Entities`；sealed 类映射表 `mobile_app_catalog_overrides`。
3. `Id` 主键默认 NewGuid；`UserId` 列 `user_id`。
4. `PackageName` MaxLength 256 默认空串。
5. `DisplayNameOverride` 可空 MaxLength 256。
6. `LifeCategory` MaxLength 128 默认 Uncategorized。
7. `IsSystemNoise`、`HideShortEvents` 布尔列。
8. `Notes` 可空 MaxLength 1024。
9. `CreatedAt`/`UpdatedAt` 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs",
      "label": "MobileAppCatalogOverrideEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogOverrideEntity.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileDtos.cs", "type": "depends_on" }
  ]
}
```
