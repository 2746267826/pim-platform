# src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件文本分块实体，关联 FileItem 与 FileVersion，记录块序号、文本与哈希、偏移及可选 Qdrant 点 ID。
- 主要依赖：
  - `FileItemEntity`、`FileVersionEntity`（导航）
- 被谁使用：Files 模块索引/检索与 DbContext 配置

## 函数级结构化伪代码

### FileChunkEntity
#### 属性与默认值（无自定义方法）
- 输入：无（POCO）
- 输出：字段
- 副作用：无
- 步骤：
  1. `Id` 默认 NewGuid。
  2. `FileItemId` + 可选 `FileItem`；`VersionId` + 可选 `Version`。
  3. `ChunkIndex`；`Text`/`TextHash` 默认空串。
  4. `StartOffset`/`EndOffset`；`QdrantPointId` 可空。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`；sealed 类。
2. 主键 Id；外键 FileItemId / VersionId 及导航。
3. ChunkIndex、Text、TextHash、起止偏移、QdrantPointId。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs",
      "label": "FileChunkEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs", "type": "depends_on" }
  ]
}
```
