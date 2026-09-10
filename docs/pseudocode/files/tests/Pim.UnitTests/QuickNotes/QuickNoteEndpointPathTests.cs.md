# tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：QuickNotes 路径常量稳定；MapEndpoints 注册预期授权路由。
- 主要依赖：`QuickNoteEndpointPaths`、`QuickNotesModule`、ASP.NET Core minimal host
- 被谁使用：dotnet test

## 函数级结构化伪代码

### QuickNoteEndpointPaths_AreStable
- 步骤：Root/Note/Attachments/AttachmentDownload 字符串 equal

### MapEndpoints_RegistersExpectedAuthorizedRoutes
- 步骤：
  1. WebApplication + Authorization；MapEndpoints；Start
  2. 从 EndpointDataSource 取 RouteEndpoint 并规范化
  3. 期望 8 条路由均存在且带 IAuthorizeData

### NormalizeRoute
- 步骤：去尾 `/`（长度>1）

## 近逐行中文伪代码

1. [L12-19] 路径常量
2. [L21-55] 启动 app 断言路由与授权元数据
3. [L57-58] NormalizeRoute

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs",
      "label": "QuickNoteEndpointPathTests",
      "path": "tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs", "to": "src/Pim.Module.QuickNotes/QuickNotesModule.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/QuickNotes/QuickNoteEndpointPathTests.cs", "to": "src/Pim.Module.QuickNotes/QuickNoteEndpointPaths.cs", "type": "tests" }
  ]
}
```
