# src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：EF 迁移 `AddMobileModule`——创建移动端设备/应用目录/位置点/同步批次/使用事件·会话·摘要表及索引，并调整 PC 活动分类默认说明为中文。
- 主要依赖：`Microsoft.EntityFrameworkCore.Migrations`
- 被谁使用：EF 迁移流水线；支撑 `module.mobile` 持久化

## 函数级结构化伪代码

### AddMobileModule : Migration
#### void Up(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：改默认值、建 7 张表与若干唯一/复合索引
- 步骤：
  1. 改 `pc_activity_classifications.explanation` 默认值：英文 → `"没有匹配到规则或启发式分类。"`
  2. 建表 `mobile_app_catalog`：设备侧应用目录（package、版本、系统应用标记、raw_json 等）
  3. 建表 `mobile_devices`：设备注册与 last_seen
  4. 建表 `mobile_location_points`：位置点（经纬度精度、速度/方位、quality 默认 usable）
  5. 建表 `mobile_sync_batches`：同步批次窗口与 accepted/failed 计数
  6. 建表 `mobile_usage_events`：使用事件
  7. 建表 `mobile_usage_sessions`：使用会话
  8. 建表 `mobile_usage_summaries`：窗口汇总
  9. 索引（含多处 unique）：
     - app_catalog: (user_id, device_id, package_name) unique
     - devices: (user_id, device_id) unique；(user_id, last_seen_at_utc)
     - location_points: (user_id, device_id, recorded_at_utc)；(user_id, quality, recorded_at_utc)
     - sync_batches: (user_id, device_id, batch_id) unique；(user_id, device_id, created_at)
     - usage_events: 时间序索引 + (user,device,package,event_type,timestamp,class_name) unique
     - usage_sessions: (user,device,start)；(user,package,start)
     - usage_summaries: 窗口唯一键 + (user,device,window_start)
- 分支与异常：迁移失败回滚
- 调用：`AlterColumn` / `CreateTable` / `CreateIndex`

#### void Down(MigrationBuilder migrationBuilder)
- 输入：`MigrationBuilder`
- 输出：无
- 副作用：删 7 表；恢复 explanation 英文默认值
- 步骤：
  1. Drop 全部 mobile_* 表
  2. explanation 默认值改回 `"No rule or heuristic matched."`
- 分支与异常：同 EF
- 调用：`DropTable` / `AlterColumn`

## 近逐行中文伪代码

1. partial 类 `AddMobileModule` 继承 Migration
2. Up：先改 pc_activity_classifications.explanation 中文默认
3. CreateTable mobile_app_catalog（id/user/device/package/display/version/system/category/installer/时间/raw_json/审计时间）
4. CreateTable mobile_devices（设备身份、硬件/OS/app 版本、metadata、registered/last_seen）
5. CreateTable mobile_location_points（坐标与精度字段、is_mock、quality、raw_json）
6. CreateTable mobile_sync_batches（batch 窗口、计数、status、error_json）
7. CreateTable mobile_usage_events / sessions / summaries
8. 为各表创建查询与唯一索引（名称可能被 EF 截断为 `...~`）
9. Down：Drop 七表；explanation 默认值恢复英文

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs",
      "label": "AddMobileModule",
      "path": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs.md",
      "layer": "infrastructure",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs", "to": "Microsoft.EntityFrameworkCore.Migrations", "type": "extends" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/Pim.Infrastructure/Data/Migrations/20260706161439_AddMobileModule.cs", "type": "depends_on" }
  ]
}
```
