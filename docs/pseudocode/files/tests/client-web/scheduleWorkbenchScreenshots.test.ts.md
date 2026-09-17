# tests/client-web/scheduleWorkbenchScreenshots.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：静态检查日程工作台截图目标路由在布局与页面中具备面板骨架。
- 主要依赖：`AppLayout.tsx`、`TodayPage`/`CalendarPage`/`TaskListPage`/`HabitsPage` 源文件文本
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 顶层脚本
#### screenshot route skeleton
- 输入：固定路由 `/today|/calendar|/tasks|/habits`
- 输出：断言通过
- 副作用：同步读源文件
- 步骤：
  1. 读 AppLayout 与四页源码
  2. 每路由：布局含路径；页面含 `pim-panel`
  3. 断言各页特色组件/文案：日程任务工作台、CalendarLayerToolbar、TaskHierarchyPanel、HabitRoutineEditor
- 调用：`readFileSync`

## 近逐行中文伪代码

1. [L1-L2] 导入 assert 与 readFileSync
2. [L4-L9] screenshotTargets 四路由
3. [L11-L17] 读布局与四页
4. [L19-L22] 循环断言路由与 pim-panel
5. [L24-L27] 断言页面特征字符串

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/scheduleWorkbenchScreenshots.test.ts",
      "label": "scheduleWorkbenchScreenshots.test",
      "path": "tests/client-web/scheduleWorkbenchScreenshots.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/scheduleWorkbenchScreenshots.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/scheduleWorkbenchScreenshots.test.ts", "to": "src/client-web/src/layout/AppLayout.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchScreenshots.test.ts", "to": "src/client-web/src/pages/TodayPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchScreenshots.test.ts", "to": "src/client-web/src/pages/CalendarPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchScreenshots.test.ts", "to": "src/client-web/src/pages/TaskListPage.tsx", "type": "tests" },
    { "from": "tests/client-web/scheduleWorkbenchScreenshots.test.ts", "to": "src/client-web/src/pages/HabitsPage.tsx", "type": "tests" }
  ]
}
```
