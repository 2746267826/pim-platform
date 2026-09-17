# tests/client-web/pcRecordsReviewLayout.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：静态渲染 PC 复盘摘要与上下文确认面板，断言中文文案与指标展示。
- 主要依赖：`PcReviewSummary`、`ContextConfirmationPanel`、types
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 顶层
- fixture：PcSummaryResponse 指标/分类；pending suggestion
- render PcReviewSummary：今日复盘、时长、待确认、Code.exe、输入合计等
- render ContextConfirmationPanel：待确认上下文、写入 App 知识库；禁止旧规则/纠错规则文案

## 近逐行中文伪代码

1. [L1-L49] 导入与 summary/suggestion fixture
2. [L51-L55] React SSR 挂载
3. [L57-L71] 复盘摘要断言
4. [L73-L85] 上下文面板断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/pcRecordsReviewLayout.test.tsx",
      "label": "pcRecordsReviewLayout.test",
      "path": "tests/client-web/pcRecordsReviewLayout.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/pcRecordsReviewLayout.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/pcRecordsReviewLayout.test.tsx", "to": "src/client-web/src/components/pc-tracker/PcReviewSummary.tsx", "type": "tests" },
    { "from": "tests/client-web/pcRecordsReviewLayout.test.tsx", "to": "src/client-web/src/components/pc-tracker/ContextConfirmationPanel.tsx", "type": "tests" }
  ]
}
```
