# src/client-web/src/components/mobile/LocationPointList.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：移动端历史定位点列表 + 选中点详情；支持点击回调与选中高亮。
- 主要依赖：`MobileLocationPoint`、`mobileFormatting.formatDateTime`、`locationFormatting` 标签函数
- 被谁使用：历史位置相关 Dashboard/地图页

## 函数级结构化伪代码

### PointDetail({ point })
- 输入：可选定位点
- 输出：详情 section
- 副作用：无
- 步骤：
  1. 无 point → 提示「请选择一个定位点」
  2. 有 point → dl 展示记录/提交时间、误差、提供方、来源、质量、坐标
- 分支与异常：未选中空态
- 调用：`formatDateTime`、`formatAccuracyLabel`、`providerLabel`、`sourceKindLabel`、`locationQualityLabel`、`formatCoordinate`

### LocationPointList({ points, selectedPointId, onSelectPoint })
- 输入：点数组、选中 id、选择回调
- 输出：列表 + 详情布局
- 副作用：点击触发 `onSelectPoint`
- 步骤：
  1. selectedPoint = 按 id 查找，否则 `points[0]`
  2. 列表头显示点数；空列表提示
  3. 映射按钮项：高亮选中；展示时间/精度/提供方/来源/质量
  4. 下方 `PointDetail`
- 分支与异常：空数组；未传 onSelectPoint 时可选链不调用
- 调用：`PointDetail`、格式化函数

## 近逐行中文伪代码

1. 引入 MobileLocationPoint、formatDateTime、locationFormatting 若干
2. 导出 LocationPointListProps
3. PointDetail：无点空态；有点网格字段
4. LocationPointList：解析 selectedPoint（id 匹配或首项）
5. 列表区标题「定位点列表」+ 计数徽章
6. 无数据提示；有数据 ol 按钮列表
7. 选中样式 border-blue；点击 onSelectPoint?.(id)
8. 渲染 PointDetail(selectedPoint)

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/LocationPointList.tsx",
      "label": "LocationPointList",
      "path": "src/client-web/src/components/mobile/LocationPointList.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/LocationPointList.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/LocationPointList.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationPointList.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "calls" },
    { "from": "src/client-web/src/components/mobile/LocationPointList.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "calls" }
  ]
}
```
