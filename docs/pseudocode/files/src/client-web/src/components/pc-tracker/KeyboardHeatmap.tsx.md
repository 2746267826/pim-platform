# src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx

## 元信息
- 语言：TypeScript/React
- 程序集或包：client-web
- 职责：键鼠统计热力图：完整键盘布局着色 + SVG 鼠标区域 + 快捷键列表。
- 主要依赖：`KeystatsSummary`（types）
- 被谁使用：PC Tracker 键鼠面板

## 函数级结构化伪代码

### keyColor(count, max)
- 输入：计数、最大值
- 输出：rgb 颜色
- 步骤：0 或 max0 → 最浅 teal；按 ratio 在 TEAL_STOPS 线性插值 RGB

### textColor(count, max)
- 输出：计数 > 0.42*max 用白字，否则深灰

### normalizeKey / normalizeKeyCountEntries
- 安全解析 topKeys 与 keyPressCounts 字典为 SafeKeyCount 数组

### key / KEYBOARD_CLUSTERS / aliasesFor / countForKey / isModifier
- 键盘规格工厂；Main/Navigation/Numpad 三簇布局；按键别名求和；修饰键判定

### KeyCap
- 空白 label → 占位宽；修饰键固定灰底；其余按热力着色；hover/focus tooltip

### KeyboardCluster
- 渲染簇名与各行 KeyCap

### MouseZone / MouseHeatmap
- SVG 区域按点击数着色；总点击标注

### KeyboardHeatmap (default export)
- 输入：keystats 可空
- 步骤：
  1. 空数据用零值骨架
  2. 优先完整 keyPressCounts，否则 topKeys
  3. Map 计数、maxKey、含 `+` 的快捷键排序
  4. 横向滚动容器：三簇键盘 + 鼠标
  5. 无数据 amber 提示；有快捷键则列表

## 近逐行中文伪代码

1. 定义 teal 色带与修饰键集合。
2. 热力色插值、对比文字色。
3. 规范化未知结构的按键计数。
4. 构建完整 101 键风格布局与别名表（含小键盘/方向键）。
5. KeyCap/MouseZone 可视化单键与鼠标区。
6. 主组件合并计数源、算 max、渲染键盘+鼠标+快捷键条。
7. keystats 为空时仍显示布局骨架并提示无数据。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx",
      "label": "KeyboardHeatmap",
      "path": "src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/pc-tracker/KeyboardHeatmap.tsx", "to": "src/client-web/src/types/index.ts", "type": "depends_on" }
  ]
}
```
