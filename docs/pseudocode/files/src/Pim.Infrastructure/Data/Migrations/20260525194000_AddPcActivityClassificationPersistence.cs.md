# src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移：创建 PC 活动分类设置与分类结果表，并种子默认 settings 行。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations.MigrationBuilder`
- 被谁使用：EF 迁移流水线（`dotnet ef database update` / 应用启动迁移）

## 函数级结构化伪代码

### AddPcActivityClassificationPersistence
#### void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：创建表与索引；插入默认 settings
- 步骤：
  1. 创建 `pc_activity_classification_settings`（id、settings_key 默认 default、推荐最短分类分钟数默认 5、created/updated）
  2. 创建 `pc_activity_classifications`（record_key/type、device、source_event_ids jsonb、时间窗、类别/颜色/项目标签、confidence、source、rule、explanation、classifier_version、classified_at、audit_id）
  3. settings 上 `settings_key` 唯一索引
  4. classifications 上 category/device/project_tag/source_rule_id/started_at 索引；`record_key` 唯一索引
  5. SQL：插入 settings_key=`default`、分钟=5，`ON CONFLICT DO NOTHING`
- 分支与异常：冲突忽略；DDL 失败由迁移框架抛出
- 调用：`CreateTable`、`CreateIndex`、`Sql`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：删除两张表
- 步骤：Drop settings 表；Drop classifications 表
- 分支与异常：表不存在时由框架处理
- 调用：`DropTable`

## 近逐行中文伪代码

1. 引入 System 与 EF Migrations；nullable disable
2. 命名空间 `Pim.Infrastructure.Data.Migrations`
3. partial 类 `AddPcActivityClassificationPersistence` 继承 `Migration`
4. `Up`：建 settings 表（uuid 主键 gen_random_uuid、key 默认 default、推荐分钟默认 5、时间默认 NOW）
5. `Up`：建 classifications 表（record_key 等字段；类别默认「其他」、颜色 #64748b、confidence 0.2、source fallback、classifier local-v1）
6. 建唯一/非唯一索引；种子 default settings
7. `Down`：按表名 Drop 两张表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs",
      "label": "AddPcActivityClassificationPersistence",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs", "to": "pc_activity_classification_settings", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260525194000_AddPcActivityClassificationPersistence.cs", "to": "pc_activity_classifications", "type": "depends_on" }
  ]
}
```
