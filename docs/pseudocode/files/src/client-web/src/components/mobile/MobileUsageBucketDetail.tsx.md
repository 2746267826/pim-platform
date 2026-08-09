# src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示热力图选中时段（bucket）明细：峰值标签、前台时长、分类条与质量提示；未选中时占位。
- 主要依赖：`HeatmapMatrixCell`、`formatDuration`
- 被谁使用：移动使用热力图联动侧栏

## 函数级结构化伪代码

### formatCellRange(cell)
- 输入：热力格 cell
- 输出：`M月D日 HH:00 至 HH:00`（结束小时 +1 mod 24）
- 副作用：无
- 步骤：解析 localDate；padStart 小时
- 分支与异常：无
- 调用：`String.padStart`

### peakLabel(cell)
- 输入：cell
- 输出：`高峰`/`活跃`/`低使用`
- 副作用：无
- 步骤：foregroundSeconds ≥45min / ≥15min / 否则
- 分支与异常：无
- 调用：无

### MobileUsageBucketDetail（默认导出）
#### render({ cell })
- 输入：`HeatmapMatrixCell | null`
- 输出：侧栏 section
- 副作用：无
- 步骤：
  1. cell 空：标题「选中时段」+ 引导 +「暂未选择时段」
  2. 有 cell：标题、时间范围、peak 徽章
  3. 大字 formatDuration(foregroundSeconds)
  4. 前 3 个分类胶囊 + 质量正常/有质量提示
  5. dl 四格：Top 分类、桶数量、最长连续、系统噪声=已隐藏
  6. 分类构成条形：占比 width 相对 cell.foregroundSeconds
  7. 脚注说明：点击不缩全局范围，右侧明细，下方可联动
- 分支与异常：categories[0] 缺省「未分类」；qualityFlags 控制警告样式
- 调用：`formatCellRange`、`peakLabel`、`formatDuration`

## 近逐行中文伪代码

1. 引入 HeatmapMatrixCell 与 formatDuration
2. formatCellRange：日期拆分 + 小时区间
3. peakLabel：45/15 分钟阈值
4. 无 cell：空态占位 UI
5. 有 cell：峰值、总时长、分类胶囊、质量、四指标、分类条、说明文案

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx",
      "label": "MobileUsageBucketDetail",
      "path": "src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx", "to": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "to": "src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx", "type": "calls" }
  ]
}
```
