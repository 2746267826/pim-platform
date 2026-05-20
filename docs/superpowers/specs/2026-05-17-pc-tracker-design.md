# PC记录 页面设计

## 概述

为 Web 前端新增"PC记录"页面，展示 PC 守护进程采集的 KeyStats 键鼠统计和 ActivityWatch 窗口/活跃事件数据。

## 数据架构

PC 本地守护程序 → 上传到服务器 → Web 前端查询服务器 API。

### 数据源

- **KeyStats** (`GET /api/stats/`): 每日聚合快照 — 键鼠统计、按键热力图、按应用细分
- **ActivityWatch** (`GET /0/buckets/{id}/events`): 原始时间序列事件 — 窗口切换、AFK 状态

### 服务端表结构

- `pc_keystats_daily` — 每日 KeyStats 快照 (device_id, snapshot_date 唯一约束)
- `pc_keystats_key_counts` — 按键热力图明细 (FK → daily)
- `pc_keystats_app_breakdown` — 按应用细分 (FK → daily)
- `pc_aw_events` — AW 事件 (event_type: window|afk, timestamp 索引)

### 服务端 API

```
POST /api/v1/pc/keystats/upload     → 上传 KeyStats 每日快照
POST /api/v1/pc/aw/upload            → 批量上传 AW 事件
GET  /api/v1/pc/summary?date=        → 综合摘要
GET  /api/v1/pc/aw/timeline?date=    → 当日时间线
GET  /api/v1/pc/aw/heatmap?start=&end= → 活动热力图
GET  /api/v1/pc/keystats/range?start=&end= → 多日键鼠趋势
```

## 前端页面

### 路由

`/pc-tracker` → PcTrackerPage

### 布局

单页纵向滚动，5 个卡片面板：

1. **活动热力图** — 24h 网格色块，颜色深浅表示活跃度（基于 AW 事件），可切换小时/日/月/年粒度，支持下钻
2. **应用使用排行** — 水平条形图，按 KeyStats appStats 排序，点击应用联动筛选其他面板
3. **每日时间线** — 水平甘特图，每个 AW 窗口事件一个色块，不同应用不同颜色
4. **输入行为分析** — 统计卡片 + 高频按键 + 应用内输入强度列表
5. **工作会话识别** — 按 AFK 间隙 > 15min 切割的连续工作块列表

### 数据获取

`GET /api/v1/pc/summary?date=` 返回所有面板所需的聚合数据。

### 交互

- 日期选择器切换日期
- 热力图支持粒度切换和下钻
- 应用排行点击联动筛选时间线和输入面板
- 工作会话可展开查看详情
