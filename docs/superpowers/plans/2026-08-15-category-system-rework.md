# PIM 分类体系重构（阶段 1）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 把四套互不咬合的分类机制统一为「7 大类字典 + 应用映射 + 情境规则」，并提供交互式打标队列（后端接口 + 三处复用前端组件）。

**架构：** 保留现有表结构（`pc_categories` / `pc_activity_category_rules` / `pc_app_categories` / `pc_activity_classifications`），用数据迁移收敛字典、用外键打通规则与字典、用新服务实现打标队列与提交；前端复用现有 `suggestions` 接口机制，新增统一打标组件。分类只做事实描述（7 大类），不做 productive/distracting 评判。

**技术栈：** .NET（EF Core + Npgsql）、Xunit + EF InMemory（现有测试模式）、React + TypeScript + Vite、TanStack Query。

**需求文档：** `PIM展示层与分类体系改造需求文档_20260815_2317.md`（§2 分类体系重构、§7 拍板项）

**worktree：** `/workspace/pim-wt/category-rework`（分支 `opencode-linux/category-rework`，基于 `origin/master` 0666a4ed）

---

## 0. 已锁定的设计决策（审查结论 + 需求拍板综合）

### 0.1 统一字典（7 大类，全部 `IsBuiltin=true`、`Productivity="neutral"`、`ParentId=null` 平铺）

| Name | Color | Icon | SortOrder |
|---|---|---|---|
| 编程/折腾 | #6B5EE4 | 💻 | 10 |
| 学习 | #14b8a6 | 📚 | 20 |
| 视频 | #F97316 | 📺 | 30 |
| 聊天 | #3B82F6 | 💬 | 40 |
| 文档 | #F59E0B | 📄 | 50 |
| 游戏 | #F43F5E | 🎮 | 60 |
| 其他 | #64748b | 📋 | 99 |

`Productivity` 字段 schema 保留（不动 DDL），全部填 `neutral`（事实描述，不评判）。

### 0.2 旧分类 → 新大类映射（数据迁移 + 启发式 + 快照共用同一张映射表）

| 旧值 | 新值 |
|---|---|
| 编程 / 前端 / 后端 / 终端 / 运维 / 设计 | 编程/折腾 |
| 学习 / 技术学习 / 外语学习 / 阅读 | 学习 |
| 视频 | 视频 |
| 沟通 / 即时消息 / 邮件 / 社交 / 会议 | 聊天 |
| 文档 / 办公 / 文件 / 浏览(仅规则层，见 0.3) | 文档 |
| 游戏 / 单机游戏 / 网络游戏 | 游戏 |
| 娱乐 / 音乐 / 工作 / 其他 / null / 空串 | 其他 |

### 0.3 builtin 规则种子重写（`PcTrackerSchemaInitializer.cs` SQL 与既有库数据同步处理）

| 现规则 | 处理 |
|---|---|
| Builtin: VS Code / Rider（编程） | category_name 更新为「编程/折腾」 |
| Builtin: Terminal（终端） | category_name 更新为「编程/折腾」 |
| Builtin: Chat apps（沟通） | category_name 更新为「聊天」 |
| Builtin: Office apps（办公） | category_name 更新为「文档」 |
| Builtin: File managers（文件） | category_name 更新为「文档」 |
| Builtin: Browser apps（浏览） | **删除**（浏览器→域名层判定；这是 98% 快照 fallback 到「其他」的根源之一，浏览器无域名信息时归「其他」是对的） |

### 0.4 ActivityClassifier 启发式（保留为兜底，输出名换成 7 大类）

- 硬编码常量（编程/学习/终端/沟通/办公/文件/娱乐）**全部删除**，替换为 7 大类名 + §0.1 颜色。
- 词表重写：
  - `CodingApps` + `TerminalApps` + `localhost` + 代码托管域名（github/gitlab）→ **编程/折腾**
  - `CommunicationApps` + `MeetingTitleSignals` → **聊天**
  - `OfficeApps` + `FileApps` → **文档**
  - `EntertainmentApps` 拆分：`steam` → **游戏**；`youtube/netflix/vlc/potplayer/bilibili` → **视频**；`spotify/music` → **其他**
  - `IsDocumentationSignal`（docs 域名/标题信号）→ **学习**
- 执行顺序不变（高优规则 → 启发式 → deferred 规则 → fallback「其他」）。

### 0.5 规则表外键

- `pc_activity_category_rules` 新增 `category_id UUID NULL REFERENCES pc_categories(id)`；**保留** `category_name` 列（快照与前端继续消费 name）。
- 保存规则时：有 `category_name` → 解析为 `category_id` 一并写入；两列都写，保证一致性。
- 分类时：规则命中优先用 `category_id` 反查字典名（字典改名自动跟随），查不到回退 `category_name`，再回退 fallback。

### 0.6 打标队列与提交（新后端 + 复用现有 UI 机制）

- **队列** `GET /api/v1/pc/classification/queue?limit=20`：应用候选 = `pc_aw_events` 按 `app_name_normalized` 聚合时长 ≥ 10 分钟且无 `pc_app_categories` 映射；域名候选 = 浏览器事件 `domain` 聚合时长 ≥ 10 分钟且无 domain 规则覆盖。按时长降序。
- **提交** `POST /api/v1/pc/classification/label`（见任务 3 契约）：app/domain/mobile_app 三种目标，写 `pc_app_categories` / `pc_activity_category_rules` / `mobile_app_category_rules`；`scope=keyword` 生成情境规则（窗口标题/URL 关键词）；自定义分类自动建 `pc_categories` 行（`IsBuiltin=false`）并纳入选项。
- **手机侧**：`MobileLifeCategories` 常量改为与 PC 同一套 7 大类（保留 `ToolsSystem` 特殊值给系统噪音，不进入用户可选 7 类）。

### 0.7 数据迁移（一个 EF migration 完成全部存量数据处理）

1. `DELETE FROM pc_categories WHERE is_builtin AND name NOT IN (7 大类)`（细分支删除；用户自建 `is_builtin=false` 保留）
2. `UPDATE pc_activity_classifications SET category_name = 映射(新)`（35,871 条历史快照重映射）
3. `UPDATE pc_activity_category_rules SET category_name = 映射(新)`；`DELETE` 其中 category_name 为 NULL/空串的规则、conditions_json 不含非空 `all` 数组的垃圾规则（`{"test":true}` 类）、Builtin: Browser apps 规则
4. `UPDATE app_signatures SET category_path = 映射(新)`（知识库展示用）
5. 新增 `category_id` 列并回填（按 category_name join）
6. 迁移映射逻辑抽成 C# 静态类 `CategoryLegacyMapper`（可单测），migration 里只写 SQL（含同表映射字面量，双份但互不依赖；测试覆盖 C# 侧）

---

## 任务 1：统一字典 + 存量数据迁移

**文件：**
- 修改：`src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs:126-208`（SeedDefaultsAsync 改 7 大类）
- 创建：`src/modules/Pim.Module.PcTracker/Services/CategoryLegacyMapper.cs`（旧→新映射静态类 + 7 大类常量）
- 创建：`src/Pim.Infrastructure/Data/Migrations/20260815XXXXXX_UnifyCategoryDictionary.cs`（见 0.7 的 6 步 SQL）
- 修改：`src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs`（builtin 规则种子 SQL 按 0.3 重写；schema 初始化 SQL 保持幂等）
- 测试：`tests/Pim.UnitTests/Services/CategoryLegacyMapperTests.cs`（新建）、`tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs`（更新）

- [x] **步骤 1：写失败测试（CategoryLegacyMapper）**

```csharp
// tests/Pim.UnitTests/Services/CategoryLegacyMapperTests.cs
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class CategoryLegacyMapperTests
{
    [Theory]
    [InlineData("编程", "编程/折腾")]
    [InlineData("前端", "编程/折腾")]
    [InlineData("后端", "编程/折腾")]
    [InlineData("终端", "编程/折腾")]
    [InlineData("运维", "编程/折腾")]
    [InlineData("设计", "编程/折腾")]
    [InlineData("技术学习", "学习")]
    [InlineData("外语学习", "学习")]
    [InlineData("阅读", "学习")]
    [InlineData("沟通", "聊天")]
    [InlineData("即时消息", "聊天")]
    [InlineData("邮件", "聊天")]
    [InlineData("社交", "聊天")]
    [InlineData("会议", "聊天")]
    [InlineData("办公", "文档")]
    [InlineData("文件", "文档")]
    [InlineData("浏览", "文档")]
    [InlineData("单机游戏", "游戏")]
    [InlineData("网络游戏", "游戏")]
    [InlineData("娱乐", "其他")]
    [InlineData("音乐", "其他")]
    [InlineData("工作", "其他")]
    [InlineData(null, "其他")]
    [InlineData("", "其他")]
    [InlineData("编程/折腾", "编程/折腾")]
    [InlineData("学习", "学习")]
    public void MapToUnified_ReturnsExpected(string? legacy, string expected)
        => Assert.Equal(expected, CategoryLegacyMapper.MapToUnified(legacy));

    [Fact]
    public void UnifiedCategories_ContainsExactlySeven()
    {
        var names = CategoryLegacyMapper.UnifiedCategoryNames;
        Assert.Equal(7, names.Length);
        Assert.Equal(["编程/折腾", "学习", "视频", "聊天", "文档", "游戏", "其他"], names);
    }
}
```

- [x] **步骤 2：运行确认失败**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~CategoryLegacyMapperTests" --no-restore`
预期：编译错误 `CS0103: The name 'CategoryLegacyMapper' does not exist in the current context`

- [x] **步骤 3：实现 CategoryLegacyMapper**

```csharp
// src/modules/Pim.Module.PcTracker/Services/CategoryLegacyMapper.cs
namespace Pim.Module.PcTracker.Services;

public static class CategoryLegacyMapper
{
    public const string ProgrammingTinkering = "编程/折腾";
    public const string Learning = "学习";
    public const string Video = "视频";
    public const string Chat = "聊天";
    public const string Documents = "文档";
    public const string Gaming = "游戏";
    public const string Other = "其他";

    public static readonly string[] UnifiedCategoryNames =
        [ProgrammingTinkering, Learning, Video, Chat, Documents, Gaming, Other];

    public static readonly IReadOnlyDictionary<string, string> UnifiedColors =
        new Dictionary<string, string>
        {
            [ProgrammingTinkering] = "#6B5EE4",
            [Learning] = "#14b8a6",
            [Video] = "#F97316",
            [Chat] = "#3B82F6",
            [Documents] = "#F59E0B",
            [Gaming] = "#F43F5E",
            [Other] = "#64748b"
        };

    public static readonly IReadOnlyDictionary<string, string> UnifiedIcons =
        new Dictionary<string, string>
        {
            [ProgrammingTinkering] = "💻",
            [Learning] = "📚",
            [Video] = "📺",
            [Chat] = "💬",
            [Documents] = "📄",
            [Gaming] = "🎮",
            [Other] = "📋"
        };

    private static readonly IReadOnlyDictionary<string, string> LegacyToUnified =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["编程"] = ProgrammingTinkering,
            ["前端"] = ProgrammingTinkering,
            ["后端"] = ProgrammingTinkering,
            ["终端"] = ProgrammingTinkering,
            ["运维"] = ProgrammingTinkering,
            ["设计"] = ProgrammingTinkering,
            ["技术学习"] = Learning,
            ["外语学习"] = Learning,
            ["阅读"] = Learning,
            ["视频"] = Video,
            ["沟通"] = Chat,
            ["即时消息"] = Chat,
            ["邮件"] = Chat,
            ["社交"] = Chat,
            ["会议"] = Chat,
            ["文档"] = Documents,
            ["办公"] = Documents,
            ["文件"] = Documents,
            ["浏览"] = Documents,
            ["游戏"] = Gaming,
            ["单机游戏"] = Gaming,
            ["网络游戏"] = Gaming
        };

    /// <summary>旧分类名 → 统一 7 大类名。未知/空值 → 其他。</summary>
    public static string MapToUnified(string? legacy)
    {
        if (string.IsNullOrWhiteSpace(legacy))
            return Other;
        return LegacyToUnified.TryGetValue(legacy.Trim(), out var unified) ? unified : Other;
    }
}
```

- [x] **步骤 4：运行确认通过**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~CategoryLegacyMapperTests" --no-restore`
预期：PASS

- [x] **步骤 5：重写 SeedDefaultsAsync 为 7 大类**

将 `PcCategoryService.SeedDefaultsAsync`（`PcCategoryService.cs:126-208`）中的 27 条种子替换为：

```csharp
public async Task SeedDefaultsAsync(CancellationToken ct)
{
    var categories = CategoryLegacyMapper.UnifiedCategoryNames
        .Select((name, index) => new PcCategoryEntity
        {
            Id = Guid.Parse($"20000000-0000-0000-0000-{index + 1:D12}"),
            Name = name,
            Color = CategoryLegacyMapper.UnifiedColors[name],
            Icon = CategoryLegacyMapper.UnifiedIcons[name],
            Productivity = "neutral",
            SortOrder = (name == CategoryLegacyMapper.Other ? 99 : 10 * (index + 1)),
            IsBuiltin = true
        })
        .ToList();
    // ……保留原 id/name 解析与缺失插入逻辑（现有 existingIdSet/existingByName 机制不动）
}
```

注意：`其他` 的 SortOrder=99，其余 10/20/30/40/50/60。

- [x] **步骤 6：更新 PcCategoryServiceTests 中依赖旧 27 类 seed 的断言**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~PcCategory" --no-restore`
按失败清单逐一修正断言（预期 seed 结果 = 7 大类）。若测试直接构造实体则不受影响。

- [x] **步骤 7：写 migration**

在 `src/Pim.Infrastructure/Data/Migrations/` 创建 `20260815XXXXXX_UnifyCategoryDictionary.cs`（时间戳取执行时 UtcNow，格式 `yyyyMMddHHmmss`），`Up()` 内执行（必须每步用独立 `migrationBuilder.Sql`，Npgsql 语法）：

```sql
-- 1) 删除旧 builtin 细分支（用户自建保留）
DELETE FROM pc_categories
 WHERE is_builtin AND name NOT IN ('编程/折腾','学习','视频','聊天','文档','游戏','其他');

-- 2) 历史快照重映射（UPDATE + CASE）
UPDATE pc_activity_classifications SET category_name = CASE category_name
  WHEN '编程' THEN '编程/折腾' WHEN '前端' THEN '编程/折腾' WHEN '后端' THEN '编程/折腾'
  WHEN '终端' THEN '编程/折腾' WHEN '运维' THEN '编程/折腾' WHEN '设计' THEN '编程/折腾'
  WHEN '技术学习' THEN '学习' WHEN '外语学习' THEN '学习' WHEN '阅读' THEN '学习'
  WHEN '视频' THEN '视频'
  WHEN '沟通' THEN '聊天' WHEN '即时消息' THEN '聊天' WHEN '邮件' THEN '聊天'
  WHEN '社交' THEN '聊天' WHEN '会议' THEN '聊天'
  WHEN '办公' THEN '文档' WHEN '文件' THEN '文档' WHEN '浏览' THEN '文档'
  WHEN '单机游戏' THEN '游戏' WHEN '网络游戏' THEN '游戏'
  WHEN '娱乐' THEN '其他' WHEN '音乐' THEN '其他' WHEN '工作' THEN '其他'
  ELSE '其他' END
 WHERE category_name NOT IN ('编程/折腾','学习','视频','聊天','文档','游戏','其他');

-- 3a) 垃圾规则删除（conditions_json 无合法 all 数组，含 {"test":true} 类）
DELETE FROM pc_activity_category_rules
 WHERE jsonb_typeof(conditions_json->'all') IS DISTINCT FROM 'array'
    OR jsonb_array_length(conditions_json->'all') = 0;

-- 3b) 空分类规则删除（mobaxterm/foxitpdfeditor 等）
DELETE FROM pc_activity_category_rules
 WHERE category_name IS NULL OR btrim(category_name) = '';

-- 3c) Browser builtin 规则删除（浏览器交给域名层）
DELETE FROM pc_activity_category_rules WHERE rule_name = 'Builtin: Browser apps';

-- 3d) 剩余规则 category_name 重映射
UPDATE pc_activity_category_rules SET category_name = CASE category_name
  WHEN '编程' THEN '编程/折腾' WHEN '前端' THEN '编程/折腾' WHEN '后端' THEN '编程/折腾'
  WHEN '终端' THEN '编程/折腾' WHEN '运维' THEN '编程/折腾' WHEN '设计' THEN '编程/折腾'
  WHEN '技术学习' THEN '学习' WHEN '外语学习' THEN '学习' WHEN '阅读' THEN '学习'
  WHEN '视频' THEN '视频'
  WHEN '沟通' THEN '聊天' WHEN '即时消息' THEN '聊天' WHEN '邮件' THEN '聊天'
  WHEN '社交' THEN '聊天' WHEN '会议' THEN '聊天'
  WHEN '办公' THEN '文档' WHEN '文件' THEN '文档' WHEN '浏览' THEN '文档'
  WHEN '单机游戏' THEN '游戏' WHEN '网络游戏' THEN '游戏'
  WHEN '娱乐' THEN '其他' WHEN '音乐' THEN '其他' WHEN '工作' THEN '其他'
  ELSE '其他' END
 WHERE category_name NOT IN ('编程/折腾','学习','视频','聊天','文档','游戏','其他');

-- 4) app_signatures.category_path 重映射
--    注意 category_path 形如 '工作·编程'（点分隔路径）。种子中 distinct 旧路径为：
--    工作、工作·编程、工作·文档、工作·会议、工作·设计、工作·运维、工作·终端、
--    工作·办公、工作·文件、工作·浏览、娱乐、娱乐·游戏、娱乐·游戏·单机、
--    娱乐·游戏·网络、娱乐·视频、娱乐·音乐、娱乐·社交、学习、学习·技术学习、
--    学习·外语学习、学习·阅读、沟通、沟通·即时消息、沟通·邮件、其他
--    逐字面量映射（含层级组合，末段按 §0.2 规则映射）：
UPDATE app_signatures SET category_path = CASE category_path
  WHEN '工作' THEN '其他'
  WHEN '工作·编程' THEN '编程/折腾' WHEN '工作·前端' THEN '编程/折腾' WHEN '工作·后端' THEN '编程/折腾'
  WHEN '工作·终端' THEN '编程/折腾' WHEN '工作·运维' THEN '编程/折腾' WHEN '工作·设计' THEN '编程/折腾'
  WHEN '工作·文档' THEN '文档' WHEN '工作·办公' THEN '文档' WHEN '工作·文件' THEN '文档' WHEN '工作·浏览' THEN '文档'
  WHEN '工作·会议' THEN '聊天'
  WHEN '娱乐' THEN '其他'
  WHEN '娱乐·游戏' THEN '游戏' WHEN '娱乐·游戏·单机' THEN '游戏' WHEN '娱乐·游戏·网络' THEN '游戏'
  WHEN '娱乐·视频' THEN '视频' WHEN '娱乐·音乐' THEN '其他' WHEN '娱乐·社交' THEN '聊天'
  WHEN '学习' THEN '学习'
  WHEN '学习·技术学习' THEN '学习' WHEN '学习·外语学习' THEN '学习' WHEN '学习·阅读' THEN '学习'
  WHEN '沟通' THEN '聊天' WHEN '沟通·即时消息' THEN '聊天' WHEN '沟通·邮件' THEN '聊天'
  ELSE '其他' END
 WHERE category_path IS NOT NULL AND category_path <> '';
--    （实现时先对 PcTrackerSchemaInitializer.cs:273-464 中 171 条种子的 category_path
--     取 distinct 值核对本 CASE，有遗漏则补齐）

-- 5) 规则表加 category_id 列 + 回填
ALTER TABLE pc_activity_category_rules ADD COLUMN IF NOT EXISTS category_id UUID NULL
  REFERENCES pc_categories(id);
UPDATE pc_activity_category_rules r
   SET category_id = c.id
  FROM pc_categories c
 WHERE c.name = r.category_name AND r.category_id IS NULL;
```

CASE 语句必须完整写出（禁止"同上"简写）。步骤 4 的 `split_part(...array_length...)` 表达式需在计划实现时验证；若复杂，改为简单 `CASE` 覆盖已知旧路径字面量（`'工作·编程'`、`'娱乐·游戏·单机'` 等 171 条种子中的全部 distinct 值，从 `PcTrackerSchemaInitializer.cs:273-464` 抄录）。

- [x] **步骤 8：同步 PcTrackerSchemaInitializer 的 SQL**

`PcTrackerSchemaInitializer.cs` 中：
1. builtin 规则种子（L467-475）按 0.3 重写（VS Code/Rider→编程/折腾、Terminal→编程/折腾、Chat→聊天、Office→文档、File managers→文档、删除 Browser 行）
2. `pc_categories` 建表 SQL 不动
3. app_signatures 种子中的 `category_path` 旧值（`'工作·编程'` 等）同步改为新大类名（171 条，纯文本替换）

- [x] **步骤 9：全量测试**

运行：`dotnet test Pim.sln --no-restore`
预期：1092+ 通过（原有测试中依赖旧分类名的失败需逐一修正断言或输入数据，不许删除断言）

- [x] **步骤 10：Commit**

```bash
git add src/modules/Pim.Module.PcTracker/Services/CategoryLegacyMapper.cs \
        src/modules/Pim.Module.PcTracker/Services/PcCategoryService.cs \
        src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs \
        src/Pim.Infrastructure/Data/Migrations/ \
        tests/Pim.UnitTests/Services/CategoryLegacyMapperTests.cs \
        tests/Pim.UnitTests/Services/PcCategoryServiceTests.cs
git commit -m "feat: unify category dictionary to 7 top-level categories with legacy data migration / 分类字典收窄为 7 大类并迁移存量数据"
```

---

## 任务 2：分类器归一 + 规则表 category_id 外键

**文件：**
- 修改：`src/modules/Pim.Module.PcTracker/Services/ActivityClassifier.cs`（删硬编码常量、词表重写、结果名用 7 大类）
- 修改：`src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs`（加 `CategoryId`）
- 修改：`src/modules/Pim.Module.PcTracker/Entities/EntityConfigurations.cs`（category_id 列映射）
- 修改：`src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs`（Save 时解析 category_id；ToDto 带 CategoryId）
- 修改：`src/modules/Pim.Module.PcTracker/DTOs/ActivityClassificationDtos.cs`（DTO 加 CategoryId）
- 测试：`tests/Pim.UnitTests/Services/ActivityClassifierTests.cs`、`ActivityClassificationRuleServiceTests.cs`（更新）

- [x] **步骤 1：写失败测试**

```csharp
// ActivityClassifierTests.cs 追加（现有测试断言旧分类名的同步更新）
[Fact]
public void Classify_HeuristicTerminal_ReturnsUnifiedProgramming()
{
    var context = CreateContext(AppNameNormalized: "windowsterminal");
    var result = ActivityClassifier.Classify(context, Array.Empty<ActivityCategoryRuleEntity>());
    Assert.Equal("编程/折腾", result.CategoryName);
}

[Fact]
public void Classify_HeuristicVideoDomain_ReturnsVideo()
{
    var context = CreateContext(AppNameNormalized: "msedge", Domain: "www.bilibili.com", UrlPath: "/video/BV1xx");
    var result = ActivityClassifier.Classify(context, Array.Empty<ActivityCategoryRuleEntity>());
    Assert.Equal("视频", result.CategoryName);
}

[Fact]
public void Classify_HeuristicDocsDomain_ReturnsLearning()
{
    var context = CreateContext(Domain: "docs.python.org", Title: "Documentation");
    var result = ActivityClassifier.Classify(context, Array.Empty<ActivityCategoryRuleEntity>());
    Assert.Equal("学习", result.CategoryName);
}

[Fact]
public void Classify_NoSignal_ReturnsFallbackOther()
{
    var context = CreateContext(AppNameNormalized: "mobaxterm");
    var result = ActivityClassifier.Classify(context, Array.Empty<ActivityCategoryRuleEntity>());
    Assert.Equal("其他", result.CategoryName);
    Assert.Equal("fallback", result.Source);
}
```

注意：现有 14 个测试中 `Classify_UserRuleBeatsHeuristic` 等断言「学习」（旧常量）恰好与新字典同名，无需改；断言「编程」「沟通」「办公」等的用例改为「编程/折腾」「聊天」「文档」。`CreateContext` 辅助方法按现有文件模式复用。

- [x] **步骤 2：运行确认失败**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~ActivityClassifierTests" --no-restore`
预期：新测试 FAIL（启发式仍返回旧名「终端」「浏览」等）

- [x] **步骤 3：重写 ActivityClassifier**

1. 删除 L11-24 全部硬编码常量；引用改为 `CategoryLegacyMapper.ProgrammingTinkering` 等（7 大类名）+ `CategoryLegacyMapper.UnifiedColors` 颜色。
2. 词表按 0.4 重写：
   - 删除 `EntertainmentApps` 单表，拆为 `VideoApps = ["youtube","netflix","vlc","potplayer","bilibili"]`、`GameApps = ["steam"]`、`OtherEntertainmentApps = ["spotify","music"]`（→其他，confidence 0.7）
   - `CodingApps` 保留 + `TerminalApps` 并入 `ProgrammingTinkering` 分支（终端 app → 编程/折腾）
   - `CommunicationApps`/`MeetingTitleSignals` → 聊天
   - `OfficeApps`/`FileApps` → 文档
3. `ClassifyWithHeuristics` 返回分支相应改名，颜色取 `CategoryLegacyMapper.UnifiedColors[...]`。

- [x] **步骤 4：规则实体加 category_id + 服务解析**

`ActivityCategoryRuleEntity` 加：

```csharp
[Column("category_id")] public Guid? CategoryId { get; set; }
```

`EntityConfigurations.cs` 对应映射（`HasColumnName("category_id")`、可选 FK）。`ActivityClassificationRuleService.ToEntity` 增加：请求含 `CategoryName` 时查 `PcCategoryEntity` 按名取 id 写入（查不到 → 抛 `ArgumentException($"分类「{categoryName}」不存在。")`，与现有 ValidateAsync 一致，直接复用该校验后查 id）。`ToDto` 输出 `CategoryId`。

分类器 `TryClassifyWithRules` 构造结果时：优先用 `rule.CategoryId` 查字典名（传入解析好的 lookup：`IReadOnlyDictionary<Guid,string>`），无则用 `rule.CategoryName`，再无则 fallback。为避免每规则查库，`Classify` 签名加可选参数 `IReadOnlyDictionary<Guid, string>? categoryNamesById = null`；`ActivityClassificationSnapshotService` 调用处预加载字典传入。

- [x] **步骤 5：运行测试确认通过**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~ActivityClassifier|ActivityClassificationRule" --no-restore`
预期：PASS

- [x] **步骤 6：全量测试 + 修正波及**

运行：`dotnet test Pim.sln --no-restore`
预期：PASS（Snapshot/Recompute/Quality 等服务测试若断言旧分类名，按新字典修正）

- [x] **步骤 7：Commit**

```bash
git add src/modules/Pim.Module.PcTracker/ tests/Pim.UnitTests/Services/
git commit -m "feat: classifier outputs unified categories and rules reference category_id / 分类器输出统一字典并打通规则外键"
```

---

## 任务 3：打标队列与提交接口（后端）

**文件：**
- 创建：`src/modules/Pim.Module.PcTracker/Services/ActivityLabelingService.cs`（队列查询 + 提交）
- 创建：`src/modules/Pim.Module.PcTracker/DTOs/ActivityLabelingDtos.cs`
- 修改：`src/modules/Pim.Module.PcTracker/PcTrackerModule.cs`（新端点）
- 修改：`src/modules/Pim.Module.Mobile/Services/MobileLifeCategories.cs`（字典对齐 7 大类）
- 测试：`tests/Pim.UnitTests/Services/ActivityLabelingServiceTests.cs`（新建）

### API 契约（锁定）

```
GET /api/v1/pc/classification/queue?limit=20
200 → { "items": [
  { "target_type": "app", "target": "mobaxterm", "display_name": "MobaXterm", "minutes": 42, "sample_titles": ["ssh to 192.168.1.1"] },
  { "target_type": "domain", "target": "csdn.net", "display_name": "csdn.net", "minutes": 31, "sample_titles": ["CSDN - 教程文章"] },
  { "target_type": "mobile_app", "target": "com.oppo.gallery", "display_name": "OPPO 相册", "minutes": 27, "sample_titles": [] }
] }

POST /api/v1/pc/classification/label
{ "target_type": "app"|"domain"|"mobile_app",
  "target": "mobaxterm",
  "category_id": "uuid 或空（用 category_name）",
  "category_name": "编程/折腾（category_id 为空时必填；自定义分类即新名）",
  "scope": "all"|"keyword",
  "keyword": "教程" }
200 → { "ok": true, "category_id": "uuid", "category_name": "编程/折腾",
        "created": "app_mapping"|"app_context_rule"|"domain_rule"|"mobile_app_rule" }
400 → 分类不存在/参数非法；自定义分类自动创建（IsBuiltin=false）并返回新 id
```

### 提交落点规则（锁定）

| target_type + scope | 写入表 | 规则内容 |
|---|---|---|
| app + all | `pc_app_categories`（app_pattern=target, category_name, priority=100） | — |
| app + keyword | `pc_activity_category_rules`（source=user, priority=500, conditions: windowTitle contains keyword） | — |
| domain + all | `pc_activity_category_rules`（priority=400, conditions: domain equals target） | — |
| domain + keyword | `pc_activity_category_rules`（priority=450, conditions: domain equals AND urlPath contains keyword） | — |
| mobile_app + all | `mobile_app_category_rules`（user 级, rule_type=package-exact, pattern=target, life_category=category_name） | — |
| mobile_app + keyword | 不支持（400），手机窗口标题不可采 | — |

`pc_app_categories` 写入时若同 `app_pattern` 已存在 → UPDATE 覆盖；`pc_activity_category_rules` 生成规则名 `Label: {target} [{keyword|all}]`，同名单规则存在则 UPDATE 而非重复插入（幂等）。

- [x] **步骤 1：写失败测试**

```csharp
// tests/Pim.UnitTests/Services/ActivityLabelingServiceTests.cs
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.PcTracker.DTOs;
using Pim.Module.PcTracker.Entities;
using Pim.Module.PcTracker.Services;
using Xunit;

namespace Pim.UnitTests.Services;

public class ActivityLabelingServiceTests
{
    private static (PimDbContext db, ActivityLabelingService svc) Create()
    {
        PimDbContext.RegisterModuleAssembly(typeof(ActivityCategoryRuleEntity).Assembly);
        var options = new DbContextOptionsBuilder<PimDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString()).Options;
        var db = new PimDbContext(options);
        foreach (var (name, i) in CategoryLegacyMapper.UnifiedCategoryNames.Select((n, i) => (n, i)))
            db.Set<PcCategoryEntity>().Add(new PcCategoryEntity
            {
                Id = Guid.Parse($"20000000-0000-0000-0000-{i + 1:D12}"),
                Name = name, Color = "#64748b", IsBuiltin = true
            });
        db.SaveChanges();
        return (db, new ActivityLabelingService(db));
    }

    [Fact]
    public async Task LabelApp_AllScope_WritesAppMapping()
    {
        var (db, svc) = Create();
        var req = new ActivityLabelingRequest("app", "mobaxterm", null, "编程/折腾", "all", null);
        var res = await svc.LabelAsync(req, CancellationToken.None);
        Assert.True(res.Ok);
        var mapping = Assert.Single(db.Set<AppCategoryEntity>());
        Assert.Equal("mobaxterm", mapping.AppPattern);
        Assert.Equal("编程/折腾", mapping.CategoryName);
    }

    [Fact]
    public async Task LabelDomain_KeywordScope_CreatesContextRule()
    {
        var (db, svc) = Create();
        var req = new ActivityLabelingRequest("domain", "bilibili.com", null, "学习", "keyword", "教程");
        await svc.LabelAsync(req, CancellationToken.None);
        var rule = Assert.Single(db.Set<ActivityCategoryRuleEntity>());
        Assert.Contains("\"field\":\"domain\"", rule.ConditionsJson);
        Assert.Contains("\"field\":\"urlPath\"", rule.ConditionsJson);
        Assert.Equal("学习", rule.CategoryName);
    }

    [Fact]
    public async Task LabelWithNewCustomCategory_CreatesCategoryRow()
    {
        var (db, svc) = Create();
        var req = new ActivityLabelingRequest("app", "obsidian", null, "写日记", "all", null);
        var res = await svc.LabelAsync(req, CancellationToken.None);
        Assert.True(res.Ok);
        var cat = db.Set<PcCategoryEntity>().Single(c => c.Name == "写日记");
        Assert.False(cat.IsBuiltin);
        Assert.Equal(cat.Id, res.CategoryId);
    }

    [Fact]
    public async Task LabelDomain_AllScope_IsIdempotent()
    {
        var (db, svc) = Create();
        var req = new ActivityLabelingRequest("domain", "csdn.net", null, "学习", "all", null);
        await svc.LabelAsync(req, CancellationToken.None);
        await svc.LabelAsync(req, CancellationToken.None);
        var rules = db.Set<ActivityCategoryRuleEntity>()
            .Where(r => r.RuleName == "Label: csdn.net [all]").ToList();
        Assert.Single(rules);
    }
}
```

- [x] **步骤 2：运行确认失败**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~ActivityLabelingServiceTests" --no-restore`
预期：编译失败（类型不存在）

- [x] **步骤 3：实现 DTOs**

`ActivityLabelingDtos.cs`（record 类型）：

```csharp
public sealed record ActivityLabelingQueueItem(
    string TargetType, string Target, string DisplayName, int Minutes, List<string> SampleTitles);
public sealed record ActivityLabelingQueueResponse(List<ActivityLabelingQueueItem> Items);
public sealed record ActivityLabelingRequest(
    string TargetType, string Target, Guid? CategoryId, string? CategoryName, string Scope, string? Keyword);
public sealed record ActivityLabelingResponse(bool Ok, Guid? CategoryId, string? CategoryName, string Created);
```

- [x] **步骤 4：实现 ActivityLabelingService**

要点：
- 队列查询（InMemory 测试难覆盖复杂 SQL，用服务层可测逻辑 + SQL 层分离：`BuildQueueAsync` 从 `pc_aw_events` 聚合——EF 组查询 `GroupBy(app_name_normalized)`，浏览器事件按 `data_json->>'domain'` 分组；把「已有映射排除」做成可注入的已映射集合查询）
- `LabelAsync`：按契约落点规则写表；`AppCategoryEntity` 即 `pc_app_categories` 实体（`Entities/AppCategoryEntity.cs`，属性 `AppPattern`/`CategoryName`/`Color`/`Priority`/`IsBuiltin`）
- 自定义分类：`category_id` 与 `category_name` 均为空 → 400；`category_id` 空且 `category_name` 不在现有字典 → 创建 `PcCategoryEntity { Name, Color="#64748b", IsBuiltin=false, Productivity="neutral", SortOrder=1000 }`
- 幂等：规则名 `Label: {target} [{keyword|all}]` 存在则 UPDATE 分类与条件

- [x] **步骤 5：注册端点**

`PcTrackerModule.cs` 加（路由与现有 `pc/classification/*` 一致）：

```csharp
group.MapGet("/classification/queue", ...);
group.MapPost("/classification/label", ...);
```

- [x] **步骤 6：MobileLifeCategories 对齐**

`src/modules/Pim.Module.Mobile/Services/MobileLifeCategories.cs`：常量改为 7 大类（与 `CategoryLegacyMapper.UnifiedCategoryNames` 一致；`Uncategorized` 保持 `"其他"` 语义——检查现有默认值，若为「未分类」改为「其他」；`ToolsSystem` 保留）。运行 mobile 相关测试修正。

- [x] **步骤 7：运行测试确认通过**

运行：`dotnet test Pim.sln --filter "FullyQualifiedName~ActivityLabeling|MobileLife|MobileAppClassif" --no-restore`
预期：PASS

- [x] **步骤 8：全量测试**

运行：`dotnet test Pim.sln --no-restore`
预期：PASS

- [x] **步骤 9：Commit**

```bash
git add src/modules/Pim.Module.PcTracker/ src/modules/Pim.Module.Mobile/ tests/Pim.UnitTests/
git commit -m "feat: labeling queue and submit endpoints with custom categories / 打标队列与提交接口，支持自定义分类"
```

---

## 任务 4：前端打标组件（三处复用）+ 首次打标问卷

**文件：**
- 创建：`src/client-web/src/api/classificationLabeling.ts`（queue + label + dictionary 客户端）
- 创建：`src/client-web/src/components/labeling/LabelingQueue.tsx`（核心组件：队列卡片、chips、自定义输入、域名情境规则）
- 创建：`src/client-web/src/components/labeling/FirstLabelingWizard.tsx`（Top 50 问卷，复用 LabelingQueue 全量模式）
- 修改：`src/client-web/src/components/today/TodayClassificationSuggestionsSection.tsx`（替换内容为 LabelingQueue）
- 修改：`src/client-web/src/pages/PcTrackerPage.tsx`（「待打标队列」区块挂 LabelingQueue）
- 修改：`src/client-web/src/components/mobile/MobileAppCatalogManager.tsx`（替换为 LabelingQueue mobile 模式）
- 测试：`src/client-web/src/components/labeling/LabelingQueue.test.tsx`（vitest）

- [x] **步骤 1：写失败测试（组件）**

```tsx
// src/client-web/src/components/labeling/LabelingQueue.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { LabelingQueue } from './LabelingQueue';

vi.mock('../../api/classificationLabeling', () => ({
  fetchLabelingQueue: vi.fn().mockResolvedValue({
    items: [{ target_type: 'app', target: 'mobaxterm', display_name: 'MobaXterm', minutes: 42, sample_titles: [] }],
  }),
  submitLabel: vi.fn().mockResolvedValue({ ok: true, category_name: '编程/折腾', created: 'app_mapping' }),
  fetchCategoryDictionary: vi.fn().mockResolvedValue([
    { id: '1', name: '编程/折腾' }, { id: '2', name: '学习' }, { id: '3', name: '其他' },
  ]),
}));

describe('LabelingQueue', () => {
  it('renders queue item and submits preset category', async () => {
    render(<LabelingQueue limit={20} />);
    expect(await screen.findByText('MobaXterm')).toBeTruthy();
    fireEvent.click(screen.getByText('编程/折腾'));
    expect(await screen.findByText(/已归入/)).toBeTruthy();
  });

  it('adds custom category via input and submits', async () => {
    render(<LabelingQueue limit={20} />);
    expect(await screen.findByText('MobaXterm')).toBeTruthy();
    const input = screen.getByPlaceholderText('自定义分类，回车添加…');
    fireEvent.change(input, { target: { value: '写日记' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(await screen.findByText(/已归入/)).toBeTruthy();
  });
});
```

- [x] **步骤 2：运行确认失败**

运行：`npm --prefix src/client-web run test -- --run LabelingQueue`
预期：FAIL（模块不存在）

- [x] **步骤 3：实现 API 客户端**

```ts
// src/client-web/src/api/classificationLabeling.ts
import { apiGet, apiPost } from './client'; // 按现有 client.ts 导出名调整

export interface LabelingQueueItem { target_type: 'app' | 'domain' | 'mobile_app'; target: string; display_name: string; minutes: number; sample_titles: string[]; }
export interface CategoryDictionaryItem { id: string; name: string; color: string; icon: string; }

export const fetchLabelingQueue = (limit = 20) =>
  apiGet<{ items: LabelingQueueItem[] }>(`/pc/classification/queue?limit=${limit}`);
export const fetchCategoryDictionary = () =>
  apiGet<CategoryDictionaryItem[]>('/pc/categories/dictionary');
export const submitLabel = (body: { target_type: string; target: string; category_id?: string; category_name?: string; scope: 'all' | 'keyword'; keyword?: string }) =>
  apiPost('/pc/classification/label', body);
```

注意：`/pc/categories/dictionary` 若现无此端点，用现有 `GET /pc/categories` 树接口展平（实现时二选一，优先加简单端点于 PcTrackerModule）。

- [x] **步骤 4：实现 LabelingQueue 组件**

视觉以原型 HTML 的 `q-item/q-head/q-body/chips/q-custom/q-scope` 交互为准（需求文档 §6 原型可交互打标队列），样式用现有 CSS 变量（`var(--pim-*)`）。逻辑要点：
- 挂载时并行取 queue + dictionary
- 每项：预置 chips（dictionary 7 类 + 已持久化自定义类）+ 自定义输入（回车即添加 → 本地持久化 localStorage `pim_custom_cats` → 提交后也由服务端记录）
- `target_type === 'domain'` 时显示作用域单选（所有情况 / 仅含关键词页面 → 展开关键词输入框）
- 提交成功 → 该项标记完成并从列表移除；无更多项 → 空态文案「暂无待分类项 🎉」（无 emoji，改「暂无待分类项」）
- 组件 props：`limit?: number`、`compact?: boolean`（今日页 compact）

- [x] **步骤 5：实现 FirstLabelingWizard**

Top 50 应用问卷：`fetchLabelingQueue({limit:50})` 过滤 `target_type==='app'`；UI = 单列滚动 + 进度（已打标/总数）+ 跳过按钮；完成 → 提示。与 LabelingQueue 共享卡片子组件（抽取 `LabelingCard`，导出供两处使用）。

- [x] **步骤 6：接入三处页面**

1. `TodayClassificationSuggestionsSection.tsx`：保留区块头（接口/提供方不动），内容替换为 `<LabelingQueue limit={5} compact />`
2. `PcTrackerPage.tsx`：新增「待打标队列」区块 `<LabelingQueue limit={20} />`（放在 KeyboardHeatmap 旁，与原型位置一致）
3. `MobileAppCatalogManager.tsx`：整体替换为 `<LabelingQueue limit={20} />`（后端队列含 mobile_app 目标；文件删除，PcTracker 引用更新）

- [x] **步骤 7：运行组件测试 + 构建**

运行：`npm --prefix src/client-web run test -- --run` 与 `npm --prefix src/client-web run build`
预期：PASS

- [x] **步骤 8：全量门禁**

运行：`dotnet test Pim.sln --no-restore`
预期：PASS（后端无改动，回归确认）

- [x] **步骤 9：Commit**

```bash
git add src/client-web/src/
git commit -m "feat: interactive labeling queue component reused across today/pc/mobile pages / 交互打标队列组件三处复用"
```

---

## 任务 5：收尾（文档 + 全量验证 + PR + 三视角 review）

- [ ] **步骤 1：更新仓库 docs**

`docs/superpowers/plans/` 下本计划文档随代码入库（已在此 worktree）；若 `docs/` 有分类设计旧文档，在 PR 描述中说明本计划替代关系，不删旧文。

- [ ] **步骤 2：全新全量门禁**

```bash
git -C /workspace/pim-wt/category-rework status --short --branch
dotnet test Pim.sln --no-restore          # 期望 1092+ 通过
npm --prefix src/client-web run build     # 期望构建成功
git diff --check                          # 期望无 whitespace 错误
```

- [ ] **步骤 3：提交剩余变更 + 推送 + 开 PR**

```bash
git add -A && git commit -m "docs: category rework plan and cleanup / 分类重构计划文档与收尾"
git push -u origin opencode-linux/category-rework
gh pr create --title "feat: PIM 分类体系重构（统一 7 大类 + 交互打标） / Category system rework: unified dictionary + interactive labeling" --body "（含 技术修改/功能变化/如何体验/测试 四节双语）"
```

- [ ] **步骤 4：CI 门禁**

运行：`gh pr checks --watch`（或 `gh pr checks <N>` 轮询）
预期：全绿；若 workflow 未触发（路径过滤），显式说明。

- [ ] **步骤 5：三视角 review**

按 subagent-orchestration：派 `review-sol` / `review-terra` / `review-flash` 并行独立审查 PR，汇总（三都提=高置信、单一=标注视角），有 Important+ 问题则修复并推新 head 第二轮，零 blocker 后才算完成。评论只由主代理发一条综合评论（含 head SHA）。

- [ ] **步骤 6：汇报 + 阶段 2 预备**

向用户汇报：审查结论要点 + 计划执行结果 + CI/review 状态。阶段 2（服务端聚合接口：专注块/应用 Top/深夜使用/分类分布/常去地点 DBSCAN）另开计划 `docs/superpowers/plans/2026-08-XX-server-aggregation.md`，依赖本阶段完成。

---

## 后续阶段指引（不在本计划执行范围内）

- **阶段 2 服务端聚合**：专注块（连续活跃段合并）、应用时长 Top（app_signatures 归一）、深夜使用（23:30 后）、分类分布统计、常去地点 DBSCAN 聚类；分类快照改为后台定时聚合（现为页面触发 EnsureClassificationsAsync）；接口路径以实况为准：`/pc/summary`、`/pc/activity-analysis`（需求文档中 `pc/activity`、`pc/activity/analysis` 为笔误）。
- **阶段 3 前端 ECharts**：npm 引入 echarts；替换 ActivityHeatmap/CategoryTimeline/ProductivityDashboard/MobileUsageHeatmap/MobileChartsGrid 等渲染层（接口不变）；KeyboardHeatmap 保留手写；轨迹地图增强（平滑 + 停留点 + 常去地点热区）。
- **阶段 4 健康状态**：Windows 客户端 PBT_APMSUSPEND/SessionEnding/SetConsoleCtrlHandler/ProcessExit 四路监听 planned_offline 上报（新增端点）；`daemon_heartbeats` 加 planned_offline_at/offline_reason；服务端四态判定（在线/正常下线/异常离线/未接入）替换 `SystemStatusService` 一刀切（10/60 分钟）。

## 勘误（需求文档待修订项，PR 描述中提及）

1. 接口路径：`pc/activity` 实为 `/pc/summary`；`pc/activity/analysis` 实为 `/pc/activity-analysis`。
2. 图表行数：ActivityHeatmap 256 行（文档称 343）、CategoryTimeline 261 行（文档称 498）；多数图表为 div 网格非 SVG（仅 ProductivityDashboard 圆环、KeyboardHeatmap 鼠标图为 SVG）——不影响换 ECharts 结论。
3. 客户端默认 server_url 代码层面为 `http://127.0.0.1:5858`（未指向阿里云）；「仍指向旧地址」需查生产库 `daemon_heartbeats.server_url` 与装机 `%LOCALAPPDATA%\PIM\config.json` 确认。
4. 规则表代码仅种 7 条 builtin；「13 条/垃圾规则」属生产库数据 → 已由任务 1 迁移清洗覆盖。
5. `pc_app_categories` 0 条的根因：`seed_pc_tables.sql` 的 37 条种子从未被执行。
6. 手机健康文案现状为「Android 采集正常/有警告/故障」（无「可能被冻结」）。
7. `app_signatures` 种子 171 条（文档称约 172）。
8. `mobile_location_points` 无持久化 segments 表（实时计算），stay/move 两态属实。
