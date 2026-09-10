# tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Mobile 模块路由映射在 /api/v1/mobile 且需授权；DI 注册定位分析服务。
- 主要依赖：`MobileModule`、WebApplication
- 被谁使用：xUnit

## 函数级结构化伪代码

1. MapEndpoints 后收集 RouteEndpoint，断言设备/同步/usage/location/analytics/apps/goals 路径集合，全部含 IAuthorizeData
2. RegisterServices 含 MobileLocationQueryService 与 AggregationService Scoped

## 近逐行中文伪代码

1. [L1-L58] 端点与授权
2. [L60-L73] DI

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs",
      "label": "MobileEndpointTests",
      "path": "tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Mobile/MobileEndpointTests.cs", "to": "src/modules/Pim.Module.Mobile/MobileModule.cs", "type": "tests" }
  ]
}
```
