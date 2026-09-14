# src/client-web/src/context/CalendarVisibilityContext.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：会话内日历图层显隐状态（hiddenCalendarIds Set）与 toggle。
- 主要依赖：React createContext/useState/useCallback
- 被谁使用：日历工作台/图层工具栏

## 函数级结构化伪代码

### CalendarVisibilityProvider
- 输入：children
- 状态：hiddenIds: Set<string>
- 步骤：
  1. toggleCalendar：有则删、无则加，返回新 Set
  2. Provider 下发 hiddenCalendarIds 与 toggleCalendar

### useCalendarVisibility
- 返回 context 值

## 近逐行中文伪代码

1. 默认空 Set 与空 toggle。
2. Provider 持有 hiddenIds。
3. toggle 不可变更新 Set。
4. hook 读取 context。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/context/CalendarVisibilityContext.tsx",
      "label": "CalendarVisibilityContext",
      "path": "src/client-web/src/context/CalendarVisibilityContext.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/context/CalendarVisibilityContext.tsx.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": []
}
```
