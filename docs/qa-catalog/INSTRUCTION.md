# PIM 全量问题目录 - 测试任务书（只验不修）

## 目标
对 PIM 全栈完整拉起测试，找出尽可能多的问题，产出准确、精简的问题目录。不要修复代码，不要给出修改建议。只描述问题、复现步骤、预期/实际、证据。

## 范围
- 服务端：Pim.Api + Pim.Core + Pim.Infrastructure + modules: Pim.Module.Mobile / PcTracker / Calendar / Files / Stats / QuickNotes / Today / Hangfire
- 客户端 Web：client-web（全路由、全按钮）
- 客户端 Android：client-android（14包：location/daemon/offline/mobile/...）
- 客户端 Windows：client-windows / client-shell-windows（仅代码静查：编译检查 + 逻辑审计，不启动 UI）
- 数据：生产库克隆到测试库回放，全部测试流量打测试库，不碰生产库
- 文档对齐：对照 docs 下文档、AGENTS.md、API 契约，Code vs Doc 不一致记为问题

## 环境与连接
- 项目根目录：/workspace/pim-platform（亦可通过 /root/projects/pim-platform 访问）
- 数据库：PostgreSQL 127.0.0.1:5432
  - 源库：Database=pim_prod，Username=pim，Password=pim_prod_2026_home
  - 测试库：Database=pim_test，Username=opencode，Password=62f0a50bb963bb648f8e400399def95a（具备 CREATEDB 权限，可创建测试库）
  - 连接示例：Host=127.0.0.1;Port=5432;Database=pim_test;Username=opencode;Password=62f0a50bb963bb648f8e400399def95a
  - 克隆方式：`PGPASSWORD=pim_prod_2026_home pg_dump -h 127.0.0.1 -p 5432 -U pim -d pim_prod --no-owner --no-privileges | PGPASSWORD=62f0a50bb963bb648f8e400399def95a psql -h 127.0.0.1 -p 5432 -U opencode -d pim_test`
- 缓存：Redis 127.0.0.1:6379，Password=redis_rMG4Jc
- Android：SDK 位于 /opt/android-sdk，adb 37.0.1，emulator 37.2.4
  - AVD：test_avd / test_avd_36 / test_avd_361 / test_avd_361ps，推荐 test_avd_36
  - 启动：`emulator -avd test_avd_36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot -memory 2048 &`
  - 等待就绪：`adb wait-for-device && adb shell getprop sys.boot_completed`
- 运行时：dotnet 8.0.424（/opt/dotnet），Java 17，Node 20（/opt/node/bin），psql、pnpm 均可用
- API 测试端口：自行选用空闲端口拉起指向 pim_test 的 Pim.Api 实例

## 环境策略
- 每个环境准备限时 5 分钟，超时或不支持标记 [SKIP] 并写明原因，继续下一项，不死磕
- 可做简单修补（apt 安装、dotnet restore、npm i 等），不可在单一环境长时间阻塞
- 明确跳过：流体云 / android live updates 等模拟器不支持的能力，标记 [SKIP] 模拟器不支持，不算失败

## 测试维度
1. 功能：每个 MapGet/MapPost 接口、每个页面路由、每个按钮/交互
2. 数据正确性：聚合（SUM/去重/分桶/分摊）、时区、跨天、重叠会话、null EndUtc、0 毫秒窗口
3. 一致性：overview.total == heatmap 桶和 == charts 和，对不上记问题
4. 幂等/重放：/usage/events、/location/points、/sync/gaps 同批重发
5. 隔离：多用户/多设备数据隔离、权限
6. 时序/空间：start<end、durationMs 一致性、轨迹速度/距离异常
7. 容差：按 App 累加 SUM 允许 > 物理时间但需有界，记录判定标准；去重后单小时 >3600*1.05、单天 >86400*1.05 记问题
8. 文档对齐：文档承诺与代码实际不一致记问题

## 方法
- 脚本自动扫广度清单：grep MapGet/MapPost + 扫 client-web 全路由 + 扫 DB 全表 + 扫 Service 列表
- 克隆生产库到测试库，脱敏回放 + Bogus/FsCheck 生成器造重叠/跨天/时区边界/脏数据 + 真库采样回放
- 真拉起：测试库上的 Pim.Api（测试端口）+ Playwright 点 Web + 安卓模拟器跑 connectedAndroidTest
- Windows 仅静查：dotnet build + 全量 grep 审计

## 交付
- 输出：docs/qa-catalog/CATALOG.md（主件）+ docs/qa-catalog/evidence/（证据附件）
  - 绝对路径：/workspace/pim-platform/docs/qa-catalog/CATALOG.md
- 格式：准确精简，不过多解释。每条问题包含：
  - ID、模块、严重级别（阻塞/严重/一般/提示）、标题
  - 描述（1-3 句）
  - 复现步骤（可执行命令/请求/数据，含测试库名、时间区间、参数）
  - 预期 vs 实际
  - 证据（日志片段路径、截图、DB 查询结果、接口返回）
  - 文档依据（若为文档对齐问题，标注文档名+章节）
- 不包含：修复方法、修改建议、代码 diff
- 开头含汇总表（按模块/级别统计 + 总数）
- 附录：测试覆盖说明（哪些真测/哪些静查/哪些 [SKIP] 及原因）、测试库名、运行命令及结果摘要

## 验收
- 每条问题复现步骤可执行
- 产出后贴关键日志摘要到附录（dotnet test / pnpm test / Playwright）

## 执行
- 在 /workspace/pim-platform 内执行，不修改业务代码
- 允许创建测试工程/脚本/临时分支，不提交到主分支
- 完成后保持 pim_test 可复查
