# src/client-web/src/ui/OperationResultBanner.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：展示日历操作结果或 ICS 导入报告横幅。
- 主要依赖：CalendarOperationResult、ImportReport
- 被谁使用：CalendarDataManager 等

## 函数级结构化伪代码

### isCalendarOperationResult
- 有 message 与 affectedCount 则操作结果

### OperationResultBanner
- result null → null
- 操作结果：teal 文案 + 影响数 + 关闭
- 导入：blue 导入/跳过数 + skippedReasons 列表 + 关闭

## 近逐行中文伪代码

1. 判别结果类型。
2. 两类样式横幅均可 dismiss。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/OperationResultBanner.tsx",
      "label": "OperationResultBanner",
      "path": "src/client-web/src/ui/OperationResultBanner.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/OperationResultBanner.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/ui/OperationResultBanner.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
