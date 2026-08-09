# src/client-web/src/components/mobile/MobileTimeline.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：手机前台活动时间线列表展示（事件明细 vs 回退汇总）。
- 主要依赖：
  - `MobileTimelineItem`（api/mobile）
  - `formatDuration` / `formatShortTime`（mobileFormatting）
- 被谁使用：Mobile 仪表盘/记录页

## 函数级结构化伪代码

### `itemKindLabel(item)`
- 输入：时间线条目
- 输出：`回退汇总` 或 `事件明细`
- 步骤：kind === 'fallback' 判定

### `sourceLabel(source)`
- 输入：source 字符串
- 输出：Usage Events / Usage Stats / 原值

### MobileTimeline（default）
#### 组件
- 输入：items、isLoading?
- 输出：JSX section
- 副作用：无
- 步骤：
  1. 标题「时间线」与段数徽章。
  2. loading → 加载文案；空 → 暂无记录。
  3. 否则 ol 映射每项：显示名/包名、kind 徽章、起止时间、时长、来源、置信度或 fallback reason。
- 分支与异常：无
- 调用：`itemKindLabel`、`sourceLabel`、`formatShortTime`、`formatDuration`

## 近逐行中文伪代码

1. 纯展示组件，无本地状态。
2. loading/空/列表三态。
3. 每段展示应用名、时间窗、时长、来源与置信度（fallback 显示 reason）。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileTimeline.tsx",
      "label": "MobileTimeline",
      "path": "src/client-web/src/components/mobile/MobileTimeline.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileTimeline.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileTimeline.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileTimeline.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" }
  ]
}
```
