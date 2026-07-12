# src/client-web/src/components/mobile/MobileTimelineBlocks.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：移动使用时间线块 UI——分页、展开块→会话→原始事件三级钻取（数据由父组件注入）。
- 主要依赖：`MobileTimelineBlock`/`Session`/`SessionEvent` 类型、`mobileFormatting`
- 被谁使用：Mobile 分析时间线容器页

## 函数级结构化伪代码

### 常量
#### `pageSizeOptions = [10, 20, 50, 100]`

### `MobileTimelineBlocks(props)`
- 输入：blocks、sessionsByBlock、eventsBySession、expanded ids、分页与加载标志、onToggleBlock/Session、onPageChange/SizeChange
- 输出：分页时间块列表 UI
- 副作用：仅通过回调通知父级（展开/翻页）
- 步骤：
  1. safeTotalPages = max(1, totalPages)；canGoPrevious/Next 由 page 比较。
  2. visiblePages：最多 5 个页码，窗口锚定当前页（page-2 起，夹紧到末尾）。
  3. 头部：标题、总块数、每页 select→onPageSizeChange。
  4. 遍历 blocks：按钮显示本地短时间、lifeCategory、topApps 名、前台时长；点击 onToggleBlock。
  5. 若块展开：列 sessions；空则加载中/暂无；会话按钮 onToggleSession。
  6. 会话展开：列 eventsBySession 事件时间与类型/className。
  7. 全局 isLoading/空块提示。
  8. 底部分页：上一页/页码/下一页，加载中禁用。
- 分支与异常：展开态、加载态、空态、分页边界
- 调用：formatDuration、formatShortTime、sourceLabel

## 近逐行中文伪代码

1. 引入 block/session/event 类型与格式化。
2. Props 含数据字典、展开 id、分页与回调。
3. 计算安全总页数与可见页码窗口。
4. 渲染标题、块总数、每页下拉。
5. 每块一行可点展开；展开后列会话。
6. 会话可再展开看原始事件；支持会话/事件加载文案。
7. 底部显示页码与上下页按钮，加载时禁用。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx",
      "label": "MobileTimelineBlocks",
      "path": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileTimelineBlocks.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileTimelineBlocks.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" }
  ]
}
```
