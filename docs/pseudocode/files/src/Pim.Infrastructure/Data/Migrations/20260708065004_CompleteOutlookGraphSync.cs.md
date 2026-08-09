# src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF Core 迁移 `CompleteOutlookGraphSync`：补齐 Outlook Graph 同步所需列与索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移管线；Calendar/Outlook 同步读写 `events` 与 `outlook_connections`

## 函数级结构化伪代码

### CompleteOutlookGraphSync
#### protected override void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：改表加列加索引
- 步骤：
  1. `outlook_connections` 增加可空 `access_token_expires_at`（timestamptz）
  2. `events` 增加可空 `outlook_change_key`、`outlook_etag`（varchar 255）
  3. 索引：`IX_events_outlook_change_key`、`IX_events_outlook_event_id`
- 分支与异常：无
- 调用：`AddColumn`、`CreateIndex`

#### protected override void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：回滚列与索引
- 步骤：先 Drop 两索引，再 Drop 三列
- 分支与异常：无
- 调用：`DropIndex`、`DropColumn`

## 近逐行中文伪代码

1. partial 类 `CompleteOutlookGraphSync : Migration`
2. Up：连接表加 access_token 过期时间
3. Up：事件表加 change_key 与 etag
4. Up：为 change_key 与 outlook_event_id 建索引
5. Down：逆序删索引与列

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs",
      "label": "CompleteOutlookGraphSync",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260708065004_CompleteOutlookGraphSync.cs", "type": "depends_on" }
  ]
}
```
