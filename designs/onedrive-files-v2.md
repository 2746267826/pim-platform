# 文件模块 v2 — OneDrive 直链版设计方案

> 状态：已评审定稿（2026-09-19，与项目主人逐项确认）。
> 决策背景：Nextcloud 未真实上线即退役；文件板块转为 OneDrive（个人版）中心，
> 服务器不落文件内容，主打「文件树 + 元数据 + 预览 + 稳定直链」，并向 MCP（Hermes）供数。

## 目录

1. [目标与非目标](#1-目标与非目标)
2. [核心决策记录](#2-核心决策记录)
3. [总体架构](#3-总体架构)
4. [认证设计](#4-认证设计)
5. [数据模型变更](#5-数据模型变更)
6. [同步设计（delta 增量）](#6-同步设计delta-增量)
7. [稳定直链端点](#7-稳定直链端点)
8. [预览与编辑矩阵](#8-预览与编辑矩阵)
9. [搜索设计（仅元数据）](#9-搜索设计仅元数据)
10. [快速记录附件迁移到 OneDrive](#10-快速记录附件迁移到-onedrive)
11. [Web UI 设计（方案 A 浅色）](#11-web-ui-设计方案-a-浅色)
12. [MCP 工具面](#12-mcp-工具面)
13. [安全与隐私清单](#13-安全与隐私清单)
14. [OneDrive 个人版 API 事实验证清单](#14-onedrive-个人版-api-事实验证清单)
15. [测试策略](#15-测试策略)
16. [分阶段交付与验收](#16-分阶段交付与验收)
17. [退役清单](#17-退役清单)

---

## 1. 目标与非目标

### 目标

- 把「文件」板块做成好用的 **OneDrive 查看器 / 预览器 / 直链分发器**：文件树、元数据、列表/网格、预览、一键打开 OneDrive。
- 服务端**只存元数据**（文件树、id、大小、时间、类型、缩略图标记），内容永远留在 OneDrive。
- 下载与预览一律走 Graph 预授权直链（短时效），由 PIM 的稳定端点 302 转发。
- 快速记录等模块的附件上收到 OneDrive，客户端用 PIM 稳定 URL 加载。
- 向 Hermes（MCP）提供文件树检索与按需读取文本的能力。

### 非目标（明确不做）

- **不做内容索引 / 向量化 / 全文检索**：服务器与文件内容零接触（除按需瞬态读取），不引入 Tika、Qdrant、嵌入模型。
- **不做迁移**：Nextcloud 无真实数据，直接退役。
- **不在 PIM 内造 Office 编辑器**：编辑一律跳 OneDrive Web / Office 网页端。
- **不做双向同步**：OneDrive 是唯一事实源，PIM 只是客户端。

---

## 2. 核心决策记录

| # | 议题 | 决策 | 理由 |
|---|---|---|---|
| D1 | 网盘选型 | OneDrive 个人版（Graph API） | 已有 Outlook 的 MSAL/Graph 全套基建；甩掉 Nextcloud 自托管负担 |
| D2 | 服务器与内容的关系 | 只存元数据；内容按需瞬态经过（302 直链 / 现取文本），不落盘 | 带宽与存储成本最低；事实源唯一 |
| D3 | 搜索档位 | 仅元数据搜索（文件名 / 文件夹 / 路径 / 类型 / 时间） | 内容级查找交给 Hermes 走 `read_file_text` 现取；避免为未验证的需求建索引管线 |
| D4 | 向量化 | 无限期推迟，留接口不留实现 | 文档规模与真实需求未出现；PostgreSQL 全文索引作为将来的中间档位备选 |
| D5 | UI 方案 | 方案 A：三栏（文件树 / 列表·网格 / 预览面板），**浅色**，shadcn 式组件 + react-arborist 文件树 | 与全站基调一致；树导航对深层目录效率最高（演示稿已评审通过） |
| D6 | 附件归属 | 快速记录等附件上收 OneDrive（`/PIM/...` 约定目录） | MinIO 在文件线上退役；附件获得版本历史与直链分发 |
| D7 | 迁移 | 无（Nextcloud 直接退役） | 未真实上线，无数据 |

---

## 3. 总体架构

```
OneDrive（个人版，唯一事实源）
   │  Microsoft Graph
   │  ├─ /me/drive/root/delta        增量同步（元数据）
   │  ├─ /drive/items/{id}/thumbnails  缩略图
   │  ├─ @microsoft.graph.downloadUrl 预授权直链（~1h）
   │  ├─ 上传会话（attachment 上传）
   │  └─ OAuth（MSAL，与 Outlook 同套基建）
   ▼
PIM Files v2（只存元数据）
   ├─ 同步层：Hangfire 定时 delta 轮询 → FileItemEntity（文件树）
   ├─ 直链层：稳定端点鉴权 + 敏感路径检查 → 302 到新鲜直链
   ├─ 附件层：上传会话转发（小文件直传）→ 落 OneDrive /PIM/ 目录
   └─ 出口：Web 三栏文件页 + MCP 工具（树检索 / 直链 / 现取文本）
```

关键性质：**PIM 永远是 OneDrive 的客户端**，不存在双向同步冲突；所有编辑发生在 OneDrive / Office 网页端，PIM 通过 delta 轮询感知变化。

---

## 4. 认证设计

- 复用 Outlook 已有 MSAL 基建：`MsalPublicClientAdapter`（设备码流）、`OutlookTokenCacheStore`（加锁 token 缓存）。
- 授权范围：在现有 scope 基础上追加 `Files.ReadWrite.All` + `offline_access`，需要一次增量同意。
- 建独立的 OneDrive 绑定实体与授权会话（不复用 Outlook binding 行），token 存入现有加密 token 缓存——**token 只进加密存储，不进日志、不进 WebView、不进任何上传物**（仓库安全红线 B5）。
- `FileProviderEntity` 增加 Graph 字段：`drive_id`、`account_id`、`delta_link`、`delta_reset_at`、`provider_type = "onedrive"`。
- 旧的 `BaseUrl / Username / AppPasswordSecret` 字段保留列但标记 deprecated（Nextcloud 适配器删除时一并清理迁移）。

---

## 5. 数据模型变更

### 保留 / 重塑

- `FileProviderEntity`：如上加 Graph 字段；`Provider` 字段值域变为 `onedrive`（`nextcloud` 进入退役流程）。
- `FileItemEntity`：**改为 id 中心模型**（Graph 原生思维）：
  - `external_file_id` = driveItem `id`（不变，已是主键映射）；
  - 新增 `parent_external_file_id`（构树，替代按 path 推导）、`drive_path`（展示用冗余路径，同步时由父链生成）、`ctag`（变更检测，替代 etag 语义）；
  - 保留 `name / item_type / mime_type / size / modified_at / is_deleted` 等。
- `FileVersionEntity`：**休眠保留**（个人版版本 API 支持面待验证，见 §14；验证通过后再接线）。

### 休眠 / 清理

- `FileChunkEntity`、`FileIndexJobEntity`、`FileAiResultEntity`、`FileSuggestionEntity`：随「不做内容索引 / AI」决策进入休眠——本设计**不删除**（避免大迁移），代码路径不引用；在 Files v2 稳定后单独 PR 清理表与实体。
- `IFileProviderAdapter` 及 `NextcloudFileProviderAdapter`：接口按 Graph id 模型重塑为 v2 契约（见 §6），Nextcloud 适配器直接删除（无历史包袱）。

### 基础设施依赖变化

- **MinIO：文件线上退役**（内容不落服务器 + 附件上 OneDrive）。文本编辑快照等小体量需求改存 PostgreSQL（见 §8）。
- **Tika / Qdrant：文件线上退役**（不部署、不配置；compose 中移除或标注 optional）。

---

## 6. 同步设计（delta 增量）

- **调度**：Hangfire recurring job，每 15~30 分钟一次；保留手动触发端点（立即同步）。
- **增量**：`GET /me/drive/root/delta`，按 `deltaLink` 游标走：
  - `created` / `updated` → upsert `FileItemEntity`（含 parent 链、ctag、大小、时间）；
  - `deleted` → 软删（`is_deleted`，保留 PIM 回收站语义，见 §7）；
  - 文件夹变更天然包含在 delta 流中。
- **游标失效**：Graph 返回 410 Gone 时全量重扫（从 root delta 第一页重放），模式与 `OutlookSyncService` 的 deltaLink 重置一致，可参考其实现与测试。
- **限流**：429 + `Retry-After` 退避 + 抖动；单次同步分页循环全程复用同一游标。
- **幂等**：delta 按 driveItem id 幂等 upsert，重复同步不产生重复行（与 PcTracker/Mobile 上传幂等同原则）。
- **parent 链重建**：delta 可能先给子后给父；同步事务内按 `parent_external_file_id` 补全 `drive_path`，父缺失的孤儿项挂 `/ orphaned` 临时路径并在下次轮询收敛。
- **同步后动作**：更新 provider 的 `LastSyncAt / LastError`；今日页/状态页文件区块读这份健康状态（同步落后、410 重置、持续 429 均可见）。

---

## 7. 稳定直链端点

Graph 的下载 / 缩略图 URL 是**预授权短时效（约 1 小时）**的，客户端不得持久化直链。PIM 提供稳定引用：

```
GET /api/v1/files/items/{id}/content            # 下载 / 内嵌预览（302 → 新鲜 downloadUrl）
GET /api/v1/files/items/{id}/thumbnail?size=    # 缩略图（302 → 新鲜缩略图 URL；小图可代理 + ETag 缓存）
```

- 每次请求现取 fresh URL（`GET /drive/items/{id}?select=@microsoft.graph.downloadUrl` 或 thumbnails 端点），即刻 302。
- 端点内完成：用户鉴权 → item 归属校验 → **敏感路径检查**（命中 `/Secrets/*` 等规则则 403，不产直链）→ 302。
- 直链值**不写日志、不进审计详情**（只记 item id 与操作）。
- 下载不经过 PIM 代理（不占带宽）；仅当直链缺失（个人版个别类型）时代理兜底并加大小上限。

---

## 8. 预览与编辑矩阵

| 类型 | 预览 | 编辑 |
|---|---|---|
| 图片 | Graph 缩略图直出（`<img>` 加载 302 后的短时效 URL），网格视图批量浏览 + 灯箱 | — |
| PDF | `content` 端点进 `<iframe>`（浏览器自带查看器） | — |
| Office（docx/xlsx/pptx） | Graph `/preview` 或分享链接 embed iframe（个人版支持性待 §14 验证） | 新标签页打开 OneDrive Web 深链编辑；改完由 delta 同步感知，PIM 不参与编辑 |
| 文本 / Markdown | 前端从 `content` 端点拉文本渲染（小文件；CORS 不满足时由该端点直接代理文本，≤ 2MB） | PIM 内置小编辑器，PUT 回写 Graph（简单上传 ≤ 4MB）；**回写前把旧内容快照进 PostgreSQL**（弥补个人版版本 API 不确定性，见 §14） |

文本编辑快照：新表 `file_text_snapshots`（item_id、content、captured_at、author），仅文本小文件启用，保留最近 N 份；未来验证 Graph versions 可用后可切换。

---

## 9. 搜索设计（仅元数据）

- 检索范围：`name`、`drive_path`、`mime_type`、大小、修改时间过滤；PostgreSQL ILIKE + trigram 索引（`pg_trgm`，如可用）即可，无需外部引擎。
- 入口：Web 文件页搜索框、现有 `GET /api/v1/files/search` 端点（改为元数据语义）、MCP `search_files`（参数不变，行为变为元数据搜索）。
- 内容级查找：Hermes 用 `read_file_text`（§12）现取现抽——服务器瞬态下载 → Tika 或按类型的轻量抽取 → 返回文本，**不落盘、不入库**（进程内即用即弃）。
- 可选增强（不在本期）：Graph `/drive/search(q=)` 代理（微软侧的名字搜索）；再往后的全文/向量档位见 D3/D4。

---

## 10. 快速记录附件迁移到 OneDrive

- 上传：QuickNotes 附件端点改为「接收流 → Graph 上传会话（小文件简单上传）→ 落 `/PIM/QuickNotes/{年份}/{笔记id}/{文件名}`」→ 记录 driveItem id 到附件元数据。
- 加载：客户端（Web / Android WebView）只记 PIM 稳定 URL（`/api/v1/quick-notes/{id}/attachments/{aid}/content`，内部复用 §7 直链机制）。
- 敏感规则沿用：附件继承笔记隐私语义；未来若接入任何 AI 处理，附件默认不参与。
- QuickNotes 现有的「无 MinIO 时 Null 存储降级」路径随此变更移除（附件必须有 OneDrive 才可用，UI 明确提示绑定状态）。
- 其他模块附件（如日历事件附件 `EventAttachmentService`）按同一模式分批迁移，本期先做 QuickNotes。

---

## 11. Web UI 设计（方案 A 浅色）

已评审定稿（高保真演示稿 `A2-shadcn-zinc-light.html`，2026-09-19 会话产出）：

- **三栏布局**：左栏文件树（懒加载展开 + 当前路径高亮）｜中栏工具条（同步状态 chip、计数、排序、列表/网格切换）+ 文件列表 / 缩略图网格｜右栏预览面板（缩略图 / PDF iframe / 文本渲染 + 元数据 + 操作按钮）。
- **操作按钮**：「在 OneDrive 打开」主按钮通栏在上，下载 / 复制直链并排在下。
- **设计 token**：实现时使用全站**基调 v1** token（`src/client-web/src/index.css`：slate 中性色 + indigo 主色 `--pim-primary` #4f46e5 + `--pim-radius-*`）——演示稿的 zinc 灰阶映射到 slate token，视觉结构不变；深浅主题跟随全站（本期先浅色）。
- **组件选型**：自绘 + 现有 `ui/` 基建（Dialog、按钮、chip）+ `react-arborist`（虚拟化文件树，headless 样式贴 token）；图片灯箱用 `yet-another-react-lightbox`（网格视图）。不引入 Ant Design（避免第二套设计语言）。
- **移动端**：三栏折叠为「列表 → 全屏预览」两级导航；树收纳进抽屉。
- **状态可见**：同步落后 / 从未同步 / 绑定失效在工具条 chip 与空状态中明确表达（数据质量优先原则）。

---

## 12. MCP 工具面

现有 14 个 files 工具**接口不变**（provider 无关设计，加 onedrive 类型零改动），行为切换为元数据语义：

- `get_files` / `get_file`：元数据树 / 单条（含 drive_path、大小、时间、mime）。
- `search_files`：元数据搜索（q、type、limit）。
- `get_file_open_link`：直链（默认 redact 语义保留；指向 PIM 稳定端点）。
- `upload_file` / `move_file` / `rename_file` / `delete_file` / `restore_file`：Graph 写操作映射；`index_file` 工具在 v2 无意义，标记废弃（返回明确错误说明）。

**新增 1 个工具**：

- `read_file_text(itemId, maxBytes?)`：服务器瞬态下载 → 按类型抽取文本（文本类直读、pdf/docx 用轻量本地抽取；超过 maxBytes 截断并告知）→ 返回文本 + 元信息（截断与否、实际字节数）。**不落盘、不入库、不建索引**；受敏感路径规则约束（命中即拒绝）。

工具权限分级、审计、Scoped Token 机制照旧；审计里只记 itemId 与字节数，不记直链与内容。

---

## 13. 安全与隐私清单

- [ ] token 只存加密 token 缓存；日志 / 审计 / 异常消息零 token。
- [ ] downloadUrl / 缩略图 URL 不写日志、不进审计、不进前端持久化（localStorage / 组件状态仅会话期）。
- [ ] 敏感路径规则（`/Secrets/*`、`/Passwords/*` 等）在直链端点、`read_file_text`、搜索结果三处一致生效。
- [ ] 稳定端点强制登录 + item 归属校验（防越权 302）。
- [ ] MCP files 工具沿用 Scoped Token 权限 + 审计。
- [ ] `read_file_text` 有并发/频次上限（防滥用瞬态下载），超限明确报错。

---

## 14. OneDrive 个人版 API 事实验证清单

**已验证（2026-09-19，项目主人授权的 device-code 只读会话；全程无任何写操作，探测脚本与原始输出存于会话工作区，不入库）。**

| # | 能力 | 结果 | 实测记录 | 对设计的影响 |
|---|---|---|---|---|
| V1 | delta 同步 | ✅ 支持 | `GET /me/drive/root/delta` 200，首页 50 条可翻页（未到 deltaLink，符合预期） | P1 delta 方案成立；注意首次全量规模（见附录） |
| V2 | 缩略图 API | ✅ 支持 | 图片项 thumbnails 200 | 预览矩阵按原设计 |
| V3 | `/preview` | ✅ 支持 | POST preview 200 且返回 `getUrl` | Office 预览走 `/preview` iframe，无需新标签页兜底 |
| V4 | 版本 API `/versions` | ✅ 列表可用 | 图片项 versions 200（count=1）；**恢复未测**（只读约束） | P2 可接线版本列表；恢复操作待 P2 实测再定，文本快照兜底保留 |
| V5 | 回收站 API | ❌ 不存在（符合预期） | `GET /me/drive/deletedItems` 返回 400 | 个人版无 Graph 回收站端点——PIM 侧软删 + 回收站表为主的设计确认 |
| V6 | downloadUrl 预授权 | ✅ 可用 | 无 `Authorization` 头 + Range 请求返回 206、读到 1KB；`cors_header` 为 null（探测未带 Origin 头，不能据此断言 CORS） | 直链 302 方案成立；浏览器 CORS 行为留待 P2 真实页面实测，文本预览默认走 PIM content 端点代理（≤2MB），不受影响 |
| V7 | 上传会话 CORS | ⏸ 推迟 | PUT 需要写权限，与「严禁影响文件」约束冲突 | 不阻塞：设计默认上传走 PIM 服务器转发，浏览器直传仅作为将来优化 |
| V8 | 429 限流 | ⏸ 不做压力探测 | 避免对真实账号制造限流 | 退避 + `Retry-After` 处理由 fake handler 单测覆盖 |

### 验证附录

- 账号与盘：`driveType = "personal"`，配额已用 378.92 GB / 1104.88 GB（个人版确认）。**已用量大，首次全量 delta 爬取可能达数万项**——P1 必须完整分页、同步进度可见、对首次同步耗时给出预期管理；验收标准「几分钟内可见」指增量场景，首次全量单独说明。
- **scope 披露**：device-code 实际返回的 token scope 比申请的宽（含 `Files.ReadWrite.All` 等）——原因是该应用注册此前已为 Outlook 同步 consent 过这些权限，AAD 返回已授权集合。本次脚本仅执行 GET 与无副作用的 preview，token 未落盘未打印，进程退出即失效。**对 P1 的意义：无需新建应用注册、无需新增 consent 流程，现有 Client ID 即可支撑读写实现**；实现中仍须在代码层遵循最小权限，只调用所需操作。

---

## 15. 测试策略

- **TDD**：先写失败测试再实现（仓库 A1 纪律）。
- Graph 交互全部通过 fake `DelegatingHandler` / fake adapter 驱动（不依赖真实 OneDrive 跑单测）：
  - delta 分页循环、游标保存与 410 重置、429 退避；
  - delta 幂等 upsert（重复流不重复建行）；
  - parent 后到的孤儿项收敛；
  - 稳定端点：未登录 401 / 他人 item 404 / 敏感路径 403 / 直链缺失代理兜底；
  - `read_file_text`：截断、敏感路径拒绝、频次上限。
- Web：文件页组件测试（树懒加载、列表/网格切换、预览面板状态、同步 chip 状态）仿 `tests/client-web` 现有模式。
- 手动验收路径写入 `docs/operations/`（绑定 → 首次同步 → 预览四类文件 → Hermes 读文本 → 附件上传加载全链）。

---

## 16. 分阶段交付与验收

| 阶段 | 内容 | 大白话验收标准 |
|---|---|---|
| **P1 同步地基** | OneDrive 绑定（MSAL + 加密缓存）+ delta 增量同步 + Hangfire 定时 + 手动同步端点 + 文件树数据模型重塑 + V1/V2 验证 | 「文件放进 OneDrive，几分钟内 PIM 里能看到（含子文件夹）；改名/删除也能跟着变」 |
| **P2 预览与直链** | 稳定直链端点 + 缩略图 + 预览矩阵（V3/V6 定案落地）+ 文本小编辑器与快照 | 「网页上能看缩略图、点开 PDF 和 Office 文档；文本能改，改完 OneDrive 里是新内容」 |
| **P3 UI 三栏页** | 方案 A 浅色文件页（树 / 列表·网格 / 预览）+ 搜索框（元数据）+ 移动端折叠 | 「打开文件页和演示稿一个样子；搜文件名能搜到；手机上也好用」 |
| **P4 附件 + MCP + 退役** | QuickNotes 附件上 OneDrive + `read_file_text` + Nextcloud/Tika/Qdrant/MinIO 退役 + 文档更新 | 「Hermes 能说出一篇文档里写了什么；快速记录的附件能传能看；compose 里不再有 Nextcloud」 |

每阶段独立 PR；阶段边界处跑 `dotnet test Pim.sln` 与 `npm --prefix src/client-web run build`。

---

## 17. 退役清单

| 项 | 动作 | 时机 |
|---|---|---|
| Nextcloud 适配器 + `IFileProviderAdapter`（v1 路径模型） | 删除；实体字段迁移清理 | P1 |
| Nextcloud 服务（docker-compose dev/prod、文档） | 移除 | P4 |
| OnlyOffice 配置与依赖 | 移除（编辑走 OneDrive Web） | P4 |
| Tika / Qdrant（文件线） | compose 移除；`Embedding`/解析配置标注废弃 | P4 |
| MinIO（文件/附件线） | 移除；文本快照改 PostgreSQL；其余引用清查 | P4 |
| `index_file` MCP 工具、`FileChunk/FileIndexJob/FileAiResult/FileSuggestion` 实体 | 废弃标记 → 单独清理 PR | P4 后 |
