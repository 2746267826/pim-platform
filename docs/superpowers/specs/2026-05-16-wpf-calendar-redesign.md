# WPF 日历窗口 UI 重新设计

**日期**: 2026-05-16
**参考**: Flutter FlowPlanV2 (`calendar/presentation` + `presentation` 目录)
**技术栈**: WPF .NET 8 + MaterialDesignThemes + CommunityToolkit.Mvvm

## 概述

基于 Flutter FlowPlanV2 的 UI 参考，重新设计 WPF 客户端日历窗口。采用 MaterialDesign 主题库实现现代风格，保持原生 WPF 架构。

## 整体架构

```
MainWindow (Shell)
├── 左侧导航栏 (200px)
│   ├── 导航 ListBox（4 视图：时间轴/本周/月视图/任务）
│   └── 日历本列表（颜色标识 + 可见性切换）
├── 中间主内容区
│   ├── 共享日期导航头
│   └── ContentControl + DataTemplate（动态切换 4 个视图）
│       ├── TimelineView（时间轴）
│       ├── WeekView（本周）
│       ├── MonthView（月视图）
│       └── TaskListView（任务）
└── 右侧面板 (280px) - 所有视图保持可见
    ├── 收件箱（未排程任务列表）
    └── 底部操作按钮（+ 新建 / ⚡ 一键重排）
```

**导航方式**: ListBox 自定义 ItemTemplate + ContentControl 动态切换，选中项用主题色高亮。
**主题**: MaterialDesignThemes NuGet 包，提供圆角、阴影、颜色板。
**MVVM**: 每个视图独立 UserControl + ViewModel，共享数据通过 DI 注入的 Service 层。

## 四个视图

### 1. 时间轴视图（默认视图 - 日视图）

与 `timeline_view.dart` 对应。

- **头部**: 日期显示（"5月16日 星期五"）+ 今日按钮 + 左右箭头
- **日期条**: 14天水平滚动（7天前 + 今天 + 6天后）。今天浅蓝圆高亮，选中日蓝色实心圆，周日红色文字。点击切换日期
- **时间网格**: 左侧时间刻度（00:00-23:00，80px/小时），垂直网格线
- **计划栏 (3/5)**: 日程/任务色块绝对定位。日程用日历本颜色左侧 4px 竖条 + 半透明底色。任务用优先级颜色（高红/中黄/低绿）
- **实际栏 (2/5)**: 保留布局但暂不接入数据，显示"暂未接入数据"占位
- **当前时间线**: 红色横线 + 左侧红色圆点，每 60 秒刷新，初始自动滚动到当前时间
- **色块内容**: 标题 + 时间范围 + 地点/备注（截断显示）
- **点击色块**: 弹出详情编辑对话框

### 2. 本周视图

与 `week_view.dart` 对应。

- **头部**: 周范围显示（"5/10 - 5/16"）+ 年月 + 导航按钮
- **日列头**: 7列，星期简称 + 日期数字。今天浅蓝底，选中日蓝色实心圆 + 白字，周日红色
- **点击日列头**: 跳转到该日的时间轴视图
- **时间网格**: 左侧时间刻度（60px/小时），列内水平网格线
- **色块**: 日程和任务按时间垂直定位（top = 时间 × 60px/h，height = 时长 × 60px/h，最小 20px）
- **共用**: 日期导航头与时间轴视图共享

### 3. 月视图

与 `month_view.dart` 对应。

- **头部**: 年月显示 + 左右翻月箭头（居中）
- **日历网格**: 标准 7列×6行，中文星期头（日～六）。今天蓝色实心圆 + 白字，选中日高亮，周日红色，非当月日灰色
- **标记点**: 每格最多 4 个彩色圆点（日程=日历本颜色，任务=优先级颜色），水平居中排列
- **下方预览列表**: 选中日的事件和任务分区显示
  - "日程"分区 + "任务"分区，各带标签标题
  - 每条：4px 竖线色标 + 标题 + 时间/地点/备注（`·` 分隔）
  - 右侧 `›` 箭头，点击打开详情对话框
- **点击日期格**: 切换选中日，预览区跟随刷新

### 4. 任务视图

全量任务管理列表。

- **头部**: "任务"标题 + 统计摘要（"共 N 个任务 · M 个未排程"）
- **筛选栏**: Chip 切换（全部/未排程/高优先级/今日），右侧搜索框
- **任务列表**: 每行包含优先级圆点 + 标题(含描述) + 任务本标签 + 时长 + 排程时间/截止日期 + 优先级标签 + 状态标签 + 详情箭头
- **状态标签**: 已排程 = 绿色 + 显示排程时间，收件箱 = 灰色 + 显示截止日期（红色）
- **底部**: "+ 新建任务"按钮 + 导出按钮
- **点击行**: 打开任务编辑对话框
- **拖拽到收件箱**: 取消排程

## 详情编辑对话框

以 MaterialDesign Dialog 弹出，宽度 560px，圆角 16px。

### 日程编辑器

日历本选择(Chip 列表) → 标题(大号输入框) → 全天开关 → 开始/结束时间(可点击弹出日期+时间选择器) → 地点 → 备注 → 颜色选择(7色圆点) → 状态(已确认/暂定/已取消 ChoiceChip) → 重复规则(不重复/每天/每周/每月) → 阻挡自动排程开关 → 保存/删除按钮

### 任务编辑器

任务本选择(Chip 列表) → 标题 → 描述 → 地点 → 预计时长(预设 Chip: 15m/30m/1h/1.5h/2h/3h/4h/6h) → 截止时间(可清除) → 优先级(高红/中黄/低绿 ChoiceChip) → 重复 → 提前提醒(准时/5分/15分/30分/1小时) → 自动排程/允许拆分/锁定排程 三个开关 → 保存/删除按钮

## 收件箱面板

与 `unscheduled_task_panel.dart` 对应，280px 宽，始终可见。

- **头部**: 📥 "收件箱 / 未排程" + ⓘ 提示（长按拖拽排程）
- **任务卡片**: 优先级圆点 + 标题(最多2行) + 任务本标签 + 时长 + 截止日期(红色) + 拖拽手柄
- **拖出**: 长按拖拽到时间轴计划栏进行排程（设置 DtStart）
- **拖入**: 从时间轴拖回 → DtStart 清空 → 退回未排程
- **空状态**: "所有任务均已排入日程" + 绿色勾图标
- **底部按钮**: "+ 新建"（蓝色主按钮）、"⚡ 一键重排"（灰色次按钮）

## 拖拽交互

| 操作 | 行为 | 吸附 | 确认 |
|------|------|------|------|
| 收件箱→时间轴 | 设置 DtStart，任务排程 | 15分钟 | 弹出确认对话框 |
| 时间轴内移动 | 修改开始时间 | 15分钟 | 弹出确认对话框 |
| 底部边缘拖拽 | 调整时长（日程改结束时间，任务改预计时长） | 15分钟 | 弹出确认对话框 |
| 时间轴→收件箱 | 清空 DtStart，退回未排程（仅任务） | — | SnackBar 提示 |

## 颜色体系

| 用途 | 颜色 | 色值 |
|------|------|------|
| 主题色 | 蓝色 | `#1565c0` |
| 高优先级 | 红色 | `#E53935` |
| 中优先级 | 橙色 | `#FFA726` |
| 低优先级 | 绿色 | `#43A047` |
| 日历本默认 | 紫色 | `#6B5EE4` |
| 日历本备选 | 青/粉/橙/蓝/绿/红 | 7色调色板 |
| 当前时间线 | 红色 | `#E53935` |
| 今天高亮 | 主题色 12% 透明度 | — |
| 周日文字 | 红色 | `#E53935` |

## ViewModel 结构

```
ViewModels/
├── ShellViewModel          # 主导航 + 日历本列表 + 日期状态
├── TimelineViewModel       # 日视图：事件/任务混合列表 + 拖拽逻辑
├── WeekViewModel           # 周视图：7天事件/任务分组
├── MonthViewModel          # 月视图：日历网格 + 选中日预览
├── TaskListViewModel       # 任务列表：筛选 + 搜索 + 全量管理
├── InboxPanelViewModel     # 收件箱：未排程任务列表
├── EventEditorViewModel    # 日程编辑器
└── TaskEditorViewModel     # 任务编辑器
```

共享状态：`SelectedDate`、`SelectedView`、日历本/任务本列表通过 DI 单例 Service 管理。

## 文件变更计划

### 新建文件
- `Views/Shell/MainWindow.xaml` — Shell 布局重写
- `Views/TimelineView.xaml` — 时间轴视图
- `Views/WeekView.xaml` — 本周视图
- `Views/MonthView.xaml` — 月视图
- `Views/TaskListView.xaml` — 任务视图
- `Views/InboxPanel.xaml` — 收件箱面板
- `Views/EventEditorDialog.xaml` — 日程编辑器
- `Views/TaskEditorDialog.xaml` — 任务编辑器
- `ViewModels/ShellViewModel.cs`
- `ViewModels/TimelineViewModel.cs`
- `ViewModels/WeekViewModel.cs`
- `ViewModels/MonthViewModel.cs`
- `ViewModels/TaskListViewModel.cs`
- `ViewModels/InboxPanelViewModel.cs`
- `ViewModels/EventEditorViewModel.cs`
- `ViewModels/TaskEditorViewModel.cs`

### 修改文件
- `App.xaml` — 添加 MaterialDesign 主题资源
- `App.xaml.cs` — 调整启动流程
- `Pim.Client.App.csproj` — 添加 MaterialDesignThemes NuGet

### NuGet 依赖
- `MaterialDesignThemes` — Material Design 主题
- `MaterialDesignColors` — 颜色板（可选，按需）
