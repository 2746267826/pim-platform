# 文件模块 v2 验收手册 / Files Module v2 Acceptance Guide

> 面向验收人与后续维护者。设计依据：[`designs/onedrive-files-v2.md`](../../designs/onedrive-files-v2.md)。
> 覆盖 P1-P4：OneDrive 绑定与同步、预览与直链、三栏文件页、附件、MCP 工具与退役。

## 前置条件

- API 已启动，并能在浏览器访问（默认 `http://127.0.0.1:5858`）。
- 已注册账号（首个注册用户自动成为管理员）。
- 一个 **OneDrive 个人版**账号（设计决策：OneDrive 为唯一事实源，PIM 只是客户端）。

## 1. 绑定 OneDrive

1. 打开「文件」页 → 点击绑定 OneDrive。
2. 页面给出**设备码**与验证地址（`https://microsoft.com/link`）。
3. 在浏览器打开该地址、输入设备码、用你的 Microsoft 账号授权。
4. 回到 PIM，绑定状态变为 **connected**，并显示账号名与配额。

**预期**：绑定成功后立即可见文件树；未绑定时文件与附件功能给出明确的绑定提示，而不是报错。

## 2. 首次同步与增量同步

1. 绑定后触发一次同步（或等待 20 分钟定时任务）。
2. 观察工具条上的同步状态 chip。

**预期**：
- 文件树按需展开，子文件夹可逐层进入。
- 在 OneDrive 里新增 / 改名 / 删除文件，几分钟内 PIM 里跟着变。
- 首次全量同步可能较久（个人版大容量账号可能数万项），增量同步很快。
- 若上次同步被进程重启打断，状态会复位为可感知的错误态并自动重试，不会永久卡在「同步中」。

## 3. 预览与直链

逐类文件点开预览：图片、PDF、Office 文档、纯文本。

**预期**：
- 缩略图与预览正常显示；Office 走 OneDrive 预览。
- 「在 OneDrive 打开」跳转到该文件的 OneDrive 网页地址。
- 下载按钮经 PIM 稳定端点 302 到 OneDrive 预授权直链（地址栏短暂跳转后开始下载）。

## 4. 文本编辑与快照

1. 打开一个 `.txt` / `.md` 文件 → 编辑 → 保存。
2. 打开快照列表。

**预期**：保存后 OneDrive 里是新内容；保存前会自动留存一份快照，可从快照恢复。

## 5. Hermes（MCP）读取文档内容

对 Hermes 说：「读一下《XX》文档里写了什么」。

**预期**：
- Hermes 调用 `read_file_text`，返回文档文本（txt / md / docx / pptx 内置支持；pdf 等需配置 Tika）。
- 超长文档会被截断，并明确告知截断。
- 内容**不落盘、不入库**；日志与审计里只有 item id 与字节数，没有正文。
- 高频调用（每分钟超过 30 次）会被限流并明确报错。

## 6. 快速记录附件

1. 在快速记录里上传一个附件（建议 ≤4MB）。
2. 上传后点击下载 / 预览。

**预期**：
- 附件存入你**自己的** OneDrive 的 `/PIM/...` 目录下，不占用 PIM 服务器存储。
- 下载优先 302 到预授权直链；拿不到直链时回退服务器代理。
- 超过 4MB 会明确拒绝并提示改用 OneDrive 客户端。
- 未绑定 OneDrive 时，上传返回明确的绑定提示。

## 7. MCP 写操作

对 Hermes 说：「把 a.txt 重命名为 b.txt」「把它移到某目录」「新建一个文档」「删掉它」。

**预期**：
- 重命名 / 移动后，OneDrive 与 PIM 本地元数据一致；目录改名时子项路径一并跟随。
- 上传 ≤4MB 文件后立即出现在文件树。
- 删除会进 OneDrive 回收站，PIM 内标记为已删除；**还原需在 OneDrive 网页版回收站操作**
  （个人版没有回收站 API，PIM 无法代为还原）。

## 8. 敏感路径保护

把文件放到 `/Secrets/` 或 `/Passwords/`（规则可用 `FILES_SENSITIVE_PATH_PATTERNS` 调整）。

**预期**：以下操作**全部**被拒绝（403）：
- 取直链 / 缩略图 / 预览地址；
- 读文本 / 保存文本 / 列表快照；
- Hermes 的 `read_file_text`；
- 「在 OneDrive 打开」的网页地址；
- 出现在搜索结果里。

## 9. 多用户隔离

用第二个账号登录，尝试访问第一个账号的任意文件 id。

**预期**：一律 404（不泄露存在性）。绑定状态、同步、断开、读文本、改名、删除、开放链接、恢复均如此。

## 10. 退役核对

**预期**：`docker-compose.yml` 与生产编排里**不再出现** Nextcloud / MinIO / Qdrant / OnlyOffice；
默认栈只有 API + PostgreSQL（+ 可选 LiteLLM）。文件与附件不再依赖任何本地对象存储。

## 自动化验证

```bash
# 后端：全套单测（含文件模块端点契约、DI 解析、MCP 路由契约、抽取器与写服务）
dotnet test Pim.sln

# 前端构建
npm --prefix src/client-web run build

# MCP Python 参考服务自检
python3 scripts/mcp/pim_mcp_server.py --check
python3 -m pytest scripts/mcp/test_pim_mcp_server.py -q
```

## 已知边界

- 个人版 OneDrive **没有回收站 API**：删除后只能去 OneDrive 网页版还原。
- 上传单请求上限 4MB（Graph 简单上传限制）；更大文件请用 OneDrive 客户端。
- 首次全量同步耗时取决于账号体量。
- 网格视图目前使用类型图标（真实缩略图与灯箱为后续项）。
- 无跨标签页编辑冲突检测：两处同时编辑同一文本文件时后写覆盖先写（快照可回退）。
