# tests/client-web/scheduleWorkbenchE2e.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态契约——完整脚本清单、日程相关路由注册、API 鉴权与 endpoint 能力符号。
- 主要依赖：`package.json`、`AppLayout.tsx`、`api/client.ts`、`api/endpoints.ts`、`node:fs`
- 被谁使用：Node 测试脚本；被 `test:schedule-workbench-complete` 引用

## 函数级结构化伪代码

### 模块顶层
#### 脚本清单
- 步骤：读 package.json scripts，要求 `test:schedule-workbench-complete` 包含 endpointShell / 本文件 / visualAudit

#### 路由与 API 符号
- 步骤：
  1. AppLayout 匹配 today/calendar/tasks/habits/reminders/reports/settings/sync/data-center/confirmations/audit/endpoint-shell
  2. api client 含 accessToken 与 Authorization
  3. endpoints 含 listEndpointStatuses 与 handleEndpointNotificationAction

## 近逐行中文伪代码

1. [L1-6] 读 package.json scripts
2. [L8-19] 循环断言 complete 脚本包含三测试文件
3. [L21-23] 读 AppLayout / client / endpoints 源码
4. [L25-39] 路由正则匹配
5. [L41-44] 鉴权与 endpoint API 符号

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchE2e.test.ts",
      "label": "scheduleWorkbenchE2e.test",
      "path": "tests/client-web/scheduleWorkbenchE2e.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchE2e.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchE2e.test.ts", "to": "src/client-web/package.json", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchE2e.test.ts", "to": "src/client-web/src/layout/AppLayout.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchE2e.test.ts", "to": "src/client-web/src/api/client.ts", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchE2e.test.ts", "to": "src/client-web/src/api/endpoints.ts", "type": "tests" }
  ]
}
```
