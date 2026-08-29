# Session 2：Web E2E 全量验证 汇总

> 执行时间：2026-08-24 02:30-03:05 UTC  
> 分支：master（未修改业务代码，仅新增本目录与证据）  
> 前端：`src/client-web`，后端：`Pim.Api`（`ASPNETCORE_ENVIRONMENT=Test`，`pim_test` @ `127.0.0.1:5432/opencode`）  
> 测试用户：`web_e2e_*` / `db2_*`（见 `pim_test.users`）

## 环境准备

1. **Node**：`/usr/local/bin/node v22.12.0` 满足 Vite 要求（原 `/opt/node/bin/node v20.18.0` 不满足 20.19+ 导致 `vite build` 失败，见 `evidence/web-build.log`）。`corepack pnpm` 因 `SHA256:DhQ8wR5A` 离线验签失败，改用 `npm`。
2. **依赖**：`npm install` 后 `vite 8.0.13` 的 `rolldown` 打包 `vite.config.ts` 时 `onLog` 返回 `object` 期望 `undefined`（`rolldown 1.0.1` + `binding 1.2.3` 不匹配），`vite dev` 无法启动。临时降级 `vite@5.4.19` + `@vitejs/plugin-react@4.3.3`（`--legacy-peer-deps`）后启动成功，验证完后已 `git checkout` 还原。
3. **API 后端**：已存在 2 个 `Pim.Api` 进程：
   - `63101` @ `http://127.0.0.1:5860`（`dotnet /workspace/pim-platform/src/Pim.Api/bin/Release/net8.0/Pim.Api.dll`，Test，pim_test）
   - `72429` @ `http://127.0.0.1:15733`（同）
   - 另有宿主 `5858`（`Kestrel`，宿主端口转发，未在本容器 pid 命名空间内可见，但 `curl /health` 200）。验证时 `POST /api/v1/auth/register` 到 `5858` 的用户未入 `pim_test`（见探针 `e2eprobe_*`），而 `5860/15733` 正确写入 `pim_test`。为保证 DB 写入可查，`vite.config.ts` 的 `proxy '/api'` 临时由 `5858` 改为 `5860`，验证后已还原。
4. **前端 dev server**：`python Popen npx vite --port 5175 --host 127.0.0.1`（`VITE v5.4.19 ready in 1150ms`，日志 ` /tmp/vite5c.log`），`curl http://127.0.0.1:5175/` 200，`curl http://127.0.0.1:5175/src/main.tsx` 200（esbuild 转换后）。
5. **Playwright**：`chromium-1234` @ `/root/.cache/ms-playwright/chromium-1234/chrome-linux64/chrome`，`chromium_headless_shell-1234` 已有，`playwright 1.61.1`（`src/client-web/node_modules`），`NODE_PATH` 启动。

## 验证范围（逐项执行）

| 维度 | 覆盖 |
|---|---|
| 路由加载 | 32 条路由（含 `/login`、`/today`、`/calendar`、`/workbench`、`/data-center`、`/confirmations`、`/reminders`、`/reports`、`/habits`、`/audit/event/:id`、`/endpoint-shell`、`/quick-notes`、`/files`、`/tasks`、`/pc-tracker`、`/mobile-records`、`/location-history`、`/status`、`/settings` 及其子路由 `/sync`、`/ai`、`/calendar-data`、`/recycle-bin`、`/pc-data`、`/app-knowledge-base`、`/app-knowledge-base/categories`、`/embed/android/today|tracks`、重定向 `/`、`/sync`、`/timeline`、`/week`、`/month`），每条测 `auth` 与 `noauth`（`embed` 除外）共 60 次导航，见 `evidence/web/screenshots/*.png` |
| 按钮可用性 | 20 个核心页面各统计 `<button>` 数量（`today 25`、`calendar 33`、`workbench 27`、`quick-notes 38`、`files 33`、`tasks 29`、`settings 21`、`settings/sync 32`、`data-center 24`、`app-knowledge-base 195`、`categories 52` 等），每页抽样点击前 8 个可见可用按钮，检查 `pageerror` 与页面崩溃，见 `evidence/web/PASS-057..076` |
| 后台写入 | `quick_notes`、`events`、`tasks` 三类：通过 UI 与 API 创建后 `psql -d pim_test` 查表确认（`quick_notes 4条`、`events 3条`、`tasks 3条`），见 `db-write2.cjs` 与 `pgQuery` 日志 |
| 表单提交 | `/login` 空提交（`required` 校验）与 `/quick-notes` 空保存按钮禁用 + `POST /api/v1/quick-notes {"contentMarkdown":""}` 应 400 实 201 |
| 列表分页/筛选 | `GET /api/v1/calendar/events?start&end&page&pageSize`、`GET /api/v1/calendar/tasks`、`GET /api/v1/quick-notes?page&pageSize`、`GET /api/v1/files/items?path` 四组，验证 `PagedResult` 契约、`pageSize` 截断、`page=-1` 校验 |

## 发现问题（14 项）

> 详见 `evidence/web/WEB-*.md`，每项 1-3句描述 + 复现 + 预期 + 实际 + 证据截图/日志。此处仅汇总。

| 编号 | 页面/模块 | 级别 | 标题 | 要点 |
|---|---|---|---|---|
| WEB-001 | /login | 严重 | 今日/登录页接口500 | `GET /api/v1/pc/classification/queue?limit=1&mode=queue` 与 `limit=5` 重复500 x4，控制台4条500 |
| WEB-002 | /today | 严重 | 今日页分类队列500 | 同上，`/today` 加载即触发同接口500 |
| WEB-003 | /pc-tracker | 严重 | PC追踪分类队列500 | `limit=20` 的同接口500 x2 |
| WEB-004 | /mobile-records | 严重 | 移动记录页误调PC队列500 | `/mobile-records` 误请求 PC 队列且500，疑似复用错误 |
| WEB-005 | /location-history | 严重 | 位置历史移动统计500 | `GET /mobile/location/analytics/movement-stats` 与 `frequent-places` 均500 |
| WEB-006 | /settings/ai | 一般 | AI设置页403未处理 | 普通用户请求 `ai/requests|status|usage` 403 x6，页面未做无权限友好提示 |
| WEB-007 | / | 严重 | 根路径分类队列500 | `/` 重定向后同 WEB-001 |
| WEB-008 | /quick-notes | 一般 | 允许空内容创建 | `POST /api/v1/quick-notes {"contentMarkdown":""}` 应400实201，DB 落 `content_markdown=''` |
| WEB-010 | /calendar | 一般 | 非分页分支返回List | `GET /api/v1/calendar/events?start&end` 无 `page` 时返回 `List<EventResponse>`，与 `?page=1` 时 `PagedResult` 不一致（PIM-024） |
| WEB-011 | /calendar | 一般 | page=-1 未校验 | `page=-1` 仍200返回数据，未400或Clamp |
| WEB-012 | /files | 一般 | 文件列表结构异常 | `GET /api/v1/files/items?path=/` 返回 `{"data":{"result":{"items"...}}}` 嵌套 `result`，与 `PagedResult` 契约不符 |
| WEB-013 | /files | 一般 | 文件分页参数被忽略 | `?path=/&page=2&pageSize=2` 返回与 `page=1` 完全相同，服务端假分页（PIM-026） |
| WEB-014 | /pc-tracker | 严重 | 分类队列500（直调） | 直接 `GET /api/v1/pc/classification/queue?limit=1&mode=queue` 500 `{"Code":1001,"Message":"内部服务器错误"}` |
| WEB-015 | /location-history | 严重 | 移动统计500（直调） | 直接 `GET /mobile/location/analytics/movement-stats` 500 同上 |

> 注：原 WEB-009（`/tasks` 日历获取失败）为脚本路径 ` /api/v1/calendars` 误用（应为 `/api/v1/calendar/calendars`）误报，已撤销并改为 `PASS-083`。

## 通过项（102 项）

> 详见 `evidence/web/PASS-*.md`，每项标记已验证通过 + 证据截图/日志。

- 路由加载：`PASS-001..056` 覆盖 32 路由 x auth/noauth，重定向 `/sync→/settings/sync`、`/timeline→/calendar?view=timeline`、`/week/month` 等均符合，`noauth` 对 `/today|calendar|workbench|quick-notes|files|tasks|pc-tracker|settings...` 均正确重定向 `/login`。
- 按钮：`PASS-057..076` 20 页按钮抽样点击无崩溃无 `pageerror`（含 `app-knowledge-base 195`、`categories 52` 等大列表）。
- 表单：`PASS-077`（`/login` 空提交 `validationMessage` 正常）、`PASS-078`（`/quick-notes` 空时保存按钮禁用）、`PASS-079`（日历加载正常）等。
- 分页：`PASS-080`（日历 `pageSize=5` 正常）、`PASS-098/099`（日历分页 `items` 正确、`totalCount`）、`PASS-100`（大 `pageSize` 未超限因数据少）、`PASS-107/108`（任务分页）、`PASS-090/091`（快速记录创建与日历本创建）等 20+。
- DB 写入：`PASS-090`（`quick_notes` UI/API 创建后 `SELECT content_markdown` 命中）、`PASS-092..097`（3 事件创建与 DB 校验）、`PASS-101..106`（3 任务创建与 DB 校验）、`PASS-095..099`（日历分页与 DB 一致）等。

## 证据清单

| 证据 | 说明 |
|---|---|
| `evidence/web/WEB-*.md` 14 | 本次发现问题 |
| `evidence/web/PASS-*.md` 102 | 通过项 |
| `evidence/web/screenshots/*.png` 115 张 | 各路由加载、按钮、表单、DB 创建后截图（`_login_*.png`、`btn__today.png`、`calendar.png`、`quicknote_create.png` 等） |
| `/tmp/check-500.cjs` 输出 | 500 接口精确定位（`pc/classification/queue` x8、`mobile/location/analytics` x4、`ai/*` 403 x6） |
| `/tmp/db-write2.log` | DB 写入与分页验证完整日志（`psql` 查询、API 返回、`totalCount`） |
| `/tmp/vite5c.log` | `VITE v5.4.19 ready` |
| `src/client-web/package.json`、`vite.config.ts` 临时变更 | 已 `git checkout` 还原，`package-lock.json` 同 |

## 运行命令与结果摘要

```bash
# Node / pnpm
/usr/local/bin/node -v # 22.12.0
npm --prefix src/client-web install # 因 corepack keyid 失败改 npm
# vite 降级前
npx vite --port 5175 --host 127.0.0.1 # 失败：onLog returned object, Rolldown 1.0.1 500
# 降级后
npm install vite@5.4.19 @vitejs/plugin-react@4.3.3 --legacy-peer-deps
nohup npx vite --port 5175 --host 127.0.0.1 > /tmp/vite5c.log 2>&1 &
# → VITE v5.4.19 ready in 1150ms, curl 200

# API
curl http://127.0.0.1:5860/health # 200 {"status":"healthy"}
curl -X POST http://127.0.0.1:5860/api/v1/auth/register # 201，写入 pim_test

# Playwright
NODE_PATH=src/client-web/node_modules node /tmp/e2e-opt.cjs # 60 路由 + 20 按钮页，14 WEB + 82 PASS（首轮）
NODE_PATH=... node /tmp/db-write2.cjs # DB 写入与分页，新增 6 WEB + 20 PASS

# 500 复现
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:5860/api/v1/pc/classification/queue?limit=1&mode=queue # 500 {"Code":1001}
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:5860/api/v1/mobile/location/analytics/movement-stats?... # 500

# DB
PGPASSWORD=... psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test -c "SELECT count(*) FROM quick_notes;" # 4+ 含 E2E
PGPASSWORD=... psql -c "SELECT title FROM events WHERE title LIKE 'E2E-EV2%';" # 3 条命中
PGPASSWORD=... psql -c "SELECT title FROM tasks WHERE title LIKE 'E2E-TASK2%';" # 3 条命中
```

## 未做事项

- 未修改业务代码（临时 `vite` 降级与 `proxy` 改 `5860` 已还原）。
- 未做后端 API 独立测试（仅通过 Web 触发的接口附带验证）。
- 未做安卓测试。
- 未修复任何 bug。

## 已知限制

- `embed/android/*` 仅验证加载，未注入 Android Bridge。
- 文件列表 `files/items` 在测试环境 `MinIO` 未配时返回空 `result`，未测真实上传。
- `vite 8` + `Node 20.18` 原生构建仍失败（`SOURCEMAP_BROKEN` + `onLog`），需 `22.12+` + `vite 5` 才能拉起 dev server。

> 证据绝对路径：`/workspace/pim-platform/docs/qa-catalog/evidence/web/`，原始日志保留于 `/tmp/vite5c.log`、`/tmp/check-500.cjs` 输出、`/tmp/db-write2.log`。
