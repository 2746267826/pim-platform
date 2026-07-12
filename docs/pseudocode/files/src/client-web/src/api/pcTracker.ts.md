# src/client-web/src/api/pcTracker.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：Web 端 PC Tracker API 客户端：摘要/时间线/热力图/明细/质量归一化、分类规则与建议、生产力与分类树、时间线 v2 等 HTTP 封装。
- 主要依赖：`./client`（apiGet/Post/Put/Delete）、`../types` 中 PC/分类/质量类型
- 被谁使用：PC 相关页面与组件（分类、质量、热力图、生产力等）

## 函数级结构化伪代码

### 查询类 API
#### getPcSummary / getPcTimeline / getPcHeatmap / getPcHeatmapGrid / queryPcDetail
- 输入：日期或起止、dimension、DetailQueryParams
- 输出：`ApiResponse` 的 `.data`（Promise）
- 副作用：HTTP GET
- 步骤：拼 query → `apiGet` → then 取 data；`queryPcDetail` 过滤空值后 URLSearchParams
- 分支与异常：委托 client 错误处理
- 调用：`apiGet`

### 质量归一化
#### textOrEmpty / normalizeHealthStatus / getHealthStatusLabel / normalizeQualityLabel / normalizeDetails / normalizeQualityComponent / normalizeQualityIssue / normalizePcQuality
- 输入：未知原始 API 载荷
- 输出：强类型 `PcQualityResponse` 及子结构
- 副作用：无
- 步骤：
  1. 数字/数字字符串 → Unknown/Healthy/Warning/Critical；合法名保留；否则 Unknown
  2. label 空或为状态枚举名时改用中文状态标签
  3. details 非对象数组时变 `{}`；值转字符串
  4. components/issues/nextSteps 非数组则空数组；nextStep 可 null
- 分支与异常：畸形输入降级为空串/Unknown/空数组
- 调用：内部辅助函数

#### getPcQuality(params?)
- 输入：可选查询参数
- 输出：归一化后的 `PcQualityResponse`
- 副作用：HTTP GET `/pc/quality`
- 步骤：拼 query；`apiGet`；`normalizePcQuality(r.data)`
- 分支与异常：原始类型 unknown
- 调用：`apiGet`、`normalizePcQuality`

### 分类与建议
#### getPcCategories / savePcCategory / deletePcCategory
- 输入：规则字段或 id
- 输出：规则列表/单条/删除结果
- 副作用：GET/POST/DELETE `/pc/categories`
- 步骤：标准 api* 包装
- 分支与异常：无额外
- 调用：`apiGet`/`apiPost`/`apiDelete`

#### pcClassificationApiPaths / pcActivityAnalysisApiPath
- 输入：date/id/blockMinutes
- 输出：路径字符串常量对象或路径
- 副作用：无
- 步骤：模板字符串
- 分支与异常：无
- 调用：无

#### getActivityClassificationRules / Suggestions / reject / accept / preview / apply / settings / saveSettings / getRecentActivityProjectTags / getPcActivityAnalysis
- 输入：规则、范围、建议 id、分钟数、日期等
- 输出：对应 DTO Promise
- 副作用：HTTP GET/POST/PUT
- 步骤：路径取自 `pcClassificationApiPaths` 或字面量；body 透传
- 分支与异常：委托 client
- 调用：`apiGet`/`apiPost`/`apiPut`

### Phase 2 类型与 API
#### CategoryTreeNode / CategorySaveRequest / getCategoryTree / saveCategory / deleteCategory / seedCategories
- 输入：树节点保存请求或 id
- 输出：树/节点/消息
- 副作用：categories tree/seed HTTP
- 步骤：GET tree；POST save；DELETE id；POST seed
- 分支与异常：无
- 调用：api*

#### ProductivityDashboard / DailyProductivity / getProductivityDashboard / getProductivityRange
- 输入：date 或 start/end
- 输出：仪表盘或日序列
- 副作用：GET productivity
- 步骤：query 参数
- 分支与异常：无
- 调用：`apiGet`

#### TimelineV2Item / getTimelineV2
- 输入：date
- 输出：v2 时间线条目数组
- 副作用：GET `/pc/timeline/v2`
- 步骤：apiGet then data
- 分支与异常：无
- 调用：`apiGet`

## 近逐行中文伪代码

1. 从 client 引入 apiGet/Post/Put/Delete；从 types 引入大量 PC/分类/质量类型
2. getPcSummary/Timeline/Heatmap/HeatmapGrid：GET 对应 `/pc/*` 路径
3. queryPcDetail：过滤空参数后 GET `/pc/detail`
4. 健康状态数字映射表、合法名 Set、中文 labels
5. Raw* 类型放宽 status/message 为 unknown
6. textOrEmpty：null/undefined → '' 否则 String
7. normalizeHealthStatus：数字/数字串/枚举名 → PimHealthStatus
8. normalizeQualityLabel：空或枚举名用中文标签，否则原 label
9. normalizeDetails：仅普通对象条目转字符串
10. normalizeQualityComponent/Issue：字段兜底与 severity 归一
11. normalizePcQuality：组装 overall/label/message/checkedAt/components/issues/nextSteps
12. getPcQuality：GET 后 normalize
13. getPcCategories；导出 classification 路径表与 activity-analysis 路径函数
14. 规则/建议/预览/应用/设置/最近项目标签/活动分析 系列函数
15. save/deletePcCategory（旧规则 API）
16. Phase2：CategoryTree 类型与 tree/save/delete/seed
17. Productivity 类型与 dashboard/range
18. TimelineV2 类型与 getTimelineV2

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/pcTracker.ts",
      "label": "pcTracker",
      "path": "src/client-web/src/api/pcTracker.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/pcTracker.ts.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "src/client-web/src/types", "type": "depends_on" },
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "/pc/summary", "type": "http" },
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "/pc/quality", "type": "http" },
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "/pc/classification/rules", "type": "http" },
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "/pc/categories", "type": "http" },
    { "from": "src/client-web/src/api/pcTracker.ts", "to": "/pc/productivity/dashboard", "type": "http" },
    { "from": "src/client-web/src/components/pc-classification", "to": "src/client-web/src/api/pcTracker.ts", "type": "calls" }
  ]
}
```
