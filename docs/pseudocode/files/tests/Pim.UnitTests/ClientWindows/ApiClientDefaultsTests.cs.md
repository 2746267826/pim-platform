# tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Windows 客户端默认 API URL、AW 游标提交语义、JSON 字段名、AW/KeyStats 上传健康消息。
- 主要依赖：`ApiClient`、`AwCollectorService`、`KeyStatsCollectorService`、反射
- 被谁使用：dotnet test

## 函数级结构化伪代码

### ApiClient 默认与 localhost→127.0.0.1
### AwCollectorCursorState 上传成功后才 Commit
### KeyStats/Aw JSON 属性名
### AW backlog limit=-1；batch 500 分块
### BuildUploadHealthMessage Theory（KeyStats/AW）

## 近逐行中文伪代码

1. [L11-28] 默认 URL
2. [L30-45] 游标
3. [L47-92] JSON 与 backlog
4. [L94-133] 健康消息
5. [L135-143] AssertJsonProperty

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs",
      "label": "ApiClientDefaultsTests",
      "path": "tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/ClientWindows/ApiClientDefaultsTests.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "type": "tests" }
  ]
}
```
