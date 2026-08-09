# src/client-web/src/api/mobile.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：移动端 Web API 客户端——路径构造、查询串、DTO 类型与 GET/POST/PUT/DELETE 封装。
- 主要依赖：`./client`（apiGet/apiPost/apiPut/apiDelete）、`../types`、`mobileAnalyticsCopy` 中的生命周期类别标签
- 被谁使用：mobile 各面板组件、历史位置/分析页面

## 函数级结构化伪代码

### 查询与路径辅助
#### `withQuery(path, entries)`
- 输入：基础 path、`[key, value]` 列表
- 输出：带 query 的 path（无有效参数则原 path）
- 步骤：跳过 undefined/null/空串；`URLSearchParams` 拼接。

#### `pathSegment(value)`
- 输出：`encodeURIComponent(value)`。

#### `withAnalyticsQuery` / `withLocationAnalyticsQuery`
- 将 `MobileAnalyticsQuery` / `MobileLocationAnalyticsParams` 映射为固定键列表后调用 `withQuery`。

### 常量与类型
#### `MOBILE_DEFAULT_TIMEZONE` / `MOBILE_LIFE_CATEGORIES` / 联合类型
- 默认时区 `Asia/Shanghai`；生命类别来自 copy 模块；粒度 hour|30m|15m|day。

#### 接口族（节选语义）
- 设备 `MobileDevice`；日摘要 `MobileSummary` + `MobileAppUsageSummary`；时间线 session/fallback；位置点/历史/轨迹/段；质量 `MobileQuality`；分析 overview/heatmap/charts/timeline-blocks/sessions/events；目录覆盖与类别规则；用量目标。

### `mobileApiPaths`
- 输入：各路径工厂参数
- 输出：相对 API 路径字符串
- 步骤：集中定义 devices、summary、timeline、location/*、quality、analytics/*、apps/*、goals 等。

### HTTP 封装函数
#### 读类（均 `apiGet` 后取 `r.data`）
- `getMobileDevices`、`getMobileSummary`、`getMobileTimeline`
- 位置：`getMobileLocationHistory`、overview/tracks/segment/segmentPoints
- `getMobileQuality`
- 分析：overview、heatmap（别名 `getMobileHeatmap`）、charts、timelineBlocks、blockSessions、sessionEvents
- 目录/规则/目标：list getters

#### 写类
- `saveMobileAppCatalogOverride` → PUT by packageName
- `deleteMobileAppCatalogOverride` → DELETE
- `createMobileAppCategoryRule` → POST；`update` → PUT；`delete` → DELETE
- `saveMobileUsageGoal` → POST；`deleteMobileUsageGoal` → DELETE
- 副作用：网络请求；无本地持久化
- 分支与异常：错误由 `client` 层 Promise reject 上抛

## 近逐行中文伪代码

1. 引入 client HTTP 与类型；从 mobileAnalyticsCopy 取生命类别标签。
2. withQuery 过滤空值建查询串；pathSegment URL 编码路径段。
3. 导出默认时区、生命类别与 Analytics/Location 查询类型。
4. withAnalyticsQuery / withLocationAnalyticsQuery 固定键映射。
5. mobileApiPaths 定义全部 /mobile/* 端点工厂。
6. 大量 export interface：设备、摘要排行、时间线、位置点/轨迹/段、质量、分析指标、热力图、图表、时间块分页、会话/事件、目录覆盖、类别规则、用量目标。
7. get* 函数统一 apiGet + 解包 data；写操作用 apiPut/apiPost/apiDelete。
8. getMobileHeatmap 为 getMobileAnalyticsHeatmap 别名。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/api/mobile.ts",
      "label": "mobile",
      "path": "src/client-web/src/api/mobile.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/api/mobile.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/api/mobile.ts", "to": "src/client-web/src/api/client.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/mobile.ts", "to": "src/client-web/src/components/mobile/mobileAnalyticsCopy.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationHistoryMap.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileAppRanking.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/api/mobile.ts", "to": "/mobile/*", "type": "http" }
  ]
}
```
