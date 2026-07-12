# tests/client-web/mobileAnalyticsComponents.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：静态渲染手机分析工作台主要面板，断言中文文案与关键诊断信息完整出现。
- 主要依赖：`MobileAnalyticsHeader`、`MobileInsightStrip`、`MobileUsageHeatmap`、`MobileUsageBucketDetail`、`buildHeatmapMatrix`、`MobileChartsGrid`、`MobileTimelineBlocks`、`MobileAnomalyPanel`、`MobileAppCatalogManager`、`src/client-web/src/api/mobile` 类型
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### test helper
#### test(name, run)
- 输入：名称（忽略）、同步回调
- 输出：无
- 副作用：执行回调
- 步骤：直接调用 `run()`

### 主测试
#### mobile analytics workbench renders real Chinese copy and all major panels
- 输入：固定 fixture（device、overview、buckets、charts、block、session、event、overrides、rules）
- 输出：断言通过
- 副作用：`renderToStaticMarkup` 拼接 HTML
- 步骤：
  1. 从 client-web `package.json` 解析 require，挂载全局 React
  2. 依次渲染 Header / InsightStrip / Heatmap / BucketDetail / ChartsGrid / TimelineBlocks / AnomalyPanel / AppCatalogManager
  3. 断言大量中文标签存在（范围快捷键、热力图、图表、异常、分页等）
  4. 断言包名、异常标题、抖音、原始事件、分页文案；否定「加载更多」「重复小时数字墙」与乱码
- 分支与异常：`assert.equal` 失败抛错
- 调用：各 Mobile* 组件、`buildHeatmapMatrix`

## 近逐行中文伪代码

1. [L1-L23] 导入 assert、path、createRequire、mobile API 类型与各面板组件
2. [L25-L29] 基于 client-web 包解析 React / react-dom/server，写入 globalThis.React
3. [L31-L33] 定义同步 test 包装
4. [L35-L52] fixture：`MobileDevice` Pixel 8
5. [L54-L106] fixture：`MobileAnalyticsOverview`（范围、时长、质量、目标、异常、建议）
6. [L108-L127] fixture：两段热力 bucket
7. [L129-L190] fixture：六类 chart
8. [L192-L233] fixture：timeline block / session / event
9. [L235-L254] fixture：catalog override 与 category rule
10. [L256-L333] 渲染全部面板并 join HTML
11. [L335-L369] 遍历中文文案列表断言 includes
12. [L371-L378] 附加诊断断言与禁止乱码

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileAnalyticsComponents.test.tsx",
      "label": "mobileAnalyticsComponents.test",
      "path": "tests/client-web/mobileAnalyticsComponents.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/mobileAnalyticsComponents.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileInsightStrip.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileUsageBucketDetail.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileChartsGrid.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileAnomalyPanel.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/components/mobile/MobileAppCatalogManager.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsComponents.test.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" }
  ]
}
```
