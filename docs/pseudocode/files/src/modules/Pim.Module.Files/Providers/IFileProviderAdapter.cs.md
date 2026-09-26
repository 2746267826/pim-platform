# src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：外部文件提供方适配器契约与提供方侧 DTO（连接、列表项、版本、回收站、打开链接、下载流）。
- 主要依赖：无项目内类型（仅 BCL：`Stream`、`Task`、集合）
- 被谁使用：`NextcloudFileProviderAdapter` 实现；`FilesModule` 注册；`FileOperationService`/`FileProviderBindingService`/`FileIndexingService` 调用

## 函数级结构化伪代码

### 提供方 DTO records
#### FileProviderConnection / FileProviderTestResult / ProviderFileItem / ProviderFileVersion / ProviderTrashItem / ProviderOpenLink / ProviderDownload
- 输入：构造字段（连接凭据、测试结果、远端元数据、下载流等）
- 输出：不可变记录
- 副作用：`ProviderDownload` 持有 `Stream`（由调用方管理生命周期）
- 步骤：字段即语义，无逻辑
- 分支与异常：无
- 调用：无

### IFileProviderAdapter
#### Task FileProviderTestResult TestConnectionAsync(FileProviderConnection, CancellationToken)
- 输入：连接信息
- 输出：成功/状态/错误
- 副作用：远端探测（实现定义）
- 步骤：实现方测试连通
- 分支与异常：失败体现在结果或异常（实现定义）
- 调用：无（接口）

#### Task ListFolderAsync / GetMetadataAsync / UploadAsync / DownloadAsync / MoveAsync / RenameAsync / DeleteToTrashAsync
- 输入：连接 + 路径或流
- 输出：列表项、元数据、下载包等
- 副作用：远端读写
- 步骤：按方法名对应的远端操作契约
- 分支与异常：实现定义
- 调用：无（接口）

#### Task ListTrashAsync / RestoreTrashAsync
- 输入：连接；恢复时 trashId
- 输出：回收站列表或完成
- 副作用：远端回收站操作
- 步骤：列出/恢复
- 分支与异常：实现定义
- 调用：无（接口）

#### Task ListVersionsAsync / DownloadVersionAsync / RestoreVersionAsync
- 输入：连接、externalFileId、可选 versionId/fileName
- 输出：版本列表、历史下载、恢复完成
- 副作用：远端版本操作
- 步骤：版本生命周期契约
- 分支与异常：实现定义
- 调用：无（接口）

#### ProviderOpenLink BuildOpenLink(FileProviderConnection, string path, string mode, string? externalFileId = null)
- 输入：连接、路径、打开模式、可选外部文件 Id
- 输出：URL + mode
- 副作用：无 I/O 预期（纯构造）
- 步骤：实现方拼打开链接
- 分支与异常：实现定义
- 调用：无（接口）

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Providers`
2. 定义连接/测试/文件项/版本/回收站/打开链接/下载等 record
3. 接口 `IFileProviderAdapter`：连通测试、列目录、元数据、上传下载、移动重命名、进回收站
4. 回收站列表与恢复
5. 版本列表/下载/恢复
6. 同步方法 `BuildOpenLink` 生成打开链接

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs",
      "label": "IFileProviderAdapter",
      "path": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs.md",
      "layer": "module.files",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Providers/NextcloudFileProviderAdapter.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileProviderBindingService.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "calls" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Providers/IFileProviderAdapter.cs", "type": "depends_on" }
  ]
}
```
