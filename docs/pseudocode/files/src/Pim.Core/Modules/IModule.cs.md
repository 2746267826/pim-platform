# src/Pim.Core/Modules/IModule.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：定义可插拔业务模块契约：元数据、DI 注册、端点映射与异步初始化
- 主要依赖：`Microsoft.AspNetCore.Routing`、`Microsoft.Extensions.Configuration`、`Microsoft.Extensions.DependencyInjection`
- 被谁使用：各业务模块实现（`StatsModule`、`QuickNotesModule`、`PcTrackerModule`、`MobileModule`、`FilesModule`、`CalendarModule`）；宿主启动时发现/注册模块

## 函数级结构化伪代码

### IModule
#### string Name { get }
- 输入：无
- 输出：模块名称字符串
- 副作用：无
- 步骤：
  1. 返回模块稳定标识名
- 分支与异常：无
- 调用：宿主枚举/日志/诊断时读取

#### string Version { get }
- 输入：无
- 输出：模块版本字符串
- 副作用：无
- 步骤：
  1. 返回模块版本
- 分支与异常：无
- 调用：宿主展示或兼容检查时读取

#### void RegisterServices(IServiceCollection services, IConfiguration configuration)
- 输入：`services` DI 容器；`configuration` 应用配置
- 输出：无
- 副作用：向 DI 注册本模块服务（实现侧）
- 步骤：
  1. 根据配置向 `services` 添加本模块依赖
- 分支与异常：契约不规定异常；实现可因配置缺失失败
- 调用：宿主在构建服务时调用各模块

#### void MapEndpoints(IEndpointRouteBuilder endpoints)
- 输入：`endpoints` 端点路由构建器
- 输出：无
- 副作用：映射本模块 HTTP 端点（实现侧）
- 步骤：
  1. 在路由表上注册本模块 API 路由
- 分支与异常：契约不规定异常
- 调用：宿主在管道配置阶段调用

#### Task InitializeAsync(IServiceProvider serviceProvider)
- 输入：`serviceProvider` 已构建的服务提供者
- 输出：完成初始化的 `Task`
- 副作用：模块启动期初始化（如 Schema 注册、预热；实现侧）
- 步骤：
  1. 从 `serviceProvider` 解析所需服务
  2. 执行异步初始化逻辑
  3. 返回完成的任务
- 分支与异常：契约不规定异常；实现可抛出启动失败
- 调用：宿主应用启动后调用

## 近逐行中文伪代码

1. 引用 `Microsoft.AspNetCore.Routing`
2. 引用 `Microsoft.Extensions.Configuration`
3. 引用 `Microsoft.Extensions.DependencyInjection`
4. 声明命名空间 `Pim.Core.Modules`
5. 声明公共接口 `IModule`
6. 只读属性 `Name`：模块名
7. 只读属性 `Version`：模块版本
8. 方法 `RegisterServices(services, configuration)`：无返回，注册 DI 服务
9. 方法 `MapEndpoints(endpoints)`：无返回，映射 HTTP 端点
10. 方法 `InitializeAsync(serviceProvider)`：返回 `Task`，异步初始化
11. 接口结束（无默认实现）

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Modules/IModule.cs",
      "label": "IModule",
      "path": "src/Pim.Core/Modules/IModule.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Modules/IModule.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/modules/Pim.Module.Stats/StatsModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.QuickNotes/QuickNotesModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.PcTracker/PcTrackerModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Mobile/MobileModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Files/FilesModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" },
    { "from": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "implements" }
  ]
}
```
