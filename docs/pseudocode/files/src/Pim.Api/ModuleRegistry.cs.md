# src/Pim.Api/ModuleRegistry.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Api
- 职责：扫描 `BaseDirectory` 下 `Pim.Module.*.dll`，加载并去重 `IModule` 实现，调用 `RegisterServices` / `MapEndpoints` / `InitializeAsync`。
- 主要依赖：
  - `Pim.Core.Modules.IModule`
  - `System.Reflection`（`Assembly.LoadFrom` / `GetTypes`）
  - `Serilog.Log`
  - ASP.NET Core `IServiceCollection` / `IConfiguration` / `IEndpointRouteBuilder`
- 被谁使用：
  - `Program.cs` 启动时发现模块、映射端点与初始化

## 函数级结构化伪代码

### ModuleRegistry
#### 字段与属性
- 输入：无
- 输出：`Modules` 只读列表
- 副作用：内部维护 `_modules`、`_loadedTypeNames`、`_loadedModuleNames`
- 步骤：
  1. `_modules`：已成功实例化的模块列表。
  2. `_loadedTypeNames`：按类型 FullName 去重（同一 DLL 可能被加载两次）。
  3. `_loadedModuleNames`：按 `module.Name` 去重（不同版本同名模块）。
  4. `Modules` 暴露 `_modules` 只读视图。
- 分支与异常：无
- 调用：无

#### `void DiscoverModules(IServiceCollection services, IConfiguration configuration)`
- 输入：DI 集合、配置
- 输出：void；副作用填充 `_modules` 并注册各模块服务
- 副作用：加载程序集、创建实例、调用 `RegisterServices`；失败仅 Warning 日志并跳过
- 步骤：
  1. `baseDir = AppDomain.CurrentDomain.BaseDirectory`。
  2. 枚举 `Directory.GetFiles(baseDir, "Pim.Module.*.dll")`。
  3. 对每个 `assemblyPath`：
     a. `Assembly.LoadFrom`；失败 → Log.Warning 后 continue。
     b. `assembly.GetTypes()` 过滤：可赋给 `IModule`、非接口、非抽象；`ReflectionTypeLoadException` → Warning continue。
     c. 对每个 `type`：`_loadedTypeNames.Add(FullName)` 失败则 skip。
     d. `Activator.CreateInstance(type)` 为 `IModule`；失败 → Warning continue。
     e. `_loadedModuleNames.Add(module.Name)` 失败 → Warning 重复名 skip。
     f. `_modules.Add(module)`。
     g. `module.RegisterServices(services, configuration)`；异常 → Warning 不移除已加入列表。
- 分支与异常：加载/反射/实例化/注册失败均吞掉并记日志
- 调用：`IModule.RegisterServices`、`Assembly.LoadFrom`、`Activator.CreateInstance`

#### `void MapAllEndpoints(IEndpointRouteBuilder endpoints)`
- 输入：路由构建器
- 输出：void
- 副作用：各模块挂载 HTTP 端点
- 步骤：1. 遍历 `_modules`，逐个 `module.MapEndpoints(endpoints)`。
- 分支与异常：模块内异常向上抛出（本方法不捕获）
- 调用：`IModule.MapEndpoints`

#### `async Task InitializeAllAsync(IServiceProvider serviceProvider)`
- 输入：根/作用域服务提供器
- 输出：Task
- 副作用：各模块启动初始化（如 EnsureCollection 等）
- 步骤：1. 按发现顺序 `await module.InitializeAsync(serviceProvider)`。
- 分支与异常：模块内异常向上抛出
- 调用：`IModule.InitializeAsync`

## 近逐行中文伪代码

1. 引入 Reflection、`Pim.Core.Modules`、Serilog。
2. 命名空间 `Pim.Api`；类 `ModuleRegistry`。
3. 私有列表 `_modules`、类型名集合 `_loadedTypeNames`、模块名集合 `_loadedModuleNames`。
4. 公开 `Modules` 返回 `_modules` 只读。
5. `DiscoverModules`：取 BaseDirectory，匹配 `Pim.Module.*.dll`。
6. 循环每个 DLL：`LoadFrom`，异常 Warning 并 continue。
7. `GetTypes` 筛 `IModule` 具体类；`ReflectionTypeLoadException` Warning continue。
8. 对类型：FullName 已在集合则跳过。
9. `CreateInstance` 为 `IModule`；失败 Warning continue。
10. 模块 `Name` 重复则 Warning 跳过。
11. 加入 `_modules`，调用 `RegisterServices`；失败 Warning。
12. `MapAllEndpoints`：foreach 调用 `MapEndpoints`。
13. `InitializeAllAsync`：foreach await `InitializeAsync`。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Api/ModuleRegistry.cs",
      "label": "ModuleRegistry",
      "path": "src/Pim.Api/ModuleRegistry.cs",
      "doc": "docs/pseudocode/files/src/Pim.Api/ModuleRegistry.cs.md",
      "layer": "api",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/Pim.Api/ModuleRegistry.cs", "to": "src/Pim.Core/Modules/IModule.cs", "type": "depends_on" },
    { "from": "src/Pim.Api/Program.cs", "to": "src/Pim.Api/ModuleRegistry.cs", "type": "calls" },
    { "from": "src/Pim.Api/ModuleRegistry.cs", "to": "src/modules/Pim.Module.Calendar/CalendarModule.cs", "type": "calls" },
    { "from": "src/Pim.Api/ModuleRegistry.cs", "to": "src/modules/Pim.Module.Files/FilesModule.cs", "type": "calls" }
  ]
}
```
