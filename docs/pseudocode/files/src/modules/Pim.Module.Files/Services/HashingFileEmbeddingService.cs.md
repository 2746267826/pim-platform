# src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：基于哈希的本地文本嵌入：分词 → SHA256 映射维度 → 累加并 L2 归一化，生成固定维 float 向量（默认 384）。
- 主要依赖：`System.Security.Cryptography`、`System.Text`、`IFileEmbeddingService`
- 被谁使用：`FilesModule` 注册为 `IFileEmbeddingService` Singleton；索引/向量检索链路调用 `EmbedAsync`

## 函数级结构化伪代码

### HashingFileEmbeddingService
#### HashingFileEmbeddingService(int dimensions = DefaultDimensions)
- 输入：向量维度，默认 384
- 输出：服务实例
- 副作用：校验并保存 `Dimensions`
- 步骤：
  1. 若 `dimensions <= 0` 抛 `ArgumentOutOfRangeException`
  2. `Dimensions = dimensions`
- 分支与异常：非法维度抛异常
- 调用：无

#### Task<float[]> EmbedAsync(string text, CancellationToken ct = default)
- 输入：文本、取消令牌
- 输出：长度 = `Dimensions` 的 float 向量（可能全零）
- 副作用：无 IO；可响应取消
- 步骤：
  1. `ct.ThrowIfCancellationRequested()`
  2. `Tokenize(text)` 得 token 列表
  3. 分配零向量；无 token 则直接返回
  4. `increment = 1 / sqrt(tokenCount)`
  5. 对每个 token：取消检查 → `HashToDimension` → 对应维 += increment
  6. `Normalize(vector)` 后返回
- 分支与异常：空/空白文本 → 零向量；取消抛 `OperationCanceledException`
- 调用：`Tokenize`、`HashToDimension`、`Normalize`

#### IEnumerable<string> Tokenize(string? text) [private static]
- 输入：可空文本
- 输出：小写字母数字 token 序列
- 副作用：无
- 步骤：
  1. null/空白 → 空序列
  2. 按 Rune 遍历：字母数字则小写追加到 builder
  3. 非字母数字时若 builder 非空则 yield token 并清空
  4. 结尾 flush 剩余 builder
- 分支与异常：无
- 调用：`Rune.IsLetterOrDigit`、`ToLowerInvariant`

#### int HashToDimension(string token) [private]
- 输入：token 字符串
- 输出：`[0, Dimensions)` 维索引
- 副作用：无
- 步骤：
  1. UTF8 字节 SHA256 → 32 字节
  2. 取前 4 字节为 UInt32
  3. `value % Dimensions` 作为维度
- 分支与异常：无
- 调用：`SHA256.HashData`、`BitConverter.ToUInt32`

#### void Normalize(float[] vector) [private static]
- 输入：向量（原地修改）
- 输出：无；向量变为单位长度或保持全零
- 副作用：原地缩放
- 步骤：
  1. 累加平方和；若为 0 直接返回
  2. 除以 sqrt(平方和) 做 L2 归一化
- 分支与异常：零向量跳过
- 调用：`MathF.Sqrt`

## 近逐行中文伪代码

1. 引用 Cryptography 与 Text
2. 命名空间 `Pim.Module.Files.Services`
3. 密封类实现 `IFileEmbeddingService`；常量 `DefaultDimensions=384`
4. 构造校验 dimensions > 0 并赋值
5. `EmbedAsync`：取消检查 → 分词 → 空则零向量 → 每 token 哈希到维并累加 1/√n → 归一化返回
6. `Tokenize`：Rune 扫描，字母数字拼 token（小写），分隔符切分
7. `HashToDimension`：SHA256 前 4 字节取模 Dimensions
8. `Normalize`：L2 归一化，零范数跳过

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs",
      "label": "HashingFileEmbeddingService",
      "path": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs", "to": "IFileEmbeddingService", "type": "implements" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Services/HashingFileEmbeddingService.cs", "type": "calls" }
  ]
}
```
