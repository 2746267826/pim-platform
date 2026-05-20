# ActivityWatch 浏览器页面解释时间线设计

## 背景

当前 PC Tracker 已采集 ActivityWatch 的窗口事件和 AFK 事件，并保留 KeyStats 作为输入统计来源。用户已安装 ActivityWatch 浏览器插件，ActivityWatch 本机实例 `http://localhost:5600/` 已出现 `web.tab.current` 类型 bucket，例如 `aw-watcher-web-edge_DESKTOP-ARJ75IN`。

本设计的目标不是新增一个孤立的浏览器历史页，而是让 PC Tracker 在解释某个时间段时更准确地回答：**这段时间我到底在做什么？**

浏览器窗口事件通常只能告诉我们用户在使用 `msedge.exe` 或 `chrome.exe`，窗口标题也可能是“某页面和另外 20 个页面”。浏览器插件的页面事件可以提供更具体的页面标题和 URL。因此，默认解释时间线应在浏览器窗口时间段内优先展示页面记录，同时保留原始窗口和页面事件用于核对。

## 资料依据

- ActivityWatch 数据模型：bucket 包含事件和元数据；事件由 `timestamp`、`duration`、`data` 组成；`web.tab.current` 的数据包含 `url`、`title`、`audible`、`incognito`；`currentwindow` 包含 `app`、`title`；`afkstatus` 包含 `status`。参考：https://docs.activitywatch.net/en/latest/buckets-and-events.html
- ActivityWatch REST API：通过 `/api/0/buckets/` 获取 bucket，通过 `/api/0/buckets/<bucket_id>/events` 获取事件；本机 API 浏览器位于 `http://localhost:5600/api/`。参考：https://docs.activitywatch.net/en/latest/api/rest.html
- `aw-watcher-web` 是 ActivityWatch 的跨浏览器 WebExtension，用于作为浏览器 watcher。参考：https://github.com/ActivityWatch/aw-watcher-web
- `aw-watcher-input` 已在本机出现过，但用户会关闭它。本轮不采集、不上传、不存储、不展示 `aw-watcher-input` 数据，也不替代 KeyStats。参考：https://github.com/ActivityWatch/aw-watcher-input

## 范围

本次包含：

- 采集和保存 ActivityWatch `web.tab.current` 页面事件。
- 在默认解释时间线中，用合成后的页面记录拆解已安装插件的浏览器窗口。
- 保留原始 ActivityWatch window、web、afk 事件视图和 raw JSON。
- 增加页面标题、域名、URL、文件路径相关的查询和展示。

本次不包含：

- 不修改 KeyStats 采集、上传、存储、汇总、键盘热力图、`input-minute`、`app-input`、`key-input` 逻辑。
- 不接入 `aw-watcher-input`。
- 不把浏览器页面事件用于输入统计。
- 不删除既有窗口事件原始数据。

## 数据源

### 保持现状的数据源

- `aw-watcher-window_*`：`currentwindow`，用于普通窗口、浏览器窗口回退、原始事件核对。
- `aw-watcher-afk_*`：`afkstatus`，用于空闲状态。
- KeyStats：现有输入统计来源，保持不变。

### 新增使用的数据源

- `aw-watcher-web-*_*`：`web.tab.current`，用于浏览器当前页面。

页面事件字段：

- `timestamp`
- `duration`
- `data.url`
- `data.title`
- `data.audible`
- `data.incognito`
- 本机样本还包含 `data.tabCount`，应保存在 raw JSON 中，展示时有则显示。

## 存储设计

继续使用 ActivityWatch 原始事件表保存原始事件。服务端应支持 `bucket_type = "web.tab.current"`，并从 `data_json` 派生页面字段。

建议扩展或复用 `pc_aw_events` 字段：

- `event_type`：新增 `web` 或 `web-page-raw` 类型，用于标识原始页面事件。
- `bucket_id`
- `bucket_type`
- `bucket_client`
- `source_event_id`
- `timestamp`
- `duration`
- `data_json`
- `app_name`
- `window_title`
- `aw_device_id`
- `aw_hostname`
- `updated_at`

页面派生字段可先从 `data_json` 查询时解析；如果性能需要，再新增列：

- `page_title`
- `page_url`
- `page_domain`
- `page_path`
- `is_local_file`

唯一性仍以 `(device_id, bucket_id, source_event_id)` 为准，重复上传同一 ActivityWatch event 时更新 `duration` 和 `data_json`。

## 采集流程

Windows daemon 的 ActivityWatch 采集器应从固定 bucket 列表改为按 bucket 元数据发现：

1. 调用 `/api/0/info` 保存 ActivityWatch 实例信息。
2. 调用 `/api/0/buckets/` 获取所有 bucket。
3. 只选择以下 bucket 类型上传：
   - `currentwindow`
   - `afkstatus`
   - `web.tab.current`
4. 明确排除：
   - `os.hid.input`
   - `aw-watcher-input_*`
5. 对每个选中的 bucket 调用 `/api/0/buckets/{bucket_id}/events?limit=-1` 或按时间范围 backfill。
6. 上传时包含 bucket 元数据、`source_event_id` 和完整 `data`。
7. 服务端按 `(device_id, bucket_id, source_event_id)` 幂等 upsert。

## 解释时间线

默认 PC detail/summary 使用“解释时间线”。解释时间线不是新的原始事实表，而是查询时由原始事件合成的视图。

### 浏览器识别

浏览器窗口事件由 `currentwindow.data.app` 或归一化后的 app 判断。初始支持：

- `msedge.exe`
- `chrome.exe`
- `firefox.exe`
- `brave.exe`
- `opera.exe`

后续可扩展为配置表或分类规则。

### 页面有效性

页面事件按时长分为：

- 有效页面：`duration >= 5s`
- 短页面：`duration < 5s`

短页面不单独出现在默认解释时间线，但其时长会并入相邻有效页面：

- 优先并入后一个有效页面。
- 如果后面没有有效页面，则并入前一个有效页面。
- 连续短页面作为一个短页面组处理，整组优先并入后一个有效页面。

示例：

```text
原始页面事件：
A 5分钟
B 2秒
C 3秒
D 6秒

解释时间线：
A 5分钟
D 11秒
```

示例：

```text
原始页面事件：
A 5分钟
B 3秒

解释时间线：
A 5分03秒
```

### 窗口替代规则

- 如果浏览器窗口时间段内存在合成后的页面记录，默认解释时间线显示页面记录并隐藏对应浏览器窗口，避免重复计时。
- 如果浏览器窗口时间段内没有任何合格页面记录，则回退显示原浏览器窗口记录。
- 非浏览器窗口照常显示。
- 原始事件视图仍显示 `window`、`web`、`afk` 原始记录。

### 合成记录字段

新增解释记录类型：

- `recordType = "web-page"`

字段：

- `start`
- `end`
- `durationSeconds`
- `deviceId`
- `title`
- `url`
- `domain`
- `path`
- `isLocalFile`
- `browserAppName`
- `browserWindowTitle`
- `audible`
- `incognito`
- `tabCount`
- `absorbedShortEventsCount`
- `absorbedDurationSeconds`
- `sourceWebEventIds`
- `sourceWindowEventIds`
- `raw`

`raw` 应保留 web 原始 JSON 和关联 window 原始 JSON 的必要摘要，完整原始事件可在原始事件视图查看。

## 展示设计

### 默认解释时间线

默认列表显示：

- 时间范围
- 类型：页面
- 页面标题
- 域名或文件标记
- 时长

示例：

```text
13:05-13:09 页面：REST API 文档 docs.activitywatch.net
13:11-13:16 页面：20222500 韩硕 卫星导航实验报告.pdf 文件
```

默认列表不直接铺开完整 URL，避免过长、泄露 token 或查询参数。

### 展开详情

页面记录展开显示：

- 完整 URL，可复制。
- 域名或本地文件路径。
- 页面标题。
- `tabCount`、`audible`、`incognito`，有则显示。
- 关联浏览器窗口 app/title。
- 吸收的短页面数量和时长。
- 原始 JSON 摘要。

### 原始事件视图

增加或保留“原始事件”筛选，用于查看：

- `window`
- `web`
- `afk`

原始事件视图不做短页面合并，也不隐藏浏览器窗口。

## 查询接口

`GET /api/v1/pc/detail` 保持现有输入相关响应不破坏，新增浏览器相关筛选：

- `eventType=web-page`：解释后的页面记录。
- `eventType=web`：原始页面事件。
- `eventType=window`：原始窗口或解释视图中的窗口回退记录。
- `domain`：按域名筛选。
- `title`：按页面标题筛选。
- `url`：按完整 URL 模糊筛选。
- `rawMode=true` 或 `view=raw`：返回原始事件视图。

如果前端暂不新增 `rawMode`，可以先在现有 event type 下提供 `web-page` 和 `web` 两种选项：

- 默认详情：使用解释视图。
- 原始事件：用筛选标签或单独切换按钮进入。

## 错误和边界

- 页面事件 `duration = 0` 或缺失：不进入解释时间线，但保留原始记录。
- 页面事件 URL 缺失：可显示标题；域名为空。
- 页面事件标题缺失：显示域名或文件名。
- 本地文件 URL：默认列表显示文件名和“文件”标记；详情显示完整路径。
- 隐私窗口：如果 ActivityWatch 提供 `incognito`，详情中显示；默认列表不特殊强调。
- 浏览器插件未运行：浏览器窗口回退显示原窗口记录。
- 页面事件和窗口事件时间不完全对齐：解释时间线以页面事件自身时段为准；窗口仅作为关联证据和回退来源。
- 多浏览器同时记录：按 bucket 和 app 关联，无法精确关联时仍以页面事件为主，详情标记关联质量。

## 测试策略

后端单元测试：

- 上传 `web.tab.current` bucket 和事件后，原始事件以 `bucket_id + source_event_id` 幂等保存。
- `duration < 5s` 的页面事件并入后一个有效页面。
- 末尾短页面并入前一个有效页面。
- 连续短页面组并入后一个有效页面。
- 有页面解释时隐藏对应浏览器窗口。
- 无页面解释时回退显示浏览器窗口。
- 非浏览器窗口不受影响。
- `aw-watcher-input` / `os.hid.input` bucket 不被采集器选择上传。

前端验证：

- 详情页能显示 `web-page` 记录。
- 默认列表不铺开完整 URL。
- 展开详情能看到完整 URL、关联窗口和 raw 摘要。
- 原始事件视图能看到短页面和被隐藏的浏览器窗口。
- 现有 KeyStats 相关 UI 和输入记录筛选不变。

验证命令：

```powershell
dotnet test Pim.sln
npm --prefix src/client-web run build
```

## 成功标准

- 安装浏览器插件后，PC Tracker 能保存 `web.tab.current` 页面事件。
- 默认解释时间线里，浏览器使用时间优先显示具体页面，而不是笼统浏览器窗口。
- 短页面按“优先并入后一个有效页面，否则并入前一个有效页面”规则合并。
- 浏览器窗口不与页面记录重复计时。
- 原始 ActivityWatch window/web/afk 事件仍可完整查看。
- KeyStats 和所有输入记录逻辑没有行为变化。
