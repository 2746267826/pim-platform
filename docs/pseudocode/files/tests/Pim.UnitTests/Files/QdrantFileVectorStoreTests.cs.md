# tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Qdrant HTTP：建集合、冲突即就绪、upsert/delete/search 载荷。
- 主要依赖：`QdrantFileVectorStore`、CapturingHandler、IConfiguration
- 被谁使用：dotnet test

## 函数级结构化伪代码

### EnsureCollectionAsync：PUT vectors size+Cosine；409 视为就绪
### UpsertChunksAsync：points 含 vector 与 payload 元数据
### DeleteFileVectorsAsync：filter fileId
### SearchAsync：filter userId 映射 hits score/path

## 近逐行中文伪代码

1. [L18-47] 集合
2. [L49-107] upsert/delete
3. [L109+] search 映射

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs",
      "label": "QdrantFileVectorStoreTests",
      "path": "tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/QdrantFileVectorStoreTests.cs", "to": "src/Pim.Module.Files/Services/QdrantFileVectorStore.cs", "type": "tests" }
  ]
}
```
