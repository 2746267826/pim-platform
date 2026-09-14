# tests/client-web/mobileAnalyticsInteractions.test.tsx

## 元信息
- 语言：TypeScript / TSX
- 程序集或包：tests/client-web
- 职责：验证手机分析面板交互回调、热力矩阵合并、目录表单归一，以及 MobileRecordsPage 源码集成点。
- 主要依赖：Mobile* 组件、`mobileFormatting`、`buildHeatmapMatrix`、`MobileRecordsPage.tsx`
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### helpers
#### flattenChildren / textContent / findElement
- 在无 DOM 下遍历 React 元素树找控件

### 测试用例
1. 默认 7d 范围与上海时区 UTC 边界
2. Header 快捷键/自定义/噪声回调
3. ChartsGrid 可过滤行才有 onClick
4. Heatmap 粒度与 bucket 选择
5. 矩阵合并同日同时段多分类
6. Timeline 分页控件（无「加载更多」）
7. Catalog 保存/删除 override 与 rule
8. 表单创建归一 package/priority 并 reset
9. MobileRecordsPage 源码包含查询与共享状态片段，禁止旧 bucket range 写法

## 近逐行中文伪代码

1. [L1-L63] 导入、React 挂载、树遍历工具
2. [L65-L109] fixture device/bucket/override/rule
3. [L111-L123] 7d 日期范围
4. [L125-L171] Header 交互
5. [L173-L204] 图表行按钮性
6. [L206-L259] 热力粒度与矩阵合并
7. [L261-L338] 时间线分页与目录按钮
8. [L340-L431] FakeFormData 创建表单
9. [L433-L470] 页面源码契约

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/mobileAnalyticsInteractions.test.tsx",
      "label": "mobileAnalyticsInteractions.test",
      "path": "tests/client-web/mobileAnalyticsInteractions.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/mobileAnalyticsInteractions.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/MobileChartsGrid.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/MobileAppCatalogManager.tsx", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "tests" },
    { "from": "tests/client-web/mobileAnalyticsInteractions.test.tsx", "to": "src/client-web/src/pages/MobileRecordsPage.tsx", "type": "tests" }
  ]
}
```
