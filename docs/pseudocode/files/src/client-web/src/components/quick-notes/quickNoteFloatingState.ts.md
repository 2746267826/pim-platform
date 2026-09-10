# src/client-web/src/components/quick-notes/quickNoteFloatingState.ts

## 元信息
- 语言：TypeScript
- 程序集或包：client-web
- 职责：快速记录浮层 localStorage 键、位置夹紧/加载/保存工具。
- 主要依赖：localStorage
- 被谁使用：QuickNoteFloatingPanel

## 函数级结构化伪代码

### clampPanelPosition(point, viewport, panel)
- 在 margin=12 内夹紧 x/y，保证面板完整可见

### loadPanelPosition
- 默认右下角；读 JSON；非法回退 fallback 并 clamp

### savePanelPosition
- best-effort 写 localStorage

## 近逐行中文伪代码

1. 导出 DRAFT/POSITION key 与 PanelPoint/Size。
2. clamp 计算 maxX/maxY。
3. load 解析 stored；缺省右下。
4. save 忽略异常。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/components/quick-notes/quickNoteFloatingState.ts",
      "label": "quickNoteFloatingState",
      "path": "src/client-web/src/components/quick-notes/quickNoteFloatingState.ts",
      "doc": "docs/pseudocode/files/src/client-web/src/components/quick-notes/quickNoteFloatingState.ts.md",
      "layer": "client-web",
      "kind": "service"
    }
  ],
  "edges": []
}
```
