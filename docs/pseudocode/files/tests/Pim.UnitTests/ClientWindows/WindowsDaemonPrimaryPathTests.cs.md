# tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：托盘菜单以守护进程为主，不含工作台业务入口。
- 主要依赖：TrayIcon.cs 源码
- 被谁使用：xUnit

## 函数级结构化伪代码

### TrayMenu_IsDaemonFocused
- 禁止任务/日历/报告/Outlook/Data Center/审计/通知中心文案
- 含状态中心、立即同步、回填 14 天 AW、浏览器打开 Web 工作台

## 近逐行中文伪代码

1. [L1-L20] 托盘断言
2. [L22-L37] RepoPath

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs",
      "label": "WindowsDaemonPrimaryPathTests",
      "path": "tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/ClientWindows/WindowsDaemonPrimaryPathTests.cs", "to": "src/client-windows/Pim.Client.App/TrayIcon.cs", "type": "tests" }
  ]
}
```
