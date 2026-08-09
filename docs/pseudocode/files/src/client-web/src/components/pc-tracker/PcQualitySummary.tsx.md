# src/client-web/src/components/pc-tracker/PcQualitySummary.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：展示 PC 数据质量总览、问题列表与下一步建议。
- 主要依赖：PcQualityResponse、StatusBadge
- 被谁使用：PC Tracker / 今日 PC 质量区

## 函数级结构化伪代码

### formatCheckedAt / errorMessage
- 检查时间本地化；错误文案

### PcQualitySummary
- 分支：loading / error / !quality / 正常
- 正常：overall badge、三列统计、issues（compact 2 否则 4）、nextSteps（2/3）
- issue 行含 severity badge 与 nextStep

## 近逐行中文伪代码

1. status→tone/label 映射。
2. 加载中中性 badge；错误红框。
3. 无数据中性提示。
4. 有 quality 渲染消息、问题与下一步。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/PcQualitySummary.tsx",
      "label": "PcQualitySummary",
      "path": "src/client-web/src/components/pc-tracker/PcQualitySummary.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/PcQualitySummary.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-tracker/PcQualitySummary.tsx", "to": "src/client-web/src/ui/StatusBadge.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/pc-tracker/PcQualitySummary.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
