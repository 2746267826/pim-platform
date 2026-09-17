# src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：定义速记附件对象存储抽象：按 objectKey 写入、打开只读流、删除。
- 主要依赖：`System.IO.Stream`、`System.Threading`
- 被谁使用：`MinioQuickNoteObjectStorage`、`NullQuickNoteObjectStorage` 实现；`QuickNoteAttachmentService` 等注入调用

## 函数级结构化伪代码

### IQuickNoteObjectStorage
#### Task<string> StoreAsync(string objectKey, Stream content, string contentType, long sizeBytes, CancellationToken ct = default)
- 输入：对象键、内容流、MIME 类型、字节大小、取消令牌
- 输出：实现约定返回的标识字符串（通常为 objectKey 或存储 URL/键）
- 副作用：将内容持久化到对象存储
- 步骤：
  1. 实现方按 objectKey 写入 content，附带 contentType 与 sizeBytes 元数据
  2. 返回存储结果键或路径字符串
- 分支与异常：取消令牌触发时取消；实现可抛存储/IO 异常
- 调用：具体实现（MinIO/空实现等）

#### Task<Stream> OpenReadAsync(string objectKey, CancellationToken ct = default)
- 输入：对象键、取消令牌
- 输出：可读 `Stream`
- 副作用：打开远程/本地对象读取句柄
- 步骤：
  1. 按 objectKey 定位对象
  2. 返回可读流
- 分支与异常：不存在或权限失败由实现抛出
- 调用：具体实现

#### Task DeleteAsync(string objectKey, CancellationToken ct = default)
- 输入：对象键、取消令牌
- 输出：无（Task）
- 副作用：删除对象
- 步骤：
  1. 按 objectKey 删除
- 分支与异常：不存在时行为由实现定义；取消可中断
- 调用：具体实现

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.QuickNotes.Services`
2. 声明接口 `IQuickNoteObjectStorage`
3. `StoreAsync`：接收 objectKey、content 流、contentType、sizeBytes、可选 ct；异步返回 string
4. `OpenReadAsync`：按 objectKey 异步打开只读 Stream
5. `DeleteAsync`：按 objectKey 异步删除对象
6. 无实现体；仅契约定义

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs",
      "label": "IQuickNoteObjectStorage",
      "path": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs.md",
      "layer": "module.quicknotes",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Services/MinioQuickNoteObjectStorage.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/Services/QuickNoteAttachmentService.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "calls" }
  ]
}
```
