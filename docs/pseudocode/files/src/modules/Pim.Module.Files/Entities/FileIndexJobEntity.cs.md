# src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件索引后台任务实体，跟踪某文件项/版本的索引流水线状态、阶段、重试与错误时间戳。
- 主要依赖：`FileItemEntity`、`FileVersionEntity`（导航）
- 被谁使用：Files 索引 Worker/服务、`PimDbContext` 与 Files 模块迁移

## 函数级结构化伪代码

### FileIndexJobEntity
#### 属性（无方法）
- 输入/输出：字段读写
- 副作用：无
- 步骤（字段语义）：
  1. `Id`：Guid，默认 NewGuid。
  2. `FileItemId` / `FileItem`：目标文件项。
  3. `VersionId` / `Version`：可选目标版本。
  4. `Status`：默认 `"pending"`。
  5. `Stage`：默认 `"metadata"`。
  6. `AttemptCount`：重试次数。
  7. `LastError`：最近错误信息。
  8. `StartedAt` / `FinishedAt`：可选起止时间。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`；sealed 类。
2. 默认 pending + metadata 阶段；可挂 FileItem 与 Version 导航。
3. 记录 AttemptCount、LastError、StartedAt、FinishedAt 供索引流水线观测。
4. 无注解表名（由 Fluent/DbContext 配置）。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs",
      "label": "FileIndexJobEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileVersionEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Files/Entities/FileIndexJobEntity.cs", "type": "depends_on" }
  ]
}
```
