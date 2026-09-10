# tests/client-web/scheduleWorkbenchChineseNavigation.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态检查工作台/侧栏导航中文-only，同步入口落在 settings。
- 主要依赖：AppLayout、Sidebar、SettingsPage、WorkbenchPage 源文本
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

1. WorkbenchPage 禁止大量英文标题/按钮文案
2. Sidebar 禁止英文 label/short
3. 无顶层 `/sync`；AppLayout 有 `/settings/sync`；Settings 链接 sync

## 近逐行中文伪代码

1. [L1-L8] 读四文件
2. [L9-L33] workbench 禁英文
3. [L35-L55] sidebar 禁英文 label
4. [L57-L60] sync 路由位置

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchChineseNavigation.test.ts",
      "label": "scheduleWorkbenchChineseNavigation.test",
      "path": "tests/client-web/scheduleWorkbenchChineseNavigation.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchChineseNavigation.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchChineseNavigation.test.ts", "to": "src/client-web/src/layout/AppLayout.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchChineseNavigation.test.ts", "to": "src/client-web/src/layout/Sidebar.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchChineseNavigation.test.ts", "to": "src/client-web/src/pages/WorkbenchPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchChineseNavigation.test.ts", "to": "src/client-web/src/pages/SettingsPage.tsx", "type": "tests" }
  ]
}
```
