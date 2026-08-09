# src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF Core 迁移 `AddMobileAnalytics`：创建移动端用量分析相关 5 张表与索引。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移管线；Mobile 模块分析/目标/时间线读模型

## 函数级结构化伪代码

### AddMobileAnalytics
#### protected override void Up(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：创建 5 表 + 多组索引
- 步骤：
  1. `mobile_app_catalog_overrides`：用户对包名的展示名/生活分类/系统噪声/短事件隐藏覆盖
  2. `mobile_app_category_rules`：规则类型+pattern → life_category，优先级与启用
  3. `mobile_timeline_blocks`：设备时间块聚合（起止、本地日、分类、前台秒、top_apps_json 等）
  4. `mobile_usage_aggregates`：按粒度桶的包/分类用量聚合（唯一键含 device+granularity+bucket+package+category）
  5. `mobile_usage_goals`：总时长/包/分类目标 limit_seconds
  6. 为 user_id 组合建唯一与过滤索引（package、life_category、is_stale、is_enabled 等）
- 分支与异常：无
- 调用：`CreateTable`、`CreateIndex`

#### protected override void Down(MigrationBuilder migrationBuilder)
- 输入：迁移构建器
- 输出：无
- 副作用：Drop 五表
- 步骤：依次 Drop overrides/rules/timeline_blocks/aggregates/goals
- 分支与异常：无
- 调用：`DropTable`

## 近逐行中文伪代码

1. partial 类 `AddMobileAnalytics : Migration`
2. Up 建 catalog overrides（user+package 唯一）
3. Up 建 category rules（user+rule_type+pattern 唯一）
4. Up 建 timeline blocks（时间线块 + jsonb 质量/来源）
5. Up 建 usage aggregates（小时等粒度桶唯一）
6. Up 建 usage goals（scope+package+category 唯一）
7. 默认 life_category=`未分类`、timezone=`Asia/Shanghai`
8. Down 全部 Drop

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs",
      "label": "AddMobileAnalytics",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/Migrations/PimDbContextModelSnapshot.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260707145521_AddMobileAnalytics.cs", "type": "depends_on" }
  ]
}
```
