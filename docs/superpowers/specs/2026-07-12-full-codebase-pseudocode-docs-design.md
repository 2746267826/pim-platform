# 全库伪代码文档与统一关系图设计

> 状态：用户已批准完整设计（方案 A + 10 子代理并行强制约束）。  
> 实现分支建议：按波次使用 `codex/pseudocode-docs-*`（从最新 `master` 开出）。

## Purpose

为仓库内 **全部生产与测试源码**（`src` + `tests`）生成独立、可对照源路径的中文伪代码文档，并交付 **Mermaid 总览/分层子图 + 可交互 HTML 全量关系图**，使读者能在不打开 IDE 的情况下理解控制流与模块连接。

## Decisions Locked With User

| 项 | 决定 |
|----|------|
| 方案 | **A**：人工逐文件通读 + 手写伪代码（禁止未读源码即生成正文） |
| 范围 | `src` + `tests`（约 660+ 手写源文件；排除生成物） |
| 粒度 | **双粒度并存**：① 函数级结构化伪代码 ② 近逐行中文伪代码 |
| 关系图 | **Mermaid 总览 + 分层子图**、**可交互 HTML**、以及二者双份交付 |
| 产出位置 | `docs/pseudocode/` |
| 语言 | 正文简体中文；代码标识符/API/路径保留英文 |
| 并行执行 | **必须使用子代理，且每一波必须同时 10 个子代理并发工作** |

## Goals

1. 每个纳入清单的源文件有一份镜像路径下的伪代码 Markdown。
2. 每份文档同时包含函数级结构说明与近逐行中文伪代码。
3. 关系边可汇总为：系统总览 Mermaid、分层/模块 Mermaid、交互 HTML 全量图。
4. 覆盖进度可追踪（manifest + coverage），支持多会话、多 PR 恢复。
5. 执行时固定 **10 路子代理并行**，总控只分发与合并。

## Non-Goals

- 修改业务源码行为或「顺便重构」。
- 文档化 `bin/`、`obj/`、`node_modules/`、`dist/`、`publish/`、构建产物、锁文件。
- 单张 Mermaid 塞入全部 660 文件节点（全量连接由交互 HTML 承担）。
- 自动 AST 生成伪代码正文替代人工阅读（与方案 A 冲突）。
- 一次会话默认写完全库（未完成时只报进度与下一批入口）。

## Scope Detail

### 纳入扩展名

- 优先：`*.cs`、`*.ts`、`*.tsx`、`*.kt`（Android 手写源）
- `*.js`：仅手写源，排除打包产物
- 测试目录 `tests/**` 与 `src/client-android/**/src/test/**`、`androidTest` 一并纳入（若存在于清单）

### 排除

- `**/bin/**`、`**/obj/**`、`**/node_modules/**`、`**/dist/**`、`**/publish/**`
- `**/.git/**`、生成的 `wwwroot` 构建物、schema 快照 JSON（非逻辑源时可排除）
- 纯配置且无控制流的文件可由总控标为「摘要级」，但仍须通读后写文档（不得跳过清单项）

### 规模基线（设计时点）

| 区域 | 约略文件数 |
|------|------------|
| `src/Pim.Core` | ~22 |
| `src/Pim.Infrastructure` | ~85 |
| `src/Pim.Api` | ~37 |
| `src/modules` | ~162 |
| `src/client-web` | ~144 |
| `src/client-windows` | ~29 |
| `src/client-android` | 以清单为准 |
| `tests` | ~183+ |
| **合计** | **660+（以 B0 生成的 manifest 为准）** |

## Directory Layout

```
docs/pseudocode/
  README.md
  _index/
    file-manifest.md       # 源路径 ↔ 文档路径 ↔ 状态
    coverage.md            # 进度统计与下一批入口
  graphs/
    overview.mmd.md        # layer 级总览 Mermaid
    layers/                # 分层/模块子图
    interactive/
      index.html
      graph-data.json      # 全量 nodes/edges
  files/                   # 与源码相对路径镜像
    src/...
    tests/...
```

### 路径映射

- 源：`src/Pim.Api/Program.cs`
- 文档：`docs/pseudocode/files/src/Pim.Api/Program.cs.md`

## Single-File Document Template

```markdown
# <相对路径>

## 元信息
- 语言 / 程序集或包
- 职责一句话
- 主要依赖（本项目相关 using/import）
- 被谁使用（阅读时积累）

## 函数级结构化伪代码
### <类型名>
#### <方法签名>
- 输入 / 输出 / 副作用
- 步骤（编号）
- 分支与异常
- 调用：外部类型.方法

## 近逐行中文伪代码
（按源码行序；标识符英文；逻辑中文；空行/纯括号可合并）

## 关系边（供总图汇总）
- depends_on: ...
- calls: ...
- implements/extends: ...
- tests: ...（仅测试文件）
- http: ...（客户端 ↔ API 时）
```

### 质量门槛

- **函数级**：每个非平凡方法具备输入/输出/步骤/分支。
- **近逐行**：与源码控制流同序，不跳过业务分支。
- **DTO/Entity**：函数级可简；近逐行按成员写清。
- **测试**：写清 Arrange-Act-Assert 与被测 API；重要分支不可省略。

## Workflow (方案 A)

### 单文件

1. 打开源文件完整通读（imports、类型、全部方法体）。
2. 写元信息 + 函数级结构化伪代码。
3. 按源码顺序写近逐行中文伪代码。
4. 填写关系边。
5. 更新 `_index/file-manifest.md` 与 `coverage.md`（由总控合并时亦可批量勾选）。

### 逻辑批次（PR 切片参考）

| 批次 | 范围 |
|------|------|
| B0 | 脚手架：目录、README、全量 manifest、空图壳、coverage |
| B1 | `Pim.Core` |
| B2 | `Pim.Infrastructure` |
| B3 | `Pim.Api` |
| B4 | `src/modules/*` |
| B5 | `client-windows` |
| B6 | `client-web` |
| B7 | `client-android` 手写源 |
| B8 | `tests`（及客户端测试树） |
| B9 | 统一关系图收尾：overview + 全部分层图 + interactive 全量校验 |

## Mandatory Parallel Execution: 10 Subagents

### Orchestrator（主会话）

- 唯一负责：从 `master` 开分支、生成/锁定文件分区、启动 **恰好 10** 个子代理、合并 coverage/manifest/graph-data、仲裁冲突、开 PR。
- **禁止**总控自己撰写大批伪代码正文（验收与补洞除外：剩余文件 < 10 时可由总控或不足 10 路代理收尾）。

### 每波并发规则（强制）

1. 每一波必须 **同时** 启动 **10** 个子代理（`Task` / 平台等价物）。
2. 文件清单按互斥路径切片均分到 10 槽；不得两代理写同一文档路径。
3. 某槽提前完成：在本波汇合前不得空转浪费——由总控在 **下一波** 重新均分剩余清单；本波内代理只处理分配列表。
4. 仅当 **剩余未完成文件数 < 10** 时，允许并发数等于剩余数。
5. 子代理必须逐文件通读源码后再写文档；返回完成列表、关系边 JSON 片段、阻塞项。

### 默认 10 槽分区（可按波次重切）

| 槽位 | 默认职责 |
|------|----------|
| A1 | `src/Pim.Core` |
| A2 | `src/Pim.Infrastructure` 半区 1 |
| A3 | `src/Pim.Infrastructure` 半区 2 |
| A4 | `src/Pim.Api` |
| A5 | modules：Stats + QuickNotes |
| A6 | modules：Files + Mobile |
| A7 | modules：PcTracker |
| A8 | modules：Calendar + `client-windows` |
| A9 | `client-web` 半区 |
| A10 | `client-web` 另半 / `client-android` / 本波 `tests` 切片 |

波次进入 B8 后，10 槽全部改为 tests 路径均分。B0/B9 以总控 + 必要子代理为主（脚手架与图合成）。

### 子代理输出契约

```json
{
  "slot": "A1",
  "completed": ["src/Pim.Core/..."],
  "docs_written": ["docs/pseudocode/files/src/Pim.Core/....md"],
  "edges": [{ "from": "...", "to": "...", "type": "calls" }],
  "blocked": [],
  "notes": ""
}
```

### 汇合

每波 10 代理全部返回后：

1. 合并文档树（应无路径冲突）。
2. 更新 `coverage.md` / `file-manifest.md`。
3. 合并 `graphs/interactive/graph-data.json` 增量。
4. 更新相关 `graphs/layers/*.md`。
5. 提交并（按批）开 PR；再开下一波 10 代理。

## Relationship Graph Model

### Node

| 字段 | 说明 |
|------|------|
| `id` | 源相对路径 |
| `label` | 类型名或文件名 |
| `path` | 源路径 |
| `doc` | 伪代码文档路径 |
| `layer` | `core` / `infrastructure` / `api` / `module.<name>` / `client-web` / `client-windows` / `client-android` / `tests` |
| `kind` | `entrypoint` / `endpoint` / `service` / `entity` / `dto` / `middleware` / `ui` / `test` / `other` |

### Edge types

| 类型 | 含义 |
|------|------|
| `depends_on` | 依赖 |
| `calls` | 跨类型调用 |
| `implements` / `extends` | 实现 / 继承 |
| `tests` | 测试 → 生产代码 |
| `http` | 客户端 ↔ API |

### Mermaid vs HTML

- **Mermaid overview**：layer 与关键入口，保持可读。
- **Mermaid layers**：层内文件/主要类型 + 跨层关键边。
- **Interactive HTML**：全量 nodes/edges，可搜索、按 layer 过滤；点击关联 doc 路径。

### Consistency

- 边两端必须存在于 manifest。
- `tests` 边只指向 `src/...` 生产代码。
- 跨层边优先进入 overview；细节留在 layer 子图与 HTML。

## Git / PR Policy

- 所有文件变更在非 `master` 的 `codex/pseudocode-docs-*` 分支。
- 不与无关功能分支混提。
- Commit 风格：`docs: pseudocode for <area>`、`docs: pseudocode graph <layer>`。
- 每批/波次 PR 以 `docs/pseudocode/**`（及本 design/plan）为主。
- 推送后等待 GitHub Actions；若仅 docs 且路径过滤未触发，PR 中显式说明。
- 设计规格本文档路径：`docs/superpowers/specs/2026-07-12-full-codebase-pseudocode-docs-design.md`。

## Definition of Done

| 级别 | 条件 |
|------|------|
| 单文件 | 已通读；双粒度齐全；关系边已填；manifest 已勾 |
| 波次 | 10 代理分区列表完成；coverage 更新；graph-data 增量合并 |
| 批次 PR | 对应范围完成 + 相关 layer 图更新 |
| 全库 | B0–B8 清单 100% + B9 总图/交互图节点覆盖全量 manifest |
| 可宣称任务完成 | 全库完成，或用户书面接受阶段性交付 |

## Risks And Mitigations

| 风险 | 缓解 |
|------|------|
| 体量过大 | coverage + 分波 10 并行 + 多 PR |
| Mermaid 渲染上限 | 全量只进 HTML |
| 子代理重复写同一文件 | 互斥路径切片 |
| 与源码漂移 | manifest 锚定路径；本任务不建自动同步 CI |
| 方案 A 耗时长 | 不降低阅读标准；用 10 并行换吞吐 |

## Implementation Order (after plan)

1. 用户审查本规格并批准。
2. `writing-plans` 产出逐步执行计划（含每波 10 代理提示词模板）。
3. B0 脚手架 + 锁定全量 file-manifest。
4. 循环：10 子代理波次 → 合并 → 提交/PR → 直到覆盖 100%。
5. B9 关系图收尾与 verification。

## Spec Self-Check

- [x] 无 TODO/待定占位作为未决需求
- [x] 方案 A 与 10 并行不互相矛盾（并行的是人工阅读任务切片）
- [x] 范围、粒度、图交付、路径、DoD 一致
- [x] 「连接所有代码」由 interactive 全量图满足；Mermaid 分层可读
