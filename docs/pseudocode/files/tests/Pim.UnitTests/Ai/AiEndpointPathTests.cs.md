# tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：AI 状态解析与管理员授权路由注册。
- 主要依赖：AiEndpoints / IAi* fakes
- 被谁使用：dotnet test

## 函数级结构化伪代码

### TryParseStatus_AcceptsPublicStatusValues / RejectsInvalid / RejectsNumericAndCombined
### MapAiEndpoints_RegistersExpectedAdminAuthorizedRoutes

## 近逐行中文伪代码

1. 状态别名解析
2. 非法/数字拒绝
3. 六条 /api/v1/ai/* 需 admin

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs",
      "label": "AiEndpointPathTests.cs",
      "path": "tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Ai/AiEndpointPathTests.cs","to":"src/Pim.Api/Endpoints/AiEndpoints.cs","type":"tests"}]
}
```