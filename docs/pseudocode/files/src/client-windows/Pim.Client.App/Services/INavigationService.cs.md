# src/client-windows/Pim.Client.App/Services/INavigationService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 客户端视图导航契约——当前视图、跳转、导航完成事件。
- 主要依赖：无（仅 BCL `Action`）
- 被谁使用：`NavigationService` 实现、主窗口/状态窗/ViewModel 注入

## 函数级结构化伪代码

### INavigationService
#### 成员契约
- 输入/输出：接口表面，无实现体
- 副作用：实现方负责切换 UI 视图
- 步骤（契约语义）：
  1. `event Action<string>? Navigated`：导航完成后以视图名通知订阅者。
  2. `string CurrentView { get; }`：当前视图标识。
  3. `void NavigateTo(string viewName)`：请求切换到指定视图。
- 分支与异常：由实现决定
- 调用：被实现类与 UI 层使用

## 近逐行中文伪代码

1. 命名空间 `Pim.Client.App.Services`。
2. 声明接口 `INavigationService`。
3. 可选事件 `Navigated`，参数为视图名字符串。
4. 只读属性 `CurrentView`。
5. 方法 `NavigateTo(viewName)` 触发导航。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/Services/INavigationService.cs",
      "label": "INavigationService",
      "path": "src/client-windows/Pim.Client.App/Services/INavigationService.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/Services/INavigationService.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/Services/NavigationService.cs", "to": "src/client-windows/Pim.Client.App/Services/INavigationService.cs", "type": "implements" }
  ]
}
```
