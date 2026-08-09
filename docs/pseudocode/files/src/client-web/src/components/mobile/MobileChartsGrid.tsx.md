# src/client-web/src/components/mobile/MobileChartsGrid.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：渲染移动分析图表网格：水平条形占比（最多 8 点），支持按分类/包名点击筛选。
- 主要依赖：`MobileAnalyticsChart`、`formatDuration`/`formatNumber`
- 被谁使用：移动分析仪表盘

## 函数级结构化伪代码

### valueLabel(unit, value)
- 输入：单位与数值
- 输出：seconds → 时长文案，否则数字文案
- 副作用：无
- 步骤：unit===`seconds` 用 formatDuration，否则 formatNumber
- 分支与异常：无
- 调用：`formatDuration`、`formatNumber`

### MobileChartsGrid（默认导出）
#### render(props)
- 输入：`charts`、`isLoading?`、`onCategorySelect?`、`onAppSelect?`
- 输出：响应式网格 section
- 副作用：点击可调用筛选回调
- 步骤：
  1. map 每个 chart：maxValue = max(1, points.values)
  2. 卡片标题 + chartType
  3. points 取前 8：条宽 = max(3%, value/max*100)
  4. 若有 lifeCategory 且 onCategorySelect → 分类点击；否则若 packageName 且 onAppSelect → 应用点击；否则静态 div
  5. 行内容：label、可选 packageName  mono、条、valueLabel
  6. loading 或空 points 提示
- 分支与异常：无回调则不可点
- 调用：`valueLabel`、`onCategorySelect`、`onAppSelect`

## 近逐行中文伪代码

1. 引入 chart 类型与 mobileFormatting
2. Props 含 charts/loading/选择回调
3. valueLabel 按 unit 格式化
4. 网格 map charts；算 maxValue
5. 每图卡片：标题、类型、最多 8 点条形
6. 优先分类选择，其次包名选择，否则只读
7. loading/空数据脚注

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileChartsGrid.tsx",
      "label": "MobileChartsGrid",
      "path": "src/client-web/src/components/mobile/MobileChartsGrid.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileChartsGrid.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileChartsGrid.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileChartsGrid.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" }
  ]
}
```
