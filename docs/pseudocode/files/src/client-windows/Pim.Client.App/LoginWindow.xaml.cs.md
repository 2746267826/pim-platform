# src/client-windows/Pim.Client.App/LoginWindow.xaml.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.App
- 职责：Windows 客户端登录对话框代码后置——校验输入、调用 AuthService、设置 DialogResult、展示错误与跳过登录。
- 主要依赖：`System.Windows`、`Microsoft.Extensions.DependencyInjection`、`AuthService`、`App.Services`、XAML 控件（UsernameBox/PasswordBox/LoginButton/ErrorText）
- 被谁使用：`App.xaml.cs` 启动登录、`StatusWindow`/`TrayIcon` 重新登录

## 函数级结构化伪代码

### LoginWindow
#### LoginWindow()
- 输入：无
- 输出：窗口实例
- 副作用：InitializeComponent；从 DI 取 AuthService
- 步骤：`App.Services.GetRequiredService<AuthService>()`
- 分支与异常：DI 缺失抛异常
- 调用：`GetRequiredService`

#### async void OnLogin(sender, e)
- 输入：按钮点击
- 输出：无
- 副作用：禁用按钮、调登录 API、成功关窗 DialogResult=true、失败/异常 ShowError
- 步骤：
  1. Trim 用户名；取密码
  2. 任一空 → ShowError「请填写用户名和密码」return
  3. 按钮禁用、文案「登录中...」、隐藏错误
  4. try：LoginAsync；true → DialogResult=true Close；false → 失败提示
  5. catch：连接失败 + Message
  6. finally：恢复按钮「登录」
- 分支与异常：网络/业务异常走 catch
- 调用：`AuthService.LoginAsync`、`ShowError`

#### void OnSkip(sender, e)
- 输入：跳过点击
- 输出：无
- 副作用：DialogResult=false；Close
- 步骤：未登录继续
- 分支与异常：无
- 调用：`Close`

#### void ShowError(msg)
- 输入：错误文案
- 输出：无
- 副作用：ErrorText 可见并设 Text
- 步骤：赋值 Visibility.Visible
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 引用 WPF、DI、`Pim.Client.Core.Services`
2. partial 类 `LoginWindow : Window`；字段 `_authService`
3. 构造：InitializeComponent；DI 取 AuthService
4. OnLogin：读用户名/密码；空则错误返回
5. 禁用登录按钮与「登录中...」；折叠错误区
6. await LoginAsync：成功 DialogResult=true 关闭；失败中文提示
7. catch 展示连接失败；finally 恢复按钮
8. OnSkip：DialogResult=false 关闭
9. ShowError：设置 ErrorText 并显示

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs",
      "label": "LoginWindow",
      "path": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.App/LoginWindow.xaml.cs.md",
      "layer": "client-windows",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "to": "src/client-windows/Pim.Client.Core/Services/AuthService.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.App/App.xaml.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/StatusWindow.xaml.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.App/TrayIcon.cs", "to": "src/client-windows/Pim.Client.App/LoginWindow.xaml.cs", "type": "calls" }
  ]
}
```
