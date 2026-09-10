# src/client-web/src/components/mobile/LocationSegmentDetail.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示选中（或回退首个）移动定位轨迹片段的详情：里程、耗时、速度、误差、质量标志。
- 主要依赖：`../../api/mobile` 类型、`./locationFormatting`
- 被谁使用：历史定位仪表盘

## 函数级结构化伪代码

### 辅助
#### allSegments(tracks) / fallbackSegment(tracks)
- 输入：轨迹列表
- 输出：扁平 segments / 第一个 segment 或 null
- 步骤：flatMap segments；取 [0]
- 调用：无

#### SegmentStats({ segment })
- 输入：片段
- 输出：2×2 统计网格（耗时/点数/均速/均误差）
- 调用：`formatDurationSeconds`、`formatSpeedMetersPerSecond`、`formatAccuracyLabel`

### LocationSegmentDetail
#### default function LocationSegmentDetail({ tracks, selectedSegmentId })
- 输入：轨迹、可选选中片段 Id
- 输出：详情 section
- 副作用：无
- 步骤：
  1. 按 selectedSegmentId 查找，否则 fallback 首段
  2. 无片段 → 空态文案
  3. qualityFlags 映射标签，空则「质量正常」
  4. 渲染时间范围、kind 徽章、里程、SegmentStats、设备与最大误差、质量 chips、交互提示
- 分支与异常：无
- 调用：`segmentKindLabel`、`formatDistanceMeters`、`qualityFlagLabel`、`SegmentStats`

## 近逐行中文伪代码

1. 导入 MobileLocationSegment/Track 与格式化工具
2. allSegments：tracks.flatMap segments
3. fallbackSegment：取第一个
4. SegmentStats：网格展示四项指标
5. 主组件：find by id 或 fallback
6. 无 segment：白底卡片「没有可展示的轨迹片段」
7. 有 segment：标题「选中片段」+ 本地起止时间 + kind 徽章
8. 大号里程数字 + 「估算里程」
9. SegmentStats
10. kind 片段说明 + deviceId + 最大误差
11. 质量标签 chips
12. 蓝色提示：点击地图/时间线更新本区

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/LocationSegmentDetail.tsx",
      "label": "LocationSegmentDetail",
      "path": "src/client-web/src/components/mobile/LocationSegmentDetail.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/LocationSegmentDetail.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/LocationSegmentDetail.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationSegmentDetail.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "depends_on" }
  ]
}
```
