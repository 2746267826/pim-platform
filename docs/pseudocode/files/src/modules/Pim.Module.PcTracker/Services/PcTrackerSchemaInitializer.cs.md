# src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：通过原始 SQL 幂等创建/补齐 PcTracker PostgreSQL 表、索引、种子数据（AW buckets/events、keystats、分类规则、应用签名知识库、Phase2 分类树等）。
- 主要依赖：`PimDbContext`、`Microsoft.EntityFrameworkCore`（ExecuteSqlRawAsync）
- 被谁使用：模块启动/初始化路径注册并调用 `InitializeAsync`

## 函数级结构化伪代码

### PcTrackerSchemaInitializer
#### 常量 SchemaSql
- 输入：无
- 输出：大段 DDL+DML 字符串
- 副作用：无（仅常量）
- 步骤：按块定义：
  1. `pc_aw_buckets` + 唯一/类型/seen_at 索引。
  2. `pc_aw_events` 建表 + 设备/时间/类型索引；ALTER 补列；bucket/source/app_normalized 索引；部分唯一 (device,bucket,source_event_id)。
  3. `pc_keystats_samples` + 设备分钟唯一与 stats_date 索引。
  4. `pc_app_categories`；`pc_activity_category_rules` 及状态/优先级/名称索引；`pc_activity_classification_suggestions`。
  5. `pc_activity_classifications` + 元数据列 ALTER 与索引；`pc_activity_classification_audits`；`pc_activity_classification_settings` 默认行。
  6. `pc_app_signatures` 与 `pc_app_knowledge_contexts`；大量 builtin 应用签名 INSERT ON CONFLICT DO NOTHING。
  7. 内置 activity category rules INSERT；从 `pc_app_categories` 迁移规则。
  8. Phase2 `pc_categories` 层次分类树表。
- 分支与异常：SQL 侧 IF NOT EXISTS / ON CONFLICT 保证幂等
- 调用：无

#### 构造(PimDbContext db)
- 输入：db
- 输出：实例
- 副作用：无
- 步骤：保存 _db
- 分支与异常：无
- 调用：无

#### InitializeAsync(ct)
- 输入：CancellationToken
- 输出：Task
- 副作用：对数据库执行 SchemaSql
- 步骤：`_db.Database.ExecuteSqlRawAsync(SchemaSql, ct)`
- 分支与异常：SQL/连接异常上抛
- 调用：EF Database facade

## 近逐行中文伪代码

1. 命名空间 Services；sealed 类；常量 SchemaSql 多表 DDL。
2. 覆盖 AW 桶/事件、键盘统计、旧 app_categories、活动规则/建议/分类结果/审计/设置。
3. 应用签名知识库种子 200+ 进程；内置规则与迁移 INSERT。
4. Phase2 pc_categories 树表。
5. 构造注入 PimDbContext。
6. InitializeAsync 原样执行 SchemaSql。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs",
      "label": "PcTrackerSchemaInitializer",
      "path": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AwEventEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "type": "calls" }
  ]
}
```
