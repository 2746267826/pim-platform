# PIM 全端版本号、图标与 GitHub Actions 设计

**日期：** 2026-07-12  
**分支：** `codex/version-icons-github-actions`  
**状态：** 已确认（§1–§4）

## 目标

统一 API / Web / Windows / Android 的产品版本号策略与运行时注入；补齐并统一各端图标资产；完善 GitHub Actions，使 master 成功构建自动发布带版本的 GitHub Release，且 Android PR 与 master 使用相同签名以便覆盖安装升级。

## 背景与现状

- 版本算法不一致：Web/Windows 用 `git describe`；API/Android 用 `0.1.${GITHUB_RUN_NUMBER}+sha`；API publish 未注入版本，`/api/version` 易读到默认值。
- 无 git tag 时各端显示退化为 `0.0.0` / 本地占位。
- 图标缺失：Web `favicon.svg` 被引用但不存在；Windows `app.ico` 被引用但未嵌入；Android 无 `res` launcher 图标、`AndroidManifest` 未声明 `android:icon`。
- 四条 workflow 各自生成版本；触发分支含临时名；产物命名与 Release 不完整。

## 决策摘要

| 项 | 选择 |
|----|------|
| 版本方案 | CalVer：`YYYY.MM.N`，`N` 为编排 workflow 的全局构建序号 |
| 端间关系 | 核心统一；客户端允许独立补丁后缀 |
| 定版时机 | master 合并后自动递增（不依赖手打 tag 定主号） |
| 实现路径 | 编排工作流 + 共享版本脚本（方案 1） |
| Release | 每次 master 成功构建都发 GitHub Release |
| 图标 | 微软四色 + 「四色拼 P」；`branding/` 源图导出各端 |
| Android 签名 | PR 与 master 同一 keystore secrets，可升级覆盖安装 |

## §1 版本规则

### 格式

- **正式显示名：** `YYYY.MM.N`（例：`2026.07.42`）
- **`N`：** 仓库级全局构建序号 = 编排 workflow（`ci.yml`）在该次运行的 `GITHUB_RUN_NUMBER`。同一次 master/PR 编排中四端共用同一 `N`。
- **短 SHA：** 写入 Release notes / artifact 元数据；**不进入** master 正式产品主显示号。
- **versionCode（Android）：** 固定公式 `version_code = 100000 + N`（单调、永不回退；与历史 CI 基数兼容）。

### 场景矩阵

| 场景 | versionName / 显示 | Android versionCode | Release |
|------|-------------------|---------------------|---------|
| master 正式 | `2026.07.42` | 与 N 映射的单调 code | 是，tag `v2026.07.42` |
| PR | `2026.07.42-pr.{pr}+{sha7}` | 同映射策略（可与 master 不同 N，但签名相同） | 否 |
| 非 master 手动 | `2026.07.42-dev+{sha7}` | 同上 | 否 |
| 客户端独立补丁 | `2026.07.42+android.1`（或 `+win.1` 等） | `100000 + N` 仍用本次编排 N；补丁仅改显示后缀 | 仅当该次 workflow 输入 `client_patch`（端名.序号）且跑在 master 时发 Release |
| 本地开发 | `0.0.0-local` 或 `YYYY.MM.0-local+dev` | 本地默认 1 | 否 |

### 单一解析出口

- 脚本：`scripts/ci/resolve-version.sh`（Windows runner 通过 bash 或薄封装 `resolve-version.ps1` 调用同一逻辑）
- Composite action：`.github/actions/resolve-version/action.yml`
- 输出至少包含：
  - `version` — 产品显示主串
  - `version_code` — Android 整数
  - `artifact_slug` — 文件名安全串
  - `git_sha_short` — 7 位
  - `is_release` — 仅 `refs/heads/master` 为 true
  - `year_month` — `YYYY.MM`（便于诊断）

### 不做什么（版本）

- 不维护需提交回仓库的手改 `VERSION` 文件 bot
- 不按各端独立 `GITHUB_RUN_NUMBER` 生成主版本号
- 不把 SemVer `0.1.x` 与 CalVer 混用

## §2 品牌与图标

### 源资产

- 目录：`branding/`
- 主源：`branding/pim-mark.svg` — **四色拼 P**
  - 色值：`#f25022`、`#7fba00`、`#00a4ef`、`#ffb900`
  - 白/透明底，适配系统图标遮罩
- 说明：`branding/README.md`（色值、导出命令、禁止手改派生文件）

### 几何（四色拼 P）

与已确认视觉一致：

- 左侧竖条：`#00a4ef`
- 上横块：`#f25022`
- 中横块：`#7fba00`
- 右下圆点：`#ffb900`
- 整体构成可识别的 **P** 字形

### 派生产物

| 目标 | 路径 | 规格 |
|------|------|------|
| Web favicon SVG | `src/client-web/public/favicon.svg` | 与源一致或精简 viewBox |
| Web favicon ICO | `src/client-web/public/favicon.ico` | 多尺寸（16/32/48） |
| Apple touch（可选最小） | `src/client-web/public/apple-touch-icon.png` | 180×180 |
| Windows | `src/client-windows/Pim.Client.App/app.ico` | 16/32/48/256；csproj `ApplicationIcon` + Resource 嵌入 |
| Android | `src/client-android/app/src/main/res/mipmap-*/ic_launcher*.png` 与/或 adaptive XML | manifest 声明 `android:icon` / `android:roundIcon` |

### 导出

- `scripts/branding/export-icons`（实现时选可复现方式：Node/sharp 或已文档化的 CLI）
- 本地一键导出；CI 可在 `branding/**` 变更时校验关键派生路径存在（源变更未导出则失败）

### 运行时

- Windows：托盘加载 `app.ico` 成功，不再长期回落 `SystemIcons.Application`
- Android：启动器显示自定义图标
- Web：`index.html` favicon 可访问

## §3 GitHub Actions 与 Release

### 编排

- 新增：`.github/workflows/ci.yml`
  - 触发：`push` → `master`；`pull_request` → `master`；`workflow_dispatch`
  - 首 job：`resolve-version`
  - **master：始终构建四端**（保证 Release 资产齐全）
  - **PR：path filter** 只构建变更相关端；版本号仍由同一 resolve job 给出
  - 末 job：`release`（仅 `is_release` 且本次 **四端均成功**）

### 现有 workflow

- `build-api.yml` / `build-web.yml` / `build-windows.yml` / `build-android.yml`：
  - 改为 `workflow_call` 可复用工作流（输入：`version`、`version_code`、`is_release` 等）
  - `ci.yml` 是 master/PR 的唯一编排入口；去掉临时分支名硬编码
  - 保留 `workflow_dispatch` 便于单端调试，但 **正式 Release 只由 `ci.yml` 创建**

### 运行时注入

| 端 | 方式 |
|----|------|
| API | `dotnet publish ... -p:VersionPrefix={YYYY.MM.N 的数值兼容形式或透传} -p:InformationalVersion={version}`；**以 `InformationalVersion={version}` 为权威**（与 `/api/version` 一致） |
| Windows | 同 API；写 `publish/VERSION`（内容=`version`）；产物名带版本 |
| Web | `VITE_APP_VERSION={version}` |
| Android | `CI_APP_VERSION` / `CI_VERSION_CODE`（保留现有 gradle 挂钩） |

### `Directory.Build.props`

- 默认本地：`InformationalVersion` = `0.0.0-local`（或现有 dev 占位）
- CI 通过 MSBuild 属性 **覆盖** `InformationalVersion` 为 resolve-version 的完整 `version` 串（含 PR 后缀时原样写入）
- `Version` / `FileVersion` 使用可解析的四段数值（由 `YYYY`、`MM`、`N` 映射，缺省补 0），避免非数字 CalVer 直接写入 AssemblyVersion 失败

### Android 签名

- 继续使用 secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`
- PR 与 master **同一 signingConfig**
- secrets 缺失：fail-fast
- 验收：两构建 APK 证书指纹一致，可覆盖安装

### 产物命名

- `pim-api-v{version}.tar.gz`
- `pim-web-v{version}`（zip 或目录 artifact 名带版本）
- `pim-windows-v{version}`
- `pim-android-v{version}-vc{versionCode}.apk`

### Release

- 条件：`master` + **四端构建 job 全部成功**
- 工具：`softprops/action-gh-release` 或等价
- Tag：`v{version}`（例 `v2026.07.42`）
- Body：版本、完整 SHA、各产物列表、可选短 changelog（提交列表即可）
- Tag 已存在：幂等更新/上传 assets，不因重复 tag 误杀整个发布策略
- 单端失败：**不**创建该次 Release（避免半套版本）

### PR 行为

- 计算 PR 版本串并注入
- 上传带版本 artifact
- 不发 Release
- Android 仍签名

## §4 各端落地、错误处理与验收

### 改动边界

| 端 | 做 | 不做 |
|----|----|------|
| API | 注入版本；校验 `/api/version` | 改业务 API |
| Windows | ico 嵌入、注入版本 | 重做托盘信息架构 |
| Web | favicon、构建注入；至少一处可观测版本（构建产物或最小 UI） | 大改布局 |
| Android | launcher 图标、manifest、版本 env、签名一致 | 改同步/业务逻辑 |
| CI | 编排、共享版本、Release、命名 | 应用商店上架 |

### 错误处理

- `resolve-version` 失败 → job 失败，无 Release
- 图标关键文件缺失 → CI 失败
- Android secrets 缺失 → fail-fast
- 单端构建失败 → 取消该次 Release
- Release 资产上传失败 → job 失败（可重跑）

### 验收清单

1. `resolve-version` 对 master / PR / 本地样例有确定性输出（脚本测试或 CI 断言）
2. API Release 产物：`AssemblyInformationalVersion` / `/api/version` = `YYYY.MM.N`
3. Web 构建产物含注入版本
4. Windows 产物 `VERSION` 文件与程序集信息一致；托盘图标非系统默认
5. Android `aapt dump badging` 含正确 versionName/versionCode 与 icon
6. PR 与 master APK 签名证书指纹一致
7. master 成功后存在 GitHub Release `v{version}` 与命名规范资产
8. 四端在同一次 master 编排中 version 主号相同

## 架构关系（简图）

```text
branding/pim-mark.svg
        │
        ▼
scripts/branding/export-icons ──► web public / windows app.ico / android mipmap

scripts/ci/resolve-version ──► .github/actions/resolve-version
        │
        ▼
.github/workflows/ci.yml
  resolve-version
       │
       ├─ build-api      ── inject version
       ├─ build-web      ── inject version
       ├─ build-windows  ── inject version + icon
       └─ build-android  ── inject version + same signing
              │
              ▼
         release (master only) ── tag vYYYY.MM.N + assets
```

## 实现顺序建议

1. `branding/` 源 SVG + 导出脚本 + 落入三端路径  
2. `resolve-version` 脚本 + composite action + 单测/断言  
3. 调整 `Directory.Build.props` 与四端构建注入  
4. 编排 `ci.yml`、收敛旧 workflow、产物命名  
5. Release job + Android 签名一致性验收  
6. 文档：`branding/README.md` 与 CI 说明（若需）

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| `GITHUB_RUN_NUMBER` 在 workflow 重命名/复制后重置 | 编排固定单一 `ci.yml` 名称；文档标明；versionCode 公式留足空间 |
| 四端并行 runner 时钟/环境差 | 版本只来自 resolve job outputs，不在各 job 重算 |
| ICO/PNG 导出工具在 Windows/Linux 差异 | 导出脚本锁定工具版本；CI 用 Linux 生成或提交已导出二进制 |
| Release 权限 | 使用 `contents: write`；确认默认 `GITHUB_TOKEN` 可建 release |
| 旧客户端比较版本字符串 | 文档说明 CalVer 排序规则；Android 以 versionCode 为准 |

## 成功标准

- 一次 master 合并后，四端产物主版本号一致，Release 一键可下  
- `/api/version`、Web 注入、Windows 程序集、Android versionName 同源  
- 三端可见统一「四色拼 P」图标  
- PR 装 Android 包后可用 master 包直接覆盖升级  
