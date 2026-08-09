# src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：经 HTTP 操作 Qdrant：确保集合、upsert 文件 chunk 向量、按 fileId 删除、用户过滤向量检索；定义 `IFileVectorStore` 与相关 record。
- 主要依赖：
  - `HttpClient`、`IConfiguration`（`Qdrant:BaseUrl`/`Collection`）
  - `IFileEmbeddingService`（向量维度）
  - `System.Net.Http.Json`
- 被谁使用：
  - `FileIndexingService` / 搜索路径
  - Files 模块 DI 注册

## 函数级结构化伪代码

### FileChunkVector / FileChunkSearchHit（record）
- 输入：构造字段
- 输出：不可变载荷
- 副作用：无
- 步骤：向量点元数据；检索命中 ChunkId/FileItemId/VersionId/Score
- 分支与异常：无
- 调用：无

### IFileVectorStore
#### 接口方法
- `EnsureCollectionAsync` / `UpsertChunksAsync` / `DeleteFileVectorsAsync` / `SearchAsync`
- 输入/输出：见实现
- 副作用：远程 Qdrant 状态变化
- 步骤：契约声明
- 分支与异常：实现决定
- 调用：实现类

### QdrantFileVectorStore
#### 主构造（primary constructor）
- 输入：httpClient、configuration、embeddingService
- 输出：实例
- 副作用：读配置 `_baseUri`（默认 `http://qdrant:6333/`）、`_collection`（默认 `file_chunks`）
- 步骤：TrimEnd BaseUrl 拼 `/`
- 分支与异常：无
- 调用：无

#### `EnsureCollectionAsync`
- 输入：ct
- 输出：Task
- 副作用：PUT 集合；Conflict 可接受
- 步骤：SendAsync PUT CollectionPath body vectors.size=Dimensions distance=Cosine；accepted Conflict
- 分支与异常：非成功且非 Conflict → EnsureSuccess 抛
- 调用：`SendAsync`、`embeddingService.Dimensions`

#### `UpsertChunksAsync`
- 输入：向量列表
- 输出：Task
- 副作用：PUT points
- 步骤：Count==0 return；映射 id/vector/payload(userId/providerId/fileId/versionId/chunkId/path/mimeType/modifiedAt)
- 分支与异常：HTTP 失败抛
- 调用：`SendAsync`

#### `DeleteFileVectorsAsync`
- 输入：fileItemId
- 输出：Task
- 副作用：POST points/delete filter must fileId match
- 步骤：SendAsync + Match helper
- 分支与异常：HTTP 失败抛
- 调用：`SendAsync`/`Match`

#### `SearchAsync(vector, userId, mode, ct)`
- 输入：查询向量、用户、mode（未使用）、ct
- 输出：最多 20 条 `FileChunkSearchHit`
- 副作用：POST points/search with_payload；filter userId
- 步骤：SendAsync；ReadFromJson `QdrantSearchResponse`；映射 Guid.Parse payload 字段与 Score；null→空列表
- 分支与异常：JSON/HTTP 异常
- 调用：`SendAsync`

#### `SendAsync` / `CollectionPath` / `Match` / 内部 record
- 输入：method、path、body、accepted codes
- 输出：HttpResponseMessage
- 副作用：HTTP 请求
- 步骤：JsonContent；RequestUri = base+path；Send；失败且不在 accepted → EnsureSuccess
- CollectionPath：`/collections/{escaped}`
- Match：Qdrant filter match value 对象
- 内部：QdrantSearchResponse/Point/Payload（JsonPropertyName fileId/versionId/chunkId）

## 近逐行中文伪代码

1. 引入 Net、Http.Json、Json.Serialization、Configuration。
2. 定义 FileChunkVector、FileChunkSearchHit、IFileVectorStore。
3. QdrantFileVectorStore 主构造读 BaseUrl/Collection。
4. EnsureCollection：PUT 向量 Cosine 维数。
5. Upsert：空跳过；PUT points 含 payload。
6. Delete：POST delete filter fileId。
7. Search：POST search limit 20 filter userId；反序列化映射命中。
8. SendAsync 统一发请求并校验状态码。
9. CollectionPath/Match 辅助；私有搜索 DTO。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs",
      "label": "QdrantFileVectorStore",
      "path": "src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs", "to": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs", "type": "depends_on" }
  ]
}
```
