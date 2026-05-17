# PC记录 页面重设计

## 概述

重设计"PC记录"页面，将现有单文件5面板布局升级为4个主模块 + 时间线 + 设置页查询模块。引入应用活动分类系统，新增键盘可视化，增强数据查询能力。

## 数据架构（不变）

```
KeyStats (本地 :18080) ─┐
                         ├── Windows 守护进程 ── PIM API ── PostgreSQL
ActivityWatch (本地 :5600) ┘                                └── Web 前端
```

### 现有表（不变）

- `pc_keystats_daily` — 每日 KeyStats 快照
- `pc_keystats_key_counts` — 按键热力图明细
- `pc_keystats_app_breakdown` — 按应用细分
- `pc_aw_events` — ActivityWatch 原始事件

### 新增表

- `pc_app_categories` — 应用分类规则

```sql
CREATE TABLE pc_app_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_pattern VARCHAR(128) NOT NULL,      -- 进程名匹配模式（支持通配符）
    category_name VARCHAR(64) NOT NULL,      -- 分类名（如"编程"、"沟通"）
    color VARCHAR(7) DEFAULT '#6B5EE4',      -- 分类颜色 hex
    priority INT DEFAULT 0,                  -- 优先级（精确匹配 > 模糊匹配）
    is_builtin BOOLEAN DEFAULT FALSE,        -- 是否内置默认规则
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

内置默认分类规则：

| app_pattern | category_name | color |
|-------------|---------------|-------|
| Code, Visual Studio, VS Code, Rider, vim, nvim | 编程 | #6B5EE4 |
| chrome, msedge, firefox, Arc, Brave | 浏览 | #0EA8A0 |
| WeChat, 微信, DingTalk, 钉钉, QQ, Telegram, Slack, Discord | 沟通 | #F5935A |
| WindowsTerminal, Terminal, cmd, PowerShell, Alacritty, iTerm2 | 终端 | #E05A7A |
| explorer, Finder, TotalCommander, Everything | 文件管理 | #3B82F6 |
| Spotify, Netease, foobar2000 | 音乐 | #10B981 |
| Word, Excel, PowerPoint, Notion, Obsidian, Typora | 办公 | #F59E0B |
| *（未匹配） | 其他 | #8B5CF6 |

## 页面布局

单页纵向滚动，从上到下：

### 模块一：日期选择器

- 左侧：今天按钮 + 前后天翻页 + 日期文本显示（"2026年5月17日 星期六"）
- 右侧：维度切换（时 / 日 / 月 / 年），切换后自动同步到模块二热力图

### 模块二：GitHub 风格热力图

- 多行多列网格，布局随维度变化：
  - 时：单行 24 列（每小时一格）
  - 日：多行×7列（周日至周六），行数取决于月天数
  - 月：多行×12列（每月一格），行数取决于年数范围
  - 年：多行×7列（周一至周日），行数取决于年数范围
- 右下格为当前时间单位，向前排列 23 个单位
- 颜色：线性绿阶渐变（`ebedf0` → `216e39`），由按键数量线性映射，不设固定档位
- 图例：渐变条 + "少"/"多" 标注
- 悬浮 tooltip 显示具体时间和按键数

### 时间线：活动分类聚合

位于模块二和模块三之间。

- 按应用分类（非单独应用）聚合相邻时间段
- 智能自动分类 + 用户可在设置中手动修正规则
- 悬浮 tooltip 显示该时段内各应用的活跃占比
- 不同分类使用不同颜色区分

分类匹配逻辑：
1. 按 priority 降序，逐个匹配 app_pattern（精确匹配 → 通配符匹配）
2. 一个时间窗内，以主导应用（时长 > 60%）的分类为块分类
3. 无主导应用时标记为"混合"，悬浮查看具体构成
4. 未匹配任何规则归入"其他"

### 模块三：当日活动分析

**11 项衍生指标**（4+4+3 网格布局）：

| 指标 | 计算方式 |
|------|----------|
| 累计记录时长 | AW 首个事件距末个事件的时间跨度 |
| 有输入时长 | 有按键或点击发生的分钟数去重 |
| 空闲时长 | AFK 事件累计 duration |
| 独立工作会话 | AFK 间隙 > 15min 切割的会话数 |
| 活跃应用数 | 去重 app_stats 的应用数 |
| 键盘按键总数 | SUM(key_presses) |
| 点击总数 | SUM(all clicks) |
| 应用切换次数 | AW 窗口事件 app 变化次数 |
| 切换频率 | 切换次数 / 累计记录时长 × 10min |
| 最专注应用 | AW 窗口事件中单次持续最长的 app |
| 按键/点击比 | keyPresses / totalClicks |

**前五分类**：按输入量（按键+点击）排序，显示分类名 + 占比百分比

**前五应用**：按输入量排序，直接显示进程名 + 占比百分比

### 模块四：键盘鼠标热力图

- 标准 ANSI 104 键布局，CSS/SVG 渲染
- 颜色线性渐变（绿阶），由按键按下次数决定，颜色越深按得越多
- 每个键帽上标注具体次数
- 功能键区不参与热力着色（如 Esc、Ctrl、Win、Alt）
- 鼠标点击区域独立展示（左/右/中/侧键计数 + 总点击）
- 组合键快捷键统计列表（提取 keyPressCounts 中含 `+` 的键名，按次数降序）

## 设置页面：PC记录详细数据

路由：`/settings/pc-data`

### 筛选栏

| 筛选项 | 类型 | 说明 |
|--------|------|------|
| 日期范围 | DateRangePicker | 起止日期 |
| 维度 | Select | 时 / 日 / 月 / 年 |
| 设备 | Select | device_id 多选 |
| 应用/分类 | Select + 搜索 | 按 appName 或 categoryName 过滤 |
| 按键名 | Input + 建议 | 按 keyName 过滤（如 Space、Ctrl+C） |
| 事件类型 | MultiSelect | 窗口切换 / AFK / 输入 |
| 排序方式 | Select | 按键数降/升、点击数降/升、时长降/升、日期降/升 |

### 查询结果

- 多功能表格：可排序、可分页、自定义列显隐
- 导出按钮：CSV 导出 / JSON 导出
- 表格列根据维度动态变化：
  - 年维度：月、分类、按键数、点击数、时长
  - 月维度：日、分类、按键数、点击数、时长
  - 日维度：小时、应用、分类、按键数、点击数、时长
  - 时维度：分钟、应用、窗口标题、按键数、点击数

## API 扩展

### 新增端点

```
GET  /api/v1/pc/detail               → 多功能查询（支持全部筛选参数）
GET  /api/v1/pc/categories            → 获取分类规则列表
POST /api/v1/pc/categories            → 新增/修改分类规则
DELETE /api/v1/pc/categories/{id}     → 删除分类规则
```

### 现有端点修改

```
GET  /api/v1/pc/summary?date=         → 响应中加入 derivedMetrics 和 categories 字段
GET  /api/v1/pc/aw/heatmap?start=&end= → 支持 dimension 参数，返回网格化数据
```

## 前端组件结构

```
src/client-web/src/
├── pages/
│   ├── PcTrackerPage.tsx                 # 主页面容器（组合 + 数据获取）
│   └── PcDetailQueryPage.tsx             # 设置页：详细数据查询
├── components/pc-tracker/
│   ├── DateDimensionBar.tsx              # 模块一：日期选择 + 维度切换
│   ├── ActivityHeatmap.tsx               # 模块二：GitHub 风格热力图
│   ├── CategoryTimeline.tsx              # 时间线：活动分类聚合
│   ├── DailyActivityPanel.tsx            # 模块三：当日活动分析
│   ├── KeyboardHeatmap.tsx               # 模块四：键盘鼠标热力图
│   └── PcDetailQueryPanel.tsx            # 设置页：筛选栏 + 数据表格
├── api/pcTracker.ts                      # API 函数扩展
└── types/index.ts                        # 类型定义扩展
```

## 新增类型定义

```typescript
interface DerivedMetrics {
  totalRecordedDuration: string;    // "10h 30m"
  activeInputDuration: string;      // "5h 12m"
  idleDuration: string;             // "2h 18m"
  sessionCount: number;
  activeAppCount: number;
  totalKeyPresses: number;
  totalClicks: number;
  appSwitchCount: number;
  switchFrequency: number;          // 次/10min
  mostFocusedApp: string;
  keyClickRatio: number;            // 按键/点击比
}

interface CategorySummary {
  categoryName: string;
  color: string;
  share: number;                    // 0-100
  keyPresses: number;
  totalClicks: number;
}

interface AppCategoryRule {
  id: string;
  appPattern: string;
  categoryName: string;
  color: string;
  priority: number;
  isBuiltin: boolean;
}

interface DetailQueryParams {
  dateFrom?: string;
  dateTo?: string;
  dimension?: 'hour' | 'day' | 'month' | 'year';
  deviceId?: string[];
  appName?: string;
  categoryName?: string;
  keyName?: string;
  eventType?: string[];
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
}
```

## 交互行为

- 维度切换联动热力图重绘
- 时间线分类块悬浮显示应用构成
- 模块三的前五分类/前五应用可点击，联动筛选时间线
- 键盘热力图键帽悬浮显示具体数值
- 详细数据查询表格支持列拖拽排序、分页跳转、导出

## 实现优先级

1. 模块一 + 模块二（核心热力图重做）
2. 分类系统 + 时间线
3. 模块三（衍生指标面板）
4. 模块四（键盘热力图）
5. 设置页查询模块
