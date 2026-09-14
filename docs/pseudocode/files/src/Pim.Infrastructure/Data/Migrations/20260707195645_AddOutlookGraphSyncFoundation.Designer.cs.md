# src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs

## 元信息
- 语言：C#（EF Core 自动生成 Designer）
- 程序集或包：Pim.Infrastructure
- 职责：迁移 `AddOutlookGraphSyncFoundation` 目标模型：Outlook 连接 Graph 同步基础字段 + `outlook_sync_batches` 表；并含当时全库实体（含 Mobile、TaskExecutionSegment 等）。
- 主要依赖：EF Core / Npgsql / `PimDbContext` / Calendar 与其它模块实体类型名
- 被谁使用：EF 迁移工具链；与同名 `.cs` `Up`/`Down` 配对

## 函数级结构化伪代码

### AddOutlookGraphSyncFoundation（partial）
#### protected override void BuildTargetModel(ModelBuilder modelBuilder)
- 输入：`ModelBuilder`
- 输出：无
- 副作用：完整目标模型（约 52 表）
- 步骤：
  1. 模型注解 + Npgsql identity
  2. 配置 AI/运维/用户/Files/Mobile/PcTracker/QuickNotes/Calendar 全量实体
  3. **Outlook 焦点**：
     - `OutlookConnectionEntity` 增列：`ClientId`、`DeltaLink`、`LastError`、`Provider`(默认 outlook)、`Scopes`(默认 Graph 日历权限串)、`Status`(not-connected)、`TenantId`(common)、`TokenHealth`(missing)；UserId 唯一
     - `OutlookSyncBatchEntity`→`outlook_sync_batches`：user_id、provider、status(running)、read/created/updated/conflict/confirmation/failure 计数、steps_json/errors_json、error_summary、started_at/finished_at；索引 UserId、UserId+StartedAt、UserId+Provider+StartedAt
  4. 配置 `TaskExecutionSegmentEntity` 等当时已有关系
  5. FK/Navigation 收尾
- 分支与异常：无
- 调用：Fluent API

## 近逐行中文伪代码

1. auto-generated；Migration Id `20260707195645_AddOutlookGraphSyncFoundation`
2. `BuildTargetModel` 写全库快照
3. 配置 `OutlookConnection`：token 密文字节 + Graph 连接元数据列
4. 配置 `OutlookSyncBatch`：同步批次统计与 jsonb 步骤/错误
5. 配置 Mobile 多表、PcTracker、Files、AI 等其余实体
6. 关系与导航；pragma restore
7. （业务增量见同名非 Designer：`AddColumn`×8 + `CreateTable outlook_sync_batches` + 索引）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs",
      "label": "AddOutlookGraphSyncFoundation.Designer",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707195645_AddOutlookGraphSyncFoundation.Designer.cs", "to": "src/modules/Pim.Module.Calendar", "type": "depends_on" }
  ]
}
```
