# tests/client-web/locationAnalyticsComponents.test.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：tests/client-web
- 职责：SSR 渲染 `HistoricalLocationDashboard` 断言中文工作台文案基线；静态检查 Leaflet 地图组件与 CSS 标记样式关键字。
- 主要依赖：`HistoricalLocationDashboard`、`mobile` API 类型、`react`/`react-dom/server`（经 client-web package）、`node:assert`/`fs`/`path`
- 被谁使用：Node 测试脚本直接执行

## 函数级结构化伪代码

### 夹具数据
#### device / overview / tracks / points
- 步骤：构造 MobileDevice、AnalyticsOverview、Track(含 move segment path)、LocationPoint

### test(name, run)
- 步骤：同步执行 run（无 runner 包装）

### 用例 1：historical location dashboard renders accepted Chinese workbench baseline
- 输入：完整 props（7d 范围、选中 segment/point）
- 输出：无
- 副作用：`renderToStaticMarkup`
- 步骤：
  1. 渲染 Dashboard
  2. 断言 HTML 含「历史位置/今天/7天/…/轨迹地图/原始点明细」等中文与 GPS
  3. 断言不含「定位点列表」「选中点详情」
  4. 断言含 `aria-label="结束日期"`
- 分支与异常：缺文案 assert 失败
- 调用：`React.createElement`、`renderToStaticMarkup`

### 用例 2：historical location map renders segment layers and marker styles
- 步骤：读 Leaflet 源与 index.css；断言 Polyline/selectedSegmentId/颜色/marker class 存在

## 近逐行中文伪代码

1. [L1-16] 导入组件与从 client-web 解析 React
2. [L18-20] 简易 test 辅助
3. [L22-138] device/overview/tracks/points 夹具
4. [L140-201] SSR 中文工作台文案与反例
5. [L203-227] Leaflet/CSS 源码关键字契约

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/locationAnalyticsComponents.test.tsx",
      "label": "locationAnalyticsComponents.test",
      "path": "tests/client-web/locationAnalyticsComponents.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/locationAnalyticsComponents.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/locationAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "type": "tests" },
    { "from": "tests/client-web/locationAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx", "type": "tests" },
    { "from": "tests/client-web/locationAnalyticsComponents.test.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "tests/client-web/locationAnalyticsComponents.test.tsx", "to": "src/client-web/src/index.css", "type": "depends_on" }
  ]
}
```
