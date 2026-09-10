# src/client-web/src/components/mobile/mobileHeatmapMatrix.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：将 `MobileHeatmapBucket[]` 聚合为按本地日×24 小时的热力矩阵，含分类切片与质量标志。
- 主要依赖：`MobileHeatmapBucket`（api/mobile）
- 被谁使用：`MobileUsageHeatmap` 等热力图 UI

## 函数级结构化伪代码

### 类型
- HeatmapCategorySlice：lifeCategory + foregroundSeconds
- HeatmapMatrixCell：日/时、UTC 区间、前台秒、qualityFlags、categories、sourceBuckets
- HeatmapMatrixDay：localDate/label/cells
- HeatmapMatrix：hours(0-23)/days/maxSeconds

### localTodayKey()
- 输入：无
- 输出：Asia/Shanghai 的 `YYYY-MM-DD`
- 副作用：无
- 步骤：Intl en-CA formatToParts 拼年-月-日
- 分支与异常：缺 part 返回 `''`
- 调用：`Intl.DateTimeFormat`

### dateLabel(localDate)
- 输入：`YYYY-MM-DD`
- 输出：`M月D日 今天|周X`
- 副作用：无
- 步骤：拆分 → UTC Date；与 localTodayKey 比较决定「今天」或 weekday 数组
- 分支与异常：无
- 调用：localTodayKey

### emptyCell(localDate, localHour)
- 输入：日、时
- 输出：零值单元格
- 副作用：无
- 步骤：返回默认字段（UTC null、秒 0、空数组）
- 分支与异常：无
- 调用：无

### buildHeatmapMatrix(buckets)
- 输入：热力桶列表
- 输出：HeatmapMatrix
- 副作用：无
- 步骤：
  1. Map\<date, Cell[24]\>
  2. 遍历 bucket：localHour 越界 skip；无日则初始化 24 空 cell
  3. 取对应 hour cell：保留首次 startUtc，更新 endUtc；累加 foregroundSeconds；push sourceBuckets
  4. 合并 qualityFlags 去重；按 lifeCategory 累加 categories 并按秒降序
  5. days 按 localDate 排序；label=dateLabel
  6. hours=0..23；maxSeconds=max(1, 所有 cell 秒)
- 分支与异常：非法 hour 跳过
- 调用：emptyCell、dateLabel

## 近逐行中文伪代码

1. 定义 cell/day/matrix 与分类切片类型
2. localTodayKey 用上海时区算「今天」键
3. dateLabel 生成中文月日与今天/周几
4. emptyCell 建 24 小时空槽
5. buildHeatmapMatrix：按日聚合小时桶，合并时长/标志/分类，算 maxSeconds

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts",
      "label": "mobileHeatmapMatrix",
      "path": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/mobileHeatmapMatrix.ts.md",
      "layer": "client-web",
      "kind": "other"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileUsageHeatmap.tsx", "to": "src/client-web/src/components/mobile/mobileHeatmapMatrix.ts", "type": "calls" }
  ]
}
```
