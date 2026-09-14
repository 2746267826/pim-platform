# tests/Pim.UnitTests/Files/FileOperationServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：全面覆盖 `FileOperationService` 同步、列表、移动重命名删除上传下载、回收站/版本/打开链接/建议接受。
- 主要依赖：`FileOperationService`、`FileProviderBindingService`、Fake 适配器
- 被谁使用：xUnit

## 函数级结构化伪代码

### 同步
- 按 externalFileId upsert 不改 Id；缺失标删；浅层不同步删嵌套；etag 新版本不改旧 Id
### 列表
- 根仅非删直接子；映射最新 IndexStatus
### 变更
- Move/Rename 调适配器+审计；文件夹移动更新子孙路径；Delete 入回收站与子孙
### 上传下载
- Upload upsert 当前版本；无文件名抛；文件夹 Download 拒；文件按 path
### 回收站/版本
- ListTrash 聚合；RestoreTrash 审计；历史版本无索引任务；Download/RestoreVersion；Preview 需确认
### 其它
- BuildOpenLink 传 externalFileId；AcceptSuggestion 仅改状态
### FakeFileProviderAdapter 全接口计数

## 近逐行中文伪代码

1. 20+ Fact 按上列分组 Arrange-Act-Assert
2. 末尾 Fake 适配器与 Seed helpers

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileOperationServiceTests.cs",
      "label": "FileOperationServiceTests",
      "path": "tests/Pim.UnitTests/Files/FileOperationServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileOperationServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Files/FileOperationServiceTests.cs", "to": "src/modules/Pim.Module.Files/Services/FileOperationService.cs", "type": "tests" }
  ]
}
```
