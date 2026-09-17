# AGENTS.md 技术确认审查规则 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `AGENTS.md` 的 Communication And Planning 之后、Start Of Every Session 之前，插入设计规范第 8 节的 8 条技术确认审查规则，并更新设计规范当前状态。

**Architecture:** 纯文档修改，不涉及代码、配置或构建系统。修改一个 Markdown 文件（`AGENTS.md`），更新一个 Markdown 文件（设计规范第 9 节）。

**Tech Stack:** Markdown, git.

---

### Task 1: 确认 AGENTS.md 当前状态

**Files:**
- Read: `AGENTS.md`
- Read: `docs/superpowers/specs/2026-07-11-lightweight-development-governance-design.md`

- [ ] **Step 1: 确认目标位置不含审查规则**

  Run: `Select-String -Path "C:\pim-lg\AGENTS.md" -Pattern "技术确认|确认卡|决策审查|审查规则|Technical Decision"`

  Expected: 无输出（表示目标文本不存在）。

- [ ] **Step 2: 确认 Communication And Planning 章节结束行和 Start Of Every Session 起始行**

  Run: `Select-String -Path "C:\pim-lg\AGENTS.md" -Pattern "^## Communication And Planning" -Context 0,8`

  Expected: 上下文中依次出现 Communication And Planning 的 3 条规则和 `## Start Of Every Session`。

- [ ] **Step 3: 确认 AGENTS.md 已有 Parallel Agent Workflow 章节且内容兼容**

  Run: `Select-String -Path "C:\pim-lg\AGENTS.md" -Pattern "^## Parallel Agent Workflow" -Context 0,4`

  Expected: `## Parallel Agent Workflow` 下包含 "Prefer multiple subagents for independent investigation" 等描述。审查规则要求主代理先由独立子代理审查后再向用户确认，这与 "Prefer multiple subagents" 不冲突（审查规则是前置检查，并行工作流是执行方式）。

### Task 2: 修改 AGENTS.md 插入审查规则章节

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 4: 在 Communication And Planning 之后、Start Of Every Session 之前插入规则章节**

  文件：`C:\pim-lg\AGENTS.md`

  当前内容（第 9-11 行）：
  ```
  - For user-facing UI, default visible text to Simplified Chinese. Keep code identifiers, API names, logs, protocol fields, and third-party product names in English unless localization is explicitly part of the task.

  ## Start Of Every Session
  ```

  替换为：
  ```
  - For user-facing UI, default visible text to Simplified Chinese. Keep code identifiers, API names, logs, protocol fields, and third-party product names in English unless localization is explicitly part of the task.

  ## Technical Decision Review

  - **技术决定必须审查后确认**：主代理在向用户确认任何技术实现细节前，必须先由独立子代理审查。完整确认卡模板见[设计规范第 4 节](docs/superpowers/specs/2026-07-11-lightweight-development-governance-design.md#4-通俗确认卡模板)。
  - **通俗确认卡**：审查结果必须按设计规范第 4 节格式输出确认卡，用非专业用户能理解的语言描述候选方案与最简单可行方案的差异。
  - **一次对应一次**：一个技术确认对应一次审查。不可独立选择的机制可合并为同一张确认卡，但不能用整包批准掩盖可独立增删的机制。判断方法：移除某个机制后方案仍能独立工作，则应当单独审查。
  - **建立比较基准**：独立审阅者必须说明最简单可行方案（完全不引入额外机制的代码路径），再指出候选方案额外增加了什么机制、假设解决什么问题、以及开发/测试/维护/故障排查成本。没有仓库证据时不得编造精确天数，只需诚实说明成本是低、中还是高。
  - **审阅者建议，用户决定**：审阅者认为过重时必须给出更简单方案和成本对比，但不能代用户做决定。用户保留最终决定权。
  - **全阶段适用**：本规则不只在 brainstorming 生效。任何阶段（spec 自审、写 plan、实施中方案变更）出现新的技术确认点都触发审查；不得在已确认方案中静默加入新机制。
  - **禁止占位符交付**：范围内功能必须端到端真实可用，禁止假数据冒充真实数据、空按钮/空页面、骨架声称完成、NotImplementedException、"后期再做"。允许真实的 loading/empty/error 状态和明确标注的阶段性子集。
  - **PROJECT_MAP.md 日常维护**：边界、共享能力和验证命令变化时同步更新，不作为审批材料。不存在"地图未更新不得提交"的阻断规则。

  ## Start Of Every Session
  ```

  使用 `apply_patch` 执行精确替换。

### Task 3: 验证 AGENTS.md 修改

**Files:**
- Verify: `AGENTS.md`

- [ ] **Step 5: 验证 8 条规则全部存在**

  Run:
  ```
  $rules = @(
    '技术决定必须审查后确认',
    '通俗确认卡',
    '一次对应一次',
    '建立比较基准',
    '审阅者建议，用户决定',
    '全阶段适用',
    '禁止占位符交付',
    'PROJECT_MAP.md 日常维护'
  )
  $content = Get-Content -Path "C:\pim-lg\AGENTS.md" -Raw -Encoding UTF8
  $missing = @()
  foreach ($r in $rules) {
    if ($content -match [regex]::Escape($r)) {
      Write-Host "$r -- 存在"
    } else {
      $missing += $r
    }
  }
  if ($missing.Count -gt 0) { throw "缺失规则: $($missing -join ', ')" }
  ```

  Expected: 8 条规则全部显示"存在"。

- [ ] **Step 6: 确认新章节在正确位置**

  Run: `Select-String -Path "C:\pim-lg\AGENTS.md" -Pattern "^## Technical Decision Review"`

  Expected: 匹配到一行，确认章节标题存在。

  Run: `Select-String -Path "C:\pim-lg\AGENTS.md" -Pattern "^## Communication And Planning" -Context 0,15`

  Expected: 输出中 Communication And Planning 之后新章节前的内容。确认 Technical Decision Review 紧跟在 Communication And Planning 之后、Start Of Every Session 之前。

- [ ] **Step 7: 确认修改与现有 Parallel Agent Workflow 不冲突**

  Run: `Select-String -Path "C:\pim-lg\AGENTS.md" -Pattern "技术决定必须审查后确认" -Context 0,2`

  Expected: 确认规则文本要求"主代理在向用户确认任何技术实现细节前，必须先由独立子代理审查"。这与 `## Parallel Agent Workflow` 的 "Prefer multiple subagents for independent investigation" 一致，且审查规则补充了在向用户展示前的审查时机要求，不覆盖或修改并行工作流的行为。

### Task 4: 更新设计文档当前状态

**Files:**
- Modify: `docs/superpowers/specs/2026-07-11-lightweight-development-governance-design.md`

- [ ] **Step 8: 更新设计规范第 9 节 "当前状态"**

  文件：`C:\pim-lg\docs\superpowers\specs\2026-07-11-lightweight-development-governance-design.md`

  先读取第 9 节实际内容，然后使用 `apply_patch` 做两项定向修改，避免覆盖 PROJECT_MAP 计划可能已经写入的状态：

  ```diff
   **已完成的：**
   - 规则的设计和讨论，产出本文档。
  +- AGENTS.md 的开发规则补充（第 8 节的草案已写入 AGENTS.md 的 `Technical Decision Review` 章节）。

   **尚未实施的：**
  -- AGENTS.md 的开发规则补充（第 8 节的草案尚未写入 AGENTS.md）。
  ```

  使用两个独立 patch hunk：新增 hunk 只以“规则的设计和讨论”这一行作为上下文；删除 hunk 只匹配 AGENTS.md 对应的未实施整行。不要把 `**已完成的：**`、`**尚未实施的：**` 或 PROJECT_MAP 状态行纳入 hunk 上下文。这样无论 PROJECT_MAP 计划是否先执行，都不会覆盖或依赖它的状态。

- [ ] **Step 9: 验证设计规范修改**

  Run: `Select-String -Path "C:\pim-lg\docs\superpowers\specs\2026-07-11-lightweight-development-governance-design.md" -Pattern "已写入 AGENTS.md" -Encoding UTF8`

  Expected: 匹配到一行，确认更新生效。

### Task 5: 只读审阅

**Files:**
- Review: `AGENTS.md`
- Review: `docs/superpowers/specs/2026-07-11-lightweight-development-governance-design.md`

- [ ] **Step 10: 由独立只读审阅者核对新规则与设计规范第 8 节一致性**

  使用与修改者不同的只读审阅者。Codex 根代理按现有 AGENTS.md 先读取 `opencode-worker-router` 配置，再使用配置中的 read-only agent 和 cheap tier；其他执行环境使用等价的独立只读子代理。只提供 `AGENTS.md` 新章节、设计规范第 8 节和当前 diff，要求审阅者逐条检查以下映射，并报告 Critical / Important / Minor 问题：

  逐条对比 `AGENTS.md` 新章节的 8 条规则与设计规范第 8 节（第 237-252 行）的 8 条草案：

  1. `技术决定必须审查后确认` vs 设计规范第 8.1 条 — 一致，额外链接了确认卡模板来源。
  2. `通俗确认卡` vs 第 8.2 条 — 一致，补充了"最简单可行方案与候选方案差异"。
  3. `一次对应一次` vs 第 8.3 条 — 一致，补充了"移除某机制后仍可独立工作"的判断方法。
  4. `建立比较基准` vs 第 8.4 条 — 一致，补充了开发/测试/维护/故障排查成本和成本等级说明。
  5. `审阅者建议，用户决定` vs 第 8.5 条 — 一致。
  6. `全阶段适用` vs 第 8.6 条 — 一致，补充了"spec 自审、写 plan、实施中方案变更"的具体例子。
  7. `禁止占位符交付` vs 第 8.7 条 — 一致，补充了允许的例外情况。
  8. `PROJECT_MAP.md 日常维护` vs 第 8.8 条 — 一致，补充了"不存在'地图未更新不得提交'阻断规则"。

  Expected: 8 条与设计规范完全一致，无 Critical 或 Important 问题；如有则先修正文档并重新审阅。

- [ ] **Step 11: 在差异分支上运行 git diff --check**

  Run:
  ```powershell
  git diff --check
  ```

  Expected: 无输出（无空白错误）。如有输出或非零退出码，修正对应空白错误后重新运行；不得跳过。

### Task 6: 提交

**Files:**
- Commit: `AGENTS.md`
- Commit: `docs/superpowers/specs/2026-07-11-lightweight-development-governance-design.md`

- [ ] **Step 12: 提交修改**

  Run:
  ```powershell
  git add AGENTS.md docs/superpowers/specs/2026-07-11-lightweight-development-governance-design.md
  git commit -m "docs: add technical decision review rules"
  ```

  Expected: 成功创建提交，显示 summary。
