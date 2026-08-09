# src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：`IQuickNoteObjectStorage` 的 MinIO 实现；委托 `MinioStorage` 完成桶确保、上传、下载、删除。
- 主要依赖：`MinioStorage`、`IQuickNoteObjectStorage`
- 被谁使用：QuickNotes 模块 DI 注册后的附件读写路径

## 函数级结构化伪代码

### MinioQuickNoteObjectStorage
#### 构造(MinioStorage storage)
- 输入：基础设施 MinIO 封装
- 输出：实例
- 副作用：无
- 步骤：主构造器捕获 `storage` 主构造参数
- 分支与异常：无
- 调用：无

#### Task\<string\> StoreAsync(objectKey, content, contentType, sizeBytes, ct)
- 输入：对象键、内容流、MIME、字节数、取消令牌
- 输出：上传后的对象键/路径（`UploadAsync` 返回值）
- 副作用：确保桶存在并写入对象
- 步骤：
  1. `EnsureBucketAsync(ct)`
  2. `UploadAsync(objectKey, content, contentType, sizeBytes, ct)` 并返回
- 分支与异常：透传底层异常
- 调用：`MinioStorage.EnsureBucketAsync`、`UploadAsync`

#### Task\<Stream\> OpenReadAsync(objectKey, ct)
- 输入：对象键、取消令牌
- 输出：可读流
- 副作用：从 MinIO 拉流
- 步骤：直接 `DownloadAsync(objectKey, ct)`
- 分支与异常：透传
- 调用：`MinioStorage.DownloadAsync`

#### Task DeleteAsync(objectKey, ct)
- 输入：对象键、取消令牌
- 输出：无
- 副作用：删除对象
- 步骤：`DeleteAsync(objectKey, ct)`
- 分支与异常：透传
- 调用：`MinioStorage.DeleteAsync`

## 近逐行中文伪代码

1. 主构造注入 `MinioStorage storage`
2. `StoreAsync`：先 `EnsureBucketAsync`，再 `UploadAsync` 返回键
3. `OpenReadAsync`：表达式体转发 `DownloadAsync`
4. `DeleteAsync`：表达式体转发 `DeleteAsync`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs",
      "label": "MinioQuickNoteObjectStorage",
      "path": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs.md",
      "layer": "module.quicknotes",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs", "to": "src/Pim.Infrastructure/Storage/MinioStorage.cs", "type": "depends_on" }
  ]
}
```
