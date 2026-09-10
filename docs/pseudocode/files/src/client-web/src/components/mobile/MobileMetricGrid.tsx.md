# src/client-web/src/components/mobile/MobileMetricGrid.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：响应式指标卡片网格（label/value/helper + tone 配色）。
- 主要依赖：无
- 被谁使用：移动端分析/质量等面板

## 函数级结构化伪代码

### MobileMetricItem (interface)
- 输入：label、value、helper、可选 tone（default|good|warning）
- 输出：类型
- 副作用：无
- 步骤：类型声明
- 分支与异常：无
- 调用：无

### MobileMetricGrid({ items })
- 输入：`MobileMetricItem[]`
- 输出：`<dl>` 网格
- 副作用：无
- 步骤：
  1. 按 tone 选边框/背景类（teal / amber / 默认 slate）。
  2. 每个 item 一块：dt=label，dd=value，p=helper；key=label。
- 分支与异常：tone 三分支
- 调用：无

## 近逐行中文伪代码

1. 定义指标项类型含 tone。
2. 1/2/6 列响应式 grid。
3. good→teal、warning→amber、否则白底 slate。
4. 渲染 label/value/helper 文本，均 truncate。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileMetricGrid.tsx",
      "label": "MobileMetricGrid",
      "path": "src/client-web/src/components/mobile/MobileMetricGrid.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileMetricGrid.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile", "to": "src/client-web/src/components/mobile/MobileMetricGrid.tsx", "type": "depends_on" }
  ]
}
```
