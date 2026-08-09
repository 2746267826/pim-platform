# src/modules/Pim.Module.Files/Services/FileChunker.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：将纯文本按最大字符数切块（空白边界、重叠、去空白控制符），并为每块计算 SHA256 小写十六进制哈希。
- 主要依赖：`System.Security.Cryptography.SHA256`、`Encoding.UTF8`
- 被谁使用：Files 索引/分块管线相关服务

## 函数级结构化伪代码

### FileTextChunk（record）
#### 记录字段
- 输入：构造参数
- 输出：`ChunkIndex`、`Text`、`TextHash`、`StartOffset`、`EndOffset`
- 副作用：无
- 步骤：不可变数据载体
- 分支与异常：无
- 调用：无

### FileChunker
#### `static IReadOnlyList<FileTextChunk> Chunk(string? text, int maxChars = 1600, int overlapChars = 160)`
- 输入：全文、最大块长、重叠长度
- 输出：有序分块列表（可能空）
- 副作用：无
- 步骤：
  1. maxChars≤0 或 overlapChars<0 → `ArgumentOutOfRangeException`。
  2. text 空或全为可跳过字符 → 空列表。
  3. effectiveOverlap = min(overlap, maxChars-1)。
  4. start = 首个内容偏移；循环直到 start≥Length：
     - end = FindChunkEnd(start, maxChars)
     - 裁剪首尾可跳过字符；若 trimmedStart < trimmedEnd 则取切片、算 SHA256、加入 chunk
     - end 到文末则 break；否则 start = max(end-overlap, start+1) 再跳到内容起点
  5. 返回 chunks。
- 分支与异常：参数非法抛异常
- 调用：`FindChunkEnd`、`FirstContentOffset`、`LastContentOffset`、`Sha256LowerHex`

#### `static int FindChunkEnd(text, start, maxChars)`
- 输入：文本、起点、最大长
- 输出：硬切或回退到空白处的结束下标
- 副作用：无
- 步骤：hardEnd = min(start+max, Length)；若已到末尾返回 Length；否则从 hardEnd 回退找空白前一位置，找不到则 hardEnd
- 分支与异常：无
- 调用：`char.IsWhiteSpace`

#### `static int FirstContentOffset` / `LastContentOffset` / `IsSkippable` / `Sha256LowerHex`
- First：从 start clamp 后跳过空白/控制符。
- Last：从 end clamp 后回退跳过空白/控制符。
- IsSkippable：空白或控制字符。
- Sha256LowerHex：UTF8 字节 → SHA256 → 小写 hex。

## 近逐行中文伪代码

1. 引入 Cryptography、Text。
2. 记录 `FileTextChunk`：索引、文本、哈希、起止偏移。
3. 静态类默认 max=1600、overlap=160。
4. Chunk：校验参数；空/全跳过 → []；循环切块、裁边、哈希、推进 start（含重叠）。
5. FindChunkEnd：优先在硬限内回退到空白边界。
6. First/LastContentOffset 跳过可跳过字符。
7. IsSkippable = 空白或控制符。
8. Sha256LowerHex = UTF8 + SHA256 + ToLowerInvariant hex。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Services/FileChunker.cs",
      "label": "FileChunker",
      "path": "src/modules/Pim.Module.Files/Services/FileChunker.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Services/FileChunker.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Services/FileChunker.cs", "to": "System.Security.Cryptography", "type": "depends_on" }
  ]
}
```
