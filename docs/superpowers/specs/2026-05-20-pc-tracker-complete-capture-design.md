# PC 追踪完整采集与存储模型设计

## 背景

当前 PC 追踪已经接入两个本地数据源：

- ActivityWatch `http://127.0.0.1:5600`：提供窗口活动、AFK 状态等时间序列事件。
- KeyStats `http://127.0.0.1:18080`：提供今日累计键鼠统计、按键明细、按应用拆分。

现有实现存在两个结构性限制：

- ActivityWatch 事件没有保存 `bucket_id` 和原始事件 `id`，只能用时间、持续时长、应用、标题等字段近似去重，无法可靠处理 AW heartbeat 更新 duration 的情况。
- KeyStats 只保存每日快照，同一天会被覆盖，缺少分钟级历史，因此详情页只能展示日汇总，无法解释某个时间段内发生了什么。

本设计的目标是：**确实保存所有能从两个 API 获得的数据；对于没有事件明细、只有累计总结的数据，按 1 分钟定时采样保存完整快照。**

## 设计原则

1. 原始数据优先：API 返回的字段必须完整保留，不能只保存当前页面用到的字段。
2. 明细能拿就存明细：ActivityWatch 有原始事件，按事件幂等保存。
3. 只有累计就采样：KeyStats 没有单次按键/点击事件，按 1 分钟保存完整累计快照。
4. 派生不替代原始：热力图、工作段、分类、分钟增量可以缓存，但原始层永远是事实来源。
5. 查询按业务友好：详情页应能按时间、设备、应用、分类、事件类型、按键、窗口标题过滤。
6. 应用名统一归一：AW 常见 `msedge.exe`，KeyStats 常见 `msedge`，查询和分类前需要统一应用标识。

## 数据源字段清单

### ActivityWatch Server

`GET /api/0/info`

已确认字段：

- `hostname`
- `version`
- `testing`
- `device_id`

用途：

- 记录 ActivityWatch 实例身份。
- 辅助跨设备排查。
- 与 PIM 守护程序的 `device_id` 区分保存。

### ActivityWatch Buckets

`GET /api/0/buckets/`

当前本机已确认 bucket：

- `aw-watcher-window_DESKTOP-ARJ75IN`
- `aw-watcher-afk_DESKTOP-ARJ75IN`
- `aw-stopwatch`

bucket 字段：

- `id`
- `created`
- `name`
- `type`
- `client`
- `hostname`
- `data`
- `last_updated`

用途：

- 发现可采集 bucket。
- 记录 watcher 类型和来源。
- 后续如安装浏览器 watcher、编辑器 watcher，也能无需改 schema 保留新增 bucket。

### ActivityWatch Events

`GET /api/0/buckets/{bucket_id}/events?start=&end=&limit=`

通用事件字段：

- `id`
- `timestamp`
- `duration`
- `data`

当前 window bucket 的 `data`：

- `app`
- `title`

当前 afk bucket 的 `data`：

- `status`，值通常为 `afk` 或 `not-afk`

用途：

- window 事件用于应用使用时间、窗口标题、应用切换、工作段、标题搜索。
- afk 事件用于空闲时间、活跃时间、工作段切分、过滤挂机时间。
- `data` 必须完整保存为 JSON，不能只保存 `app/title/status`。

### KeyStats

`GET /api/stats/` 和 `GET /api/stats/today`

已确认两个端点返回相同结构。

顶层字段：

- `date`
- `keyPresses`
- `keyPressCounts`
- `leftClicks`
- `rightClicks`
- `middleClicks`
- `sideBackClicks`
- `sideForwardClicks`
- `mouseDistance`
- `scrollDistance`
- `peakKPS`
- `peakCPS`
- `appStats`
- `FormattedMouseDistance`
- `FormattedScrollDistance`

`keyPressCounts`：

- 字典结构，键为按键名或组合键，如 `Space`、`Backspace`、`Ctrl+C`、`Win+Space`。
- 值为今日累计次数。

`appStats` 每个应用字段：

- `AppName`
- `DisplayName`
- `KeyPresses`
- `LeftClicks`
- `RightClicks`
- `MiddleClicks`
- `SideBackClicks`
- `SideForwardClicks`
- `ScrollDistance`

用途：

- 顶层累计字段用于整体输入强度。
- `keyPressCounts` 用于键盘热力图、快捷键排行、按键查询。
- `appStats` 用于每个应用的输入、点击、滚动贡献。
- 格式化字段用于 UI 原样展示，计算仍使用原始数值。

## 推荐存储模型

### 原始层：ActivityWatch

新增或改造 `pc_aw_events` 为幂等事件表：

- `id`
- `pim_device_id`
- `aw_device_id`
- `aw_hostname`
- `bucket_id`
- `bucket_type`
- `bucket_client`
- `source_event_id`
- `timestamp_utc`
- `duration_seconds`
- `data_json`
- `event_kind`
- `app_name_raw`
- `app_name_normalized`
- `window_title`
- `afk_status`
- `created_at`
- `updated_at`

唯一约束：

- `(pim_device_id, bucket_id, source_event_id)`

写入语义：

- 首次看到事件时 insert。
- 再次看到同一个 `source_event_id` 时 update `duration_seconds`、`data_json`、解析字段和 `updated_at`。
- 这样可以正确处理 ActivityWatch heartbeat 对上一条事件 duration 的更新。

### 原始层：ActivityWatch Bucket 快照

新增 `pc_aw_buckets`：

- `pim_device_id`
- `aw_device_id`
- `bucket_id`
- `name`
- `type`
- `client`
- `hostname`
- `created_at_source`
- `last_updated_source`
- `data_json`
- `seen_at`

用途：

- 保存 watcher 元数据。
- 支持动态发现 bucket。
- 排查某段时间为何没有某类事件。

### 原始层：KeyStats 分钟快照

新增 `pc_keystats_samples`：

- `id`
- `pim_device_id`
- `sampled_at_utc`
- `stats_date`
- `stats_timezone_offset_minutes`
- `key_presses`
- `left_clicks`
- `right_clicks`
- `middle_clicks`
- `side_back_clicks`
- `side_forward_clicks`
- `mouse_distance`
- `scroll_distance`
- `peak_kps`
- `peak_cps`
- `formatted_mouse_distance`
- `formatted_scroll_distance`
- `key_counts_json`
- `app_stats_json`
- `raw_json`
- `created_at`

唯一约束：

- `(pim_device_id, sampled_at_utc)`，其中 `sampled_at_utc` 取分钟精度。

写入语义：

- 守护程序每 1 分钟采样一次。
- 每次保存完整累计快照，不只保存 delta。
- 如果同一分钟重复上传，用最新快照 upsert。

### 派生层：KeyStats 分钟增量

可以通过查询实时计算，也可以缓存为 `pc_keystats_minute_deltas`：

- `pim_device_id`
- `minute_start_utc`
- `stats_date`
- `key_presses_delta`
- `left_clicks_delta`
- `right_clicks_delta`
- `middle_clicks_delta`
- `side_back_clicks_delta`
- `side_forward_clicks_delta`
- `mouse_distance_delta`
- `scroll_distance_delta`
- `key_counts_delta_json`
- `app_stats_delta_json`
- `source_sample_id`
- `previous_sample_id`
- `is_gap`
- `is_reset`
- `quality_flags_json`

计算规则：

- 同一设备、同一 `stats_date` 内，将当前 sample 减去上一 sample。
- 如果上一 sample 缺失或跨天，delta 记为 null 或从 0 起算，但要标记 `is_gap=true`。
- 如果出现负数，说明 KeyStats 当天重置或数据源异常，应标记异常，不直接混入正常趋势。

### 归一层：应用标识

新增或扩展应用归一规则：

- `raw_app_name`
- `normalized_app_key`
- `display_name`
- `source`
- `category_id`

归一示例：

- `msedge.exe` -> `msedge`
- `msedge` -> `msedge`
- `chrome.exe` -> `chrome`
- `Codex.exe` -> `codex`
- `Codex` -> `codex`

用途：

- 让 AW 的窗口时间和 KeyStats 的输入统计能合并。
- 让分类规则不需要同时维护 `.exe` 和非 `.exe` 两套。

## 采集流程

### ActivityWatch

1. 启动时调用 `/api/0/info` 保存 server 元数据。
2. 调用 `/api/0/buckets/` 保存 bucket 元数据。
3. 选择支持的 bucket 类型采集：至少 `currentwindow`、`afkstatus`，其他 bucket 先按通用事件保存。
4. 每 30 秒或 60 秒按 bucket 拉取事件。
5. 上传事件时包含 `bucket_id`、bucket 元数据、`source_event_id` 和完整 `data_json`。
6. 服务端按唯一键 upsert。
7. 本地 cursor 按 bucket 持久化；只有服务端确认成功后推进。

### KeyStats

1. 每 1 分钟调用 `/api/stats/`。
2. 上传完整原始快照，包括 `raw_json`。
3. 服务端按设备和分钟 upsert。
4. 服务端可同步计算分钟 delta，也可查询时计算。
5. 每日汇总表可以继续存在，但只能作为缓存或兼容层，不能再作为详情页事实来源。

## API 设计

### 上传 API

`POST /api/v1/pc/aw/upload`

请求应包含：

- `pimDeviceId`
- `awInfo`
- `bucket`
- `events[]`

每个事件包含：

- `sourceEventId`
- `timestamp`
- `duration`
- `data`

`POST /api/v1/pc/keystats/samples`

请求应包含：

- `pimDeviceId`
- `sampledAt`
- `snapshot`

snapshot 保留完整 KeyStats 响应。

### 查询 API

`GET /api/v1/pc/detail`

核心参数：

- `dateFrom`
- `dateTo`
- `deviceId`
- `eventType`: `window | afk | input-minute | app-input | key-input`
- `appName`
- `categoryName`
- `keyName`
- `windowTitle`
- `page`
- `pageSize`
- `sortBy`
- `sortDir`

返回结果应包含统一字段：

- `recordType`
- `start`
- `end`
- `durationSeconds`
- `deviceId`
- `appName`
- `displayName`
- `categoryName`
- `title`
- `keyPresses`
- `clicks`
- `scrollDistance`
- `keyCounts`
- `raw`

## 详情页行为

详情页应支持五类记录：

1. 窗口记录：来自 AW window 事件。
2. 空闲记录：来自 AW afk 事件。
3. 分钟输入记录：来自 KeyStats 分钟 delta。
4. 应用输入记录：来自 KeyStats `appStats` 分钟 delta。
5. 按键明细记录：来自 KeyStats `keyPressCounts` 分钟 delta。

默认视图建议：

- 默认展示混合时间线，按时间倒序。
- 用户可切换只看窗口、空闲、输入、应用输入、按键。
- 点击某条记录可展开查看完整 raw JSON。

## 历史回填

ActivityWatch：

- 本机已有历史事件，应提供一次性 backfill。
- backfill 按 bucket、按时间窗口分页拉取。
- 服务端用 `(pim_device_id, bucket_id, source_event_id)` 幂等 upsert，所以重复回填安全。

KeyStats：

- KeyStats API 只暴露当前今日累计快照，无法回填历史分钟级数据。
- 只能从启用新采样后开始获得分钟历史。
- 已有 daily 快照可保留为历史汇总，但不能伪造成分钟明细。

## 数据质量与异常标记

需要显式标记以下情况：

- AW bucket 不存在或长时间未更新。
- AW 事件 duration 为 0。
- AW 事件 duration 异常过长。
- KeyStats sample 缺口超过 2 分钟。
- KeyStats delta 为负数。
- KeyStats 与 AW 应用名无法归一。
- 分类未匹配，归入 `Other` 并允许用户补规则。

## 迁移策略

1. 保留现有表，新增完整采集表。
2. 新采集开始后，查询优先使用新表。
3. 旧 `pc_keystats_daily` 作为兼容汇总。
4. 旧 `pc_aw_events` 可以通过脚本迁移到新结构，但没有 `source_event_id` 的旧数据只能生成 legacy key，不能达到完整幂等。
5. ActivityWatch backfill 会逐步补齐旧 AW 事件，并用原始 AW event id 替代 legacy 数据。

## 成功标准

- 每个 AW 事件保存完整 `data_json` 和原始 `source_event_id`。
- KeyStats 每分钟保存完整原始快照，包括 `keyPressCounts`、`appStats`、格式化字段和 `raw_json`。
- 详情页不再只返回每日汇总，而能返回窗口、空闲、分钟输入、应用输入、按键明细。
- 同一个 AW 事件重复上传不会重复插入，只会更新。
- KeyStats 缺采样、跨天重置、负 delta 都有可见异常标记。
- 新数据模型能够解释“某一分钟在哪个应用、哪个窗口、产生了多少键鼠活动”。
