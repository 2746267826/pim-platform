# PIM 生产 Docker 化部署 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 生产端改用 Docker 部署 PIM 单容器（web+api 一体镜像），GitHub Actions 同时提供原有 tar.gz 产物与 GHCR 镜像；容器内提供受限 SSH 日志读取通道（`pimlog` 用户，仅密钥认证，base64 密钥经环境变量注入）；PIM 自身日志保留当天 + 前一天。

**架构：** 修改现有 `src/Pim.Api/Dockerfile` 运行时阶段为 aspnet:8.0 + openssh-server + supervisor + tini（tini 为 PID 1，supervisor 托管 sshd 与 dotnet）。新增容器脚本（entrypoint / 受限日志读取脚本 / supervisor 配置 / sshd 配置）、生产 compose（单容器，全部配置走环境变量）、build-docker CI workflow。`Program.cs` 的 Serilog 保留份数改为读 `PIM_LOG_RETAINED_FILES` 环境变量（默认 30，生产 2）。

**技术栈：** Docker / docker compose / GitHub Actions / bash / .NET 8 / Serilog / OpenSSH / supervisord / tini

**规格：** `docs/superpowers/specs/2026-08-10-pim-docker-deployment-design.md`

---

## 文件结构

| 文件 | 职责 | 操作 |
|------|------|------|
| `src/Pim.Api/Infrastructure/LoggingConfig.cs` | 解析 `PIM_LOG_RETAINED_FILES`（非法/缺失回退默认 30，下限 1） | 新建 |
| `src/Pim.Api/Program.cs` | Serilog `retainedFileCountLimit` 改用 `LoggingConfig.ResolveRetainedFileCount` | 修改（约 15-22 行） |
| `tests/Pim.UnitTests/Api/LoggingConfigTests.cs` | `LoggingConfig` 全部分支单元测试 | 新建 |
| `src/Pim.Api/Dockerfile` | 运行时阶段：装 openssh-server/supervisor/tini、建 pimlog 用户、复制脚本、ENTRYPOINT 改 tini | 修改（运行时阶段整体替换） |
| `scripts/docker/entrypoint.sh` | base64 密钥解码写 authorized_keys + 权限、生成 host key、chown 日志目录、启动 supervisord | 新建 |
| `scripts/docker/pim-log-cat.sh` | 容器内受限日志读取强制命令（tail/cat/ls/find/du，10MB 配额，文件名白名单） | 新建 |
| `scripts/docker/supervisord.conf` | 托管 sshd + dotnet Pim.Api.dll（nodaemon，日志丢弃） | 新建 |
| `scripts/docker/sshd-pim.conf` | sshd 安全配置 + `Match User pimlog` ForceCommand + HostKey 显式声明 | 新建 |
| `docker-compose.prod.yml` | 生产单容器编排（端口/环境变量/卷/healthcheck/logging） | 新建 |
| `.env.prod.example` | 全部环境变量模板（含默认值注明与中文注释） | 新建 |
| `.github/workflows/build-docker.yml` | 镜像构建 + smoke + 按 `is_release` 推送 GHCR / 导出 tar 产物 | 新建 |
| `.github/workflows/ci.yml` | docker 路径过滤、build-docker job、summarize 行、release job 集成 | 修改（4 处） |

**任务边界说明：** 任务 1（C# 代码）独立成任务因为它有独立的 TDD 测试循环与审查价值（Program.cs 行为变更）；任务 2-3（镜像与部署文件）彼此独立；任务 4（CI）依赖任务 1-3 的文件路径与脚本名（构建上下文与路径过滤引用），排在最后；任务 5 为端到端验收。

---

### 任务 1：PIM_LOG_RETAINED_FILES 环境变量支持（TDD）

**文件：**
- 创建：`src/Pim.Api/Infrastructure/LoggingConfig.cs`
- 创建：`tests/Pim.UnitTests/Api/LoggingConfigTests.cs`
- 修改：`src/Pim.Api/Program.cs:20-22`

- [ ] **步骤 1：编写失败的测试**

```csharp
using Pim.Api.Infrastructure;
using Xunit;

namespace Pim.UnitTests.Api;

public class LoggingConfigTests
{
    [Theory]
    [InlineData(null, 30)]                 // 未设置 -> 默认 30
    [InlineData("", 30)]                   // 空字符串 -> 默认 30
    [InlineData("   ", 30)]                // 空白 -> 默认 30
    [InlineData("2", 2)]                   // 正常值
    [InlineData("30", 30)]
    [InlineData("0", 30)]                  // 下限保护：0 -> 默认
    [InlineData("-1", 30)]                 // 负数 -> 默认
    [InlineData("abc", 30)]                // 非数字 -> 默认
    [InlineData("1.5", 30)]                // 非整数 -> 默认
    public void ResolveRetainedFileCount_ReturnsExpected(string? raw, int expected)
    {
        Assert.Equal(expected, LoggingConfig.ResolveRetainedFileCount(raw));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~LoggingConfigTests`
预期：FAIL（编译错误，`LoggingConfig` 不存在）

- [ ] **步骤 3：编写最少实现代码**

`src/Pim.Api/Infrastructure/LoggingConfig.cs`：

```csharp
namespace Pim.Api.Infrastructure;

public static class LoggingConfig
{
    public const int DefaultRetainedFileCount = 30;

    public static int ResolveRetainedFileCount(string? rawValue)
    {
        if (string.IsNullOrWhiteSpace(rawValue))
            return DefaultRetainedFileCount;
        if (int.TryParse(rawValue, out var parsed) && parsed >= 1)
            return parsed;
        return DefaultRetainedFileCount;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj --filter FullyQualifiedName~LoggingConfigTests`
预期：PASS（9/9）

- [ ] **步骤 5：接入 Program.cs**

`src/Pim.Api/Program.cs:15-23` 改为：

```csharp
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Debug()
    .Enrich.FromLogContext()
    .Enrich.WithProperty("Service", "pim-api")
    .WriteTo.Console(new CompactJsonFormatter())
    .WriteTo.File(new CompactJsonFormatter(), "/data/pim/logs/pim-api-.jsonl",
        rollingInterval: RollingInterval.Day,
        retainedFileCountLimit: LoggingConfig.ResolveRetainedFileCount(
            Environment.GetEnvironmentVariable("PIM_LOG_RETAINED_FILES")))
    .CreateLogger();
```

（`using Pim.Api.Infrastructure;` 已在 Program.cs:7 存在。）

- [ ] **步骤 6：全量回归**

运行：`dotnet test Pim.sln --no-restore`
预期：全部通过（基线 1092–1377 passing）

- [ ] **步骤 7：Commit**

```bash
git add src/Pim.Api/Infrastructure/LoggingConfig.cs src/Pim.Api/Program.cs tests/Pim.UnitTests/Api/LoggingConfigTests.cs
git commit -m "feat: Serilog 保留份数支持 PIM_LOG_RETAINED_FILES 环境变量"
```

---

### 任务 2：Dockerfile 运行时阶段 + 容器脚本

**文件：**
- 修改：`src/Pim.Api/Dockerfile`（运行时阶段整体替换）
- 创建：`scripts/docker/entrypoint.sh`
- 创建：`scripts/docker/pim-log-cat.sh`
- 创建：`scripts/docker/supervisord.conf`
- 创建：`scripts/docker/sshd-pim.conf`

- [ ] **步骤 1：创建 `scripts/docker/sshd-pim.conf`**

```ini
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

- [ ] **步骤 2：创建 `scripts/docker/supervisord.conf`**

```ini
[supervisord]
nodaemon=true
logfile=/dev/null
pidfile=/tmp/supervisord.pid

[unix_http_server]
file=/tmp/supervisor.sock

[rpcinterface:supervisor]
supervisor.rpcinterface.factory = supervisor.rpcinterface:make_main_rpcinterface

[supervisorctl]
serverurl=unix:///tmp/supervisor.sock

[program:sshd]
command=/usr/sbin/sshd -D -e
autorestart=true
stdout_logfile=/dev/null
stderr_logfile=/dev/null

[program:pim-api]
command=dotnet Pim.Api.dll
directory=/app
autorestart=true
stdout_logfile=/dev/null
stderr_logfile=/dev/null
```

- [ ] **步骤 3：创建 `scripts/docker/pim-log-cat.sh`**（容器内受限日志读取，无 journal 命令）

```bash
#!/bin/bash
# PIM container log reader - restricted forced command for user pimlog
set -euo pipefail

readonly LOG_DIR="/data/pim/logs"
readonly MAX_BYTES=$(( 10 * 1024 * 1024 ))  # 10 MB per session cap
readonly SESSION_FILE=$(mktemp /tmp/pim-log-session-XXXXXX)
trap 'rm -f "$SESSION_FILE"' EXIT
echo 0 > "$SESSION_FILE"

if [[ -z "${SSH_ORIGINAL_COMMAND:-}" ]]; then
    echo "PIM container log reader (read-only)"
    echo ""
    echo "可用命令:"
    echo "  tail <filename> [lines=N]   — 查看日志文件尾部（默认 50，最大 200）"
    echo "  cat <filename> [lines=N]    — 查看日志文件开头（默认 50，最大 200）"
    echo "  ls [pattern]                — 列出日志目录中的文件"
    echo "  find <keyword> [filename]   — 搜索关键词（默认最近 3 个文件，最大 200 行）"
    echo "  du                          — 查看日志目录总大小（不读取内容）"
    echo ""
    echo "安全限制: 每次会话最多读取 10MB 数据，文件名仅允许字母数字._-"
    echo ""
    echo "示例:"
    echo "  ssh pim@host tail pim-api-20260810.jsonl lines=100"
    echo "  ssh pim@host find 'ERROR'"
    exit 0
fi

IFS=' ' read -ra ARGS <<< "$SSH_ORIGINAL_COMMAND"
CMD="${ARGS[0]}"
REMAINING=("${ARGS[@]:1}")

check_quota() {
    local bytes="$1"
    local used=$(cat "$SESSION_FILE")
    local total=$(( used + bytes ))
    if (( total > MAX_BYTES )); then
        echo "错误: 会话读取量超过限制 (已用: ${used}B, 请求: ${bytes}B, 上限: ${MAX_BYTES}B)" >&2
        exit 1
    fi
    echo "$total" > "$SESSION_FILE"
}

lines_to_bytes() { echo $(( $1 * 2048 )); }

validate_filename() {
    local name="$1"
    if [[ ! "$name" =~ ^[a-zA-Z0-9_.-]+$ ]]; then
        echo "错误: 文件名包含非法字符: ${name}" >&2
        exit 1
    fi
    if [[ "$name" == *"/"* ]]; then
        echo "错误: 不允许使用路径，请在 ${LOG_DIR} 下操作" >&2
        exit 1
    fi
}

validate_lines() {
    local lines="$1"
    if [[ ! "$lines" =~ ^[0-9]+$ ]] || (( lines < 1 )) || (( lines > 200 )); then
        echo "错误: lines 必须为 1-200 的整数" >&2
        exit 1
    fi
}

case "$CMD" in
    tail)
        [[ ${#REMAINING[@]} -ge 1 ]] || { echo "用法: tail <filename> [lines=N]" >&2; exit 1; }
        FILENAME="${REMAINING[0]}"; LINES=50
        [[ ${#REMAINING[@]} -ge 2 ]] && { LINES="${REMAINING[1]#lines=}"; validate_lines "$LINES"; }
        validate_filename "$FILENAME"
        check_quota $(lines_to_bytes "$LINES")
        exec tail -n "$LINES" "${LOG_DIR}/${FILENAME}"
        ;;
    cat)
        [[ ${#REMAINING[@]} -ge 1 ]] || { echo "用法: cat <filename> [lines=N]" >&2; exit 1; }
        FILENAME="${REMAINING[0]}"; LINES=50
        [[ ${#REMAINING[@]} -ge 2 ]] && { LINES="${REMAINING[1]#lines=}"; validate_lines "$LINES"; }
        validate_filename "$FILENAME"
        check_quota $(lines_to_bytes "$LINES")
        exec head -n "$LINES" "${LOG_DIR}/${FILENAME}"
        ;;
    ls)
        check_quota 50000
        if [[ ${#REMAINING[@]} -ge 1 ]]; then
            PATTERN="${REMAINING[0]}"
            if [[ ! "$PATTERN" =~ ^[a-zA-Z0-9_.*-]+$ ]]; then
                echo "错误: pattern 包含非法字符" >&2
                exit 1
            fi
            ls -lh ${LOG_DIR}/${PATTERN} 2>/dev/null || echo "无匹配文件"
        else
            exec ls -lh "${LOG_DIR}/"
        fi
        ;;
    find)
        [[ ${#REMAINING[@]} -ge 1 ]] || { echo "用法: find <keyword> [filename]" >&2; exit 1; }
        KEYWORD="${REMAINING[0]}"; MAX_MATCHES=200
        if [[ ${#REMAINING[@]} -ge 2 ]]; then
            FILENAME="${REMAINING[1]}"; validate_filename "$FILENAME"
            check_quota 524288
            grep -h -i --max-count="$MAX_MATCHES" "$KEYWORD" "${LOG_DIR}/${FILENAME}" 2>/dev/null || echo "无匹配"
        else
            check_quota 1048576
            FILES=($(ls -t "${LOG_DIR}/"*.jsonl 2>/dev/null | head -3))
            for f in "${FILES[@]}"; do
                echo "=== $(basename "$f") ==="
                grep -h -i --max-count="$MAX_MATCHES" "$KEYWORD" "$f" 2>/dev/null || echo "(无匹配)"
            done
        fi
        ;;
    du)
        check_quota 50000
        exec du -sh "${LOG_DIR}/"
        ;;
    *)
        echo "错误: 未知命令: ${CMD}" >&2
        echo "可用命令: tail, cat, ls, find, du" >&2
        exit 1
        ;;
esac
```

- [ ] **步骤 4：创建 `scripts/docker/entrypoint.sh`**

```bash
#!/bin/bash
set -euo pipefail

# 1. authorized_keys：PIM_SSH_AUTHORIZED_KEYS 为 base64 单行，解码后逐行写入
if [[ -n "${PIM_SSH_AUTHORIZED_KEYS:-}" ]]; then
    mkdir -p /home/pimlog/.ssh
    echo "$PIM_SSH_AUTHORIZED_KEYS" | base64 -d > /home/pimlog/.ssh/authorized_keys
    chown -R pimlog:pimlog /home/pimlog/.ssh
    chmod 700 /home/pimlog/.ssh
    chmod 600 /home/pimlog/.ssh/authorized_keys
fi

# 2. SSH host keys（/etc/pim/ssh 为持久化卷；缺失才生成，重建容器指纹不变）
mkdir -p /etc/pim/ssh
if [[ ! -s /etc/pim/ssh/ssh_host_ed25519_key ]]; then
    ssh-keygen -A -f /etc/pim/ssh/ssh_host_
fi

# 3. sshd 运行所需目录
mkdir -p /run/sshd

# 4. 日志目录归属（只 chown 目录本身，不递归；文件 0644 世界可读兜底）
install -d -o pimlog -g pimlog /data/pim/logs

# 5. 启动 supervisord（tini 为 PID 1，负责回收僵尸与转发信号）
exec supervisord -n -c /etc/supervisor/supervisord.conf
```

- [ ] **步骤 5：替换 Dockerfile 运行时阶段**

`src/Pim.Api/Dockerfile` 末尾（`FROM mcr.microsoft.com/dotnet/aspnet:8.0` 起，即最后 6 行）整体替换为：

```dockerfile
FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=server-build /app/publish .
EXPOSE 5000 22

RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-server supervisor tini \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 -m -d /home/pimlog -s /usr/sbin/nologin pimlog

COPY scripts/docker/sshd-pim.conf /etc/ssh/sshd_config.d/pim.conf
COPY scripts/docker/supervisord.conf /etc/supervisor/supervisord.conf
COPY scripts/docker/entrypoint.sh /entrypoint.sh
COPY scripts/docker/pim-log-cat.sh /usr/local/bin/pim-log-cat.sh

RUN chmod 755 /entrypoint.sh /usr/local/bin/pim-log-cat.sh \
    && chmod 644 /etc/ssh/sshd_config.d/pim.conf /etc/supervisor/supervisord.conf

ENTRYPOINT ["tini", "--", "/entrypoint.sh"]
```

- [ ] **步骤 6：静态验证脚本语法**

运行：`bash -n scripts/docker/entrypoint.sh scripts/docker/pim-log-cat.sh`
预期：无输出（语法通过）

- [ ] **步骤 7：构建镜像（CI 环境或本地有 docker daemon 的机器）**

运行：`docker build -f src/Pim.Api/Dockerfile -t pim-server:test .`
预期：构建成功；最后一步显示 `ENTRYPOINT ["tini", "--", "/entrypoint.sh"]`
（本机无 docker daemon 时此步骤在 CI 验证，见任务 4。）

- [ ] **步骤 8：Commit**

```bash
git add src/Pim.Api/Dockerfile scripts/docker/
git commit -m "feat: 镜像运行时加 tini/supervisord/sshd 与受限日志读取脚本"
```

---

### 任务 3：生产 compose 与环境变量模板

**文件：**
- 创建：`docker-compose.prod.yml`
- 创建：`.env.prod.example`

- [ ] **步骤 1：创建 `docker-compose.prod.yml`**

```yaml
services:
  pim:
    image: ghcr.io/2746267826/pim-platform-server:${PIM_IMAGE_TAG:-latest}
    restart: unless-stopped
    ports:
      - "127.0.0.1:${PIM_HTTP_PORT:-5858}:5000"
      - "127.0.0.1:${PIM_SSH_PORT:-2222}:22"
    environment:
      - ASPNETCORE_ENVIRONMENT=Production
      - ASPNETCORE_URLS=http://+:5000
      - TZ=Asia/Shanghai
      - ConnectionStrings__DefaultConnection=${PG_CONNECTION}
      - Jwt__PrivateKeyPath=/data/keys/jwt_private.pem
      - DataProtection__KeysPath=/data/data-protection
      - Minio__Endpoint=${MINIO_ENDPOINT}
      - Minio__AccessKey=${MINIO_ACCESS_KEY}
      - Minio__SecretKey=${MINIO_SECRET_KEY}
      - Kopia__RepositoryPath=/data/kopia-repo
      - Kopia__Password=${KOPIA_PASSWORD}
      - Tika__BaseUrl=${TIKA_BASE_URL}
      - Ai__Enabled=${AI_ENABLED:-false}
      - Ai__Provider=litellm
      - Ai__BaseUrl=${AI_BASE_URL}
      - Ai__ApiKey=${AI_API_KEY}
      - Ai__DefaultModel=${AI_DEFAULT_MODEL:-pim-default}
      - Ai__TimeoutSeconds=30
      - Ai__MaxOutputTokensPerRequest=1000
      - Ai__MaxAttemptsPerRequest=2
      - Ai__SaveFullPrompts=true
      - Ai__SaveFullResponses=true
      - Nextcloud__PublicBaseUrl=${NEXTCLOUD_PUBLIC_BASE_URL}
      - Nextcloud__InternalBaseUrl=${NEXTCLOUD_INTERNAL_BASE_URL}
      - OnlyOffice__PublicUrl=${ONLYOFFICE_PUBLIC_URL}
      - OnlyOffice__JwtSecret=${ONLYOFFICE_JWT_SECRET}
      - Qdrant__BaseUrl=${QDRANT_BASE_URL}
      - Qdrant__Url=${QDRANT_BASE_URL}
      - Qdrant__Collection=pim_file_chunks
      - Files__AiDisabledPathPatterns__0=/Secrets/*
      - Files__AiDisabledPathPatterns__1=/Passwords/*
      - Files__MaxInlineTextBytes=1048576
      - Embedding__Provider=hashing
      - Embedding__Dimensions=384
      - PIM_SSH_AUTHORIZED_KEYS=${PIM_SSH_AUTHORIZED_KEYS}
      - PIM_LOG_RETAINED_FILES=2
    volumes:
      - pim_data:/data
      - /data/pim/logs:/data/pim/logs
      - /data/keys:/data/keys:ro
      - pim_ssh_keys:/etc/pim/ssh
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

- [ ] **步骤 2：创建 `.env.prod.example`**

```bash
# PIM Docker 生产部署环境变量模板
# 使用: cp .env.prod.example .env.prod && 填写真实值
# 部署: docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

# --- 镜像与端口 ---
PIM_IMAGE_TAG=latest
PIM_HTTP_PORT=5858            # 宿主机 HTTP 端口（默认仅绑 127.0.0.1）
PIM_SSH_PORT=2222             # 宿主机 SSH 端口（默认仅绑 127.0.0.1）

# --- 数据库 ---
PG_CONNECTION=Host=CHANGE_ME;Database=pim;Username=pim;Password=CHANGE_ME

# --- 容器 SSH 公钥（base64 单行，必须）---
# 生成: printf 'ssh-ed25519 AAAA...\nssh-ed25519 AAAA...\n' | base64 -w0
# 建议包含 .reasonix/production-log-key.pub 以便沿用现有调试通道
PIM_SSH_AUTHORIZED_KEYS=CHANGE_ME

# --- 日志保留（当天 + 前一天 = 2）---
PIM_LOG_RETAINED_FILES=2

# --- 应用配置 ---
MINIO_ENDPOINT=CHANGE_ME
MINIO_ACCESS_KEY=CHANGE_ME
MINIO_SECRET_KEY=CHANGE_ME
KOPIA_PASSWORD=CHANGE_ME
TIKA_BASE_URL=http://CHANGE_ME:9998
AI_ENABLED=false
AI_BASE_URL=http://CHANGE_ME:4000
AI_API_KEY=CHANGE_ME
AI_DEFAULT_MODEL=pim-default
NEXTCLOUD_PUBLIC_BASE_URL=http://CHANGE_ME
NEXTCLOUD_INTERNAL_BASE_URL=http://CHANGE_ME
ONLYOFFICE_PUBLIC_URL=http://CHANGE_ME
ONLYOFFICE_JWT_SECRET=CHANGE_ME
QDRANT_BASE_URL=http://CHANGE_ME:6333
```

- [ ] **步骤 3：校验 compose 语法**

运行：`docker compose -f docker-compose.prod.yml config`（需 .env.prod 或环境变量；无 daemon 也可解析）
预期：输出渲染后的完整配置，`image: ghcr.io/2746267826/pim-platform-server:latest`，端口 `127.0.0.1:5858->5000`
（若本机无法运行 docker compose，至少执行 `bash -n` 不可行——compose 用 YAML 解析器；备选：`docker compose config` 在有 docker 的机器/CI 上执行）

- [ ] **步骤 4：Commit**

```bash
git add docker-compose.prod.yml .env.prod.example
git commit -m "feat: 生产单容器 compose 与环境变量模板"
```

---

### 任务 4：build-docker workflow + ci.yml 集成

**文件：**
- 创建：`.github/workflows/build-docker.yml`
- 修改：`.github/workflows/ci.yml`（4 处）

- [ ] **步骤 1：创建 `.github/workflows/build-docker.yml`**

```yaml
name: Build Docker Image

on:
  workflow_call:
    inputs:
      version:
        required: true
        type: string
      artifact_slug:
        required: true
        type: string
      git_sha_short:
        required: true
        type: string
      is_release:
        required: true
        type: string
  workflow_dispatch:
    inputs:
      version:
        required: false
        default: ''
        type: string
      artifact_slug:
        required: false
        default: ''
        type: string
      git_sha_short:
        required: false
        default: ''
        type: string
      is_release:
        required: false
        default: 'false'
        type: string

env:
  IMAGE: ghcr.io/2746267826/pim-platform-server

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Resolve version if needed
        id: ver
        shell: bash
        run: |
          if [ -n "${{ inputs.version }}" ]; then
            echo "version=${{ inputs.version }}" >> "$GITHUB_OUTPUT"
            echo "artifact_slug=${{ inputs.artifact_slug }}" >> "$GITHUB_OUTPUT"
            echo "git_sha_short=${{ inputs.git_sha_short }}" >> "$GITHUB_OUTPUT"
            echo "is_release=${{ inputs.is_release }}" >> "$GITHUB_OUTPUT"
          else
            bash "$GITHUB_WORKSPACE/scripts/ci/resolve-version.sh"
          fi

      - uses: docker/setup-buildx-action@v3

      - name: Build and load image
        uses: docker/build-push-action@v6
        with:
          context: .
          file: src/Pim.Api/Dockerfile
          push: false
          load: true
          tags: |
            ${{ env.IMAGE }}:v${{ steps.ver.outputs.artifact_slug }}
            ${{ env.IMAGE }}:latest
          cache-from: type=gha,scope=pim-server
          cache-to: type=gha,mode=max,scope=pim-server

      - name: Smoke test
        run: |
          CID="$(docker run -d "${{ env.IMAGE }}:latest")"
          trap 'docker rm -f "$CID" >/dev/null 2>&1 || true' EXIT
          for i in $(seq 1 30); do
            if docker exec "$CID" bash -c \
              'exec 3<>/dev/tcp/localhost/5000; printf "GET /health HTTP/1.0\r\n\r\n" >&3; grep -q healthy <&3' 2>/dev/null; then
              echo "smoke: /health OK"
              exit 0
            fi
            sleep 2
          done
          echo "smoke: /health FAILED" >&2
          exit 1

      - name: Login to GHCR
        if: steps.ver.outputs.is_release == 'true'
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Push tags
        if: steps.ver.outputs.is_release == 'true'
        run: |
          docker push "${{ env.IMAGE }}:v${{ steps.ver.outputs.artifact_slug }}"
          docker push "${{ env.IMAGE }}:latest"

      - name: Export image tarball
        if: steps.ver.outputs.is_release == 'true'
        run: |
          mkdir -p build/artifacts
          docker save "${{ env.IMAGE }}:v${{ steps.ver.outputs.artifact_slug }}" \
            | gzip > "build/artifacts/pim-platform-server-v${{ steps.ver.outputs.artifact_slug }}.tar.gz"
          echo "Artifact size: $(du -sh build/artifacts/*.tar.gz | cut -f1)"

      - name: Upload docker artifact
        if: steps.ver.outputs.is_release == 'true'
        uses: actions/upload-artifact@v7
        with:
          name: pim-platform-server-v${{ steps.ver.outputs.artifact_slug }}
          path: build/artifacts/pim-platform-server-*.tar.gz
```

- [ ] **步骤 2：ci.yml — `changes` 过滤器新增 docker 分组**

`ci.yml` 的 `dorny/paths-filter@v3` filters 块（android 分组之后）追加：

```yaml
            docker:
              - 'src/Pim.Api/Dockerfile'
              - 'scripts/docker/**'
              - '.github/workflows/build-docker.yml'
              - '.github/workflows/ci.yml'
              - 'docker-compose.prod.yml'
              - '.env.prod.example'
```

`flags` 步骤输出追加（`echo "android=${{ steps.filter.outputs.android }}"` 行之后）：

```bash
          echo "docker=${{ steps.filter.outputs.docker }}" >> "$GITHUB_OUTPUT"
```

`changes` job 的 outputs 追加 `docker: ${{ steps.flags.outputs.docker }}`。

- [ ] **步骤 3：ci.yml — 新增 build-docker job**

`build-android` job 之后追加：

```yaml
  build-docker:
    needs: [resolve-version, changes]
    if: needs.changes.outputs.all == 'true' || needs.changes.outputs.api == 'true' || needs.changes.outputs.web == 'true' || needs.changes.outputs.docker == 'true'
    uses: ./.github/workflows/build-docker.yml
    with:
      version: ${{ needs.resolve-version.outputs.version }}
      artifact_slug: ${{ needs.resolve-version.outputs.artifact_slug }}
      git_sha_short: ${{ needs.resolve-version.outputs.git_sha_short }}
      is_release: ${{ needs.resolve-version.outputs.is_release }}
```

- [ ] **步骤 4：ci.yml — summarize 加 Docker 行**

`summarize` job 的 `needs` 追加 `build-docker`；表格追加行（Windows 行之后）：

```yaml
          echo "| 🐳  Docker  | ${{ needs.build-docker.result == 'success' && '✅ Built' || needs.build-docker.result == 'skipped' && '⏭️ Skipped' || '❌ Failed' }} |"
```

- [ ] **步骤 5：ci.yml — release job 集成（4 处）**

5a. `release` 的 `needs` 追加 `build-docker`。
5b. `decide` 步骤 BUILT 循环追加 `"${{ needs.build-docker.result }}"`（docker-only 变更也计入构建）。
5c. `fill_if_skipped` 调用与 sanity check 循环追加 `pim-platform-server-*.tar.gz`：

```bash
          fill_if_skipped "${{ needs.build-docker.result }}" 'pim-platform-server-*.tar.gz'
```
```bash
          for p in pim-api-*.tar.gz pim-web-*.tar.gz pim-windows-*.zip pim-android-*.apk pim-platform-server-*.tar.gz; do
```
5d. `action-gh-release` 的 `files` 追加：

```yaml
            release-assets/pim-platform-server-*.tar.gz
```

- [ ] **步骤 6：YAML 校验**

运行：`ruby -e "require 'yaml'; YAML.load_file('.github/workflows/build-docker.yml'); YAML.load_file('.github/workflows/ci.yml'); puts 'YAML OK'"`（或 python：`python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/build-docker.yml')); yaml.safe_load(open('.github/workflows/ci.yml')); print('YAML OK')"`）
预期：YAML OK（GitHub Actions 表达式 `${...}` 在双引号字符串内，YAML 解析不受影响）

- [ ] **步骤 7：Commit**

```bash
git add .github/workflows/build-docker.yml .github/workflows/ci.yml
git commit -m "ci: 新增 Docker 镜像构建/推送/导出与 release 集成"
```

---

### 任务 5：端到端验收

**文件：** 无（验证任务）

- [ ] **步骤 1：推送分支并等待 CI**

```bash
git push -u origin opencode-linux/docker-deploy
```

预期：
- PR 上 CI：api/web/windows/android 正常构建；build-docker 构建 + smoke 通过；无 GHCR push（is_release=false）
- master 合并后：GHCR 出现 `v{version}` 与 `latest` 两个 tag；release assets 含 `pim-platform-server-v{version}.tar.gz`

- [ ] **步骤 2：本地全流程验证（有 docker daemon 的机器）**

```bash
# 构建
docker build -f src/Pim.Api/Dockerfile -t pim-server:test .
# 生成测试密钥对
ssh-keygen -t ed25519 -f /tmp/pimtestkey -N '' -C pim-test
# 运行（映射测试端口 + 注入 base64 公钥）
KEYS=$(cat /tmp/pimtestkey.pub | base64 -w0)
docker run -d --name pim-test -e PIM_SSH_AUTHORIZED_KEYS="$KEYS" \
  -e ConnectionStrings__DefaultConnection="Host=127.0.0.1;Database=pim;Username=pim;Password=x" \
  -p 127.0.0.1:5858:5000 -p 127.0.0.1:2222:22 -v pim-test-ssh:/etc/pim/ssh pim-server:test
```

预期：
- `curl -s http://127.0.0.1:5858/health` 返回 `{"status":"healthy",...}`

- [ ] **步骤 3：SSH 日志读取与安全验证**

```bash
# 受限读取成功
ssh -i /tmp/pimtestkey -p 2222 -o StrictHostKeyChecking=no pimlog@127.0.0.1 du
# tail 成功（若容器内已产生日志）
ssh -i /tmp/pimtestkey -p 2222 -o StrictHostKeyChecking=no pimlog@127.0.0.1 tail pim-api-20260810.jsonl lines=20
# 越权被拒（路径穿越）
ssh -i /tmp/pimtestkey -p 2222 -o StrictHostKeyChecking=no pimlog@127.0.0.1 tail ../../etc/passwd
# 任意命令被拒（ForceCommand 生效）
ssh -i /tmp/pimtestkey -p 2222 -o StrictHostKeyChecking=no pimlog@127.0.0.1 ls /etc
```

预期：du/tail 正常输出；`../../etc/passwd` 报"文件名包含非法字符"；`ls /etc` 报"未知命令: ls"

- [ ] **步骤 4：认证安全验证**

预期（以下均失败）：
- 密码认证：`ssh -p 2222 pimlog@127.0.0.1` 无密钥 → 拒绝（PasswordAuthentication no）
- root 登录：`ssh -i /tmp/pimtestkey -p 2222 root@127.0.0.1` → 拒绝（PermitRootLogin no）

- [ ] **步骤 5：持久化与权限验证**

```bash
# 权限
docker exec pim-test bash -c 'ls -la /home/pimlog/.ssh/; stat -c "%U:%G %a %n" /home/pimlog/.ssh/authorized_keys'
# host key 持久化：记录指纹 → 重建容器 → 指纹不变
ssh-keyscan -p 2222 127.0.0.1 | sha256sum > /tmp/fp1
docker rm -f pim-test && docker run -d --name pim-test2 ...（同上参数）
ssh-keyscan -p 2222 127.0.0.1 | sha256sum > /tmp/fp2 && diff /tmp/fp1 /tmp/fp2
```

预期：`.ssh` 700 / authorized_keys 600 且属主 pimlog；重建后指纹一致（无 known_hosts 告警）

- [ ] **步骤 6：日志保留验证（本 PR 代码已生效）**

```bash
# 容器内造 4 个历史滚动文件，重启服务触发滚动清理
docker exec pim-test bash -c 'cd /data/pim/logs && for d in 20260806 20260807 20260808 20260809; do touch pim-api-$d.jsonl; done'
docker exec pim-test bash -c 'supervisorctl -c /etc/supervisor/supervisord.conf restart pim-api'
docker exec pim-test ls /data/pim/logs/
```

预期：`PIM_LOG_RETAINED_FILES=2` 生效后仅剩当天 + 前一天文件（历史 4 个被 Serilog 滚动清理）

- [ ] **步骤 7：清理测试容器**

```bash
docker rm -f pim-test pim-test2 2>/dev/null || true
docker volume rm pim-test-ssh 2>/dev/null || true
```

- [ ] **步骤 8：回归收尾**

运行：`dotnet test Pim.sln --no-restore` 与 `npm --prefix src/client-web run build`
预期：全部通过；无前端改动

---

## 自检结果

- **规格覆盖度**：规格 6 节全部有对应任务——第 1 节（镜像/entrypoint/tini/权限）→ 任务 2；第 2 节（SSH 用户/脚本/sshd 配置）→ 任务 2；第 3 节（compose）→ 任务 3；第 4 节（CI）→ 任务 4；第 5 节（环境变量清单）→ 任务 3 `.env.prod.example` + compose environment；第 6 节（测试）→ 任务 1 步骤 6、任务 4 步骤 1 smoke、任务 5 全量；验收标准 → 任务 5。
- **代码变更并入**：`PIM_LOG_RETAINED_FILES`（Program.cs）在任务 1，无独立 PR 拆分，与规格决策摘要一致。
- **占位符扫描**：无 TODO/待定；每个步骤含完整代码或命令。
- **类型/名称一致性**：`LoggingConfig.ResolveRetainedFileCount(string?)` 在测试与 Program.cs 中签名一致；脚本名 `pim-log-cat.sh`、用户 `pimlog`、卷 `pim_ssh_keys`、镜像 `ghcr.io/2746267826/pim-platform-server`、artifact `pim-platform-server-v*.tar.gz` 在 Dockerfile/compose/CI 三处一致。
