# src/Pim.Infrastructure/Storage/MinioStorage.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Infrastructure
- 职责：封装 MinIO 客户端，对固定桶 `pim-files` 做桶确保、上传、下载、预签名 URL、删除。
- 主要依赖：`Minio`、`Minio.DataModel.Args`、`IMinioClient`
- 被谁使用：`ServiceCollectionExtensions` 注册 Singleton；`MinioQuickNoteObjectStorage` 等包装调用

## 函数级结构化伪代码

### MinioStorage
#### MinioStorage(string endpoint, string accessKey, string secretKey)
- 输入：MinIO 端点与凭证
- 输出：持有已 Build 的 `IMinioClient` 的实例
- 副作用：构建客户端（未立刻联网）
- 步骤：`new MinioClient().WithEndpoint.WithCredentials.Build()`
- 分支与异常：参数非法时客户端后续调用失败
- 调用：MinIO SDK 构建链

#### Task EnsureBucketAsync(CancellationToken ct = default)
- 输入：取消令牌
- 输出：无
- 副作用：桶不存在则创建 `pim-files`
- 步骤：
  1. `BucketExistsAsync(BucketName)`
  2. 不存在则 `MakeBucketAsync`
- 分支与异常：存在则跳过创建；网络/权限异常向上抛
- 调用：`IMinioClient.BucketExistsAsync`、`MakeBucketAsync`

#### Task<string> UploadAsync(string objectName, Stream data, string contentType, long size, CancellationToken ct = default)
- 输入：对象名、数据流、Content-Type、大小、取消令牌
- 输出：上传后的 `objectName`
- 副作用：向桶 Put 对象
- 步骤：`PutObjectAsync` 配置 bucket/object/stream/size/contentType
- 分支与异常：上传失败向上抛
- 调用：`IMinioClient.PutObjectAsync`

#### Task<Stream> DownloadAsync(string objectName, CancellationToken ct = default)
- 输入：对象名、取消令牌
- 输出：定位到 0 的 `MemoryStream`（完整对象内容）
- 副作用：从 MinIO 拉取对象到内存
- 步骤：
  1. 新建 MemoryStream
  2. `GetObjectAsync` 回调中 `CopyTo(stream)`
  3. `Position = 0` 后返回
- 分支与异常：对象不存在/网络失败向上抛
- 调用：`IMinioClient.GetObjectAsync`

#### Task<string> GetPresignedUrlAsync(string objectName, int expirySeconds = 300, CancellationToken ct = default)
- 输入：对象名、过期秒数（默认 300）、取消令牌
- 输出：预签名 GET URL 字符串
- 副作用：无对象读写；生成签名 URL
- 步骤：
  1. `ct.ThrowIfCancellationRequested`
  2. `PresignedGetObjectAsync`；用 `WaitAsync(ct)` 可取消等待
- 分支与异常：取消则抛；签名失败向上抛
- 调用：`IMinioClient.PresignedGetObjectAsync`、`Task.WaitAsync`

#### Task DeleteAsync(string objectName, CancellationToken ct = default)
- 输入：对象名、取消令牌
- 输出：无
- 副作用：从桶删除对象
- 步骤：`RemoveObjectAsync`
- 分支与异常：删除失败向上抛
- 调用：`IMinioClient.RemoveObjectAsync`

## 近逐行中文伪代码

1. 引入 Minio 与 Args
2. 命名空间 `Pim.Infrastructure.Storage`
3. 类 `MinioStorage`；私有 `_client`；常量桶名 `pim-files`
4. 构造：按 endpoint/accessKey/secretKey 构建 `IMinioClient`
5. `EnsureBucketAsync`：存在则返回，否则 MakeBucket
6. `UploadAsync`：PutObject 后返回 objectName
7. `DownloadAsync`：MemoryStream + GetObject 回调拷贝，复位 Position 返回
8. `GetPresignedUrlAsync`：先检查取消，再预签名 GET，WaitAsync 绑定 ct
9. `DeleteAsync`：RemoveObject

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Infrastructure/Storage/MinioStorage.cs",
      "label": "MinioStorage",
      "path": "src/Pim.Infrastructure/Storage/MinioStorage.cs",
      "doc": "docs/pseudocode/files/src/Pim.Infrastructure/Storage/MinioStorage.cs.md",
      "layer": "infrastructure",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/Pim.Infrastructure/Storage/MinioStorage.cs", "to": "IMinioClient", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Extensions/ServiceCollectionExtensions.cs", "to": "src/Pim.Infrastructure/Storage/MinioStorage.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs", "to": "src/Pim.Infrastructure/Storage/MinioStorage.cs", "type": "calls" },
    { "from": "src/Pim.Infrastructure/Storage/MinioStorage.cs", "to": "minio:pim-files", "type": "http" }
  ]
}
```
