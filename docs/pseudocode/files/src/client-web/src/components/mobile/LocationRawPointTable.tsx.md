# src/client-web/src/components/mobile/LocationRawPointTable.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：展示当前定位片段内的原始点表格；支持选中高亮与点击回调。
- 主要依赖：`MobileLocationPoint`、`mobileFormatting.formatDateTime`、`locationFormatting` 标签/坐标格式化
- 被谁使用：历史定位/片段明细工作台

## 函数级结构化伪代码

### LocationRawPointTableProps
- points、selectedPointId?、onSelectPoint?

### LocationRawPointTable(props)
- 输入：点列表与选择态
- 输出：表格 section
- 副作用：点击触发 onSelectPoint
- 步骤：
  1. 头部：标题「原始点明细」+ 说明 + 点数徽章
  2. points 空 → 「选择片段后显示原始点。」
  3. 否则 table：列 时间/来源/误差/质量/坐标
  4. 每行 map：key=id；onClick 选中；选中行 bg-blue-50
  5. 单元格：formatDateTime(recordedAtUtc)；provider/sourceKind 标签；精度/质量标签；formatCoordinate
- 分支与异常：无
- 调用：formatDateTime、providerLabel、sourceKindLabel、formatAccuracyLabel、locationQualityLabel、formatCoordinate

## 近逐行中文伪代码

1. 接收 points 与选中回调
2. 头部展示点数
3. 无数据提示；有数据渲染五列表格
4. 行点击 onSelectPoint；选中高亮
5. 用 formatting 模块格式化时间/来源/误差/质量/坐标

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/LocationRawPointTable.tsx",
      "label": "LocationRawPointTable",
      "path": "src/client-web/src/components/mobile/LocationRawPointTable.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/LocationRawPointTable.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/LocationRawPointTable.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationRawPointTable.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/LocationRawPointTable.tsx", "to": "src/client-web/src/components/mobile/locationFormatting.ts", "type": "depends_on" }
  ]
}
```
