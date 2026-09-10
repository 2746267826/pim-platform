# src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：PC 活动分类规则详情只读面板：空态引导 + 规则元数据/说明/conditions JSON 美化展示。
- 主要依赖：`ActivityClassificationRule` 类型
- 被谁使用：PC 分类规则管理 UI

## 函数级结构化伪代码

### formatJson(value: string)
- 输入：JSON 字符串
- 输出：美化后的字符串或原串
- 副作用：无
- 步骤：`JSON.parse` 再 `stringify(..., null, 2)`；失败返回原值
- 分支与异常：catch 返回 value
- 调用：无

### formatPercent(value: number)
- 输入：0–1 置信度
- 输出：`"N%"`
- 副作用：无
- 步骤：`Math.round(value*100)` 加 `%`
- 分支与异常：无
- 调用：无

### ClassificationRuleEditor({ rule, onClose })
- 输入：`rule` 可 null；关闭回调
- 输出：React section
- 副作用：点击关闭
- 步骤：
  1. `rule==null`：空态“从规则列表中选择一条规则”。
  2. 有 rule：色点 + 名称 + source/status；关闭按钮。
  3. dl：分类、项目标签、优先级、置信度百分比。
  4. 可选 explanation 蓝底块。
  5. conditionsJson 以 pre 显示 formatJson 结果。
- 分支与异常：空 rule / 有无 explanation
- 调用：`formatJson`、`formatPercent`、`onClose`

## 近逐行中文伪代码

1. 工具：JSON 美化失败回退；置信度转百分比。
2. 无选中规则显示虚线空态。
3. 有规则：头部色点与名称、来源状态、关闭。
4. 四宫格字段 + 可选说明 + 条件 JSON 代码块。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx",
      "label": "ClassificationRuleEditor",
      "path": "src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/components/pc-classification", "to": "src/client-web/src/components/pc-classification/ClassificationRuleEditor.tsx", "type": "depends_on" }
  ]
}
```
