# src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：渲染 PC 活动分析热力格子，支持选中时间块并展示分类/应用摘要。
- 主要依赖：`PcActivityAnalysisBlock`、`PcActivityAnalysisResponse`（types）
- 被谁使用：PC Tracker 相关页面/面板

## 函数级结构化伪代码

### colorForIntensity(score)
- 输入：强度分数 number
- 输出：十六进制颜色字符串
- 副作用：无
- 步骤：
  1. score<=0 → 浅灰 `#f8fafc`
  2. 1/2/3 分档青绿；其余更深青绿
- 分支与异常：无
- 调用：无

### formatMinutes(seconds)
- 输入：秒数
- 输出：四舍五入分钟的中文 locale 字符串
- 步骤：`Math.round(seconds/60).toLocaleString('zh-CN')`

### formatTime(value)
- 输入：ISO/时间字符串
- 输出：本地时分或原串
- 步骤：解析 Date；非法则返回原值；否则 `toLocaleTimeString` 时:分

### ActivityAnalysisHeatmap (default export)
- 输入：`analysis`、`selectedStart`、`onSelectBlock`
- 输出：React 节点
- 副作用：点击回调父组件
- 步骤：
  1. `blocks = analysis?.blocks ?? []`
  2. 选中：匹配 selectedStart → 否则首个有活跃时长 → 否则 blocks[0]
  3. 无 analysis 或空 blocks → 虚线空状态「暂无活动分析数据」
  4. 网格 6/12 列渲染按钮：背景按 intensity；琥珀边框表示待分类；展示待分类数
  5. 图例说明颜色与边框含义
  6. 若有 selected：展示时段、活跃分钟、上下文切换、待分类；前 4 类目与前 4 应用时长
- 分支与异常：空数据分支
- 调用：colorForIntensity、formatMinutes、formatTime、onSelectBlock

## 近逐行中文伪代码

1. 导入活动分析类型。
2. Props：分析响应、选中开始时间、选块回调。
3. 强度→颜色四档映射。
4. 秒转分钟本地化显示。
5. 时间串转中文时分，非法保留原串。
6. 组件取 blocks；解析 selected（优先选中、再活跃、再首块）。
7. 无数据返回空状态卡片。
8. 否则：map 每个 block 为可点按钮，title/aria 含时间与指标。
9. 选中黑边，有待分类琥珀边，背景按强度。
10. 图例三行说明。
11. selected 详情区：时段标题、三项指标、双列 categories/apps 各最多 4 条。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx",
      "label": "ActivityAnalysisHeatmap",
      "path": "src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-tracker/ActivityAnalysisHeatmap.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
