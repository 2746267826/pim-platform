# src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：PC 明细查询面板：多条件筛选、分页表、CSV/JSON 导出、空态质量提示。
- 主要依赖：`queryPcDetail`、`getPcQuality`、DetailQueryParams/PcDetailRecord
- 被谁使用：PC 明细查询页

## 函数级结构化伪代码

### formatCsvValue / downloadCSV
- CSV 公式注入防护；BOM 下载

### formatDate/Number/Duration/Boolean/RecordType
- 展示格式化

### renderMainDetail / renderBrowserSource / renderExtraInfo
- 按 recordType（web-page/web/其他）差异化列内容

### getEmptyStateText
- 根据 quality.issues code 映射中文空态

### PcDetailQueryPanel
- params state page/pageSize；query detail；空结果时拉 quality
- update 改筛选项重置 page（page 键除外）
- 筛选项网格 + 导出按钮 + 表格 + 分页

## 近逐行中文伪代码

1. 定义 CSV 全列映射。
2. 格式化与按类型渲染单元格。
3. 质量问题码转空态文案。
4. 查询明细；无数据查质量。
5. 多输入/下拉更新 params。
6. 有数据可导出 CSV/JSON。
7. 表格列：类型/时间/设备/来源/详情/补充/持续。
8. 多页时上一页/下一页。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx",
      "label": "PcDetailQueryPanel",
      "path": "src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx", "to": "src/client-web/src/api/pcTracker.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/pc-tracker/PcDetailQueryPanel.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
