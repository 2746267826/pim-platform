# src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：活动分类规则的加载、列表、校验与新建保存；规范化 scope 并校验 conditions JSON 结构/字段/操作符/正则。
- 主要依赖：`PimDbContext`、`ActivityCategoryRuleEntity`、`PcCategoryEntity`、`ActivityClassificationRuleDto` / `SaveActivityClassificationRuleRequest`、`System.Text.Json`、`Regex`
- 被谁使用：`PcTrackerService`、分类规则 API

## 函数级结构化伪代码

### ActivityClassificationRuleService
#### 构造(PimDbContext db)
- 步骤：保存 `_db`
- 调用：无

#### Task\<List\<ActivityCategoryRuleEntity\>\> LoadActiveAsync(ct)
- 输入：取消令牌
- 输出：Status==active 的规则，按 Priority/CreatedAt/RuleName/Id 排序
- 副作用：读库
- 调用：EF Where/OrderBy

#### Task\<List\<ActivityClassificationRuleDto\>\> ListAsync(ct)
- 输入：取消令牌
- 输出：全部规则 DTO，Priority 降序 + RuleName
- 调用：`ToDto`

#### Task\<ActivityClassificationRuleDto\> SaveAsync(request, ct)
- 输入：保存请求
- 输出：新建规则 DTO
- 副作用：Add + SaveChanges
- 步骤：Validate（含唯一名）→ ToEntity → Add → Save → ToDto
- 分支：校验失败抛 Argument/InvalidOperation
- 调用：`ValidateAsync`、`ToEntity`

#### Task ValidateAsync(request, ensureUniqueRuleName, ct)
- 输入：请求、是否校验唯一规则名
- 输出：无（失败抛异常）
- 步骤：
  1. RuleName 非空；NormalizeScope；ValidateConditionsJson。
  2. ensureUnique 时查 RuleName 是否已存在。
  3. 若有 CategoryName，须在 `PcCategoryEntity` 中存在。
- 分支：空名、重复名、分类不存在、JSON 非法

#### static NormalizeScope(scope)
- 输入：scope 字符串
- 输出：`activity` | `both` | `project`（空/`app`→activity）
- 分支：未知 scope 抛 ArgumentException

#### static ToEntity / ToDto
- 输入：请求或实体
- 输出：实体（新 Guid、Source=user、Status=active、时间戳）或 DTO 投影
- 调用：`NormalizeScope`

#### ValidateConditionsJson / ValidateCondition / ValidateConditionValue
- 输入：conditions JSON
- 输出：无
- 步骤：
  1. 解析为 Object，必须含非空 `all` 数组。
  2. 每项含 field/op/value；field/op 在允许集合内。
  3. containsAny 要求非空字符串数组；其他 op 要求非空字符串；regex 用 100ms 超时编译。
- 分支：JsonException / RegexParseException → ArgumentException

#### TryGetStringProperty
- 输入：JsonElement 与属性名
- 输出：非空白字符串或 false

## 近逐行中文伪代码

1. 正则超时 100ms；构造注入 Db。
2. LoadActive：active 排序列表；List：全量 ToDto。
3. Save：校验→实体→落库。
4. Validate：名称、scope、条件 JSON、唯一名、分类存在性。
5. NormalizeScope 映射 app/activity/both/project。
6. ToEntity/ToDto 字段映射。
7. 条件 JSON：all 数组；字段白名单；操作符白名单；containsAny/regex 特殊校验。
8. 允许字段：recordType/appName/domain/urlPath/title 等；操作符 equals/contains/regex 等。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs",
      "label": "ActivityClassificationRuleService",
      "path": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs.md",
      "layer": "module.pctracker",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/ActivityCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/PcCategoryEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "to": "src/modules/Pim.Module.PcTracker/DTOs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationRuleService.cs", "type": "calls" }
  ]
}
```
