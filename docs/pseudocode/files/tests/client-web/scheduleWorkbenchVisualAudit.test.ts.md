# tests/client-web/scheduleWorkbenchVisualAudit.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：Playwright 多视口视觉审计日程工作台路由：主内容、裁切按钮、负尺寸、英文标题、横向溢出。
- 主要依赖：Playwright chromium、Vite dev server、mock API
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### main
- 分配端口、启 Vite、launch browser
- 三视口 × 多路由：注入 token、mock /api/v1、goto、assertRoute
### assertRoute
- evaluate 主文本/裁切按钮/负盒/英文 heading/横向溢出
### mockApiResponse
- status/summary、data-center、outlook settings、audit、collection-quality、today sections 等
### startVite / waitForServer / freePort / stopServer

## 近逐行中文伪代码

1. [L1-L38] 路由/视口/禁标题
2. [L40-L84] main 生命周期
3. [L86-L123] assertRoute
4. [L125-L212] 服务与 mock
5. [L214-L217] main catch exit 1

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchVisualAudit.test.ts",
      "label": "scheduleWorkbenchVisualAudit.test",
      "path": "tests/client-web/scheduleWorkbenchVisualAudit.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchVisualAudit.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchVisualAudit.test.ts", "to": "src/client-web", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchVisualAudit.test.ts", "to": "playwright", "type": "depends_on" }
  ]
}
```
