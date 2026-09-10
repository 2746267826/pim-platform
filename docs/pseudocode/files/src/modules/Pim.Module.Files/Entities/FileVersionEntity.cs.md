# src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件版本行实体（外部版本 Id、ETag、大小、是否当前、同步时间），关联 `FileItemEntity`。
- 主要依赖：BCL；导航 `FileItemEntity`（同模块）
- 被谁使用：文件同步/索引/操作服务与 EF 模型；多处 Files 单元测试

## 函数级结构化伪代码

### FileVersionEntity
#### 属性集（无行为方法）
- 输入：各属性赋值
- 输出：行状态
- 副作用：无（纯 POCO）
- 步骤：
  1. `Id`：默认 `NewGuid`
  2. `FileItemId` + 导航 `FileItem`
  3. `ExternalVersionId`：外部版本标识，默认空串
  4. `Etag`：可选
  5. `Size`：可选长整型
  6. `ModifiedAt`：默认 UTC 现在
  7. `Source`：默认 `"history"`
  8. `IsCurrent`：是否当前版本
  9. `SyncedAt`：默认 UTC 现在
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`
2. 密封类 `FileVersionEntity`
3. Id、FileItemId、导航 FileItem
4. ExternalVersionId、Etag、Size、ModifiedAt
5. Source 默认 history；IsCurrent；SyncedAt

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs",
      "label": "FileVersionEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" }
  ]
}
```
