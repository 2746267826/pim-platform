# src/client-windows/Pim.Client.App/Services/NavigationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 客户端壳层视图导航：维护当前视图名，变更时触发 `Navigated` 事件。
- 主要依赖：`INavigationService`
- 被谁使用：主壳窗口/菜单等注入 `INavigationService` 后调用 `NavigateTo`

## 函数级结构化伪代码

### NavigationService
#### event Action<string>? Navigated
- 输入：订阅方
- 输出：视图名字符串事件
- 副作用：订阅者响应导航
- 步骤：字段式事件声明
- 分支与异常：无
- 调用：无

#### string CurrentView { get; private set; }
- 输入：无（初始 `"calendar"`）
- 输出：当前视图名
- 副作用：仅由 `NavigateTo` 写入
- 步骤：属性读写；私有 set
- 分支与异常：无
- 调用：无

#### void NavigateTo(string viewName)
- 输入：目标视图名
- 输出：无
- 副作用：可能更新 `CurrentView` 并触发 `Navigated`
- 步骤：
  1. 若 `CurrentView == viewName` → 直接 return（幂等）
  2. 赋值 `CurrentView = viewName`
  3. `Navigated?.Invoke(viewName)`
- 分支与异常：同名短路
- 调用：事件订阅者

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.App.Services`
2. 类 `NavigationService` 实现 `INavigationService`
3. 声明 `Navigated` 事件（`Action<string>?`）
4. `CurrentView` 默认 `"calendar"`，私有 set
5. `NavigateTo`：目标与当前相同则返回
6. 否则更新 `CurrentView` 并 Invoke `Navigated`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/Services/NavigationService.cs",
      "label": "NavigationService",
      "path": "src/client-windows/Pim.Client.App/Services/NavigationService.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/Services/NavigationService.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/Services/NavigationService.cs", "to": "src/client-windows/Pim.Client.App/Services/INavigationService.cs", "type": "implements" },
    { "from": "src/client-windows/Pim.Client.App/MainShellWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/Services/NavigationService.cs", "type": "calls" }
  ]
}
```
