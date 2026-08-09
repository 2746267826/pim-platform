# src/Pim.Core/Planning/PlanningEnums.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Core
- 职责：规划域枚举——任务状态、片段状态、习惯节奏、提醒渠道/状态、报告类型、数据来源
- 主要依赖：无（纯枚举定义）
- 被谁使用：规划实体、服务、API DTO 与客户端展示逻辑

## 函数级结构化伪代码

### TaskPlanningState
#### enum TaskPlanningState
- 输入：无
- 输出：任务规划生命周期状态取值
- 副作用：无
- 步骤：
  1. 枚举 Inbox → ToPlan → Planned → InProgress → Waiting/Blocked/Deferred/Paused → Completed/Cancelled
- 分支与异常：无
- 调用：无

### TaskSegmentStatus
#### enum TaskSegmentStatus
- 输入：无
- 输出：任务时间片段状态取值
- 副作用：无
- 步骤：
  1. 枚举 Planned / Active / Paused / Completed / Cancelled
- 分支与异常：无
- 调用：无

### HabitCadence
#### enum HabitCadence
- 输入：无
- 输出：习惯重复节奏取值
- 副作用：无
- 步骤：
  1. 枚举 Daily / Weekly / Monthly / Custom
- 分支与异常：无
- 调用：无

### ReminderChannel
#### enum ReminderChannel
- 输入：无
- 输出：提醒投递渠道取值
- 副作用：无
- 步骤：
  1. 枚举 Web / WindowsToast / AndroidNotification / Email
- 分支与异常：无
- 调用：无

### ReminderStatus
#### enum ReminderStatus
- 输入：无
- 输出：提醒生命周期状态取值
- 副作用：无
- 步骤：
  1. 枚举 Open / Snoozed / Sent / Acknowledged / Dismissed / Failed
- 分支与异常：无
- 调用：无

### ReportKind
#### enum ReportKind
- 输入：无
- 输出：报告类型取值
- 副作用：无
- 步骤：
  1. 枚举 Daily / Weekly / Monthly / Project
- 分支与异常：无
- 调用：无

### PlanningSource
#### enum PlanningSource
- 输入：无
- 输出：规划数据来源取值
- 副作用：无
- 步骤：
  1. 枚举 Manual / Pim / Outlook / Ai / Template / Import
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 声明命名空间 `Pim.Core.Planning`
2. 定义枚举 `TaskPlanningState`：
3.   - `Inbox`：收件箱/未分类
4.   - `ToPlan`：待规划
5.   - `Planned`：已规划
6.   - `InProgress`：进行中
7.   - `Waiting`：等待中
8.   - `Blocked`：阻塞
9.   - `Deferred`：延期
10.   - `Paused`：暂停
11.   - `Completed`：已完成
12.   - `Cancelled`：已取消
13. 定义枚举 `TaskSegmentStatus`：
14.   - `Planned`：已计划
15.   - `Active`：进行中
16.   - `Paused`：暂停
17.   - `Completed`：已完成
18.   - `Cancelled`：已取消
19. 定义枚举 `HabitCadence`：
20.   - `Daily`：每日
21.   - `Weekly`：每周
22.   - `Monthly`：每月
23.   - `Custom`：自定义
24. 定义枚举 `ReminderChannel`：
25.   - `Web`：Web 端
26.   - `WindowsToast`：Windows 通知
27.   - `AndroidNotification`：Android 通知
28.   - `Email`：邮件
29. 定义枚举 `ReminderStatus`：
30.   - `Open`：待处理
31.   - `Snoozed`：已延后
32.   - `Sent`：已发送
33.   - `Acknowledged`：已确认
34.   - `Dismissed`：已关闭
35.   - `Failed`：失败
36. 定义枚举 `ReportKind`：
37.   - `Daily`：日报
38.   - `Weekly`：周报
39.   - `Monthly`：月报
40.   - `Project`：项目报告
41. 定义枚举 `PlanningSource`：
42.   - `Manual`：手工
43.   - `Pim`：PIM 内部
44.   - `Outlook`：Outlook 同步
45.   - `Ai`：AI 生成
46.   - `Template`：模板
47.   - `Import`：导入

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/Pim.Core/Planning/PlanningEnums.cs",
      "label": "PlanningEnums",
      "path": "src/Pim.Core/Planning/PlanningEnums.cs",
      "doc": "docs/pseudocode/files/src/Pim.Core/Planning/PlanningEnums.cs.md",
      "layer": "core",
      "kind": "other"
    }
  ],
  "edges": []
}
```
