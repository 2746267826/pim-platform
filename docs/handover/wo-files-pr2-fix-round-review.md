# PR-2 修复轮（F-1/F-2/F-3）独立 review 记录

> 目的：记录本轮独立 review 的**可用结论**与**不可用结论**，避免把不可靠的内容当成证据。

## 被审对象

- 分支 `dsh-linux/wo-files-pr2`
- 被审提交：`1022521d`（F-1/F-2/F-3 修复）、`cb97d54a`（文档）
- review 期间 HEAD：`cb97d54a`；最终推送 HEAD：`51393ca1`（其后仅追加一条测试与一条文档提交）

## review agent 与结论

agent：antigravity（具备工具执行能力，非 Codex；本轮 Codex 仍未提供可用工具访问）。

**结论：允许合入（0 Critical / 0 Important），仅 1 项 Minor。**

### 该 review 中**可信**的部分（与本地复核一致）

1. **F-1 故障对照**：把端点改回缺少 `:/children` 的旧形态后，
   `OneDriveCreateFolderRealHttpTests` **4 项转红**，报 `Graph 400 invalidRequest`；
   恢复后 6 项全绿，且 `git status` 干净。这与我自己做过的同款对照结论一致。
2. **F-3 故障对照**：把 `startDownload` 改回 fetch 跟随 302 的实现后，
   `FilesDownloadNoBodyFetch.test.tsx` 断言失败（`expected true to be false`）；
   恢复后全绿。与我此前观察到的一致。
3. **F-2 逻辑核查**：确认以服务端返回 id 回读、回读为空时抛 5300 而非沿用输入名。
4. **回归面核查**：确认旧 302 `/download` 端点仍保留（兼容外部直接链接）、
   `apiDownloadBlob` 仍被预览缩略图与 quickNotes 合法使用、
   重名预测格式改为空格+序号符合服务端真实行为。
5. **Minor（属实且我此前未注意）**：`OneDriveGraphClient.cs` 顶部
   `using System.Net.Http.Json;` **重复出现两次**（第 3、4 行）。
   经查**非本次改动引入**（源自 `73d7c065`，PR-2 之前就已存在），
   且实际构建为 `0 Warning(s)`，属于冗余而非缺陷。

### 该 review 中**不可信**的部分（已核实为编造，不得引用）

以下内容在仓库中**不存在**，属于编造，不得作为证据：

1. 它引用的提交标题并非真实提交信息。例如它写 `1022521d` 的标题是
   「修复 OneDrive 新建文件夹端点语法、权威回读及大文件前端下载链路」，
   而真实标题是 `fix(files): address PR-2 acceptance findings F-1/F-2/F-3`；
   它还把 `cb97d54a`（一条 docs 提交）描述成「补充 F-1 畸形端点契约测试」。
2. 它引用的测试函数名不存在。它列出的
   `CreateFolder_InRoot_HitsRootChildrenEndpoint`、
   `CreateFolder_InSubfolder_HitsPathChildrenEndpoint`、
   `CreateFolder_NameConflict_AppendsRenameBehavior`、
   `CreateFolder_AuthoritativeReadBack_CapturesRenamedName` 等
   均不在仓库中；真实用例名为
   `CreateFolder_UsesChildrenEndpointForm`、`CreateFolder_InRoot_UsesRootChildrenEndpoint`、
   `MalformedEndpointForm_IsRejectedLikeRealGraph`、
   `CreateFolder_DuplicateName_ReportsServerSideName`、
   `CreateFolder_RequiresRenameConflictBehaviour`、
   `CreateFolder_SubfolderWithSpacesAndNonAscii_IsEscapedOnce`。
3. 它引用的源码片段与真实实现不符（例如称 `CreateFolderAsync` 返回带 `Id` 的对象、
   服务签名写作 `(accessToken, parentPath, name, ct)`，而真实签名返回 `string`、
   参数名为 `folderPath`）。
4. 它报告的测试计数有偏差（称后端 5385、前端 `vitest run` 255 项），
   而本仓库 CI 入口与全量门禁的实际计数见下。

**处理方式**：只采信其与本地复核相互印证的部分；其引用的具体文本/名称/计数一律不采信，
本文件与 PR 说明中的证据均改用本地实跑输出。

## 本地复核（权威证据）

| 检查 | 命令 | 结果 |
|---|---|---|
| F-1 | `dotnet test Pim.sln --filter FullyQualifiedName~OneDriveCreateFolderRealHttpTests` | Passed 6 / Failed 0 |
| F-2 | `dotnet test Pim.sln --filter FullyQualifiedName~OneDriveWriteServiceTests.CreateFolder` | Passed 3 / Failed 0 |
| F-3 | `TZ=UTC npx vitest run FilesDownloadNoBodyFetch.test.tsx OneDrivePreviewPane.test.tsx` | Test Files 2 passed / Tests 7 passed |
| 后端全量 | `dotnet test Pim.sln` | Passed 5384 / Failed 0 / Skipped 44 |
| 网页 CI 入口 | `TZ=UTC npm --prefix src/client-web run test:schedule-workbench-complete` | 全链路通过（含 180 项） |
| 端到端 PR-2 | `e2e-files-pr2.mjs` | 31/31 |
| 端到端 PR-1 回归 | `e2e-files.mjs` | 41/41 |
| GitHub Actions（HEAD `51393ca1`） | CI Gate / build-web / build-api / build-docker | 全部 success |

故障对照（本地实跑，非引用 review）：F-1 改回错误形态 → 4 项红；F-2 改回以输入名登记 → 1 项红；
F-3 改回 fetch 跟随 302 → 对应用例红；三者恢复后均全绿且工作区干净。

## 待人工确认（不阻塞）

1. 验收方需在真实账号现场复核 F-1（新建文件夹真的 201、重名真的自动改名）。
2. F-3 要求以真实账号 ≥500MB 文件实测内存/网络留证 —— 本环境无微软凭据，保持 NOT-VERIFIED。
3. 预览面板 PDF 内联预览仍会把整份文件读进内存（既有行为、无尺寸闸），
   因涉及改变已验收的预览呈现方式，未擅自修改，见验证文档。
