# V1~V4 技术验证记录（WO-FILES-20260923 · PR-2）

> 绑定 head SHA：见本 PR 最新提交。本文件是**验证记录**，不是验收结论。
> 环境：隔离合成镜像库（容器 `pim-wof-pg`，库 `wo_files_e2e`，127.0.0.1:55577），
> **不含任何生产数据**。本环境**没有任何 Microsoft Graph 凭据**（无 client id / refresh token）。

## 验证手段：真实 HTTP 的假 Graph 服务

因为没有真实账号，为了**不把「没法验」当成「不用验」**，本 PR 搭了一个
`FakeGraphServer`（`tests/Pim.UnitTests/Files/FakeGraphServer.cs`）：

- 真实 `HttpListener` + 真实 `HttpClientHandler`（**不是** stub handler）；
- 只绑定回环地址，不访问外网、不接触任何真实账号；
- 实现 `createUploadSession` / 分片 `PUT`（按 `Content-Range` 顺序拼接）/ 会话查询
  （`nextExpectedRanges`）/ `createLink`，并记录**请求全序**与字节数；
- 同时统计「到达服务器的字节」与「被接受的字节」——断点续传断言必须区分这两个。

配套：`OneDriveGraphClient` 的 Graph 基址改为可配置（`Files:OneDrive:GraphBaseUrl`），
默认仍是官方基址；主机白名单**由实际基址推导**（内容主机 + 登录主机），
不削弱既有安全属性（PR-1/#343 的「恶意游标不得携带 token」用例继续守着）。

---

## V1｜浏览器直传上传会话的 CORS 可用性

**结论：机制已用真实 HTTP 验证；真实账号下的浏览器直传 = NOT-VERIFIED。**

| 检查项 | 方式 | 结果 |
|---|---|---|
| 预检协商（OPTIONS） | 真实 HTTP：`CorsPreflight_IsAnsweredForBrowserDirectUpload` | PASS：服务端正确回应 `Access-Control-Allow-Origin` / `-Methods`（含 PUT）/ `-Headers`（含 Content-Range），且预检不携带内容体 |
| 上传会话真实往返 | `CreateUploadSession_CarriesNoFileBytes` | PASS：`createUploadSession` 返回可用 `uploadUrl`，且**请求体零字节**（AC-14.2 口径） |
| 分片直发 uploadUrl | `SequentialChunkedPut_IsAcceptedAndAccountedOnRealServer` | PASS：分片 `PUT` 直发 uploadUrl、顺序被接受、被接受字节数 = 文件大小 |
| 分片**不带 Authorization** | 同上（断言请求头） | PASS：`uploadUrl` 已预授权，分片不带 token（避免把凭据发给错误主机） |

**NOT-VERIFIED（需真实账号）**：真实微软端点对该账号的 CORS 行为与浏览器实测。
本环境无凭据，无法证明；按工单 §7，V1 的端到端结论待现场实测。

## V2｜分享「有效期」在个人版的支持性

**结论：需求方已裁定「按不支持处理」→ 已按降级方案实现，NOT-VERIFIED。**

| 检查项 | 方式 | 结果 |
|---|---|---|
| 请求携带 `expirationDateTime` | `CreateShareLinkAsync`（仅在用户选 7/30 天时带上） | 已实现 |
| 档位只有 无/7/30（P6），其余拒绝 | `CreateAsync_WithUnconfirmedExpiration_IsRejected` | PASS：不静默降级成「不过期」，返回可读错误 |
| 平台不支持时的 UI 说明 | `ShareDialog` 的 `expirationSupported={false}` → 明确提示「若平台忽略该设置，链接将长期有效，请用撤销手动失效」 | PASS（组件用例 + 真浏览器 E2E） |
| 兜底：手动撤销 | `RevokeSharePermissionAsync`（404 视为已撤销，幂等） | PASS |

**NOT-VERIFIED（需真实账号）**：个人版是否真的接受并执行 `expirationDateTime`。
无真实账号时无法探测；因此 UI 不假设支持，而是**如实说明 + 提供手动撤销兜底**。

## V3｜分享权限（可看 / 可编辑）可用性

**结论：请求形状与档位已实现并有单测；真实可用性 = NOT-VERIFIED。**

| 检查项 | 方式 | 结果 |
|---|---|---|
| `view` / `edit` 两档 | `CreateShareLinkAsync`（`type` = view/edit，`scope` = anonymous） | 已实现 |
| 非法档位拒绝 | `CreateAsync_WithUnknownPermissionType_IsRejected` | PASS |
| 安全闸门 | `CreateAsync_OnSensitivePath_IsRefused`（AC-21.4） | PASS：敏感路径拒绝分享，且**不先建链再报错** |
| 链接不进审计 | `CreateAsync_DoesNotWriteTheLinkIntoAudit` | PASS：审计只记条目 id 与动作 |

**NOT-VERIFIED（需真实账号）**：生成的可看链接能否在无痕窗口打开（AC-21.1）、
可编辑链接对方是否真的可编辑（AC-21.2）。均需真实账号与他人视角，本环境无法证明。

## V4｜分片上传平台限制

**结论：已按 references/03 规范实现并用真实 HTTP 验证。**

| 平台要求 | 实现与验证 | 结果 |
|---|---|---|
| 分片大小是 **320 KiB 整数倍** | `uploadChunkPlan.ts`：`UPLOAD_CHUNK_ALIGNMENT`；用例逐块断言（末块按实际收尾，是平台允许的例外） | PASS |
| 单块 **≤60 MiB** | `UPLOAD_CHUNK_MAX` + 用例断言；请求超过上限时被夹到 60MiB | PASS |
| 推荐 10 MiB | `UPLOAD_CHUNK_SIZE = 10MiB`（320KiB 整数倍） | PASS |
| 必须**顺序**上传 | `uploadEngine` 串行 await；真实服务按 `Content-Range` 拼接，乱序返回 416 并给出 `nextExpectedRanges` | PASS |
| `Content-Range` 格式 | `contentRangeHeader()` → `bytes start-end/total`，用例断言 | PASS |
| 断点续传 | 分片失败后查询 `nextExpectedRanges` 并从该偏移补齐（真实 HTTP 用例） | PASS |
| >4MB 必须走上传会话 | `requiresUploadSession()` + 边界用例（4MB 整走简单上传，4MB+1 走会话） | PASS |
| P5 单文件 ≥2GB 支持、>2GB 拒绝 | `exceedsUploadLimit()` + 明确提示「改用 OneDrive 客户端」 | PASS（上限判定）；真实 500MB/2GB 上传 = NOT-VERIFIED |

---

## 汇总

| 验证 | 本环境结论 | 说明 |
|---|---|---|
| V1 | **机制 PASS / 端到端 NOT-VERIFIED** | CORS 协商、会话往返、分片直发已用真实 HTTP 验证；真实账号待实测 |
| V2 | **降级实现 PASS / 支持性 NOT-VERIFIED** | 已按「可能不支持」实现并给出手动撤销兜底与 UI 说明 |
| V3 | **档位实现 PASS / 真实可用性 NOT-VERIFIED** | view/edit 已实现并有单测；无痕窗口与对方可编辑需真实账号 |
| V4 | **PASS** | 320KiB 倍数 / ≤60MiB / 顺序 / Content-Range / 断点续传全部验证 |

## 端到端过程中发现并修复的真实缺陷（b124d26b）

浏览器端到端脚本在「刷新后传输历史」之后**卡死**，现象是一个全屏遮罩
（`data-testid="my-shares-dialog"`，`fixed inset-0 z-[60]`）拦住后续所有点击。

用真实浏览器探针定位到根因，**不是脚本不稳，而是产品缺陷**：
工具条上「我的分享」被写成了「传输任务」按钮**内部的子按钮**。按钮嵌套按钮是非法 HTML
（React 也会打印 `In HTML, <button> cannot be a descendant of <button>`），
且子按钮覆盖了父按钮的命中区，实测：

- `elementFromPoint(transfer-toggle 中心)` 返回 `BUTTON[my-shares-button] 「我的分享」`；
- 点「传输任务」→ 命中的是「我的分享」→ 点击又冒泡到父按钮，
  于是**同时**打开传输面板和「我的分享」全屏遮罩；
- 遮罩出现后拦掉页面上所有点击，端到端流程死锁。

修复：两者改为平级兄弟节点（同一个 flex 容器内）。回归测试
`src/client-web/src/pages/__tests__/FilesToolbar.test.tsx`（4 项，已接入 CI 既有入口
`test:files` → `test:files-vitest`）守两件事：四个入口互不嵌套；点谁只触发谁。
该测试先 RED、修复后 GREEN，并做了故障对照（把子按钮塞回父按钮内部）确认重新变红。

诚实边界：jsdom **不做命中测试**，因此它只能证明「非法嵌套」与「点击冒泡串台」，
不能证明真实布局下的遮挡；遮挡证据来自 Chromium 探针（`elementFromPoint`）与端到端脚本。

### 需要现场（真实账号）补验的事项

1. 真实浏览器对微软上传端点的 CORS 直传（V1 端到端）；
2. 真实账号下的 500MB 上传成功、进度递增、暂停后续传（AC-14.1）；
3. 分享链接在无痕窗口打开（AC-21.1）、可编辑链接对方可编辑（AC-21.2）；
4. 个人版是否接受 `expirationDateTime`（V2）；
5. 上传流量证据（字节未流经 PIM 服务器）在真实链路上的抓包/日志对照。
