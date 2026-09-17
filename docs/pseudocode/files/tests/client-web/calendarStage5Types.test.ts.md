# tests/client-web/calendarStage5Types.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：类型级契约测试：构造日历 Stage5 相关类型样例，并用 `@ts-expect-error` 确保 `EventResponse` 不含 raw ICS 字段。
- 主要依赖：`src/client-web/src/types` 中日历/事件/任务类型
- 被谁使用：TypeScript 编译/类型检查流水线

## 函数级结构化伪代码

### 模块级样例构造（无导出函数）
#### 类型样例赋值
- 输入：无运行时输入
- 输出：编译期类型通过；非法字段赋值应报错
- 副作用：无（`void` 引用防止 unused）
- 步骤：
  1. 构造 `CalendarOperationSample` 样例
  2. 构造 `CalendarDeletePreviewResponse`（含 samples、严格确认）
  3. 构造 `CalendarOperationResult`
  4. 构造 `CalendarRecycleBinItem`
  5. 构造 `CalendarRestorePreviewResponse`（含 conflicts、canRestoreWithoutConflict=false）
  6. 构造完整 `EventResponse` 字段（无 sourceIcsComponent）
  7. 构造 `ImportReport` 含 skippedReasons 与 samples
  8. 构造 `TaskResponse`
  9. `@ts-expect-error` 试图写入 `event.sourceIcsComponent`（应被类型系统拒绝）
  10. void 引用各对象避免 unused 诊断
- 分支与异常：类型错误在编译期由 `@ts-expect-error` 捕获
- 调用：无运行时调用

## 近逐行中文伪代码

1. [L1-10] 从 client-web types 导入日历/事件/任务类型
2. [L12-19] operationSample 样例
3. [L21-30] deletePreview 样例
4. [L32-39] operationResult 样例
5. [L41-51] recycleItem 样例
6. [L53-71] restorePreview 含冲突
7. [L73-90] EventResponse 完整字段（无 ICS 原文）
8. [L92-106] ImportReport 样例
9. [L108-116] TaskResponse 样例
10. [L118-119] ts-expect-error：禁止 sourceIcsComponent
11. [L121-126] void 保留引用

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/calendarStage5Types.test.ts",
      "label": "calendarStage5Types.test",
      "path": "tests/client-web/calendarStage5Types.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/calendarStage5Types.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/calendarStage5Types.test.ts", "to": "src/client-web/src/types", "type": "tests" },
    { "from": "tests/client-web/calendarStage5Types.test.ts", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
