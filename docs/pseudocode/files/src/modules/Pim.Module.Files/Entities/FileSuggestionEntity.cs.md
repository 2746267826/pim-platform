# src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件 AI/规则建议实体：关联文件项、建议类型/标题/原因/置信度、PayloadJson、状态与可选 AiRequestLogId。
- 主要依赖：`FileItemEntity` 导航
- 被谁使用：`FileOperationService` 列表/接受/驳回；索引或 AI 流水写入

## 函数级结构化伪代码

### FileSuggestionEntity
#### 属性集合
- 输入：属性赋值
- 输出：实体状态
- 副作用：无
- 步骤：
  1. `Id` 默认 NewGuid；`FileItemId` + 可选 `FileItem`
  2. `SuggestionType`/`Title`/`Reason` 默认空串
  3. `Confidence` decimal；`PayloadJson` 默认 `"{}"`
  4. `Status` 默认 `"pending"`；`AiRequestLogId` 可空
  5. `CreatedAt`/`UpdatedAt` 默认 UtcNow
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`
2. sealed 类 `FileSuggestionEntity`
3. Id、FileItemId、FileItem 导航
4. SuggestionType/Title/Reason/Confidence/PayloadJson
5. Status=pending；AiRequestLogId；Created/Updated 时间

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs",
      "label": "FileSuggestionEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Files/Entities/FileSuggestionEntity.cs", "type": "depends_on" }
  ]
}
```
