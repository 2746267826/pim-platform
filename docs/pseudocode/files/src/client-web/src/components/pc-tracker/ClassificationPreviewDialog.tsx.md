# src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：分类建议预览/应用对话框：分类树选项、范围、预览确认键防误用、规则影响面板。
- 主要依赖：RuleImpactPreviewPanel、CategoryTreeNode、分类 preview/apply 类型
- 被谁使用：PcTrackerPage

## 函数级结构化伪代码

### buildClassificationCategoryOptions / classificationPreviewRequestKey / canApplyClassificationPreview / resolveConfirmedClassificationPreviewKey
- 扁平化分类树选项；请求指纹；仅当预览确认键匹配才可应用

### ClassificationPreviewDialog
- suggestion 变化重置表单；request useMemo
- onPreview 设 pending key；onApply 校验 canApply
- UI：分类/项目/范围、预览结果、RuleImpactPreviewPanel、错误

## 近逐行中文伪代码

1. 从树与建议构建下拉选项。
2. 组装 range mode today/range 请求。
3. 预览成功后锁定 confirmation key。
4. 仅 key 匹配且非 busy 才允许应用。
5. 关闭清空 suggestion 上下文。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx",
      "label": "ClassificationPreviewDialog",
      "path": "src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx",
      "to": "src/client-web/src/components/pc-tracker/RuleImpactPreviewPanel.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx",
      "to": "src/client-web/src/types/index.ts",
      "type": "depends_on"
    }
  ]
}
`
