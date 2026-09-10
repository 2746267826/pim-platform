# src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：移动端按窗口聚合的使用时长汇总实体（package + 时间窗 + 可见时长 + 来源与质量 JSON）。
- 主要依赖：`System.ComponentModel.DataAnnotations`
- 被谁使用：`MobileQualityService`、使用查询/摄入服务、`PimDbContext` 表 `mobile_usage_summaries`

## 函数级结构化伪代码

### MobileUsageSummaryEntity
#### 属性与默认值
- 输入：无（EF 实体属性）
- 输出：表行字段
- 副作用：无
- 步骤：
  1. 映射表 `mobile_usage_summaries`
  2. `Id` 主键 Guid
  3. `UserId` / `DeviceId`（设备最长 128）
  4. `PackageName` 应用包名最长 256
  5. `WindowStartUtc` / `WindowEndUtc` 汇总时间窗
  6. `TotalTimeVisibleMs` 可见总时长毫秒
  7. `LastTimeUsedUtc` 可选最近使用时间
  8. `SourceKind` 来源类型字符串
  9. `RawJson` jsonb，默认 `"{}"`
  10. `QualityFlagsJson` jsonb，默认 `"[]"`
  11. `CreatedAt` / `UpdatedAt` UTC
- 分支与异常：无运行时逻辑
- 调用：无

## 近逐行中文伪代码

1. 引入 DataAnnotations / Schema
2. sealed 类映射 `mobile_usage_summaries`
3. Id、UserId、DeviceId、PackageName
4. WindowStartUtc、WindowEndUtc、TotalTimeVisibleMs、LastTimeUsedUtc
5. SourceKind、RawJson、QualityFlagsJson
6. CreatedAt、UpdatedAt

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs",
      "label": "MobileUsageSummaryEntity",
      "path": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs.md",
      "layer": "module.mobile",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileQualityService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileUsageSummaryEntity.cs", "type": "depends_on" }
  ]
}
```
