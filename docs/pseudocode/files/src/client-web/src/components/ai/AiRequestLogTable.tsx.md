# src/client-web/src/components/ai/AiRequestLogTable.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示 AI 请求日志分页表：加载/错误/空态；行点击选择；状态徽章与时间/Token/耗时格式化。
- 主要依赖：`AiRequestLogListItem`/`AiRequestStatus`/`PagedResult` 类型、`StatusBadge` UI
- 被谁使用：AI 运维/日志页面父组件

## 函数级结构化伪代码

### 模块级
#### statusTone(status)
- 输入：`AiRequestStatus`
- 输出：徽章 tone：`activity`/`warning`/`danger`
- 副作用：无
- 步骤：Succeeded→activity；FailedValidation|Blocked→warning；其余 danger
- 分支与异常：无
- 调用：无

#### formatDateTime(value?)
- 输入：可选 ISO 时间字符串
- 输出：`zh-CN` 本地化时间或 `-`/原串
- 副作用：无
- 步骤：空→`-`；非法 Date→原值；否则 `toLocaleString('zh-CN')`
- 分支与异常：NaN 时间回退原字符串
- 调用：`Date`

#### formatNumber(value?)
- 输入：可选数字
- 输出：本地化数字或 `-`
- 副作用：无
- 步骤：nullish → `-`；否则 `toLocaleString('zh-CN')`
- 分支与异常：无
- 调用：无

### AiRequestLogTable（默认导出组件）
#### render(Props)
- 输入：`data` 分页、`selectedId`、`isLoading`、`error`、`onSelect`
- 输出：React 节点（面板+表）
- 副作用：点击行调用 `onSelect(id)`
- 步骤：
  1. items = data?.items ?? []
  2. 标题「请求日志」与总数徽章
  3. loading / error / 空列表 三态文案
  4. 有数据：表格列 时间、模块/用途、模型、状态、Token、耗时、错误摘要
  5. 行：选中高亮；StatusBadge+中文 statusLabels；duration 带 ms
- 分支与异常：互斥展示 loading/error/empty/table
- 调用：`statusTone`、`formatDateTime`、`formatNumber`、`StatusBadge`、`onSelect`

## 近逐行中文伪代码

1. 引入类型与 StatusBadge
2. Props：data、selectedId、isLoading、error、onSelect
3. statusLabels 中文映射五种状态
4. statusTone / formatDateTime / formatNumber 工具
5. 组件：取 items
6. 面板头：标题、副标题、总条数
7. 加载中/错误/空态提示
8. 有数据渲染可横滚表格
9. map 每行：点击选中；展示时间、module/purpose、model、徽章、tokens、durationMs、errorSummary

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/ai/AiRequestLogTable.tsx",
      "label": "AiRequestLogTable",
      "path": "src/client-web/src/components/ai/AiRequestLogTable.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/ai/AiRequestLogTable.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/ai/AiRequestLogTable.tsx", "to": "src/client-web/src/ui/StatusBadge", "type": "depends_on" },
    { "from": "src/client-web/src/components/ai/AiRequestLogTable.tsx", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/components/ai/AiRequestLogTable.tsx", "to": "src/client-web/src/api/ai.ts", "type": "depends_on" }
  ]
}
```
