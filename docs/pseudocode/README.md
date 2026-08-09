# PIM 全库伪代码文档

- 规格：`docs/superpowers/specs/2026-07-12-full-codebase-pseudocode-docs-design.md`
- 计划：`docs/superpowers/plans/2026-07-12-full-codebase-pseudocode-docs.md`
- 清单：`_index/file-manifest.md`
- 进度：`_index/coverage.md`（**775 / 775 = 100%**）
- 总览图：`graphs/overview.mmd.md`
- 分层图：`graphs/layers/`
- 交互图：`graphs/interactive/index.html`（`graph-data.json`：775 nodes / 1074 edges）

## 约定
- 方案 A：逐文件通读后手写
- 双粒度：函数级 + 近逐行
- 每波 10 子代理并行
- 路径映射：`src/X.cs` → `files/src/X.cs.md`

## 使用
1. 查清单与进度：`_index/file-manifest.md`、`_index/coverage.md`
2. 读某文件伪代码：`files/<源相对路径>.md`
3. 浏览关系：打开 `graphs/interactive/index.html`（需本地 HTTP 或允许 file 协议读 JSON）
4. 分层静态图：`graphs/overview.mmd.md` 与 `graphs/layers/*.md`
