# src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.PcTracker
- 职责：PC 应用签名/目录实体：进程名、展示名、分类路径、生产力标签、描述、来源与置信度、图标与搜索词、最后可见时间。
- 主要依赖：无外部类型（简单 POCO）
- 被谁使用：`AppKnowledgeContextService` 解析 DisplayName；应用目录/分类相关服务；`PimDbContext`

## 函数级结构化伪代码

### AppSignatureEntity
#### 属性集合
- 输入：属性赋值
- 输出：实体状态
- 副作用：无
- 步骤：
  1. `Id` Guid
  2. `ProcessName`/`DisplayName` 默认空串
  3. 可选 `CategoryPath`/`Productivity`/`Description`
  4. `Source` 默认 `"builtin"`；`Confidence` 默认 1.0
  5. 可选 `Icon`/`SearchKeywords`
  6. 可选 `LastSeenAt`；`CreatedAt`/`UpdatedAt`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.PcTracker.Entities`
2. 类 `AppSignatureEntity`（非 sealed）
3. Id/ProcessName/DisplayName/CategoryPath/Productivity/Description
4. Source=builtin、Confidence=1.0、Icon、SearchKeywords
5. LastSeenAt、CreatedAt、UpdatedAt

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs",
      "label": "AppSignatureEntity",
      "path": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs.md",
      "layer": "module.pctracker",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.PcTracker/Services/AppKnowledgeContextService.cs", "to": "src/modules/Pim.Module.PcTracker/Entities/AppSignatureEntity.cs", "type": "depends_on" }
  ]
}
```
