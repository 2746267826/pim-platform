# src/modules/Pim.Module.Stats/Services/StatsService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Stats
- 职责：批量写入应用使用统计（AppUsage），并清理 30 天前旧记录。
- 主要依赖：`PimDbContext`、`UploadBatch`/`AppUsageEntity`、EF Core
- 被谁使用：Stats 模块端点（上传 batch 时注入调用）

## 函数级结构化伪代码

### StatsService
#### StatsService(PimDbContext db)
- 输入：数据库上下文
- 输出：实例
- 副作用：保存 `_db` 字段
- 步骤：赋值构造
- 分支与异常：无
- 调用：无

#### Task<int> IngestBatchAsync(UploadBatch batch, CancellationToken ct)
- 输入：上传批次（DeviceId + Entries）；取消令牌
- 输出：本次新增实体条数
- 副作用：插入 `AppUsageEntity`；可能删除 30 天前数据并两次 `SaveChanges`
- 步骤：
  1. 取 UTC `now`
  2. 将每条 Entry 映射为实体：DeviceId、PackageName、毫秒 epoch→`DateTimeOffset` 的 Start/End/LastTimeUsed、DurationMs、CreatedAt=now
  3. `AddRange` + `SaveChangesAsync`
  4. 计算 cutoff=`now-30d`；查询 `CreatedAt < cutoff` 的旧记录
  5. 若有旧记录：`RemoveRange` + 再次 `SaveChangesAsync`
  6. 返回新增数量
- 分支与异常：旧记录为空则跳过清理；EF/取消异常向上传播
- 调用：`DateTimeOffset.FromUnixTimeMilliseconds`、`_db.Set<AppUsageEntity>()`、`SaveChangesAsync`、`ToListAsync`

## 近逐行中文伪代码

1. 引入 EF Core、`PimDbContext`、Stats DTO 与 Entities
2. 命名空间 `Pim.Module.Stats.Services`
3. 类 `StatsService`：构造注入 `_db`
4. `IngestBatchAsync`：记当前 UTC
5. 将 batch.Entries 投影为 `AppUsageEntity` 列表（epoch 毫秒转时间、绑定 DeviceId）
6. AddRange 并保存
7. 计算 30 天 cutoff；查出更早 CreatedAt 记录
8. 有旧数据则 RemoveRange 再保存
9. 返回本次 entities.Count

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Stats/Services/StatsService.cs",
      "label": "StatsService",
      "path": "src/modules/Pim.Module.Stats/Services/StatsService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Stats/Services/StatsService.cs.md",
      "layer": "module.stats",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Stats/Services/StatsService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/Services/StatsService.cs", "to": "src/modules/Pim.Module.Stats/Entities/AppUsageEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/Services/StatsService.cs", "to": "src/modules/Pim.Module.Stats/DTOs/StatsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/modules/Pim.Module.Stats/Services/StatsService.cs", "type": "depends_on" }
  ]
}
```
