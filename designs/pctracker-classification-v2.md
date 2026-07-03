# PcTracker 分类模块 2.0 — 重构设计方案

## 目录
1. [三大痛点](#1-三大痛点)
2. [核心设计理念](#2-核心设计理念)
3. [数据模型](#3-数据模型)
4. [分类引擎](#4-分类引擎)
5. [App 知识库（互联网查询）](#5-app-知识库互联网查询)
6. [建议与反馈闭环](#6-建议与反馈闭环)
7. [分类管理 UI](#7-分类管理-ui)
8. [时间线 & 热力图改造](#8-时间线--热力图改造)
9. [API 设计](#9-api-设计)
10. [实施路线](#10-实施路线)

---

## 1. 三大痛点

| 痛点 | 具体表现 | 根因 |
|:--|:--|:--|
| **分类建议太蠢** | 看到 `isaac-ng` 不知道是游戏，还问用户"这是什么" | 没有 App 知识库，没有互联网查询，纯靠内置白名单 |
| **UI 效果差** | 时间线简陋，热力图颜色不直观，分类看板缺失 | 前端是纯展示，没有好的交互设计 |
| **分类体系太薄** | 分类是平铺的，没有父子层级，没有 productivity 评分 | 数据模型太过简单 |

---

## 2. 核心设计理念

```
RescueTime 的 productivity 评分
  + Timely 的 AI 建议闭环（但用规则引擎代替 AI）
  + Rize 的零干预体验（内置 App 知识库兜底）
  = 我们的分类系统
```

三个关键转变：

| 旧 | 新 |
|:--|:--|
| 分类是平的 | **分类树**（父/子层级，父级时间 = 子和） |
| 规则硬编码在代码里 | **全部规则存 DB**，用户可编辑，可新增 |
| 不认识的应用问用户 | **内置 App 知识库 + 可选的互联网查询**，先查再问 |
| 分类只管分 | **分类 + Productivity 评分**两条线 |

---

## 3. 数据模型

### 3.1 分类表 `pc_categories`

```sql
CREATE TABLE pc_categories (
    id              UUID PRIMARY KEY,
    parent_id       UUID REFERENCES pc_categories(id),  -- NULL = 根分类
    name            TEXT NOT NULL,                       -- 分类名，如 "游戏"
    color           TEXT NOT NULL,                       -- 色值 #EC4899
    icon            TEXT,                                -- 可选图标名
    productivity    TEXT NOT NULL DEFAULT 'neutral',     -- productive / neutral / distracting
    sort_order      INT NOT NULL DEFAULT 0,
    is_builtin      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

内置种子数据：

```
娱乐                        [distracting]
├── 游戏                   [distracting]
│   ├── 单机游戏           [distracting]
│   └── 网络游戏           [distracting]
├── 视频                   [distracting]
├── 音乐                   [neutral]
└── 社交                   [neutral]
工作                        [productive]
├── 编程                   [productive]
│   ├── 前端               [productive]
│   └── 后端               [productive]
├── 文档                   [productive]
├── 会议                   [productive]
├── 设计                   [productive]
└── 运维                   [productive]
学习                        [productive]
├── 技术学习               [productive]
├── 外语学习               [productive]
└── 阅读                   [neutral]
沟通                        [productive]
├── 即时消息               [neutral]
└── 邮件                   [productive]
其他                        [neutral]
```

### 3.2 分类规则表 `pc_classification_rules`

当前已有这个表，需要扩展字段：

```sql
-- 现有字段基础上增加
ALTER TABLE pc_classification_rules ADD COLUMN IF NOT EXISTS match_type TEXT NOT NULL DEFAULT 'condition_json';
-- 新增匹配类型:
--   'condition_json' — 现有 JSON 条件
--   'app_signature'  — 引用 App 知识库签名
--   'domain_glob'    — 域名通配
--   'title_regex'    — 窗口标题正则

ALTER TABLE pc_classification_rules ADD COLUMN IF NOT EXISTS time_restriction JSONB;
-- 时间限制，可选:
-- {"days": [1,2,3,4,5], "start": "09:00", "end": "18:00"}
-- NULL = 全天

ALTER TABLE pc_classification_rules ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES pc_categories(id);
```

### 3.3 App 知识库表 `pc_app_signatures`（新增）

```sql
CREATE TABLE pc_app_signatures (
    id              UUID PRIMARY KEY,
    process_name    TEXT NOT NULL UNIQUE,         -- isaac-ng.exe, VALORANT-Win64-Shipping.exe
    display_name    TEXT NOT NULL,                -- "以撒的结合", "无畏契约"
    category_id     UUID REFERENCES pc_categories(id),  -- 映射到分类树
    productivity    TEXT NOT NULL DEFAULT 'neutral',
    description     TEXT,                         -- "一款 roguelike 射击游戏"
    source          TEXT NOT NULL DEFAULT 'manual', -- manual / web_lookup / crowd / auto
    confidence      DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    search_keywords TEXT,                         -- 用于互联网搜索的关键词
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

内置种子（至少 200+ 条，覆盖常见软件）：

```
process_name                   | display_name       | category_path          | productivity
-------------------------------|--------------------|------------------------|-------------
Code.exe / code                | VS Code            | 工作→编程              | productive
devenv.exe                     | Visual Studio      | 工作→编程              | productive
idea*.exe                      | IntelliJ IDEA      | 工作→编程              | productive
WeChat.exe                     | 微信               | 沟通→即时消息          | neutral
QQ.exe                         | QQ                 | 沟通→即时消息          | neutral
DingTalk.exe                   | 钉钉               | 沟通→即时消息          | productive
Slack.exe                      | Slack              | 沟通→即时消息          | productive
Discord.exe                    | Discord            | 沟通→即时消息          | neutral
Telegram.exe                   | Telegram           | 沟通→即时消息          | neutral
chrome.exe                     | Chrome             | —（按标题/域名动态分） | —
firefox.exe                    | Firefox            | —（同上）              | —
explorer.exe                   | 资源管理器         | 工作→文档              | neutral
WINWORD.EXE                    | Word               | 工作→文档              | productive
EXCEL.EXE                      | Excel              | 工作→文档              | productive
POWERPNT.EXE                   | PowerPoint         | 工作→文档              | productive
OUTLOOK.EXE                    | Outlook            | 沟通→邮件              | productive
WeMeeting.exe / wemeetapp.exe | 腾讯会议           | 沟通→会议              | productive
isaac-ng.exe                   | 以撒的结合         | 娱乐→游戏→单机游戏     | distracting
VALORANT-Win64-Shipping.exe   | 无畏契约           | 娱乐→游戏→网络游戏     | distracting
MobaXterm.exe                  | MobaXterm          | 工作→运维              | productive
WindowsTerminal.exe            | Windows Terminal   | 工作→编程              | productive
...
```

### 3.4 分类活动记录表 `pc_activity_classifications`（已有，优化）

当前表有分类结果，需要加：

```sql
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS productivity TEXT;
ALTER TABLE pc_activity_classifications ADD COLUMN IF NOT EXISTS app_signature_id UUID REFERENCES pc_app_signatures(id);
```

### 3.5 Productivity 统计物化视图（新增）

```sql
CREATE MATERIALIZED VIEW pc_daily_productivity AS
SELECT
    DATE(ac.start_time) AS date,
    ac.device_id,
    c.productivity,
    SUM(ac.duration_ms) / 60000.0 AS total_minutes,
    COUNT(*) AS event_count
FROM pc_activity_classifications ac
JOIN pc_categories c ON c.id = ac.category_id
GROUP BY DATE(ac.start_time), ac.device_id, c.productivity;
```

---

## 4. 分类引擎

### 4.1 匹配优先级（重新设计）

```
1. 用户手动规则（最高优先级）
   └── condition_json / title_regex / domain_glob → 匹配 → 返回
   
2. App 知识库匹配
   └── process_name 精确匹配 / 通配符匹配 → 匹配 → 返回
   
3. 浏览器域名分类（标题中提取 URL 域名）
   └── github.com → 工作→编程
   └── bilibili.com → 娱乐→视频
   └── leetcode.com → 学习
   
4. 启发式（Classifier.cs 现有逻辑，降级到最低优先级）
   
5. 兜底 → "其他"
```

### 4.2 规则条件 DSL（现有 ConditionsJson 扩展）

当前是 JSON，扩展支持：

```json
{
  "match": {
    "any": [
      {"app_name": {"contains": "code"}},
      {"window_title": {"regex": ".*\\.cs$"}},
      {"domain": {"in": ["github.com", "gitlab.com"]}},
      {"time": {"between": ["09:00", "12:00"], "days": [1,2,3,4,5]}}
    ]
  }
}
```

### 4.3 浏览器活动特殊处理

浏览器（Chrome/Firefox/Edge）是最大的未分类来源。策略：

1. 标题中提取 URL（浏览器窗口标题通常含域名）
2. 域名 → 预设分类映射
3. 域名的 path 也参与匹配

示例映射（内置）：

```
github.com/*           → 工作→编程（productive）
gitlab.com/*           → 工作→编程（productive）
stackoverflow.com/*    → 学习→技术学习（productive）
bilibili.com/*         → 娱乐→视频（distracting）
youtube.com/*          → 娱乐→视频（distracting）
chat.openai.com/*      → 工作→编程（productive）
mail.google.com/*      → 沟通→邮件（productive）
calendar.google.com/*  → 工作→会议（productive）
```

---

## 5. App 知识库（互联网查询）

### 5.1 自动查询流程

当遇到不认识的应用（不在签名库中）时：

```
未知进程名出现 → 检查签名库 → 未命中
  → 按优先级:
    1. 本地模糊匹配（进程名相似度）
    2. 可选：互联网搜索进程名描述
    3. 生成建议分类（低置信度）
    4. 用户确认/修改后入库
```

### 5.2 互联网查询设计

不依赖特定 API，可插拔：

```csharp
interface IAppLookupProvider
{
    Task<AppLookupResult?> LookupAsync(string processName, CancellationToken ct);
}

// 内置实现: 通过 Web 搜索进程名
// 例如搜索 "isaac-ng.exe application" 获取描述
```

查询结果缓存到 `pc_app_signatures`，下次直接命中。

### 5.3 种子库维护

初始种子库手工整理 200+ 条常见应用。后续通过以下方式增长：
- 用户手动指定分类 → 自动入库
- 互联网查询匹配 → 确认后入库
- 用户可导出/分享签名库

---

## 6. 建议与反馈闭环

### 6.1 建议触发条件

每小时/每日扫描，条件（可配置）：
1. 分类为 "其他"（fallback）
2. 置信度 < 0.5
3. 同类活动累计 > 30 分钟

### 6.2 智能去重

同进程名/同域名的建议只提一次。用户拒绝后：
- 加入黑名单：3 天内不再提同类建议
- 拒绝 3 次以上：永久沉默该建议

### 6.3 建议操作

| 操作 | 效果 |
|:--|:--|
| **接受** | 创建规则/映射签名，立即重分类 |
| **拒绝** | 静默 3 天，记录拒绝原因 |
| **修改再接受** | 用户调整分类后接受 |
| **批量接受** | 选中多条同类，一键全部分类 |
| **永不建议** | 永久静默该进程/域名 |

### 6.4 接受率追踪

```sql
CREATE TABLE pc_suggestion_feedback (
    id              UUID PRIMARY KEY,
    process_name    TEXT,
    domain          TEXT,
    suggested_category_id UUID REFERENCES pc_categories(id),
    accepted_category_id  UUID REFERENCES pc_categories(id), -- NULL = 拒绝
    action          TEXT NOT NULL, -- accept / reject / modified
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

建议质量报告：接受率 < 30% 的自动静默。

---

## 7. 分类管理 UI

### 7.1 分类树设置页

```
┌────────────────────────────────────────────┐
│  分类管理                        [+ 添加]   │
│                                             │
│  ┌─ 娱乐 😄 [distracting]                  │
│  │  ├─ 游戏 🎮                              │
│  │  │  ├─ 单机游戏                          │
│  │  │  └─ 网络游戏                          │
│  │  ├─ 视频 📺                              │
│  │  └─ 音乐 🎵                              │
│  ├─ 工作 💼 [productive]                    │
│  │  ├─ 编程 💻                              │
│  │  │  ├─ Pim 平台            ← 自定义子级  │
│  │  │  └─ 其他项目                          │
│  │  ├─ 文档 📄                              │
│  │  └─ 会议 📞                              │
│  └─ 学习 📚 [productive]                    │
│     ├─ 技术学习                              │
│     └─ 外语学习                              │
│                                             │
│  拖拽排序 / 右键编辑                        │
└────────────────────────────────────────────┘
```

点击分类 → 编辑：
- 名称 / 颜色 / 图标 / Productivity
- 关联的规则列表
- 今日耗时 / 趋势

### 7.2 建议面板（重构）

当前是列表，改为：

```
┌─────────────────────────────────────┐
│  未分类活动                    [批量] │
│                                      │
│  [☐] MobaXterm (9.1 分钟)           │
│       建议 → 工作→运维    99% 置信  │
│       └─ [接受] [修改] [拒绝]       │
│                                      │
│  [☐] 以撒的结合 (80.9 分钟)         │
│       ℹ️ 已联网识别为游戏            │
│       建议 → 娱乐→游戏→单机  85%    │
│       └─ [接受] [修改] [拒绝]       │
│                                      │
│  [☐] 无畏契约 (34.6 分钟)           │
│       ℹ️ 已联网识别为游戏            │
│       建议 → 娱乐→游戏→网游  85%    │
│       └─ [接受] [修改] [拒绝]       │
│                                      │
│  底部: "还有 12 条未分类"            │
└─────────────────────────────────────┘
```

关键变化：
- **识别标记** `ℹ️ 已联网识别` 增加可信度
- **批量勾选** → 一键全部分类为同一类
- **置信度**显示（让用户知道系统有多确定）

### 7.3 App 知识库管理页

```
┌──────────────────────────────────┐
│  App 知识库           [搜索]     │
│                                   │
│  isaac-ng.exe                     │
│  → 以撒的结合                     │
│  → 娱乐→游戏→单机  [编辑] [删除]  │
│                                   │
│  mobaxterm*.exe                   │
│  → MobaXterm                      │
│  → 工作→运维         [编辑] [删除] │
│                                   │
│  共 247 条记录       [导入] [导出] │
└──────────────────────────────────┘
```

---

## 8. 时间线 & 热力图改造

### 8.1 时间线 v2（Gantt 样式）

当前：列表展示。

改造后：

```
┌─────────────────────────────────────────────┐
│  2026-07-03 周三                             │
│                                              │
│  06:00                                        │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  07:00                                        │
│  08:00  ████████████████                     │  ← 游戏（以撒）
│         娱乐→游戏                              │
│  09:00  ██████████████████████████████        │  ← 工作→编程（VS Code）
│         工作→编程                              │
│  10:00  ███████████████                       │
│  11:00  ████░░░░█████████████                 │  ← 开会 + 编程交错
│         会议/编程                              │
│  12:00  ░░░░░░░░░░                            │  ← 午休
│  13:00  ████████████████████████████          │
│         工作→运维（MobaXterm）                  │
│  14:00  ████████████████░░░░████              │
│         工作→编程/运维                          │
│  15:00  ████████████████████████              │
│  16:00  ████████████████                      │
│  17:00  ░░░░░░░░░░████████                    │  ← 下班前摸了会鱼
│         娱乐→游戏（无畏契约）                    │
│  18:00                                        │
│                                              │
│  图例: █ 工作 █ 娱乐 █ 学习 █ 沟通 █ 未分类    │
│                                              │
│  今日总计: 8.2h  生产性 5.4h (66%)            │
│  编程 3.1h | 运维 1.5h | 会议 0.8h | 游戏 1.2h│
└─────────────────────────────────────────────┘
```

交互：
- 悬停/点击 → 查看该时间段的具体活动详情
- 拖拽选中 → 批量分类
- 日期切换按钮

### 8.2 热力图 v2

当前问题：颜色不直观，粒度不够。

改造：

```
┌────────────────────────────────────────┐
│  活动热力图    [周] [月] [季] [年]      │
│                                          │
│  周次  一  二  三  四  五  六  日        │
│  W27   ■■  ■■  ■■  ■■  ■■  □□  □□    │
│        2h  3h  1h  4h  2h  -   -       │  ← 颜色深浅=总时长
│  W28   ■■  ■■  ■■  ■■  ■■  ■■  □□    │
│        4h  2h  5h  3h  6h  1h  -       │
│  W29   ■■  ■■  ■■  ■■  ■■  ■□  □□    │
│  ...                                    │
│                                          │
│  [🎯 过滤: 全部分类 ▾]                   │
│  显示: 今日 6.2h █ 编程 █ 游戏 █ 会议   │
│                                          │
│  下方: 今日分类饼图                        │
│  ┌──────────────────────┐                │
│  │     🎮 游戏 25%       │                │
│  │   📞                 │                │
│  │  会议   💻 编程       │                │
│  │  10%     40%         │                │
│  │        📄             │                │
│  │   运维 文档           │                │
│  │   15%  10%            │                │
│  └──────────────────────┘                │
└────────────────────────────────────────┘
```

交互：
- 点击格子 → 查看当日详细时间线
- 右上角切换分类过滤（只看编程/只看娱乐）
- 饼图可钻取（点击"游戏"→ 展开子分类占比）

### 8.3 Production / 质量评分仪表盘

新增一个页面/面板：

```
┌───────────────────────────────────────┐
│  今日效率评分: 66%                     │
│                                        │
│  ████████████████████░░░░░░░░░░       │
│  生产性 5.4h     分心 1.2h  其他 1.6h │
│                                        │
│  本周趋势                               │
│  一 ████████████████ 5.2h  72%        │
│  二 ██████████████ 4.1h  65%          │
│  三 ████████████████ 5.8h  78%        │ ← 最好
│  四 ████████████ 3.2h  55%            │ ← 最差
│  五 ██████████████████ 6.8h  82%      │
│                                        │
│  目标: 每天生产性 ≥ 5h                 │
│  ✅ 今天达标                          │
└──────────────────────────────────────┘
```

---

## 9. API 设计

### 9.1 分类树

```
GET  /api/v1/pc/categories          → 分类树（完整层级）
POST /api/v1/pc/categories          → 新建分类
PUT  /api/v1/pc/categories/{id}     → 编辑分类
DELETE /api/v1/pc/categories/{id}   → 删除（有子节点不允许）
PUT  /api/v1/pc/categories/reorder  → 拖拽排序
```

### 9.2 App 知识库

```
GET    /api/v1/pc/app-signatures           → 列表（分页/搜索）
POST   /api/v1/pc/app-signatures           → 手动添加
PUT    /api/v1/pc/app-signatures/{id}      → 编辑
DELETE /api/v1/pc/app-signatures/{id}      → 删除
POST   /api/v1/pc/app-signatures/lookup    → 查询单个进程名（触发互联网查询）
POST   /api/v1/pc/app-signatures/import    → 导入签名库
GET    /api/v1/pc/app-signatures/export    → 导出签名库
```

### 9.3 分类建议

```
GET  /api/v1/pc/classification/suggestions/v2
     → 返回建议列表（含联网识别标记、置信度、进程名匹配度）
POST /api/v1/pc/classification/suggestions/batch-accept
     → 批量接受（[{suggestionId, categoryId, createRule}]）
```

### 9.4 时间线 v2

```
GET /api/v1/pc/timeline/v2?date=2026-07-03
    → 返回按时间线格式化的活动块
    → 格式: [{start, end, app, title, category, productivity, color, confidence}]
```

### 9.5 Productivity

```
GET /api/v1/pc/productivity/daily?date=...
GET /api/v1/pc/productivity/range?start=...&end=...
GET /api/v1/pc/productivity/goals  → 目标设置
PUT /api/v1/pc/productivity/goals  → 保存目标
```

---

## 10. 实施路线

按价值/工作量比排序，建议分 4 个阶段：

### Phase 1：快速见效（1-2 天）

| 任务 | 说明 |
|:--|:--|
| **App 种子库 200+ 条** | 手工整理常见应用的分类映射，覆盖 90% 日常软件 |
| **App 签名表 + 匹配逻辑** | 建表，分类引擎优先匹配签名库 |
| **分类建议显示进程名中文 + 联网标记** | 让用户一眼知道系统"认识"这个应用 |
| **批量接受建议** | 勾选多条，一键分类 |

**效果**：装上后立刻能看到"以撒的结合 → 娱乐→游戏"而不是 "app:isaac-ng（其他）"

### Phase 2：体验提升（2-3 天）

| 任务 | 说明 |
|:--|:--|
| **分类树** | 建 `pc_categories` 表 + 父子层级 + 迁移现有分类 |
| **Productivity 评分** | 给每个分类加 productivity 属性 + 统计 |
| **时间线 v2（Gantt 样式）** | 重写前端时间线组件 |
| **热力图 v2** | 改进颜色 / 过滤 / 交互 |

### Phase 3：智能升级（3-5 天）

| 任务 | 说明 |
|:--|:--|
| **互联网查询** | 实现 `IAppLookupProvider`，遇到未知进程名自动搜索 |
| **建议反馈闭环** | 接受率追踪，拒绝去重，自动静默低质量建议 |
| **浏览器域名分类** | 从窗口标题提取 URL，域名 → 预设分类映射 |

### Phase 4：打磨（持续）

| 任务 | 说明 |
|:--|:--|
| 效率评分看板 | 目标设置 / 趋势 / 提醒 |
| 分享签名库 | 导出/导入社区贡献的 App 映射 |
| 规则条件扩展 | 时间维度、组合条件 |
| AI 辅助（可选） | 如果后续规则引擎不够用，可在建议层接入 AI |
