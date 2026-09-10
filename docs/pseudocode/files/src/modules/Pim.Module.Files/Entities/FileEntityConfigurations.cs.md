# src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：Files 模块全部实体的 EF Core `IEntityTypeConfiguration`：表名、列映射、默认值、关系、索引与检查约束。
- 主要依赖：`Microsoft.EntityFrameworkCore`、Files 各 Entity 类型
- 被谁使用：`PimDbContext` 模型配置/模块注册时 ApplyConfiguration

## 函数级结构化伪代码

### FileProviderEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileProviderEntity\> builder)
- 输入：实体构建器
- 输出：无
- 副作用：配置 `file_providers`
- 步骤：列映射；Provider 默认 nextcloud；Status 默认 pending；时间默认 now()；唯一索引 (UserId,Provider,BaseUrl,Username)；索引 (UserId,Status)
- 分支与异常：无
- 调用：无

### FileItemEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileItemEntity\> builder)
- 输入：构建器
- 输出：无
- 副作用：配置 `file_items`
- 步骤：
  1. 列映射；ItemType 默认 file
  2. HasOne Provider Cascade
  3. 可选当前版本：FK (Id, CurrentVersionId) → FileVersion (FileItemId, Id) Restrict
  4. 唯一 (ProviderId, ExternalFileId)；索引 Path/Parent/IsDeleted
- 分支与异常：无
- 调用：无

### FileVersionEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileVersionEntity\> builder)
- 输入：构建器
- 输出：无
- 副作用：配置 `file_versions`
- 步骤：Source 默认 history；AlternateKey (FileItemId, Id)；与 FileItem Cascade；唯一 (FileItemId, ExternalVersionId)；过滤唯一索引 is_current=true
- 分支与异常：无
- 调用：无

### FileIndexJobEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileIndexJobEntity\> builder)
- 输入：构建器
- 输出：无
- 副作用：配置 `file_index_jobs`
- 步骤：Status 默认 pending；Stage 默认 metadata；FileItem Cascade；Version 复合 FK Restrict；索引 (FileItemId,Status)/(Status,Stage)
- 分支与异常：无
- 调用：无

### FileChunkEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileChunkEntity\> builder)
- 输入：构建器
- 输出：无
- 副作用：配置 `file_chunks`
- 步骤：Text text；与 FileItem/Version Cascade；唯一 (FileItemId,VersionId,ChunkIndex)；QdrantPointId 非空唯一
- 分支与异常：无
- 调用：无

### FileAiResultEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileAiResultEntity\> builder)
- 输入：构建器
- 输出：无
- 副作用：配置 `file_ai_results`
- 步骤：Tags/Evidence jsonb 默认 []；GeneratedAt now()；FileItem/Version Cascade；唯一 (FileItemId,VersionId)；索引 AiRequestLogId
- 分支与异常：无
- 调用：无

### FileSuggestionEntityConfiguration
#### void Configure(EntityTypeBuilder\<FileSuggestionEntity\> builder)
- 输入：构建器
- 输出：无
- 副作用：配置 `file_suggestions`
- 步骤：CheckConstraint confidence 0..1；Status 默认 pending；Payload jsonb 默认 {}；FileItem Cascade；索引 (FileItemId,Status)/SuggestionType/AiRequestLogId
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`，多个 sealed Configuration 类
2. FileProvider：表 file_providers，凭证与同步状态，用户+提供商唯一
3. FileItem：表 file_items，树形外部 id，当前版本可选 FK
4. FileVersion：表 file_versions，当前版本过滤唯一
5. FileIndexJob：索引任务阶段与状态
6. FileChunk：文本块与 Qdrant 点 id
7. FileAiResult：摘要标签与证据块
8. FileSuggestion：置信度检查约束与建议状态

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs",
      "label": "FileEntityConfigurations",
      "path": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileChunkEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "type": "depends_on" }
  ]
}
```
