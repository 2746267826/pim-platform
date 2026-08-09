# tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：PcTracker EF 模型 UUID 默认、分类快照/审计/设置、AppKnowledge 索引、SchemaInitializer SQL 对齐。
- 主要依赖：`PimDbContext`、PcTracker 实体、`PcTrackerSchemaInitializer`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### PcTrackerUuidIds_UseDatabaseGeneratedDefaults
### Snapshot/Audit/Settings 表名/默认值/索引
### AppKnowledgeContext 复合唯一与 FK SetNull
### SchemaInitializer 字符串契约

## 近逐行中文伪代码

1. [L11-31] UUID Theory
2. [L33-100] 三模型配置
3. [L102-142] AppKnowledge
4. [L144-156] Initializer SQL
5. [L158+] CreateDbContext

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs",
      "label": "PimPcTrackerModelTests",
      "path": "tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Operations/PimPcTrackerModelTests.cs", "to": "src/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "type": "tests" }
  ]
}
```
