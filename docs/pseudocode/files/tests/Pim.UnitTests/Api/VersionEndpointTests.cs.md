# tests/Pim.UnitTests/Api/VersionEndpointTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：版本能力仅 mobileItemResultsV1；/api/version JSON 契约。
- 主要依赖：VersionEndpoints
- 被谁使用：dotnet test

## 函数级结构化伪代码

### PhaseOneCapabilitiesAdvertiseItemResultsOnly
### MapVersionEndpoints_ReturnsTypedJsonContract

## 近逐行中文伪代码

1. Capabilities 含 MobileItemResultsV1 不含 androidEmbedV1
2. 启动 app GET /api/version

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Api/VersionEndpointTests.cs",
      "label": "VersionEndpointTests.cs",
      "path": "tests/Pim.UnitTests/Api/VersionEndpointTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Api/VersionEndpointTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [{"from":"tests/Pim.UnitTests/Api/VersionEndpointTests.cs","to":"src/Pim.Api/Endpoints/VersionEndpoints.cs","type":"tests"}]
}
```