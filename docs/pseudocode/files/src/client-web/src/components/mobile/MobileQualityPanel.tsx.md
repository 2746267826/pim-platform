# src/client-web/src/components/mobile/MobileQualityPanel.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：移动端质量面板——展示整体健康状态标签、诊断消息、问题数/检查时间、前 4 个组件状态与前 3 条待处理问题。
- 主要依赖：`MobileQuality`（`../../api/mobile`）、`formatDateTime`/`healthStatusLabel`/`healthToneClass`（`mobileFormatting`）
- 被谁使用：`MobileRecordsDashboard.tsx`

## 函数级结构化伪代码

### MobileQualityPanelProps
#### quality? / qualityIssueCount? / isLoading?
- 输入：质量对象；问题数默认 `quality?.issues.length ?? 0`；loading 默认 false
- 输出：Props
- 副作用：无
- 步骤：默认参数在解构时计算
- 分支与异常：无
- 调用：无

### MobileQualityPanel(props) 默认导出
- 输入：Props
- 输出：JSX section
- 副作用：无（纯展示）
- 步骤：
  1. status = quality?.overallStatus ?? 'Unknown'
  2. 标题「质量面板」+ 状态徽章（label 或 healthStatusLabel）
  3. isLoading → 加载文案；否则 message、问题数、checkedAt
  4. components 前 4 条卡片：name + status + message
  5. issues 前 3 条琥珀色列表
- 分支与异常：无 quality 时默认文案；空 components/issues 不渲染块
- 调用：`healthToneClass`、`healthStatusLabel`、`formatDateTime`

## 近逐行中文伪代码

1. 引入 MobileQuality 与格式化工具
2. 解构 props 与默认 issueCount/loading
3. overallStatus 缺省 Unknown
4. loading 分支；否则展示 message 与 dl 指标
5. 切片渲染 components 与 issues

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileQualityPanel.tsx",
      "label": "MobileQualityPanel",
      "path": "src/client-web/src/components/mobile/MobileQualityPanel.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileQualityPanel.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileQualityPanel.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileQualityPanel.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "to": "src/client-web/src/components/mobile/MobileQualityPanel.tsx", "type": "calls" }
  ]
}
```
