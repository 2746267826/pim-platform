# OneDrive 文件模块 v2 交接文档（2026-09-19，zcode-win 会话产出）

> 本文档供后续接手人（人或 AI 代理）使用。包含：设计决策背景、P1-P4 各阶段准确状态、
> 未完成项（P4b）的详细实施方案与已知坑点、以及本仓库的关键操作约束。
>
> **设计文档（必读）**：[designs/onedrive-files-v2.md](../../designs/onedrive-files-v2.md) —
> 17 个章节，含决策记录 D1-D7、个人版 API 实测结果（§14）、安全清单与分阶段交付。

---

## 1. 背景与核心决策

项目主人决定把「文件」板块从自托管 Nextcloud 切换为 **OneDrive 个人版**，理由是
Nextcloud 太重且融合差；同时已通过 Outlook 同步拿到 Microsoft Graph 的完整授权栈。

评审拍板的核心决策（**后续工作必须遵守**）：

| 议题 | 决策 | 理由 |
|---|---|---|
| 事实源 | OneDrive 为唯一事实源，PIM 永远是客户端 | 消除双向同步冲突 |
| 服务器与内容 | **只存元数据**；内容按需瞬态经过（302 直链 / 现取文本），不落盘 | 带宽与存储成本最低 |
| 搜索档位 | **仅元数据搜索**（D3，P4 轮已强制落实） | 内容级查找交给 Hermes 走 `read_file_text` |
| 向量化 | **无限期推迟**，语义档位已从代码中摘除 | 无 Qdrant 的生产部署曾因此必 500 |
| UI | 方案 A：三栏（文件树 / 列表·网格 / 预览），浅色，对齐全站基调 v1 | 演示稿已评审通过 |
| 附件 | 快速记录等附件上收 OneDrive（P4b 待做） | MinIO 在文件线退役 |
| Nextcloud | 直接退役，无迁移（未真实上线过） | — |

---

## 2. 阶段状态总览

| 阶段 | 内容 | 状态 | PR |
|---|---|---|---|
| **P1** | OneDrive 绑定（MSAL 设备码）+ delta 增量同步 + 文件树元数据 | ✅ **已合并** | [#319](https://github.com/2746267826/pim-platform/pull/319) |
| **P2** | 稳定直链端点、缩略图、预览、文本编辑与快照 | ✅ **已合并** | [#321](https://github.com/2746267826/pim-platform/pull/321) |
| **P3** | 三栏浅色文件页（react-arborist 树 / 列表·网格 / 预览） | ✅ **已合并** | [#322](https://github.com/2746267826/pim-platform/pull/322) |
| **复审 R1** | P1-P3 全面复审修复（并发互斥、令牌锁、安全闸门等） | ✅ **已合并** | [#323](https://github.com/2746267826/pim-platform/pull/323) |
| **复审 R2** | 修复代码自身复审（DI 接线、元数据搜索、上限一致） | ✅ **已合并** | [#325](https://github.com/2746267826/pim-platform/pull/325) |
| **P4a** | `read_file_text` MCP 工具 + OneDrive 写操作映射 + 审计与限流 | 🟡 **PR 已开，CI 全绿，待合并** | [#328](https://github.com/2746267826/pim-platform/pull/328) |
| **P4b** | QuickNotes 附件上 OneDrive + Nextcloud/MinIO/Tika/Qdrant 退役 + 文档 | ⬜ **未完成**（见 §4） | — |

**当前 master 上的实际能力**（P1-P3 + R1 + R2）：

- 绑定 OneDrive（设备码流），每 20 分钟自动 delta 增量同步，文件树元数据落库；
- 三栏文件页：懒加载树、列表/网格、预览（图片/PDF/Office/文本）、文本编辑与快照恢复；
- 稳定直链端点：`/items/{id}/content`、`/thumbnail`（302 到 Graph 预授权直链）；
- 敏感路径保护（`/Secrets/*`、`/Passwords/*`）在**所有**出口生效（含搜索结果）；
- 同步互斥、令牌刷新锁、绑定竞争守卫等并发安全修复。

---

## 3. P4a 内容（PR #328，待合并）

- **`read_file_text` MCP 工具（第 152 个工具）**：瞬态下载 + 按类型抽取
  （文本直读 / docx、pptx 走 BCL zip+XML / pdf 等在 Tika 已配置时增强）。
  `maxBytes` 默认 64KB、封顶 1MB；每用户每分钟 30 次限流；调用写审计。
- **`OneDriveWriteService`**：move / rename / delete（Graph + 本地软删）/
  restore（远端存在性校验）/ upload（≤4MB）/ open-link（Graph webUrl）。
- **端点按 provider 类型分发**：upload/move/rename/delete/open-link 对 OneDrive
  走 Graph；新增 `GET /items/{id}/extracted-text`、`POST /items/{id}/restore`。
- **补齐 R2 遗漏**：同步 DI 单例接线（`OneDriveSyncGate`/`OneDriveTokenCache`
  —— #325 因补丁丢失未含）+ 启动复位卡死的 `syncing` + Graph HttpClient 30s 超时。
- 测试：+17 例；全套 **5172 passed / 0 failed**。

---

## 4. P4b 未完成工作方案（**接手请从这里开始**）

### 4.1 目标

1. **QuickNotes 附件上收 OneDrive**（设计 §10）：附件按 `/PIM/{objectKey}` 约定存入
   用户 OneDrive，objectKey 持久化为 driveItem id；下载走 302 直链。
2. **退役**（设计 §17）：删除 Nextcloud 适配器与遗留端点、移除 MinIO/Tika/Qdrant/
   OnlyOffice 依赖与配置、清理休眠实体（`FileChunk`/`FileIndexJob`/`FileAiResult`/
   `FileSuggestion`）。
3. **文档**：`.env.prod.example` / README 配置参考补 `Files:OneDrive:Tenant` 与
   `Files:SensitivePathPatterns`；新增 files 模块验收文档到 `docs/operations/`；
   设计文档 §16 勾选 P4；补 `backup-restore.md` 对新表 `file_text_snapshots`
   与 files 系表的说明。

### 4.2 已完成但未提交的资产（**可直接复用**）

半成品工作被放弃（编译未通过，原因见 4.4），但以下三个文件是干净的、可直接搬进新分支：

存放位置：`C:\pim-wt\p4b-assets\`

| 文件 | 内容 | 目标路径 |
|---|---|---|
| `IOneDriveAttachmentStore.cs` | 用户级附件存储抽象（`IOneDriveAttachmentStore`，`Guid userId` 参数化） | `src/Pim.Core/Storage/` |
| `OneDriveAttachmentStore.cs` | 文件模块实现：附件按 `/PIM/{objectKey}` 存入 OneDrive，objectKey = driveItem id；读瞬态、删进回收站 | `src/modules/Pim.Module.Files/Services/` |
| `OneDriveQuickNoteObjectStorage.cs` | QuickNotes 侧适配（含 `IQuickNoteDirectLinkStorage` 直链能力接口，从 objectKey 解析 userId） | `src/modules/Pim.Module.QuickNotes/Services/` |

这三份实现的设计要点：QuickNotes 现有的 `IQuickNoteObjectStorage` 接口签名是
`StoreAsync(objectKey, content, contentType, sizeBytes, ct)`（**无 userId**），
适配器通过 objectKey 约定 `quick-notes/{userId:N}/{attachmentId:N}/{fileName}`
解析出 userId——这是当时验证可行的方案。

### 4.3 P4a 之后仍存在的耦合（P4b 必须处理）

P4a 提交时 **`FileOperationService` 仍保留 Nextcloud 适配器依赖**（`IFileProviderAdapter
adapter` 构造参数与 `providerBindings`），其 `MoveAsync`/`RenameAsync`/`DeleteAsync`/
`UploadAsync`/`DownloadAsync`/`ListTrashAsync`/`RestoreTrashAsync`/`ListVersionsAsync`/
`DownloadVersionAsync`/`RestoreVersionAsync`/`RestoreVersionPreviewAsync`/
`BuildOpenLinkAsync` 等仍走 v1 WebDAV 适配器。P4a 的端点层已改为「OneDrive 优先 +
legacy 回退」，因此：

- **OneDrive 数据的全部操作已可用**（走 Graph）；
- legacy 分支对 OneDrive 数据不可达，但**代码仍在**，退役时需要删除；
- `FileOperationService` 需要保留的只有：`ListItemsAsync`、`GetItemAsync`、
  `ListSuggestionsAsync`/`DismissSuggestionAsync`/`AcceptSuggestionAsync`、
  以及私有助手（`LoadItemAsync`、`RecordAuditAsync`、`MapFileItem`、`MapSuggestion`、
  `IsDirectChildPath`、`PathHasFileName`、`NormalizePath`、`NormalizeDisplayName`、
  `LatestIndexStatus`）。

### 4.4 已知坑点（**血泪教训，务必阅读**）

1. **不要用行级文本脚本删除 C# 方法**。本次 P4b 尝试用 Python 按签名行 + 大括号配平
   删除方法，导致：误删仍在使用的私有助手（`MapSuggestion`/`LatestIndexStatus`/
   `IsDirectChildPath`）、截断表达式体方法的分号（`PathHasFileName`）、破坏方法边界。
   **正确做法**：用 Read 看清文件，然后 `Write` 整文件重写，或用 Edit 做精确替换。
   如果一个文件要改超过 3 处，直接整体重写。
2. **`git show <ref>:<path>` 的输出不要写 `/tmp`**：本环境（Git Bash on Windows）
   `/tmp` 与 Python 的路径解析不一致，会出现"文件已写入但读不到"。用仓库外的固定目录
   （如 `C:/pim-wt/tmp-backup/`）。
3. **改了 worktree 一定要 `cd` 进去**：本次曾出现「补丁脚本未 cd 就执行」，
   把 DI 注册与启动复位写进了**主工作区**（`C:\Users\a2746\Desktop\PIM\pim-platform`），
   导致 #325 合并后 master 仍缺 DI 接线，直到 P4a 才补上。
   **每次运行改文件的脚本前，先 `pwd` 确认。**
4. **迁移手工修剪流程**（P1/P2 用过）：`dotnet ef migrations add` 会把 master 上
   **PcTracker 的既有漂移**（4 张表 `pc_browser_site_daily`/`meta`/`tick`、
   `pc_suggestion_feedback`，以及 8 个列）一起吸进新迁移。必须：
   - 手工把新迁移裁剪成只含本模块变更；
   - Designer 与 `PimDbContextModelSnapshot` 用「`git checkout origin/master -- <快照>`
     + 提取本模块实体块拼接」的方式处理；
   - **注意 EF 8 会把一对多关系写成第二个同名 `modelBuilder.Entity(...)` 块**，
     拼接时两个块都要搬（这是 P2 漏掉外键关系块、造成快照与模型永久漂移的原因）；
   - 完成后用「再生成一个临时迁移，确认 Up 中本模块操作为 0」验证自洽，然后删除临时迁移。
5. **PcTracker 迁移欠账仍未清**（4 表 8 列，PR #311 引入）——建议单独开 PR 处理，
   它影响生产库运行时的正确性，且每次生成迁移都会干扰。
6. **EF 全局用户过滤器**：`PimDbContext` 对 `IUserOwnedEntity` 自动挂 `UserId` 过滤；
   新实体必须实现该接口并配置映射（有反射防护测试，漏配直接红）。Hangfire 系统上下文
   （`CurrentUserId == null`）不过滤。

### 4.5 建议的 P4b 实施顺序

1. 新建 worktree（`C:/pim-wt/od-p4b`），分支 `zcode-win/onedrive-p4b-retire`，
   基于 **P4a 合并后的 master**；
2. 搬入 4.2 的三个文件，接线 QuickNotes DI（替换 `MinioQuickNoteObjectStorage` 与
   `NullQuickNoteObjectStorage` 的选择逻辑）与下载端点的 302 分支；
3. 整体重写 `FileOperationService`（按 4.3 的保留清单）与 `FilesModule`（删除遗留
   端点路由与 DI 注册）；
4. 删除 Nextcloud 适配器四件套和 MinIO 存储；清理 compose/env/README 中的
   MinIO/Tika/Qdrant/OnlyOffice 配置；
5. 补文档（4.1 第 3 条）；
6. 测试：改造 `FileEndpointPathTests`（已删除的端点）、QuickNotes 附件测试、
   新增附件走 OneDrive 的路径测试；全套 `dotnet test` + `npm --prefix src/client-web run build`；
7. 开 PR，等 CI 全绿。

---

## 5. 本仓库关键操作约束（本会话验证有效）

| 事项 | 做法 |
|---|---|
| 分支命名 | `{agent}-{os}/{topic}`，本系列用 `zcode-win/*` |
| worktree 根 | Windows：`C:/pim-wt/{topic}`（短名，≤12 字符） |
| 本地工具链 | `dotnet 8.0.421` 可用；`dotnet ef` 可用（8.0.11）；前端需在 worktree 内 `npm install` |
| 前端验证 | `npm --prefix src/client-web run build`、`npm test`（vitest）、`npm run test:files` |
| 后端验证 | `dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj`（全套约 3.5 分钟） |
| 迁移漂移检查 | 生成临时迁移 → 确认本模块操作数为 0 → 删除临时迁移 |
| PR 描述 | 必须含四个双语段落（技术修改 / 功能变化 / 如何体验 / 测试），CI 提取进 Release changelog |
| CI 时长 | build-api 约 8-10 分钟、build-docker 约 6 分钟；文档变更按路径过滤跳过 |
| 并发限制 | 同一会话同时最多 2 个 subagent（超出报 `user concurrency limit exceeded`） |

---

## 6. 复审遗留（已记录，非阻塞）

以下问题在 R1/R2 复审中被识别，评估为可接受或归入后续阶段，**未修复**：

| 项 | 说明 | 归属 |
|---|---|---|
| 内容出口审计 | content/thumbnail/preview 出口只记 ILogger，无 `IAuditLogService`（`read_file_text` 已补审计） | P4b 或后续 |
| 今日页/状态页健康区块 | 设计 §6 要求「同步落后、410 重置、持续 429 可见」，实际未实现（文件页有 chip，状态页无） | 后续 |
| 软删与快照治理 | 软删文件无回收站 UI 入口；快照随 item 硬删级联，无保留期巡检 | 后续 |
| 网格缩略图与灯箱 | 网格视图用类型图标，无真实缩略图与灯箱（`yet-another-react-lightbox` 已装未用） | P3 验收偏差 |
| CORS 真实浏览器验证 | 设计 §14-V6：直链在真实浏览器 fetch 的 CORS 行为未实测（本地测试用 TestServer） | 需真实绑定会话 |
| 断开批量删除性能 | `DisconnectAsync` 全量加载所有 item 逐行删除 | 后续 |
| 双标签页编辑冲突 | 无跨 tab 冲突检测，后写覆盖先写（快照只留一条 pre-edit） | 后续 |

---

## 7. 接手检查清单

1. 读 `AGENTS.md`（仓库规范）与 [`designs/onedrive-files-v2.md`](../../designs/onedrive-files-v2.md)（设计）；
2. `gh pr view 328` 看 P4a 状态：已全绿则合并；
3. 按 §4.5 顺序实施 P4b；**动 `FileOperationService` 前务必读 §4.4 坑点 1**；
4. 顺手评估是否清理 PcTracker 迁移欠账（§4.4 坑点 5）；
5. 每个 PR 遵循四段式双语描述 + CI 全绿 + 合并后清理 worktree/分支。
