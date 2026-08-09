# src/client-web/src/utils/pcBusinessDay.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：PC 业务日工具：按业务日边界计算日期区间与展示。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### getPcBusinessDate
#### getPcBusinessDate(date = new Date()
- 输入：date = new Date(
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `getPcBusinessDate`
  2. 赋值 `businessDate` = new Date(date)
  3. 若 (businessDate.getHours() < PC_BUSINESS_DAY_START_HOUR) 则
  4. 执行：businessDate.setDate(businessDate.getDate() - 1);
  5. 返回 businessDate
- 分支与异常：if (businessDate.getHours() < PC_BUSINESS_DAY_START_HOUR) {
- 调用：getPcBusinessDate、Date、businessDate.getHours、businessDate.setDate、businessDate.getDate

### pcHourLabel
#### pcHourLabel(hour: number)
- 输入：hour: number
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `pcHourLabel`
  2. 返回 `${String(hour).padStart(2, '0')}:00`
- 分支与异常：无显著分支
- 调用：pcHourLabel、String、padStart

### getPcBusinessDayStart
#### getPcBusinessDayStart(date: Date)
- 输入：date: Date
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 导出函数 `getPcBusinessDayStart`
  2. 赋值 `start` = new Date(date)
  3. 若 (start.getHours() < PC_BUSINESS_DAY_START_HOUR) 则
  4. 执行：start.setDate(start.getDate() - 1);
  5. 执行：start.setHours(PC_BUSINESS_DAY_START_HOUR, 0, 0, 0);
  6. 返回 start
- 分支与异常：if (start.getHours() < PC_BUSINESS_DAY_START_HOUR) {
- 调用：getPcBusinessDayStart、Date、start.getHours、start.setDate、start.getDate、start.setHours

## 近逐行中文伪代码

1. [L1] 导出符号 `PC_BUSINESS_DAY_START_HOUR`
2. [L3] 导出符号 `PC_BUSINESS_HOURS`
3. [L4] 执行：{ length: 24 },
4. [L5] 执行：(_, index) => (index + PC_BUSINESS_DAY_START_HOUR) % 24,
5. [L8] 导出函数 `getPcBusinessDate`
6. [L9] 赋值 `businessDate` = new Date(date)
7. [L10] 若 (businessDate.getHours() < PC_BUSINESS_DAY_START_HOUR) 则
8. [L11] 执行：businessDate.setDate(businessDate.getDate() - 1);
9. [L13] 返回 businessDate
10. [L16] 导出函数 `pcHourLabel`
11. [L17] 返回 `${String(hour).padStart(2, '0')}:00`
12. [L20] 导出函数 `getPcBusinessDayStart`
13. [L21] 赋值 `start` = new Date(date)
14. [L22] 若 (start.getHours() < PC_BUSINESS_DAY_START_HOUR) 则
15. [L23] 执行：start.setDate(start.getDate() - 1);
16. [L25] 执行：start.setHours(PC_BUSINESS_DAY_START_HOUR, 0, 0, 0);
17. [L26] 返回 start

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/utils/pcBusinessDay.ts",
      "label": "getPcBusinessDate",
      "path": "src/client-web/src/utils/pcBusinessDay.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/utils/pcBusinessDay.ts.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": []
}
```
