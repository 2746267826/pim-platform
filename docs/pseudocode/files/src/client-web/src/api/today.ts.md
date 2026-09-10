# src/client-web/src/api/today.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Today 分区注册表与单分区数据的 GET 客户端封装。
- 主要依赖：`apiGet`、`TodaySection`、`TodaySectionRegistry`、`ApiResponse`
- 被谁使用：Today / 工作台前端页面

## 函数级结构化伪代码

### todayApiPaths
- 输入：date / sectionId
- 输出：路径字符串
- 副作用：无
- 步骤：
  1. `sections(date)` → `/today/sections?date=...`
  2. `section(sectionId, date)` → `/today/sections/{id}?date=...`
- 分支与异常：无
- 调用：`encodeURIComponent`

### getTodaySectionRegistry(date: string)
- 输入：日期字符串
- 输出：`Promise<TodaySectionRegistry>`
- 副作用：GET 分区注册表
- 步骤：`apiGet(todayApiPaths.sections(date))` → data
- 分支与异常：透传
- 调用：`apiGet`

### getTodaySection<TData>(sectionId: string, date: string)
- 输入：分区 id、日期
- 输出：`Promise<TodaySection<TData>>`
- 副作用：GET 单分区
- 步骤：`apiGet(todayApiPaths.section(...))` → data
- 分支与异常：透传
- 调用：`apiGet`

## 近逐行中文伪代码

1. 导出路径工厂 `todayApiPaths`（sections / section）。
2. `getTodaySectionRegistry` 拉当日分区清单。
3. `getTodaySection` 泛型拉指定分区 payload。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/today.ts",
      "label": "today",
      "path": "src/client-web/src/api/today.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/today.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/today.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/today.ts", "to": "src/client-web/src/api/client.ts", "type": "calls" },
    { "from": "src/client-web/src/api/today.ts", "to": "/today/sections", "type": "http" }
  ]
}
```
