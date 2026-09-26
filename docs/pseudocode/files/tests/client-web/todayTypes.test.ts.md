# tests/client-web/todayTypes.test.ts

## 元信息
- 语言：TypeScript
- 程序集或包：tests/client-web
- 职责：构造 Today 区块注册表/区块/任务数据样例并断言关键字段。
- 主要依赖：`src/client-web/src/types` 的 Today* 类型
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### 模块顶层
- 步骤：status=warning；registry.sections calendar.tasks；CalendarTasksTodayData；TodaySection 泛型绑定；assert kind/count/error

## 近逐行中文伪代码

1. [L1-7] 导入类型
2. [L9-40] 构造 registry/section
3. [L42-44] 断言

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/todayTypes.test.ts",
      "label": "todayTypes.test",
      "path": "tests/client-web/todayTypes.test.ts",
      "doc": "docs/pseudocode/files/tests/client-web/todayTypes.test.ts.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/todayTypes.test.ts", "to": "src/client-web/src/types/index.ts", "type": "tests" }
  ]
}
```
