# src/client-web/src/api/ai.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Web 端 AI 网关 API 封装：状态、测试、健康检查、请求日志分页/详情、用量汇总。
- 主要依赖：`./client` 的 `apiGet`/`apiPost`、`../types` 中 ApiResponse/PagedResult/Ai* 类型
- 被谁使用：AI 相关页面与组件（状态面板、请求表、用量概览等）

## 函数级结构化伪代码

### aiApiPaths
- 常量路径：`/ai/status`、`/ai/test`、`/ai/requests`、`requestDetail(id)`、`/ai/usage/summary`、`/ai/health-check`

### AiRequestFilters
- 可选：module/purpose/model/status/page/pageSize

### query(params)
- 输入：过滤器对象
- 输出：`?k=v&...` 或空串
- 副作用：无
- 步骤：URLSearchParams 跳过 undefined/空串；有内容则前缀 `?`
- 分支与异常：无
- 调用：`URLSearchParams`

### getAiStatus / runAiTest / runAiHealthCheck
- 输入：无
- 输出：`response.data`（状态或测试结果）
- 副作用：HTTP GET/POST
- 步骤：对应路径调用 apiGet/apiPost 后取 data
- 分支与异常：透传 client 错误
- 调用：`apiGet`/`apiPost`

### getAiRequests(filters)
- 输入：AiRequestFilters
- 输出：分页 `AiRequestLogListItem`
- 副作用：GET
- 步骤：`requests + query(filters)` → data
- 分支与异常：透传
- 调用：`apiGet`、`query`

### getAiRequestDetail(id)
- 输入：请求 ID
- 输出：`AiRequestLogDetail`
- 副作用：GET
- 步骤：`requestDetail(id)` → data
- 分支与异常：透传
- 调用：`apiGet`

### getAiUsageSummary()
- 输入：无
- 输出：`AiUsageSummary`
- 副作用：GET
- 步骤：usageSummary 路径 → data
- 分支与异常：透传
- 调用：`apiGet`

## 近逐行中文伪代码

1. 导出路径表与过滤器接口
2. query：拼查询串，忽略空值
3. getAiStatus / runAiTest / runAiHealthCheck：GET/POST 后返回 data
4. getAiRequests：带过滤分页列表
5. getAiRequestDetail / getAiUsageSummary：详情与用量汇总

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/ai.ts",
      "label": "aiApi",
      "path": "src/client-web/src/api/ai.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/ai.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/ai.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/ai.ts", "to": "/ai/status", "type": "http" },
    { "from": "src/client-web/src/api/ai.ts", "to": "/ai/test", "type": "http" },
    { "from": "src/client-web/src/api/ai.ts", "to": "/ai/requests", "type": "http" },
    { "from": "src/client-web/src/api/ai.ts", "to": "/ai/usage/summary", "type": "http" },
    { "from": "src/client-web/src/api/ai.ts", "to": "/ai/health-check", "type": "http" },
    { "from": "src/client-web/src/components/ai/AiUsageOverview.tsx", "to": "src/client-web/src/api/ai.ts", "type": "depends_on" }
  ]
}
```
