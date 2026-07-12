# tests/client-web/locationAnalyticsInteractions.test.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：tests/client-web
- 职责：无 DOM 环境模拟 React 树，验证历史定位仪表盘控件回调；并静态检查 HistoricalLocationPage 使用 analytics API 与 7 天默认。
- 主要依赖：`HistoricalLocationDashboard`、`LocationRawPointTable`、`LocationStayMoveTimeline`、mobile API 类型、node:assert/fs
- 被谁使用：Node 测试脚本

## 函数级结构化伪代码

### helpers
#### test / flattenChildren / textContent / findElement
- 输入：节点与谓词
- 输出：执行用例或找到元素
- 副作用：无（find 失败抛错）
- 步骤：
  1. `test` 直接 run
  2. 展平 children；递归拼 text
  3. 深度优先 find，找不到抛 Error
- 分支与异常：sibling 继续搜索
- 调用：无

### fixtures
#### device / overview / tracks / points
- 输入：无
- 输出：固定样例数据
- 副作用：无
- 步骤：构造设备、概览、轨迹段、原始点
- 分支与异常：无
- 调用：无

### test: dashboard controls callbacks
#### 交互模拟
- 输入：组件 props 与收集数组
- 输出：assert 回调序列
- 副作用：调用 onClick/onChange
- 步骤：
  1. 渲染 Dashboard 树
  2. 点「30天」→ shortcut 30d
  3. 改开始/结束日期 → 两次 custom range
  4. 切换「隐藏已拒绝点」→ includeRejected true（checked false 的取反语义由组件决定，测试期望 true）
  5. Timeline 选 segment；Table 选 point
- 分支与异常：元素缺失抛错
- 调用：组件函数式调用

### test: page source contract
#### 静态源码包含/排除
- 输入：HistoricalLocationPage.tsx 文本
- 输出：assert includes / excludes
- 副作用：读文件
- 步骤：必须含 analytics API 与 7d 默认；不得含 getMobileLocationHistory / startOfTodayInput
- 分支与异常：无
- 调用：`readFileSync`

## 近逐行中文伪代码

1. [L1-18] 导入组件/类型；从 client-web 解析 React 挂 global
2. [L20-54] 轻量 test 与 React 树遍历工具
3. [L56-164] device/overview/tracks/points 夹具
4. [L166-241] 仪表盘控件与时间线/点表回调断言
5. [L243-264] 页面源码契约：analytics API 与北京 7 天默认

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/client-web/locationAnalyticsInteractions.test.tsx",
      "label": "locationAnalyticsInteractions.test",
      "path": "tests/client-web/locationAnalyticsInteractions.test.tsx",
      "doc": "docs/pseudocode/files/tests/client-web/locationAnalyticsInteractions.test.tsx.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/client-web/locationAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/HistoricalLocationDashboard.tsx", "type": "tests" },
    { "from": "tests/client-web/locationAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/LocationRawPointTable.tsx", "type": "tests" },
    { "from": "tests/client-web/locationAnalyticsInteractions.test.tsx", "to": "src/client-web/src/components/mobile/LocationStayMoveTimeline.tsx", "type": "tests" },
    { "from": "tests/client-web/locationAnalyticsInteractions.test.tsx", "to": "src/client-web/src/pages/HistoricalLocationPage.tsx", "type": "tests" },
    { "from": "tests/client-web/locationAnalyticsInteractions.test.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" }
  ]
}
```
