# src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：PC 活动分类规则表格：加载/空态、列表展示来源与状态标签、选中高亮、触发编辑。
- 主要依赖：类型 `ActivityClassificationRule`
- 被谁使用：`PcClassificationPage`

## 函数级结构化伪代码

### getSourceLabel(source) / getStatusLabel(status)
- 输入：来源/状态字符串
- 输出：中文标签或原串
- 副作用：无
- 步骤：查 `sourceLabels`（builtin/heuristic/user/llm）或 `statusLabels`（active/inactive）
- 分支与异常：未知键回退原文
- 调用：无

### ClassificationRuleTable(props)
- 输入：`rules`、`selectedRuleId?`、`isLoading?`、`onEdit`
- 输出：section 面板
- 副作用：点击「查看」调用 `onEdit(rule)`
- 步骤：
  1. loading → 虚线框「正在加载分类规则...」。
  2. 空列表 → 「暂无分类规则。」。
  3. 否则：标题「规则列表」+ 条数 badge；表头规则/分类/项目/来源/优先级/操作。
  4. 每行：色点+规则名+说明或 conditionsJson；分类名；项目标签；来源+状态 pill；优先级；查看按钮。
  5. 选中行 `bg-blue-50/70`。
- 分支与异常：loading/empty/list 三态
- 调用：`getSourceLabel`、`getStatusLabel`、`onEdit`

## 近逐行中文伪代码

1. 映射来源与状态中文标签。
2. loading/empty 早返回。
3. 表格 map rules；选中高亮；查看触发 onEdit。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx",
      "label": "ClassificationRuleTable",
      "path": "src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/PcClassificationPage.tsx", "to": "src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/components/pc-classification/ClassificationRuleTable.tsx", "to": "src/client-web/src/types", "type": "depends_on" }
  ]
}
```
