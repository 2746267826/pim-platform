# src/client-web/src/pages/PcClassificationPage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：PC 分类管理页：推荐最小分类时长设置、规则表与规则编辑器。
- 主要依赖：pcTracker API、ClassificationRecomputePanel/RuleTable/RuleEditor、PageHeader
- 被谁使用：路由分类管理

## 函数级结构化伪代码

### PcClassificationPage
- 查询 rules 与 settings
- selectedRuleId → selectedRule
- saveSettingsMut 写最小分钟并 invalidate summary/suggestions
- effect 同步 selectedMinutes
- 布局：Header + RecomputePanel + 表|编辑器

## 近逐行中文伪代码

1. 拉规则与设置。
2. 选中规则驱动编辑器。
3. 保存设置失败红条。
4. 双栏规则表与编辑。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/PcClassificationPage.tsx",
      "label": "PcClassificationPage",
      "path": "src/client-web/src/pages/PcClassificationPage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/PcClassificationPage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/PcClassificationPage.tsx", "to": "src/client-web/src/api/pcTracker.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/PcClassificationPage.tsx", "to": "src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/PcClassificationPage.tsx", "to": "src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/PcClassificationPage.tsx", "to": "src/client-web/src/ui/PageHeader.tsx", "type": "depends_on" }
  ]
}
```
