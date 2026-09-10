# src/modules/Pim.Module.Mobile/Testing/MobileEndpointTestCompat.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Module.Mobile（测试兼容层，命名空间 `Pim.UnitTests.Mobile`）
- 职责：为 Mobile 端点单测提供与 ASP.NET Core 同名类型的轻量包装，便于测试代码以 `WebApplication.CreateBuilder` 风格挂接路由与 DI。
- 主要依赖：`Microsoft.AspNetCore.Builder`、`Microsoft.AspNetCore.Routing`、`Microsoft.Extensions.DependencyInjection`、`Microsoft.Extensions.FileProviders`
- 被谁使用：Mobile 相关单元测试

## 函数级结构化伪代码

### ServiceCollection
#### 类型定义
- 输入：无
- 输出：继承 `Microsoft.Extensions.DependencyInjection.ServiceCollection` 的本地别名类型
- 副作用：无
- 步骤：
  1. 在测试命名空间下声明同名 `ServiceCollection`，直接继承框架实现。
- 分支与异常：无
- 调用：无

### ConfigurationBuilder
#### 类型定义
- 输入：无
- 输出：继承框架 `ConfigurationBuilder` 的本地别名
- 副作用：无
- 步骤：
  1. 便于测试代码无需完整 using 路径即可构造配置构建器。
- 分支与异常：无
- 调用：无

### WebApplication
#### CreateBuilder() [static]
- 输入：无
- 输出：`MobileWebApplicationBuilder`
- 副作用：创建真实 `WebApplicationBuilder`
- 步骤：
  1. 调用 `Microsoft.AspNetCore.Builder.WebApplication.CreateBuilder()`。
  2. 包装为 `MobileWebApplicationBuilder` 返回。
- 分支与异常：无
- 调用：框架 CreateBuilder

#### IEndpointRouteBuilder 成员
- 输入：无（属性/方法转发）
- 输出：内层 `WebApplication` 的 DataSources / ServiceProvider / CreateApplicationBuilder
- 副作用：无（转发）
- 步骤：
  1. 私有构造保存 `_inner`。
  2. 接口成员全部委托 `_inner`。
- 分支与异常：无
- 调用：内层 WebApplication

### WebApplication.MobileWebApplicationBuilder
#### 构造 / Services / Build
- 输入：框架 `WebApplicationBuilder`
- 输出：`Services` 暴露 DI；`Build()` 得到包装后的 `WebApplication`
- 副作用：Build 触发内层 Build
- 步骤：
  1. 保存 `_inner`；`Services` 返回 `_inner.Services`。
  2. `Build()`：`new WebApplication(_inner.Build())`。
- 分支与异常：无
- 调用：`WebApplicationBuilder.Build`

### MobileEndpointTestServiceCollectionExtensions
#### AddRouting(IServiceCollection)
- 输入：服务集合
- 输出：注册 Routing 后的服务集合
- 副作用：调用框架 `RoutingServiceCollectionExtensions.AddRouting`
- 步骤：
  1. 转发到框架 AddRouting。
- 分支与异常：无
- 调用：框架扩展

#### AddAuthorization(IServiceCollection)
- 输入：服务集合
- 输出：原服务集合（空操作）
- 副作用：无
- 步骤：
  1. 测试环境不真正加授权策略，直接返回 services。
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引入 AspNetCore Builder/Routing、DI、FileProviders。
2. 命名空间 `Pim.UnitTests.Mobile`（虽文件在 Module.Mobile/Testing）。
3. `ServiceCollection`/`ConfigurationBuilder` 为框架类型别名子类。
4. `WebApplication` 实现 `IEndpointRouteBuilder`，包装内层真实应用。
5. `CreateBuilder` 创建框架 builder 并包成 `MobileWebApplicationBuilder`。
6. 路由相关成员全部转发 `_inner`。
7. Builder 暴露 Services；Build 后包成测试用 WebApplication。
8. 扩展方法：AddRouting 真注册；AddAuthorization 空实现便于测试编译/运行。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/modules/Pim.Module.Mobile/Testing/MobileEndpointTestCompat.cs",
      "label": "MobileEndpointTestCompat",
      "path": "src/modules/Pim.Module.Mobile/Testing/MobileEndpointTestCompat.cs",
      "doc": "docs/pseudocode/files/src/modules/Pim.Module.Mobile/Testing/MobileEndpointTestCompat.cs.md",
      "layer": "module.mobile",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Mobile/Testing/MobileEndpointTestCompat.cs", "to": "Microsoft.AspNetCore.Builder.WebApplication", "type": "depends_on" },
    { "from": "src/modules/Pim.Module.Mobile/Testing/MobileEndpointTestCompat.cs", "to": "Microsoft.Extensions.DependencyInjection", "type": "depends_on" }
  ]
}
```
