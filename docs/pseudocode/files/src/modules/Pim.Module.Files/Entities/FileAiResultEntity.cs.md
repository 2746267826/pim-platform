# src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件 AI 分析结果实体（摘要、标签、语言、敏感度、模型与证据 chunk），关联 FileItem 与 FileVersion。
- 主要依赖：
  - `FileItemEntity`、`FileVersionEntity` 导航
- 被谁使用：
  - `FileAiService` 写入/读取
  - `FileEntityConfigurations`（若配置）
  - Files 模块 DbSet

## 函数级结构化伪代码

### FileAiResultEntity
#### 属性集合（sealed POCO，无方法）
- 输入：AI 服务赋值
- 输出：EF 行状态
- 副作用：无
- 步骤：
  1. `Id` 默认 NewGuid。
  2. `FileItemId` + 导航 `FileItem`；`VersionId` + 导航 `Version`。
  3. `Summary` 默认空；`TagsJson` 默认 `"[]"`。
  4. 可选 `Language`/`Sensitivity`/`Model`；`GeneratedAt` 默认 UtcNow。
  5. 可选 `AiRequestLogId` 关联网关日志。
  6. `EvidenceChunkIdsJson` 默认 `"[]"`。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`。
2. sealed 类 `FileAiResultEntity`。
3. Id 默认新 Guid。
4. FileItemId/FileItem；VersionId/Version。
5. Summary 空串；TagsJson `"[]"`。
6. Language/Sensitivity 可空；GeneratedAt UtcNow。
7. Model 可空；AiRequestLogId 可空。
8. EvidenceChunkIdsJson `"[]"`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs",
      "label": "FileAiResultEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileAiService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileEntityConfigurations.cs", "to": "src/modules/Pim.Module.Files/Entities/FileAiResultEntity.cs", "type": "depends_on" }
  ]
}
```
