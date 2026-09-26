# src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件文本向量嵌入服务契约；暴露维度与异步 Embed。
- 主要依赖：无（BCL `Task`/`float[]`）
- 被谁使用：`HashingFileEmbeddingService` 实现；`FilesModule` 注册；`FileIndexingService`、`QdrantFileVectorStore` 注入调用

## 函数级结构化伪代码

### IFileEmbeddingService
#### int Dimensions { get; }
- 输入：无
- 输出：向量维度
- 副作用：无
- 步骤：实现方返回固定或配置维度
- 分支与异常：无
- 调用：无（接口）

#### Task float[] EmbedAsync(string text, CancellationToken ct = default)
- 输入：待嵌入文本、取消令牌
- 输出：浮点向量
- 副作用：可能调用外部模型（实现定义）；默认实现为本地哈希嵌入
- 步骤：实现方将 text 转为 `Dimensions` 维向量
- 分支与异常：取消/实现错误向上抛
- 调用：无（接口）

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Services`
2. 接口 `IFileEmbeddingService`
3. 只读属性 `Dimensions`
4. 方法 `EmbedAsync(text, ct)` 返回 `float[]`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs",
      "label": "IFileEmbeddingService",
      "path": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs", "to": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/QdrantFileVectorStore.cs", "to": "src/modules/Pim.Module.Files/Services/IFileEmbeddingService.cs", "type": "calls" }
  ]
}
```
