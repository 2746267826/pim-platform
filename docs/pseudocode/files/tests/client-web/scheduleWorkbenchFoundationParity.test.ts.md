# tests/client-web/scheduleWorkbenchFoundationParity.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态检查工作台基础路由与类型符号在布局/侧栏/设置/types 中的一致性。
- 主要依赖：AppLayout、Sidebar、SettingsPage、types/index
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：
  1. AppLayout 含 workbench/settings/sync/data-center/confirmations/reminders/reports/habits
  2. Sidebar 含主要工作台路由
  3. SettingsPage 含 /settings/sync
  4. types 声明 OperationConfirmation 等 interface/type

## 近逐行中文伪代码

1. [L1-7] 读四份源码
2. [L9-11] AppLayout 路由
3. [L13-15] Sidebar 路由
4. [L17] settings sync
5. [L19-20] 类型符号

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchFoundationParity.test.ts",
      "label": "scheduleWorkbenchFoundationParity.test",
      "path": "tests/client-web/scheduleWorkbenchFoundationParity.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchFoundationParity.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchFoundationParity.test.ts", "to": "src/client-web/src/layout/AppLayout.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchFoundationParity.test.ts", "to": "src/client-web/src/layout/Sidebar.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchFoundationParity.test.ts", "to": "src/client-web/src/pages/SettingsPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchFoundationParity.test.ts", "to": "src/client-web/src/types/index.ts", "type": "tests" }
  ]
}
```
