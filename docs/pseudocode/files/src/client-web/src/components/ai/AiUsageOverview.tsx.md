# src/client-web/src/components/ai/AiUsageOverview.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示 AI 用量概览：请求数/Token/成本/失败率四卡 + Top 模块/模型分组列表。
- 主要依赖：`AiUsageSummary`/`AiUsageGroup` 类型、`MetricCard`
- 被谁使用：AI 管理/用量页面（传入 summary/loading/error）

## 函数级结构化伪代码

### formatNumber(value?)
- 输入：可空数字
- 输出：zh-CN 本地化字符串，默认 0
- 副作用：无
- 步骤：`(value ?? 0).toLocaleString('zh-CN')`
- 分支与异常：无
- 调用：无

### formatCost(value?)
- 输入：可空数字
- 输出：`$` + 四位小数
- 副作用：无
- 步骤：toFixed(4)
- 分支与异常：无
- 调用：无

### failureRate(summary?)
- 输入：汇总
- 输出：百分比字符串
- 副作用：无
- 步骤：无 summary 或 requestCount=0 → `0.0%`；否则 failure/request * 100 一位小数
- 分支与异常：无
- 调用：无

### CompactGroupRows({ title, groups })
- 输入：标题与分组数组
- 输出：最多 5 行的分组 UI
- 副作用：无
- 步骤：slice(0,5)；空则虚线「暂无数据」；否则每行显示 groupKey、次数、成功/失败/Token/成本
- 分支与异常：空 key 显示「未命名」
- 调用：formatNumber/formatCost

### AiUsageOverview({ summary, isLoading, error })
- 输入：汇总、加载、错误
- 输出：面板 section
- 副作用：无（纯展示）
- 步骤：
  1. 标题「用量概览」+ 副标题；loading 显示「正在刷新...」
  2. error 红框 message
  3. 四 MetricCard：请求数、总 Token、预估成本、失败率（有失败用 warning tone）
  4. 两列 CompactGroupRows：byModule / byModel
- 分支与异常：无
- 调用：MetricCard、CompactGroupRows

## 近逐行中文伪代码

1. 数字/成本/失败率格式化助手
2. CompactGroupRows 截断 Top5 展示分组指标
3. 主组件：加载/错误态 + 四指标卡 + 模块/模型 Top 列表

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/ai/AiUsageOverview.tsx",
      "label": "AiUsageOverview",
      "path": "src/client-web/src/components/ai/AiUsageOverview.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/ai/AiUsageOverview.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/ai/AiUsageOverview.tsx", "to": "src/client-web/src/ui/MetricCard.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/ai/AiUsageOverview.tsx", "to": "src/client-web/src/api/ai.ts", "type": "depends_on" }
  ]
}
```
