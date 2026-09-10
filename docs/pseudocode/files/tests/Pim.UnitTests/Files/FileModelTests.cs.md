# tests/Pim.UnitTests/Files/FileModelTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：锁定 Files 模块 EF 模型默认值、唯一索引、版本 FK、snake_case 列与 confidence 约束。
- 主要依赖：File* 实体、`PimDbContext` 元数据 API
- 被谁使用：xUnit

## 函数级结构化伪代码

1. Provider 默认 nextcloud/pending
2. 用户+provider+url+username 唯一索引
3. ExternalFileId 稳定身份
4. 历史版本非 current
5. 删除项仍可查询
6. snake_case 列名
7. CurrentVersion 复合 FK Restrict + 单 current 过滤索引
8. 版本子实体复合 FK 与级联策略
9. QdrantPointId 唯一过滤
10. Confidence 精度与 check 约束

## 近逐行中文伪代码

1. [L1-L49] Provider 默认与唯一索引
2. [L51-L131] Item/Version/Deleted 行为
3. [L133-L227] 元数据约束系列
4. [L229-L314] helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileModelTests.cs",
      "label": "FileModelTests",
      "path": "tests/Pim.UnitTests/Files/FileModelTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileModelTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/FileModelTests.cs", "to": "src/modules/Pim.Module.Files/Entities", "type": "tests" }
  ]
}
```
