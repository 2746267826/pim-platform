# 独立验收链路冒烟测试（2026-09-21）

本文件是「PIM Independent Acceptance」独立验收链路的上线冒烟测试产物，用于验证：

1. 独立验收身份（GitHub App `pim-independent-acceptance`，app id `5015717`）可以在 PR 上发布 `PIM Independent Acceptance` Check Run；
2. Check Run 先进入「验收进行中」，验收完成后更新为最终结论；
3. 验收结果绑定 PR 当前 head 提交；PR 产生新提交后，旧验收结果不适用于新提交；
4. 验收结果无法被开发身份伪造（GitHub 仅允许 GitHub App 创建 Check Run）。

本文件无业务含义，可随测试 PR 保留或关闭。

相关约定见《PIM AI 驱动开发质量工作流》（落地后位于 `docs/development/ai-workflow/`）。

---

（二段演示追加：2026-09-21——用于验证 PR 产生新提交后，旧验收结果不再适用于新提交。）

