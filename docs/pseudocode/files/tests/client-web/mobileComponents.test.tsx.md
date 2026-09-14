# tests/client-web/mobileComponents.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：SSR 渲染手机记录/定位/诊断组件，断言中文 UI 与 Leaflet 地图源码。
- 主要依赖：MobileRecordsDashboard、HistoricalLocationDashboard、MobileMetricStrip、LocationPointList、MobileDiagnosticsPanel
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### fixtures
- device、ranking、syncBatch、timeline、quality、locationPoints/tracks、canonical/legacy diagnostics

### 测试
1. 记录仪表盘：使用/排行/同步/质量/fallback 中文
2. MetricStrip 稳定标签与 fallback 模式
3. 历史定位：控件、地图壳、点详情
4. LocationPointList 选中与米制格式
5. formatAccuracyLabel 一位小数
6. 地图源码含 Leaflet tiles/markers
7. Diagnostics 接受规范与 legacy 组件键

## 近逐行中文伪代码

1. [L1-L337] React 挂载与全部 fixture
2. [L339+] dynamic import 组件并 renderToStaticMarkup 断言
3. 读地图组件源码断言 Leaflet 引用

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileComponents.test.tsx",
      "label": "mobileComponents.test",
      "path": "tests/client-web/mobileComponents.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/mobileComponents.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/mobileComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileRecordsDashboard.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileComponents.test.tsx", "to": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileComponents.test.tsx", "to": "src/client-web/src/components/status/MobileDiagnosticsPanel.tsx", "type": "tests" }
  ]
}
```
