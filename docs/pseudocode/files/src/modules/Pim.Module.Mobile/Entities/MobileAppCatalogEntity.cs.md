# src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端已安装应用目录表实体，按用户与设备记录包名、显示名、版本与安装元数据。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`System.ComponentModel.DataAnnotations.Schema`
- 被谁使用：Mobile 模块同步/查询服务、EF 映射与迁移

## 函数级结构化伪代码

### MobileAppCatalogEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：同步/服务层赋值后由 EF 持久化
- 输出：`mobile_app_catalog` 表一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 表名 `mobile_app_catalog`；主键 `Id` 默认 `Guid.NewGuid()`。
  2. 归属：`UserId`、`DeviceId`（最长 128）。
  3. 应用标识：`PackageName`、`DisplayName`（各最长 256）。
  4. 版本：`VersionName`（可空）、`VersionCode`（可空 long）。
  5. 标志与分类：`IsSystemApp`、`Category`、`InstallerPackage`。
  6. 安装时间：`FirstInstallTimeUtc`、`LastUpdateTimeUtc`（可空）。
  7. 原始载荷：`RawJson` 列类型 jsonb，默认 `"{}"`。
  8. 审计：`CreatedAt`/`UpdatedAt` 默认 `DateTimeOffset.UtcNow`。
- 分支与异常：本类型无校验逻辑
- 调用：被 Mobile 服务读写

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema。
2. 命名空间 `Pim.Module.Mobile.Entities`；sealed 类映射表 `mobile_app_catalog`。
3. `Id` 主键默认 NewGuid；`UserId`；`DeviceId` MaxLength 128。
4. `PackageName`/`DisplayName` MaxLength 256。
5. `VersionName`/`VersionCode` 可空；`IsSystemApp`；`Category`/`InstallerPackage` 可空。
6. `FirstInstallTimeUtc`/`LastUpdateTimeUtc` 可空。
7. `RawJson` jsonb 默认 `"{}"`；`CreatedAt`/`UpdatedAt` 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs",
      "label": "MobileAppCatalogEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs", "type": "depends_on" }
  ]
}
```
