# src/modules/Pim.Module.Files/Services/FileIndexingService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件文本抽取接口与 Tika 实现；对当前版本建索引（下载→抽取→分块→嵌入→Qdrant）；关键词/语义/混合搜索。
- 主要依赖：`PimDbContext`、`ICurrentUserService`、`FileOperationService`、`IFileTextExtractionService`/`TikaClient`、`IFileEmbeddingService`、`IFileVectorStore`、`FileChunker`、Files 实体/DTO
- 被谁使用：Files 模块端点/作业触发索引与搜索

## 函数级结构化伪代码

### IFileTextExtractionService
#### Task\<string\> ExtractTextAsync(Stream, fileName, ct)
- 输入：文件流与文件名
- 输出：抽取文本
- 副作用：依赖实现
- 步骤：接口契约
- 分支与异常：由实现定义
- 调用：无

### TikaFileTextExtractionService
#### Task\<string\> ExtractTextAsync(...)
- 输入：流、文件名、ct
- 输出：文本
- 副作用：HTTP 调 Tika（经客户端）
- 步骤：委托 `tikaClient.ExtractTextAsync`
- 分支与异常：透传
- 调用：`TikaClient`

### FileIndexingService
#### 主构造注入
- 输入：db、currentUser、fileOperations、textExtraction、embeddings、vectorStore
- 输出：实例
- 副作用：无
- 步骤：主构造参数捕获；静态支持 MIME 集合（plain/md/csv/pdf/docx/xlsx/pptx）
- 分支与异常：无
- 调用：无

#### Guid UserId
- 输入：无
- 输出：当前用户
- 副作用：无
- 步骤：空则 `DomainException(1002, "未登录")`
- 分支与异常：未登录
- 调用：`ICurrentUserService`

#### Task\<FileIndexJobDto\> IndexCurrentVersionAsync(Guid fileItemId, ct)
- 输入：文件项 ID
- 输出：索引作业 DTO
- 副作用：写 `FileIndexJobEntity`/`FileChunkEntity`、向量库 upsert/delete、多阶段 Save
- 步骤：
  1. `LoadItemAsync`；`CreateJob`（running/metadata）并 Add+Save
  2. folder → `SkipJobAsync`(metadata, 文件夹不能建立索引)
  3. `LoadCurrentVersionAsync`，更新 job.VersionId
  4. MIME 不支持 → skip(mime_type)
  5. try：stage download→`fileOperations.DownloadAsync`；extract→抽文本，空则 skip；chunk→`FileChunker.Chunk`；EnsureCollection+Delete 旧向量与 DB chunks；写入新 chunks（含 QdrantPointId）；embed 逐块；qdrant Upsert；status succeeded
  6. catch 非 DomainException：failed+LastError+rethrow
- 分支与异常：文件/版本不存在域异常；其它异常标记失败后抛出
- 调用：下载、抽取、分块、嵌入、向量存储、EF

#### Task\<FileSearchResultDto\> SearchAsync(FileSearchQuery, ct)
- 输入：查询 Q/Mode
- 输出：文件项列表 + chunk 命中
- 副作用：可能调用嵌入与向量搜索
- 步骤：
  1. Q 空白 → 双空列表
  2. Mode 默认 hybrid；非法回退 hybrid；决定是否 keyword/semantic
  3. keyword：`SearchItemsAsync`
  4. semantic：Embed 查询 → vectorStore.Search → 按 ChunkId 加载 chunk+FileItem+Provider（用户且未删）→ 按命中序 Map 分数
- 分支与异常：未登录
- 调用：embeddings、vectorStore、EF

#### SearchItemsAsync / LoadItemAsync / LoadCurrentVersionAsync / CreateJob / SkipJobAsync / BuildPointId / MapJob / MapFileItem / LatestIndexStatus
- 输入：见签名
- 输出：DTO/实体/点 ID/状态串
- 副作用：Skip 会 Save
- 步骤：
  1. SearchItems：拉用户未删项，内存过滤 Name/Path/Mime，文件夹优先，最多 20
  2. LoadItem：含 Provider，可选 IndexJobs；不存在 5300
  3. LoadCurrentVersion：CurrentVersionId 空或非 current/IsCurrent → 5304
  4. CreateJob：running/metadata/AttemptCount=1
  5. Skip：status skipped + stage/reason
  6. BuildPointId：SHA256(file:version:index) 前 16 字节变 Guid
  7. MapJob/MapFileItem/LatestIndexStatus：投影与最新作业状态
- 分支与异常：5300/5304
- 调用：SHA256、EF

## 近逐行中文伪代码

1. 引入加密哈希、EF、异常、Auth、Data、Tika、Files DTO/实体
2. 接口 `IFileTextExtractionService`；`TikaFileTextExtractionService` 转调 TikaClient
3. `FileIndexingService` 主构造注入六依赖；静态支持 MIME 集合
4. `UserId` 未登录 1002
5. `IndexCurrentVersionAsync`：建作业→跳过文件夹/不支持 MIME→下载抽取分块→替换 chunk 与向量→成功；失败标记后抛
6. `SearchAsync`：hybrid/keyword/semantic 分流；关键词内存过滤；语义向量检索后回填 chunk 文本
7. 私有加载/跳过/点 ID/映射辅助方法

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs",
      "label": "FileIndexingService",
      "path": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/FileIndexingService.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/Pim.Infrastructure/Data/PimDbContext.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/Pim.Infrastructure/TextExtraction/TikaClient.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "IFileEmbeddingService", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "IFileVectorStore", "type": "depends_on" }
  ]
}
```
