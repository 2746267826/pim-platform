# src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端单次应用使用会话实体，记录用户/设备/包名、起止时间、时长与质量标记。
- 主要依赖：`System.ComponentModel.DataAnnotations`、`System.ComponentModel.DataAnnotations.Schema`
- 被谁使用：Mobile 使用量同步/分析服务、EF 映射与迁移

## 函数级结构化伪代码

### MobileUsageSessionEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：客户端同步或服务层写入后由 EF 持久化
- 输出：`mobile_usage_sessions` 表一行
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. 表名 `mobile_usage_sessions`；主键 `Id` 默认 `Guid.NewGuid()`。
  2. 归属：`UserId`、`DeviceId`（最长 128）。
  3. 应用：`PackageName`（最长 256）。
  4. 时间：`StartUtc` 必填；`EndUtc` 可空（会话未结束时可空）。
  5. 时长：`DurationMs` 可空 long。
  6. 质量：`QualityFlagsJson` 列类型 jsonb，默认 `"[]"`。
  7. 审计：`CreatedAt` 默认 `DateTimeOffset.UtcNow`。
- 分支与异常：本类型无校验逻辑
- 调用：被 Mobile 服务读写

## 近逐行中文伪代码

1. 引入 DataAnnotations 与 Schema。
2. 命名空间 `Pim.Module.Mobile.Entities`；sealed 类映射表 `mobile_usage_sessions`。
3. `Id` 主键默认 NewGuid；`UserId`；`DeviceId` MaxLength 128。
4. `PackageName` MaxLength 256。
5. `StartUtc`；`EndUtc` 可空；`DurationMs` 可空。
6. `QualityFlagsJson` jsonb 默认 `"[]"`；`CreatedAt` 默认 UtcNow。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs",
      "label": "MobileUsageSessionEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSessionEntity.cs", "type": "depends_on" }
  ]
}
```
