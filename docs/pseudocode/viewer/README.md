# Pseudocode Viewer（伪代码工作台）

本地 Web 工作台：浏览 `docs/pseudocode` 下的逐文件伪代码、关系图与摘流水线。基于 **结构级（structure-level）** 文档与边关系，**不是**运行时调用追踪（runtime tracing）。

## 前置

- Node.js（建议 18+）
- 在仓库根目录已生成伪代码文档（`docs/pseudocode/files/`、`_index/` 等）

## 安装与命令

在本目录执行：

```bash
cd docs/pseudocode/viewer
npm install
```

| 命令 | 说明 |
|------|------|
| `npm run catalog` | 扫描伪代码目录，生成 `public/catalog.json`（及构建用副本） |
| `npm run dev` | 先跑 catalog，再启动 Vite 开发服务器 |
| `npm run build` | 先跑 catalog，再 `tsc -b` + Vite 生产构建 → `dist/` |
| `npm run preview` | 预览已构建的 `dist/`（需先 `build`） |
| `npm test` | 运行 vitest 单元测试 |

典型流程：

```bash
npm install
npm run dev          # 开发：自动 catalog + 热更新
# 或
npm run catalog
npm run build
npm run preview      # 本地预览生产包
```

`dev` / `build` 都会先执行 `catalog`；单独改了 `docs/pseudocode` 源文档时，也可手动 `npm run catalog` 再刷新。

## 功能

### 阅读（Read）

- 左侧文件树：按 catalog 节点浏览，支持搜索与 layer 过滤
- 中间文档区：渲染对应 `files/<path>.md` 伪代码（函数级 / 近逐行等 section）
- 右侧边面板：查看当前文件的结构依赖 / 被依赖边，可跳转到相关文件

### 关系图（Graph）

- 基于 catalog 中的 nodes / edges 做结构关系可视化（G6）
- 可点选节点，并切回阅读模式打开对应伪代码
- 边表示文档化的结构关系，**非** profiler / 调用栈采样

### 摘流水线（Pipeline）

- 从当前文件或边关系出发，按结构边扩展「摘流」路径
- 用于串读相关伪代码切片，辅助理解模块链路
- 同样是 **structure-level** 图遍历，不是线上请求或进程级 runtime trace

## 范围说明

| 是 | 否 |
|----|----|
| 伪代码 Markdown 阅读 | 源码调试器 |
| 文档边关系图 | 运行时 call graph / 动态 tracing |
| catalog 驱动的离线工作台 | 连接真实 API / 进程 |

数据来自 `docs/pseudocode` 的手写/生成文档与索引，反映**代码结构与文档关系**，不保证与某一时刻进程行为一致。

## 目录提示

- 应用源码：`src/`
- catalog 脚本：`scripts/build-catalog.mjs`
- 构建产物：`dist/`（勿手改；由 `build` 生成）
- 上游伪代码：`../files/`、`../_index/`、`../graphs/`
