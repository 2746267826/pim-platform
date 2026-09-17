# src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.QuickNotes
- 职责：`IQuickNoteObjectStorage` 的空实现；MinIO 未配置时拒绝读写附件，删除操作为空成功。
- 主要依赖：`IQuickNoteObjectStorage`
- 被谁使用：QuickNotes 模块 DI 在未配置对象存储时注册

## 函数级结构化伪代码

### NullQuickNoteObjectStorage
#### Task\<string\> StoreAsync(string objectKey, Stream content, string contentType, long sizeBytes, CancellationToken ct)
- 输入：对象键、内容流、MIME、大小、取消令牌
- 输出：永不正常返回
- 副作用：无
- 步骤：
  1. 抛出 `InvalidOperationException("MinIO 未配置，附件功能不可用")`
- 分支与异常：始终失败
- 调用：无

#### Task\<Stream\> OpenReadAsync(string objectKey, CancellationToken ct)
- 输入：对象键、取消令牌
- 输出：永不正常返回
- 副作用：无
- 步骤：
  1. 抛出同一 `InvalidOperationException`
- 分支与异常：始终失败
- 调用：无

#### Task DeleteAsync(string objectKey, CancellationToken ct)
- 输入：对象键、取消令牌
- 输出：已完成的空 Task
- 副作用：无
- 步骤：
  1. 返回 `Task.CompletedTask`（幂等无操作）
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.QuickNotes.Services`
2. 密封类 `NullQuickNoteObjectStorage` 实现 `IQuickNoteObjectStorage`
3. 常量 `NotConfiguredMessage` = 「MinIO 未配置，附件功能不可用」
4. `StoreAsync`：直接抛 `InvalidOperationException(NotConfiguredMessage)`
5. `OpenReadAsync`：同样抛该异常
6. `DeleteAsync`：返回 `Task.CompletedTask`，不校验 objectKey

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs",
      "label": "NullQuickNoteObjectStorage",
      "path": "src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs.md",
      "layer": "module.quicknotes",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.QuickNotes/Services/NullQuickNoteObjectStorage.cs", "to": "src/modules/Pim.Module.QuickNotes/Services/IQuickNoteObjectStorage.cs", "type": "implements" }
  ]
}
```
