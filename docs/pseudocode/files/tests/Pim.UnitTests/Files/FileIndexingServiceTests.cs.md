# tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：覆盖 `FileIndexingService` 索引跳过/替换分块向量/无版本 404/混合搜索。
- 主要依赖：`FileIndexingService`、`FileOperationService`、`FileProviderBindingService`、`IFileTextExtractionService`、`IFileEmbeddingService`、`IFileVectorStore`
- 被谁使用：xUnit

## 函数级结构化伪代码

### FileIndexingServiceTests
#### IndexCurrentVersionAsync_WhenMimeTypeUnsupportedCreatesSkippedJobAndNoChunks
- image/png → skipped/mime_type，不下载、无 chunk/vector
#### IndexCurrentVersionAsync_WhenExtractedTextEmptyCreatesSkippedJobAndNoVectors
- 空白提取 → skipped/extract
#### IndexCurrentVersionAsync_WhenCurrentVersionChangesReplacesChunksAndVectors
- 旧 chunk 存在；成功后 stage=qdrant，删旧向量、新 chunk 全属当前版本
#### IndexCurrentVersionAsync_WhenNoCurrentVersionThrowsNotFound
- DomainException 5304
#### SearchAsync_ReturnsKeywordAndSemanticResultsForCurrentUser
- hybrid 搜索返回 item+chunk，vectorStore 记录 user/mode
#### helpers
- CreateDb/CreateService/Seed* / FixedCurrentUser / FakeSecretProtector / FakeTextExtraction / FakeEmbedding / FakeVectorStore / FakeFileProviderAdapter

## 近逐行中文伪代码

1. [L1-L19] using 与 UserId
2. [L20-L36] 不支持 MIME 跳过
3. [L38-L55] 空文本跳过
4. [L57-L115] 版本变更替换索引
5. [L117-L127] 无当前版本 NotFound
6. [L129-L160] hybrid Search
7. [L162-L434] 装配与大量 Fake 适配器实现

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs",
      "label": "FileIndexingServiceTests",
      "path": "tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs", "to": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs", "to": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "type": "depends_on" },
    { "from": "tests/Pim.UnitTests/Files/FileIndexingServiceTests.cs", "to": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "type": "depends_on" }
  ]
}
```
