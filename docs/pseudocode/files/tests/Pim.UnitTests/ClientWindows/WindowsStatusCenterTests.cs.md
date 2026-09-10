# tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：状态窗 XAML/代码四分区与 KeyStats 动作；StatusCenterEvaluator.Rate 评级。
- 主要依赖：`StatusWindow.xaml(.cs)`、`StatusCenterEvaluator`
- 被谁使用：dotnet test

## 函数级结构化伪代码

### StatusWindow_DeclaresFourSectionsAndKeyStatsActions
- 步骤：源码含 概览/数据源/上传/设置；重启 KeyStats/复制诊断/浏览器打开；KeyStatsProcessManager

### RateOverall_MatchesExpected
- 步骤：Theory 组合 authenticated+AW+KS → 正常/部分异常/不可用

### RepoPath
- 步骤：自 BaseDirectory 向上找仓库内文件

## 近逐行中文伪代码

1. [L8-22] 读 XAML/CS 字符串断言
2. [L24-38] Rate Theory
3. [L40-50] RepoPath

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs",
      "label": "WindowsStatusCenterTests",
      "path": "tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs", "to": "src/client-windows/Pim.Client.Core/Services/StatusCenterEvaluator.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/ClientWindows/WindowsStatusCenterTests.cs", "to": "src/client-windows/Pim.Client.App/StatusWindow.xaml", "type": "tests" }
  ]
}
```
