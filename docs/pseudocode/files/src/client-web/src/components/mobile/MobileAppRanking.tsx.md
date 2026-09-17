# src/client-web/src/components/mobile/MobileAppRanking.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：手机 App 前台时长排行列表——占比条、启动/会话/最近使用与数据源标签。
- 主要依赖：`MobileAppUsageSummary`、`mobileFormatting`（formatDateTime/Duration/Percent）
- 被谁使用：Mobile 日摘要/分析页

## 函数级结构化伪代码

### `sourceLabel(source)`
- events→事件明细；fallback→回退汇总；其它原样。

### `MobileAppRanking({ apps, totalForegroundSeconds, isLoading })`
- 输入：排行数组、总前台秒、加载标志
- 输出：列表 section
- 副作用：无
- 步骤：
  1. 标题 + App 数量徽章。
  2. isLoading → 加载文案；apps 空 → 暂无数据。
  3. 否则 map 每条 app：
     a. share = total>0 ? foreground/total : app.share。
     b. 序号圆标、displayName/category/package、时长。
     c. 进度条宽度 clamp 3%–100%。
     d. 占比/启动/会话/最近使用；sourceLabel。
- 分支与异常：加载/空/列表三态
- 调用：formatDuration、formatPercent、formatDateTime

## 近逐行中文伪代码

1. 引入 MobileAppUsageSummary 与格式化函数。
2. sourceLabel 映射 events/fallback。
3. 渲染标题与数量；加载或空列表提示。
4. 有数据时按序输出 article：排名、名称、分类、包名、前台时长。
5. 用 share 画蓝条；展示占比、启动次数、会话段数、最近使用、数据源。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileAppRanking.tsx",
      "label": "MobileAppRanking",
      "path": "src/client-web/src/components/mobile/MobileAppRanking.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileAppRanking.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileAppRanking.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileAppRanking.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" }
  ]
}
```
