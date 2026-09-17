# 伪代码可视化工作台设计

> 状态：用户已批准完整设计（双模式 + 浅色三栏 + 数据流 C + Vite/React 效果优先）。  
> 实现分支建议：`codex/pseudocode-visual-workbench`（从含 `docs/pseudocode` 的最新分支开出）。

## Purpose

把已生成的全库中文伪代码（775 文件）与关系图数据做成**好用、直观、本机性能足够**的浏览器工作台：能读文档、看连接，并能把一条数据处理链路「摘」成可导出的流水线。

## Decisions Locked With User

| 项 | 决定 |
|----|------|
| 主入口 | **双模式一体**：阅读 / 关系图，默认阅读 |
| 阅读布局 | **三栏**：左树 / 中伪代码 / 右关系边 |
| 视觉 | **浅色纸质文档**风 |
| 数据走向 | **方案 C**：类型或 API 双起点 + 流水线画布 + 导出 Markdown |
| 技术取向 | **效果与性能优先**（本机可承受）：Vite + React + TS + WebGL 图库 |
| 交付位置 | `docs/pseudocode/viewer/`（静态构建，可本地 serve） |
| 数据性质 | **结构级**溯源（基于伪代码 md + 静态 edges），非运行时埋点 |

## Goals

1. 2 秒内可搜索并打开任一伪代码文档（本机 SSD 预期）。
2. 775 节点关系图可缩放、拖拽、按 layer 过滤，不掉帧到不可用。
3. 从「类型/文件」或「HTTP/API」起点生成处理流水线，步骤可展开/固定/移除。
4. 流水线一键复制/下载 Markdown。
5. 不修改业务源码与 775 份伪代码正文（只读消费）。

## Non-Goals

- 生产环境实时请求追踪、日志埋点、APM。
- 在线多人协作编辑伪代码。
- 替换 IDE；本工具是文档与结构导航增强。
- 单张图无过滤地强行展示全部 tests 节点（默认可折叠/隐藏 tests）。

## Existing Assets

| 路径 | 用途 |
|------|------|
| `docs/pseudocode/files/**/*.md` | 双粒度伪代码正文（775） |
| `docs/pseudocode/graphs/interactive/graph-data.json` | 全量 nodes/edges（约 775 / 1074） |
| `docs/pseudocode/graphs/overview.mmd.md`、`graphs/layers/*` | 静态 Mermaid 分层图 |
| `docs/pseudocode/graphs/interactive/index.html` | 旧极简 canvas 图（由新工作台入口取代或链过去） |

## Information Architecture

### Top bar

- 模式切换：`阅读` | `关系图`
- 全局搜索：路径、label、API 路径
- layer 过滤芯片
- 主按钮：`摘流水线`

### Read mode (default) — three panes

| 左 | 中 | 右 |
|----|----|----|
| 虚拟滚动文件树（按 layer/目录） | 元信息 + `函数级` / `近逐行` 切换渲染 | 当前文件关系边列表；跳转；`从这里摘流水线` |

### Graph mode

- 主区：WebGL 力导向（可切换简单分层布局）
- 侧栏：选中节点摘要、打开伪代码、上下游
- 与阅读模式共享「当前选中文件」与搜索高亮

### Pipeline canvas (data flow C)

- 入口：顶栏 / 右栏 / 图选中节点
- 起点：file（类型/文件）或 api（http 边）
- 默认：沿 `calls` / `depends_on` / `http` 有限深度 BFS（默认 depth=3，可调 1–6）
- 步骤卡：名称、layer、边类型、职责一行、伪代码要点
- 操作：展开上下游、固定、移除、打开全文、图中高亮
- 导出：复制/下载 Markdown（格式见下）

## Pipeline export format

```markdown
# 数据流水线：<起点标题>
- 生成时间：…
- 起点类型：file | api
- 深度：n

## 步骤
### 1. <文件或 API>
- 关系：from --type--> to
- 职责：…
- 伪代码要点：
  1. …
  2. …

## 关系边清单
| from | type | to |
|------|------|-----|
```

## Tech Stack

| 层 | 选择 |
|----|------|
| 应用 | Vite + React + TypeScript |
| 样式 | 纸质浅色 CSS 变量（非紫渐变、非 Inter 套路） |
| Markdown | markdown-it 或 marked |
| 图 | @antv/g6 或 sigma.js + graphology（WebGL） |
| 树 | 虚拟列表 |
| 流水线 UI | 自研步骤条 + HTML/SVG（不上重型流程图引擎） |

## Directory Layout

```
docs/pseudocode/viewer/
  package.json
  vite.config.ts
  index.html
  public/
    catalog.json              # build-catalog 生成
  scripts/
    build-catalog.mjs         # 扫 files + graph-data → catalog
  src/
    App.tsx
    modes/ReadMode.tsx
    modes/GraphMode.tsx
    modes/PipelineCanvas.tsx
    components/
    lib/catalog.ts
    lib/pipeline.ts
    styles/paper.css
  dist/                       # build 输出（可 gitignore 或按需提交）
```

## Data build

**输入**

- `docs/pseudocode/files/**/*.md`
- `docs/pseudocode/graphs/interactive/graph-data.json`

**输出 `public/catalog.json`（示意）**

```json
{
  "generated": "ISO-8601",
  "nodes": [{ "id", "label", "path", "doc", "layer", "kind", "title" }],
  "edges": [{ "from", "to", "type" }],
  "apiIndex": [{ "path", "method", "nodeId" }],
  "stats": { "nodeCount", "edgeCount", "docCount" }
}
```

**正文策略**

- catalog 不含 775 全文；打开文件时 `fetch` 对应 md（dev 下通过 Vite 静态目录或拷贝到 public/docs-files）。
- 可选：catalog 内缓存「函数级要点」短摘要以加速流水线卡片。

**脚本命令**

```bash
npm run catalog   # node scripts/build-catalog.mjs
npm run dev
npm run build
```

## Performance

- 启动只加载 catalog（索引 + 边）。
- 文件树虚拟滚动。
- 图默认可隐藏 `tests` layer；搜索时高亮子图。
- 流水线子图默认 ≤ ~80 节点；超出提示降深度或裁 layer。
- 大 md 分段渲染（先函数级再按需近逐行）。

## Visual design (paper light)

- 背景：暖纸色（如 `#f7f3eb`），面板近白，细石色边框。
- 正文：高对比深墨；伪代码区等宽字体。
- layer 用克制色点区分，避免整页单色渐变。
- 运动：适度（模式切换、面板展开），不夸张。

## Error handling

- catalog 缺失：明确空态 + 提示运行 `npm run catalog`。
- 单文件 fetch 失败：右栏/中栏错误条，可重试。
- 流水线无边：提示「无静态关系边，可手动加深或换起点」。
- 非法 depth：钳制到 1–6。

## Testing

- 单元：`pipeline` 图遍历（深度、边类型过滤、环检测）。
- 单元：catalog 解析与搜索索引。
- 手工：打开 5 个抽样文件（core/api/module/web/test）；图缩放；两条流水线（file 起点 + api 起点）导出。

## Implementation order

1. scaffold Vite app + paper 三栏壳  
2. `build-catalog.mjs` + 接 graph-data  
3. 阅读模式：树 + md 渲染 + 右栏边  
4. 关系图模式  
5. 流水线画布 + 导出  
6. 性能打磨与 README 使用说明  
7. PR：docs/viewer 相关；说明 docs-only / 如何本地运行  

## Spec Self-Check

- [x] 无未决 TODO 作为需求占位  
- [x] 双模式、三栏、纸质、数据流 C、技术栈一致  
- [x] 非目标排除运行时追踪  
- [x] 与现有 775 文档资产只读对齐  
