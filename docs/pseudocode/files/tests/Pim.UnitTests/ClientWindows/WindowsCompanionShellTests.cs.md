# tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：Companion Shell 代码保留但非主路径：不自动开 shell，托盘走状态/浏览器。
- 主要依赖：client-windows App 源文件
- 被谁使用：xUnit

## 函数级结构化伪代码

### CompanionShellCodeRemainsAvailableButIsNotPrimaryPath
- csproj 含 WebView2；host/MainShell 存在
- App.xaml.cs 无 ShowMainShellWindow；Tray 无 OpenShell(/today)，有 ShowStatusWindow 与「在浏览器打开 Web 工作台」
### RepoPath：向上找仓库文件

## 近逐行中文伪代码

1. [L1-L24] 源码契约
2. [L26-L41] RepoPath

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs",
      "label": "WindowsCompanionShellTests",
      "path": "tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/WindowsCompanionShellTests.cs", "to": "src/client-windows/Pim.Client.App", "type": "tests" }
  ]
}
```
