# src/client-web/src/pages/AuditTimelinePage.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：按路由 objectType/objectId 展示审计版本列表、详情、恢复预览与导出。
- 主要依赖：`getAuditTimeline`、`getRestorePreview`、`exportAudit`、`BeforeAfterDiff`、`PageHeader`
- 被谁使用：路由审计时间线页

## 函数级结构化伪代码

### formatDateTime / versionTitle
- 时间本地化；标题 = source · changedFields 拼接

### AuditTimelinePage
- 读 params；query 拉 timeline（objectId 非空）
- mutations：恢复预览、导出审计
- selectedVersion：选中或首条；effect 自动选中首条
- 左栏版本列表；右栏元数据 + 预览/导出结果 + BeforeAfterDiff

## 近逐行中文伪代码

1. 解析路由类型与对象 ID。
2. 加载版本列表，失败/空态处理。
3. 点击切换选中；恢复预览 mutation 展示 summary。
4. 导出成功显示文件名。
5. 差异组件展示 before/after JSON 与变更字段。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/pages/AuditTimelinePage.tsx",
      "label": "AuditTimelinePage",
      "path": "src/client-web/src/pages/AuditTimelinePage.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/pages/AuditTimelinePage.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/pages/AuditTimelinePage.tsx", "to": "src/client-web/src/api/operations.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/AuditTimelinePage.tsx", "to": "src/client-web/src/components/schedule/BeforeAfterDiff.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/AuditTimelinePage.tsx", "to": "src/client-web/src/ui/PageHeader.tsx", "type": "depends_on" },
    { "from": "src/client-web/src/pages/AuditTimelinePage.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
