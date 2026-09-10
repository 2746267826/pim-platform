# tests/Pim.UnitTests/Files/FileEndpointPathTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Files 路径常量稳定；MapEndpoints 授权路由与命名 handler；绑定输入执行操作 handler。
- 主要依赖：FilesModule 端点 / 假适配器
- 被谁使用：dotnet test

## 函数级结构化伪代码

### FileEndpointPaths_AreStable
### MapEndpoints_RegistersAuthorizedRoutes
### MapEndpoints_FileOperationRoutesUseNamedHandlers
### MapEndpoints_ExecutesOperationHandlersWithBoundInputs

## 近逐行中文伪代码

1. 路径常量
2. 授权路由注册
3. 命名 handler
4. 集成调用 upload/move 等绑定

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Files/FileEndpointPathTests.cs",
      "label": "FileEndpointPathTests.cs",
      "path": "tests/Pim.UnitTests/Files/FileEndpointPathTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Files/FileEndpointPathTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Files/FileEndpointPathTests.cs","to":"src/Pim.Module.Files","type":"tests"}]
}
```