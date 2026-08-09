# src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `AddMobileAnalytics` 目标模型快照；在既有 Auth/运营/AI/Calendar/Files/PcTracker/QuickNotes 之上固化 Mobile 分析相关表
- 主要依赖：EF Core Migrations、Npgsql、`PimDbContext`
- 被谁使用：EF 工具链与 `AddMobileAnalytics` partial

## 函数级结构化伪代码

### AddMobileAnalytics（partial）
#### 特性
- 输入：无
- 输出：绑定 `PimDbContext` 与迁移 Id `20260707145521_AddMobileAnalytics`
- 副作用：无
- 步骤：DbContext/Migration 注解
- 分支与异常：无
- 调用：无

#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：描述约 50 实体 / 约 50 表的完整目标模型
- 步骤：
  1. 模型级注解 ProductVersion 8.0.11
  2. 先映射既有核心与 AI 实体（`AiProviderSetting`/`AiRequestLog`/Audit/Daemon/Auth 等）
  3. Calendar 实体集；Files 模块七表（`file_items`/`file_chunks`/`file_versions`/`file_providers`/`file_index_jobs`/`file_suggestions`/`file_ai_results`）
  4. **本迁移增量焦点 — Mobile 实体**（均在 `Pim.Module.Mobile.Entities`）：
     - 目录与规则：`MobileAppCatalog`、`MobileAppCatalogOverride`、`MobileAppCategoryRule`
     - 设备与同步：`MobileDevice`、`MobileSyncBatch`
     - 位置：`MobileLocationPoint`
     - 时间线：`MobileTimelineBlock`
     - 用量：`MobileUsageEvent`/`Session`/`Aggregate`/`Summary`/`Goal`
  5. PcTracker 扩展：含 `ActivityClassificationAudit`、`AppKnowledgeContext`、`AppSignature`、`PcCategory` 等
  6. QuickNotes；关系与 Navigation 收尾
- 分支与异常：无
- 调用：Fluent `Entity`/`Property`/`HasIndex`/`ToTable`/`HasOne`

### 本快照 Mobile 表清单
- `mobile_app_catalog`、`mobile_app_catalog_overrides`、`mobile_app_category_rules`
- `mobile_devices`、`mobile_sync_batches`、`mobile_location_points`
- `mobile_timeline_blocks`
- `mobile_usage_aggregates`、`mobile_usage_events`、`mobile_usage_goals`、`mobile_usage_sessions`、`mobile_usage_summaries`

### 对应 Up 迁移意图（对照 .cs）
- 创建 Mobile 分析相关表（目录覆盖、分类规则、时间线块、用量会话/汇总等）
- Designer 则快照「迁移后」全库模型，而非仅增量 DDL

## 近逐行中文伪代码

1. auto-generated 头与标准 EF/Npgsql using
2. partial `AddMobileAnalytics` 标注迁移 Id
3. `BuildTargetModel`：注解 → 全量实体属性映射（uuid、jsonb 默认 `[]`/`{}`、时间默认 now、varchar 长度）
4. Mobile 实体字段典型模式：user_id、device_id、package_name、life_category、timezone 默认 Asia/Shanghai、质量/来源 JSON
5. Files/PcTracker 等既有模块实体完整复述（Designer 惯例：每迁移全量快照）
6. 尾部 FK/Navigation
7. pragma restore 结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs",
      "label": "AddMobileAnalytics.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs", "to": "src/modules/Pim.Module.Mobile/Entities", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.Designer.cs", "to": "src/modules/Pim.Module.Files/Entities", "type": "depends_on" }
  ]
}
```
