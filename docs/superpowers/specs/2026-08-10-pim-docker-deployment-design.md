# PIM 生产 Docker 化部署设计

- 日期：2026-08-10
- 状态：已批准（设计评审通过）
- 目标：生产端由"直接运行 GitHub Actions 产物"迁移为 Docker 部署，GitHub Actions 同时提供原有 tar.gz 产物与 Docker 镜像（双通道并行）。

## 决策摘要

| 问题 | 决策 |
|------|------|
| 容器形态 | 精简镜像 + supervisord（无 systemd、无 journald、无 syslog） |
| 进程管理 | **tini 作为 PID 1**（回收僵尸），supervisord 托管 sshd + dotnet |
| 镜像构建 | 单镜像（web + api 一体），复用 `src/Pim.Api/Dockerfile` 多阶段思路 |
| 镜像仓库 | GHCR：`ghcr.io/2746267826/pim-platform-server` |
| 部署范围 | 只容器化 PIM 单容器；数据库/MinIO/Nextcloud 等继续用生产外部服务，全部经环境变量接入 |
| 容器 SSH | sshd 监听 22；宿主机映射 `127.0.0.1:${PIM_SSH_PORT:-2222}:22`；仅密钥认证 |
| SSH 用户 | 非 root 用户 `pimlog`（UID 1001），仅可读取 PIM 自身日志（强制命令包装脚本） |
| 密钥注入 | `PIM_SSH_AUTHORIZED_KEYS` 环境变量（**base64 单行**，entrypoint 解码写入） |
| 日志清理 | PIM 自身日志保留当天 + 前一天（Serilog 滚动，`PIM_LOG_RETAINED_FILES=2`）；容器 stdout 由 Docker json-file driver max-size 轮转 |
| 宿主机 logreader | 保持现状，不做任何改动 |
| CI | 新增 `build-docker.yml`，api/web/docker 变更时构建；`is_release=true` 时 push GHCR 并把 `docker save` 产物经 release job 上传为 release asset |
| 代码变更 | `PIM_LOG_RETAINED_FILES`（Program.cs 读环境变量）**并入本 PR**（几行代码） |

## 现状分析

- 生产端当前直接运行 GitHub Actions 构建的 tar.gz 产物（`build-api.yml` linux-x64 framework-dependent + `build-web.yml` Vite 产物打包进 wwwroot），经 GitHub Release 分发。
- 已有 `src/Pim.Api/Dockerfile`（多阶段：node 构建 web → sdk 发布 server → aspnet 运行时）与开发用 `docker-compose.yml`（完整栈）。
- `Program.cs:15-23`：Serilog 写 `/data/pim/logs/pim-api-.jsonl`（按天滚动，`retainedFileCountLimit: 30` 硬编码）。
- 宿主机存在受限 SSH 日志读取通道（`logreader` 用户 + `log-reader.sh` 强制命令，读 `/data/pim/logs/*.jsonl` + systemd journal）。
- `appsettings.json` 含全部配置键（ConnectionStrings/Minio/Kopia/Tika/Ai/Nextcloud/OnlyOffice/Qdrant/Files/Embedding/Jwt/DataProtection）。

## 方案选择

- **方案 A（选定）**：单镜像（web+api 一体）+ 新增 build-docker workflow 推 GHCR；tar.gz 流程不动。
- 方案 B（双镜像 api+nginx）：与"API 单进程服务 wwwroot"架构相悖，需改 `Program.cs` 与端口暴露。排除。
- 方案 C（复用现有 Dockerfile 仅加 sshd、不新增 CI）：违背"Actions 同时提供 docker"需求。排除。

## 第 1 节：镜像结构与运行时

```
Dockerfile（修改现有 src/Pim.Api/Dockerfile）
├── 阶段1 client-build : node:22-alpine, vite build（不变）
├── 阶段2 server-build : dotnet/sdk:8.0, publish（不变）
└── 阶段3 runtime      : mcr.microsoft.com/dotnet/aspnet:8.0
    ├── apt: openssh-server supervisor tini
    ├── 创建受限用户 pimlog（UID 1001，/usr/sbin/nologin，家目录 /home/pimlog）
    ├── 复制受限读取脚本 pim-log-cat.sh → /usr/local/bin/
    ├── 复制 entrypoint.sh → 容器入口
    └── supervisord.conf：托管 sshd + dotnet Pim.Api.dll
```

**ENTRYPOINT：** `["tini", "--", "/entrypoint.sh"]`

- tini 为 PID 1，回收 sshd 高频 fork 产生的僵尸进程；`docker stop` 时 tini → SIGTERM → supervisord → dotnet（.NET 优雅停机）。
- **进程运行用户：** supervisord 以 root 启动，dotnet 子进程以 root 运行（个人项目可接受）。权限链自洽：root 写日志（0644 世界可读）→ pimlog 可读。

**entrypoint.sh 职责（顺序）：**

1. 若 `PIM_SSH_AUTHORIZED_KEYS` 非空 → `echo "$VAR" | base64 -d` 解码 → 写入 `/home/pimlog/.ssh/authorized_keys`，随后强制：
   `chmod 700 /home/pimlog/.ssh && chmod 600 /home/pimlog/.ssh/authorized_keys && chown -R pimlog:pimlog /home/pimlog`（满足 StrictModes）
2. `mkdir -p /etc/pim/ssh`；若该目录下无 host key → `ssh-keygen -A -f /etc/pim/ssh/ssh_host_` 生成（ed25519/rsa/ecdsa）
3. 日志目录权限：`install -d -o pimlog -g pimlog /data/pim/logs`；**只 chown 目录本身，不递归**（避免日志多时启动变慢），历史文件靠 0644 世界可读兜底
4. 启动 supervisord

`pim.conf` 为静态文件，镜像构建时写入 `/etc/ssh/sshd_config.d/`（Debian 自带 Include 机制自动加载），entrypoint 不负责生成。

## 第 2 节：受限 SSH 用户与日志读取脚本

**用户：** `pimlog`，UID 1001，shell `/usr/sbin/nologin`。

**强制命令脚本 `/usr/local/bin/pim-log-cat.sh`**（容器内版 log-reader，无 journal 子命令）：

| 命令 | 说明 |
|------|------|
| `tail <file> [lines=N]` | 尾部 N 行（默认 50，上限 200） |
| `cat <file> [lines=N]` | 头部 N 行（默认 50，上限 200） |
| `ls [pattern]` | 列出日志文件 |
| `find <keyword> [file]` | 搜索（默认最近 3 个文件，上限 200 条） |
| `du` | 日志目录大小 |

- 只允许 `/data/pim/logs` 下文件名，白名单 `[a-zA-Z0-9_.-]+`，拒绝 `/`、`..`、路径穿越。
- 会话流量上限 10MB（沿用宿主机 logreader 同款配额逻辑）。
- 无 `tail -f` 实时跟踪（10MB 配额下合理），与宿主机 logreader 能力有差异，接受。
- sshd 自身日志不处理（容器内无 syslog 可丢弃），排障以 PIM jsonl 为准。

**`/etc/ssh/sshd_config.d/pim.conf`**（Debian 自带 Include 机制，容器内统一用 ForceCommand，不在 authorized_keys 逐行加 command=）：

```
PermitRootLogin no
PasswordAuthentication no
AllowUsers pimlog
X11Forwarding no
AllowTcpForwarding no
HostKey /etc/pim/ssh/ssh_host_ed25519_key
HostKey /etc/pim/ssh/ssh_host_rsa_key
Match User pimlog
    ForceCommand /usr/local/bin/pim-log-cat.sh
```

**HostKey 显式声明必须存在**——host key 自定义到 `/etc/pim/ssh/` 后 sshd 不会自动查找，不声明则启动报错。

**密钥注入：** `PIM_SSH_AUTHORIZED_KEYS` 为 base64 单行（多行公钥直放 .env 有解析风险），entrypoint 解码后逐行写入。

**host keys 持久化：** compose 挂命名卷 `pim_ssh_keys:/etc/pim/ssh`，容器重建指纹不变。

## 第 3 节：生产 compose（单容器）

新增 `docker-compose.prod.yml`（dev 版 `docker-compose.yml` 不动）：

```yaml
services:
  pim:
    image: ghcr.io/2746267826/pim-platform-server:${PIM_IMAGE_TAG:-latest}
    restart: unless-stopped
    ports:
      - "127.0.0.1:${PIM_HTTP_PORT:-5858}:5000"
      - "127.0.0.1:${PIM_SSH_PORT:-2222}:22"
    environment:
      # 全部配置走环境变量（清单见第 5 节）
      - ASPNETCORE_ENVIRONMENT=Production
      - ASPNETCORE_URLS=http://+:5000
      - TZ=Asia/Shanghai
      - ConnectionStrings__DefaultConnection=${PG_CONNECTION}
      - PIM_SSH_AUTHORIZED_KEYS=${PIM_SSH_AUTHORIZED_KEYS}
      - PIM_LOG_RETAINED_FILES=2
    volumes:
      - pim_data:/data                      # kopia/data-protection 等持久数据
      - /data/pim/logs:/data/pim/logs       # bind mount，宿主机 logreader 通道不变
      - /data/keys:/data/keys:ro            # jwt 私钥只读
      - pim_ssh_keys:/etc/pim/ssh           # SSH host keys 持久化
    healthcheck:
      test: ["CMD-SHELL", "bash -c '</dev/tcp/localhost/5000'"]
      interval: 15s
      timeout: 5s
      retries: 5
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  pim_data:
  pim_ssh_keys:
```

- 部署命令：`docker compose --env-file .env.prod -f docker-compose.prod.yml up -d`
- 配套 `.env.prod.example`（全部变量 + 中文注释）

## 第 4 节：CI 变更

**新增 `.github/workflows/build-docker.yml`**（workflow_call + workflow_dispatch，模式同 build-api.yml）：

```
inputs: version, artifact_slug, git_sha_short, is_release

build job:
  permissions: { contents: read, packages: write }
  steps:
    1. checkout
    2. resolve-version（复用 scripts/ci/resolve-version.sh）
    3. docker/setup-buildx-action
    4. docker/build-push-action：
       - context: .，file: src/Pim.Api/Dockerfile
       - push: false，load: true
       - tags: ghcr.io/2746267826/pim-platform-server:v{version} + :latest
       - cache-from/to: type=gha, scope=pim-server
    5. smoke test（始终执行）：docker run -d → docker exec bash -c '</dev/tcp/localhost/5000' 轮询 /health
       （Program.cs:104 已有 /health；无 DB 也能起，Program.cs 对迁移失败已容错）→ docker stop
       （用 exec 而非 -p 固定端口，避免并行 job 端口冲突）
    6. is_release == 'true' 时：
       - docker/login-action（GHCR，GITHUB_TOKEN）→ docker push 两个 tag
       - docker save ghcr.io/...:v{version} | gzip → pim-platform-server-v{version}.tar.gz
       - upload-artifact（供 release job 上传到 GitHub Release）
```

**ci.yml 修改（4 处）：**

1. `changes` 过滤器新增 `docker` 分组：`src/Pim.Api/Dockerfile`、`.github/workflows/build-docker.yml`、`docker-compose.prod.yml`、`.env.prod.example`
2. 新增 `build-docker` job：`needs: [resolve-version, changes]`，条件 `all || api || web || docker`，传 `is_release` 输入
3. `summarize` 表格加 Docker 行并纳入 needs
4. `release` job：
   - `needs` 追加 `build-docker`
   - `decide` 步骤的 BUILT 统计纳入 `needs.build-docker.result`（否则仅 docker 变更时误判"无构建"跳过 release）
   - 下载 pattern 与 `fill_if_skipped` 列表加入 `pim-platform-server-*.tar.gz`（docker 被跳过时从上次 release 复制）
   - `action-gh-release` 的 files 加入 `release-assets/pim-platform-server-*.tar.gz`

**守卫：** PR 场景 GITHUB_TOKEN 无 `packages: write`，因此 GHCR push 与镜像 tar 产物均以 `is_release == 'true'` 守卫；PR 只构建 + smoke，不推送。

**竞态规避：** build-docker 不直接 `gh release upload`（release 由独立 job 创建，并行时可能尚未存在）；镜像 tar 由 build-docker 产出 artifact，统一由 release job 在上传阶段附带发布，天然无竞态。

## 第 5 节：环境变量清单

**Docker 专属：**

| 变量 | 说明 |
|------|------|
| `PIM_SSH_AUTHORIZED_KEYS` | base64 单行公钥（entrypoint 解码写入 authorized_keys） |
| `PIM_LOG_RETAINED_FILES` | Serilog 保留份数（默认 30，生产 2 = 当天+昨天） |
| `PIM_IMAGE_TAG` / `PIM_HTTP_PORT` / `PIM_SSH_PORT` / `PG_CONNECTION` | compose 层插值（不进容器） |

**应用配置（`__` 嵌套映射 .NET 配置，对照 appsettings.json 全键）：**

```
运行环境：
  ASPNETCORE_ENVIRONMENT        # Production
  ASPNETCORE_URLS               # http://+:5000
  TZ                            # Asia/Shanghai

数据库与密钥：
  ConnectionStrings__DefaultConnection
  Jwt__PrivateKeyPath           # 默认 /data/keys/jwt_private.pem
  DataProtection__KeysPath      # 默认 /data/data-protection

对象存储与备份：
  Minio__Endpoint
  Minio__AccessKey
  Minio__SecretKey
  Kopia__RepositoryPath         # 默认 /data/kopia-repo
  Kopia__Password

文档解析：
  Tika__BaseUrl

AI（统一网关）：
  Ai__Enabled                   # 默认 false
  Ai__Provider                  # 默认 litellm
  Ai__BaseUrl
  Ai__ApiKey
  Ai__DefaultModel
  Ai__TimeoutSeconds            # 默认 30
  Ai__MaxOutputTokensPerRequest # 默认 1000
  Ai__MaxAttemptsPerRequest     # 默认 2
  Ai__SaveFullPrompts           # 默认 true
  Ai__SaveFullResponses         # 默认 true

第三方集成：
  Nextcloud__PublicBaseUrl
  Nextcloud__InternalBaseUrl
  OnlyOffice__PublicUrl
  OnlyOffice__JwtSecret

向量检索：
  Qdrant__BaseUrl
  Qdrant__Url
  Qdrant__Collection            # 默认 pim_file_chunks

文件安全（对照 appsettings.json 补齐，安全相关必须可配置）：
  Files__AiDisabledPathPatterns # JSON 数组，须用索引式环境变量：
                               # Files__AiDisabledPathPatterns__0=/Secrets/*
                               # Files__AiDisabledPathPatterns__1=/Passwords/*
                               # （默认值如上，逗号拼接的单值 env 对数组无效）
  Files__MaxInlineTextBytes     # 默认 1048576

嵌入：
  Embedding__Provider           # 默认 hashing
  Embedding__Dimensions         # 默认 384

日志级别（可选）：
  Logging__LogLevel__Default    # 默认 Information，需要时经 env 覆盖
```

- 镜像不内置任何真实配置；appsettings.json 仅保留开发默认值，全部由 env 覆盖。
- `.env.prod.example` 列出上述全部键（含默认值注明 + 中文注释），部署时复制为 `.env.prod` 填写。

## 第 6 节：测试策略

**CI 内（build-docker job）：**

1. `docker build`（push:false + load:true，带 gha 缓存）
2. smoke：`docker run -d` → `docker exec` + `/dev/tcp` 轮询 `/health` → 记录结果 → `docker stop`
3. `is_release=true` 时：push GHCR 两个 tag + `docker save | gzip` 产 artifact（由 release job 上传，上限 2GB，tar.gz 约 100-150MB，无风险）

**本地验收（实现后人工执行）：**

| 场景 | 验证点 |
|------|--------|
| SSH 日志读取 | `ssh -p 2222 -i key pimlog@127.0.0.1 tail pim-api-YYYYMMDD.jsonl lines=20` 成功 |
| SSH 安全 | 密码认证被拒；`PermitRootLogin no`；非白名单文件名（含 `/`、`..`）被拒 |
| 权限链 | authorized_keys 600/目录 700；root 运行 dotnet → 0644 日志 → pimlog 可读 |
| base64 密钥 | 多行公钥 → base64 → 容器内解码后 authorized_keys 内容一致 |
| host keys | 容器重建后指纹不变（named volume 生效） |
| 日志保留 | `PIM_LOG_RETAINED_FILES=2` 下滚动后仅剩当天+昨天（本 PR 内直接生效） |
| 回归 | `dotnet test Pim.sln` 与 web 构建不受影响（本 PR 无 C#/前端代码改动） |

## 范围外 / 后续工作

- 宿主机 `logreader` 通道保持现状（不做支持、不改动）；`/data/pim/logs` bind mount 保证其继续可读。
- dev 版 `docker-compose.yml` 与现有 Dockerfile 的本地构建路径不受影响。

> 注：`PIM_LOG_RETAINED_FILES`（Program.cs 读环境变量）已决定并入本 PR，不再是后续工作。

## 验收标准

1. GHCR 存在 `v{version}` 与 `latest` 两个 tag，且 release assets 中同时存在 `pim-platform-server-v{version}.tar.gz`。
2. 生产按 `.env.prod.example` 填写后 `docker compose -f docker-compose.prod.yml up -d` 可运行，全部配置经环境变量生效。
3. 容器内 `pimlog` 用户仅密钥可登录，仅能读取 `/data/pim/logs` 下日志；root/密码/任意命令均不可用。
4. 容器重建后 SSH host key 指纹不变。
5. 日志滚动仅保留当天 + 前一天（代码 PR 合并后验证）。
6. 原 tar.gz 部署通道与宿主机 logreader 不受影响。
