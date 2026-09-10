# src/client-web/src/components/today/TodayHealthSection.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：今日系统健康区块：总摘要、windows-daemon 组件、下一步、链到 /status。
- 主要依赖：EmptyState、StatusBadge
- 被谁使用：TodaySectionHost

## 函数级结构化伪代码

### statusTone
- critical/warning/normal 映射 tone

### TodayHealthSection
- 展示 summary；找 daemon 组件；firstNextStep 或 EmptyState；Link 状态页

## 近逐行中文伪代码

1. 读 section.data detail/summary。
2. badge 状态。
3. daemon 卡 + 下一步 amber。
4. 查看状态链接。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/today/TodayHealthSection.tsx",
      "label": "TodayHealthSection",
      "path": "src/client-web/src/components/today/TodayHealthSection.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/today/TodayHealthSection.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    {
      "from": "src/client-web/src/components/today/TodayHealthSection.tsx",
      "to": "src/client-web/src/ui/StatusBadge.tsx",
      "type": "depends_on"
    },
    {
      "from": "src/client-web/src/components/today/TodayHealthSection.tsx",
      "to": "src/client-web/src/ui/EmptyState.tsx",
      "type": "depends_on"
    }
  ]
}
`
