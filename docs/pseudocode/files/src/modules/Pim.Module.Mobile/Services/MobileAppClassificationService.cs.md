# src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile
- 职责：按用户覆盖、用户规则、Android 元数据、内置规则与系统噪声启发式，将 package 分类为生活类别并解析展示名。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`MobileAppCatalogEntity`/`Override`/`CategoryRule`、`MobileLifeCategories`、`System.Text.Json`
- 被谁使用：`MobileUsageAggregationService`、Mobile 分析/规则 API

## 函数级结构化伪代码

### MobileAppClassificationService
#### 构造 MobileAppClassificationService(PimDbContext, ICurrentUserService)
- 输入：EF 上下文、当前用户
- 输出：服务实例
- 副作用：无
- 步骤：保存 `_db`、`_currentUser`
- 分支与异常：无
- 调用：无

#### Task\<MobileAppClassificationResult\> ClassifyAsync(string packageName, CancellationToken)
- 输入：包名
- 输出：分类结果
- 副作用：读库
- 步骤：包装为 `MobileAppClassificationInput` 后调用完整重载
- 分支与异常：透传
- 调用：`ClassifyAsync(MobileAppClassificationInput, ...)`

#### Task\<MobileAppClassificationResult\> ClassifyAsync(MobileAppClassificationInput input, CancellationToken)
- 输入：包名 + 可选 DisplayName/AndroidCategory/InstallerPackage/IsSystemApp
- 输出：`MobileAppClassificationResult`（包名、展示名、生活分类、噪声、隐藏短事件、来源、是否有元数据）
- 副作用：只读查询 catalog/override/rules
- 步骤：
  1. `RequireUserId`；规范化 packageName；加载最新 catalog 元数据；解析 displayName。
  2. 若存在用户 override：返回 user-override 结果（覆盖展示名/分类/噪声）。
  3. 加载启用用户规则；`TryClassifyWithRules` 命中则 `BuildRuleResult`。
  4. 合并 input 与 metadata 的 AndroidCategory/Installer/IsSystemApp；`DetectSystemNoise`。
  5. `TryMapAndroidCategory` 成功 → source=`android-metadata`（噪声则强制 ToolsSystem）。
  6. 依次：内置 package 精确 → 前缀 → 关键字。
  7. `TryMapRawMetadata`（RawJson 中 category 类字段）→ android-metadata。
  8. 若 systemNoise → ToolsSystem + built-in-system-noise。
  9. 否则 Uncategorized + fallback。
- 分支与异常：空包名抛 `ArgumentException`
- 调用：`LoadLatestMetadataAsync`、`LoadEnabledRulesAsync`、各类 TryClassify/Map/Detect

#### LoadLatestMetadataAsync / LoadEnabledRulesAsync
- 输入：userId、packageName 或仅 userId
- 输出：最新 catalog 行；按 Priority/CreatedAt/Pattern/Id 排序的启用规则列表
- 副作用：AsNoTracking 查询
- 步骤：catalog 按 UpdatedAt/LastUpdateTimeUtc/CreatedAt/DeviceId 降序取首；rules 过滤 IsEnabled
- 调用：EF

#### TryClassifyWithRules / RuleMatches / BuildRuleResult
- 输入：package、displayName、规则列表
- 输出：是否命中及匹配规则；或完整分类结果
- 步骤：
  1. 按 `UserRuleOrder`（exact→prefix→keyword→display-keyword→package-keyword）扫描。
  2. `RuleMatches`：规范化 pattern 后按 ruleType 做 equals/StartsWith/Contains。
  3. `BuildRuleResult`：合并规则噪声与 DetectSystemNoise；Source=`user-rule:{type}`。
- 调用：`NormalizeRuleType`、`NormalizeLifeCategory`

#### BuildBuiltInResult / ResolveDisplayName / TryMapAndroidCategory / TryMapRawMetadata
- 输入：内置分类或 Android/Raw 元数据
- 输出：结果或 lifeCategory
- 步骤：噪声覆盖为 ToolsSystem；展示名 FirstNonBlank 链；AndroidCategoryMap 与 InstallerPackageMap；解析 RawJson 属性名列表
- 分支：JsonException → false

#### TryClassifyBuiltInPackage/Prefix/Keyword / DetectSystemNoise
- 输入：package、displayName、androidCategory、isSystemApp
- 输出：内置分类或是否系统噪声
- 步骤：字典精确匹配；前缀数组；关键字 Contains；噪声：category 白名单、精确包、子串、前缀、系统前缀+isSystemApp
- 调用：静态表 `BuiltIn*` / `SystemNoise*`

#### Normalize* / FirstNonBlank / ContainsIgnoreCase
- 输入：字符串
- 输出：规范化小写/合法生活分类/首个非空白
- 分支：空 package 抛异常；未知 lifeCategory → Uncategorized

### MobileAppClassificationInput / MobileAppClassificationResult
- 输入/输出：record 字段契约
- 副作用：无

## 近逐行中文伪代码

1. 定义规则类型常量与服务字段。
2. 构造注入 Db 与当前用户。
3. 包名重载转 Input；完整 Classify：取用户、规范化包名、加载元数据与展示名。
4. 用户 override 优先返回。
5. 用户规则按类型顺序匹配，命中构建 rule 结果。
6. 合并 Android 元数据，检测系统噪声；映射 Android 分类/安装器。
7. 内置精确包、前缀、关键字；RawJson 分类字段；噪声回落；最终 fallback。
8. 辅助：加载 catalog/rules、规则匹配、内置表、噪声检测、字符串规范化。
9. 静态表：友好名、BuiltInPackage/Prefix/Keyword、AndroidCategoryMap、InstallerMap、噪声集合。
10. 文件末尾定义 Input/Result record。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs",
      "label": "MobileAppClassificationService",
      "path": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs.md",
      "layer": "module.mobile",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "to": "src/Pim.Infrastructure/Auth/CurrentUserService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCatalogEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "to": "src/modules/Pim.Module.Mobile/Entities/MobileAppCategoryRuleEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "to": "src/modules/Pim.Module.Mobile/DTOs/MobileAnalyticsDtos.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Services/MobileUsageAggregationService.cs", "to": "src/modules/Pim.Module.Mobile/Services/MobileAppClassificationService.cs", "type": "calls" }
  ]
}
```
