# src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：手机使用分析页顶栏——日期快捷/自定义、设备/分类/包名/系统噪声过滤、刷新与错误展示；纯受控 UI。
- 主要依赖：`MobileDevice`、`MOBILE_LIFE_CATEGORIES`（`../../api/mobile`）、`MobileRangeShortcut`（`mobileFormatting`）
- 被谁使用：`pages/MobileRecordsPage.tsx`

## 函数级结构化伪代码

### MobileAnalyticsHeaderProps
#### 字段
- 输入：rangeShortcut、起止日、selectedDeviceId、devices、selectedCategory、packageName、includeSystemNoise、isFetching、errorMessage、on* 回调
- 输出：Props
- 副作用：无
- 步骤：onShortcutChange 排除 custom；custom 经 onCustomRangeChange
- 分支与异常：无
- 调用：无

### MobileAnalyticsHeader(props) 默认导出
- 输入：Props（errorMessage 默认 null）
- 输出：JSX header section
- 副作用：点击/变更触发回调
- 步骤：
  1. 标题「手机记录」与说明
  2. 快捷 today/7d/30d + 自定义（点自定义时传入当前起止日）+ 北京时间 + 刷新文案
  3. 网格：设备 select、分类 MOBILE_LIFE_CATEGORIES、App search、隐藏系统噪声 checkbox（checked=!includeSystemNoise）、粒度固定「小时视图」、起止 date
  4. 有 errorMessage 红框
- 分支与异常：isActive 切换样式；error 条件渲染
- 调用：onShortcutChange/onDeviceChange/onCategoryChange 等

## 近逐行中文伪代码

1. 引入 MobileDevice 与 MOBILE_LIFE_CATEGORIES、MobileRangeShortcut
2. 定义 Props 与 shortcuts 标签
3. 渲染标题与快捷按钮（aria-pressed）
4. 自定义按钮调用 onCustomRangeChange 当前范围
5. 设备/分类/包名/噪声/日期控件绑定 props
6. 噪声 checkbox 反转 includeSystemNoise
7. 可选错误条

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx",
      "label": "MobileAnalyticsHeader",
      "path": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": [
    { "from": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx", "to": "src/client-web/src/api/mobile.ts", "type": "depends_on" },
    { "from": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx", "to": "src/client-web/src/components/mobile/mobileFormatting.ts", "type": "depends_on" },
    { "from": "src/client-web/src/pages/MobileRecordsPage.tsx", "to": "src/client-web/src/components/mobile/MobileAnalyticsHeader.tsx", "type": "calls" }
  ]
}
```
