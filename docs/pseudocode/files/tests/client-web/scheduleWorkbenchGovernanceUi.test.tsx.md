# tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态检查治理相关页面源码含关键 UI/能力符号。
- 主要依赖：Sync/Confirmations/DataCenter/Reminders/Reports/AuditTimeline 页面源
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### assertPageSourceContains
- 步骤：读文件；每个 snippet includes

### 页面清单
- Sync：设备代码/tokenHealth/poll/冲突/delta/writeback
- Confirmations：Diff/Strict/二级确认/allowedActions
- DataCenter：批预览/审计导出/版本恢复/Outlook-only
- Reminders：提醒中心/DND/历史/按钮
- Reports：日周月项目/后续确认
- AuditTimeline：恢复预览/导出审计

## 近逐行中文伪代码

1. [L4-10] 辅助函数
2. [L12-52] 六页断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx",
      "label": "scheduleWorkbenchGovernanceUi.test",
      "path": "tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx", "to": "src/client-web/src/pages/SyncPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx", "to": "src/client-web/src/pages/ConfirmationsPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchGovernanceUi.test.tsx", "to": "src/client-web/src/pages/DataCenterPage.tsx", "type": "tests" }
  ]
}
```
