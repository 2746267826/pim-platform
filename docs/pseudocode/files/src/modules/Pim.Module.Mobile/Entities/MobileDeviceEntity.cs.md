# src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端设备注册表实体（表 `mobile_devices`）：用户维度设备标识、硬件/OS/App 元数据、注册与最后可见时间。
- 主要依赖：DataAnnotations / Column 映射；PostgreSQL `jsonb` MetadataJson
- 被谁使用：`MobileDeviceService` 注册/列表；`PimDbContext` 模块程序集扫描；用量/位置服务按 DeviceId 关联

## 函数级结构化伪代码

### MobileDeviceEntity
#### 属性集合
- 输入：属性赋值
- 输出：实体状态
- 副作用：无（纯 POCO）
- 步骤：
  1. 表名 `mobile_devices`；sealed class
  2. `Id` Key，默认 `Guid.NewGuid()`
  3. `UserId` 归属用户
  4. `DeviceId` MaxLength 128：客户端设备标识
  5. `DeviceHash` MaxLength 256：设备指纹/哈希
  6. `DisplayName` MaxLength 256
  7. `Manufacturer`/`Brand`/`Model` MaxLength 128
  8. `OsVersion` MaxLength 64；`ApiLevel` int
  9. `AppVersion` MaxLength 64
  10. `MetadataJson` jsonb，默认 `"{}"`
  11. `RegisteredAtUtc`/`LastSeenAtUtc` 默认 UtcNow
  12. `CreatedAt`/`UpdatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema
2. 命名空间 `Pim.Module.Mobile.Entities`
3. `[Table("mobile_devices")]` sealed 类
4. Id/UserId/DeviceId/DeviceHash/DisplayName
5. Manufacturer/Brand/Model/OsVersion/ApiLevel/AppVersion
6. MetadataJson jsonb 默认空对象
7. RegisteredAtUtc/LastSeenAtUtc/CreatedAt/UpdatedAt 默认当前 UTC

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs",
      "label": "MobileDeviceEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileDeviceService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileDeviceEntity.cs", "type": "depends_on" }
  ]
}
```
