# src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Files
- 职责：文件提供方（如 Nextcloud）连接配置与同步状态实体。
- 主要依赖：`FileItemEntity` 集合导航
- 被谁使用：Files 模块服务（索引/同步/列表）、`PimDbContext`

## 函数级结构化伪代码

### FileProviderEntity
#### 属性模型（无方法）
- 输入：ORM 字段
- 输出：提供方配置行
- 副作用：无运行时逻辑
- 步骤：
  1. `Id` 默认 NewGuid；`UserId` 归属
  2. `Provider` 默认 `"nextcloud"`；`BaseUrl`/`InternalBaseUrl`
  3. `Username`、`AppPasswordSecret`（密钥引用/密文）
  4. `Status` 默认 `"pending"`；`LastSyncAt`/`LastError`
  5. `CreatedAt`/`UpdatedAt` 默认 UtcNow
  6. 导航集合 `Items` → `List<FileItemEntity>`
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 命名空间 `Pim.Module.Files.Entities`
2. 密封类 `FileProviderEntity`
3. 定义 Id/UserId/Provider/URL/账号密钥/状态/同步时间错误/时间戳
4. 初始化 `Items` 列表承载下属文件项

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs",
      "label": "FileProviderEntity",
      "path": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs.md",
      "layer": "module.files",
      "kind": "entity"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "to": "src/modules/Pim.Module.Files/Entities/FileItemEntity.cs", "type": "depends_on" },
    { "from": "src/Pim.Infrastructure/Data/PimDbContext.cs", "to": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Files/Services/FileIndexingService.cs", "to": "src/modules/Pim.Module.Files/Entities/FileProviderEntity.cs", "type": "depends_on" }
  ]
}
```
