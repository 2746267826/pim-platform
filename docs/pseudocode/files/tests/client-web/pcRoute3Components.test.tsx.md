# tests/client-web/pcRoute3Components.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：PC Route3 分类队列/预览/活动分析 UI 与预览确认键逻辑，以及 PcTrackerPage 集成源码契约。
- 主要依赖：ClassificationActionQueue、RuleImpactPreviewPanel、ActivityAnalysisHeatmap、ClassificationPreviewDialog、PcTrackerPage helpers
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

1. 队列中文「处理并预览」，禁 Accept/Later
2. 影响面板记录/分钟/分类迁移
3. 活动分析热力中文与 aria-pressed
4. nextPcRoute3RequestId / isCurrentPcRoute3Request
5. canApplyClassificationPreview 键匹配与 loading 门禁
6. resolveConfirmedClassificationPreviewKey 新旧预览切换
7. PreviewDialog 中文与禁用写入按钮；范围模式「今天」
8. PcTrackerPage：App 知识库 API，无旧 classification accept 路径

## 近逐行中文伪代码

1. [L1-L141] fixture 与组件渲染断言
2. [L142-L235] 请求 id 与确认键
3. [L237-L298] 对话框与页面源码

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/pcRoute3Components.test.tsx",
      "label": "pcRoute3Components.test",
      "path": "tests/client-web/pcRoute3Components.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/pcRoute3Components.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/pcRoute3Components.test.tsx", "to": "src/client-web/src/components/pc-tracker/ClassificationActionQueue.tsx", "type": "tests" },
    { "from": "tests/client-web/pcRoute3Components.test.tsx", "to": "src/client-web/src/components/pc-tracker/ClassificationPreviewDialog.tsx", "type": "tests" },
    { "from": "tests/client-web/pcRoute3Components.test.tsx", "to": "src/client-web/src/pages/PcTrackerPage.tsx", "type": "tests" }
  ]
}
```
