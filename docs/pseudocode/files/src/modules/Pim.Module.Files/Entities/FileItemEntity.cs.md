# src/modules/Pim.Module.Files/Entities/FileItemEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件树条目实体（文件/文件夹元数据、同步与软删标记），并导航版本、索引任务、分块、AI 结果与建议。
- 主要依赖：同模块 `FileProviderEntity`、`FileVersionEntity`、`FileIndexJobEntity`、`FileChunkEntity`、`FileAiResultEntity`、`FileSuggestionEntity`
- 被谁使用：Files 模块服务（同步、索引、分块、搜索等）、EF 配置与迁移

## 函数级结构化伪代码

### FileItemEntity
#### 属性组（EF 实体 POCO，无业务方法）
- 输入：同步/服务层赋值后由 EF 持久化
- 输出：文件项一行及关联集合
- 副作用：默认值在属性初始化时给出
- 步骤：
  1. `Id` 默认 NewGuid；`ProviderId` + 导航 `Provider`。
  2. 外部标识：`ExternalFileId`、`ParentExternalFileId`、`Path` 默认 `"/"`、`Name`。
  3. `ItemType` 默认 `"file"`；`MimeType`/`Size`/`Etag`/`ContentHash` 可空。
  4. `CurrentVersionId`；`Permissions`；`IsDeleted` + `DeletedAt`。
  5. 时间：`LastSeenAt` 可空；`CreatedAt`/`ModifiedAt`/`SyncedAt` 默认 UtcNow。
  6. 集合：Versions、IndexJobs、Chunks、AiResults、Suggestions（初始化空 List）。
- 分支与异常：本类型无校验逻辑
- 调用：被 Files 服务与索引管线读写

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`；sealed 类 `FileItemEntity`。
2. Id、ProviderId、Provider 导航。
3. ExternalFileId、ParentExternalFileId、Path、Name、ItemType、MimeType、Size、Etag、ContentHash。
4. CurrentVersionId、Permissions、IsDeleted、DeletedAt、LastSeenAt。
5. CreatedAt/ModifiedAt/SyncedAt 默认 UtcNow。
6. 五个导航集合初始化为空 List。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs",
      "label": "FileItemEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileItemEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs", "type": "depends_on" }
  ]
}
```
