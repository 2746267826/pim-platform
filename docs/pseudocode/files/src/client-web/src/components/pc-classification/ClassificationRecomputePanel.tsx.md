# src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：PC 活动分类「推荐最短分类时长」设置面板：预设分钟按钮 + 保存。
- 主要依赖：`ActivityClassificationSettings`（types）
- 被谁使用：PC 分类设置/重算相关页面

## 函数级结构化伪代码

### ClassificationRecomputePanel
#### default function ClassificationRecomputePanel({ settings, selectedMinutes, onSelectedMinutesChange, onSaveSettings, isSaving, isDirty })
- 输入：设置、当前选中分钟、变更/保存回调、保存中与脏标志
- 输出：设置面板 UI
- 副作用：点击按钮触发父级回调
- 步骤：
  1. presets = settings.supportedRecommendedMinimumDurations 或默认 [1,3,5,10,15]
  2. 渲染标题与说明文案
  3. map presets 为分钟按钮；选中者蓝边蓝底
  4. 保存按钮：isSaving 显示「保存中...」；disabled 当 isSaving 或 !isDirty
- 分支与异常：无
- 调用：`onSelectedMinutesChange`、`onSaveSettings`

## 近逐行中文伪代码

1. 导入 ActivityClassificationSettings 类型
2. Props：settings、selectedMinutes、onSelectedMinutesChange、onSaveSettings、isSaving、isDirty
3. presets 取设置或默认五档
4. section.pim-panel：左说明+预设按钮组，右主按钮保存
5. 选中分钟样式切换；保存禁用逻辑

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx",
      "label": "ClassificationRecomputePanel",
      "path": "src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-classification/ClassificationRecomputePanel.tsx", "to": "src/client-web/src/types", "type": "depends_on" }
  ]
}
```
