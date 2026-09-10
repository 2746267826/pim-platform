# tests/Pim.UnitTests/Files/FileChunkerTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：文本分块偏移/重叠/哈希；空白优先；硬切；空文本；Hashing 嵌入 384 维归一。
- 主要依赖：FileChunker / HashingFileEmbeddingService
- 被谁使用：dotnet test

## 函数级结构化伪代码

### Chunk_SplitsTextWithOffsetsOverlapAndStableHashes
### Chunk_PrefersWhitespaceBeforeHardLimit / HardLimit / Empty
### EmbedAsync_ReturnsDeterministicNormalized384DimensionalVector / ZeroVector

## 近逐行中文伪代码

1. 分块索引与 hash
2. 空白切分与硬切
3. 嵌入确定性与零向量

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileChunkerTests.cs",
      "label": "FileChunkerTests.cs",
      "path": "tests/Pim.UnitTests/Files/FileChunkerTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileChunkerTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/Files/FileChunkerTests.cs","to":"src/Pim.Module.Files/Services/FileChunker.cs","type":"tests"}
}
```