# 历史定位地图可靠轨迹与路线匹配设计

## 1. 目标与范围

历史定位页面当前把同一片段中的原始定位点按时间顺序直接交给 Leaflet `Polyline`。当采样稀疏、定位漂移或中间长时间没有数据时，地图会画出用户实际上不可能经过的直线，形成“轨迹飞跃”。

本设计把定位事实与路线推测彻底分开：

- 原始定位记录保持不变，并以点的形式呈现。
- 后端首先产生确定性的停留和断口，不允许跨断口推测。
- 只有连续、可匹配且证据充分的移动片段才交给当前选中的路线服务。
- 路线服务不可用或结果不可信时保留断口，绝不回退到原始点直连。
- 推测路线是独立、可完全关闭的图层；关闭时不产生路线服务请求。

### 1.1 当前实现

- `src/client-web/src/components/mobile/HistoricalLocationLeafletMap.tsx` 把每个 `segment.path` 映射为一条 `Polyline`，同时把相同数据渲染为 Marker。
- `src/modules/Pim.Module.Mobile/Services/MobileLocationAggregationService.cs` 目前只用固定 2 小时的 `TrackGapThreshold` 拆分 track；`stay`/`move` segment 的 `Path` 是原始点序列。
- `MobileLocationAggregationService` 以相邻原始点的球面直线距离计算 track/segment `DistanceMeters`，该值目前被多个历史定位组件展示为里程。
- 当前没有地图匹配 Provider、推测路线结果、逐点匹配证据或独立断口合同。

### 1.2 关键不变量

1. 任何前端组件都不得用原始定位点序列绘制连接线。
2. 时间缺口、无效点、低质量点或匹配失败形成真实空白，不得跨接。
3. 速度不参与删点、断线、异常判定、交通方式推断或路线验收。
4. 长缺口不发送给 Provider、不连接、不计入可计算里程。
5. 只有验收通过的推测路线 geometry 才计入“可计算里程”。
6. 无 Provider、Provider 故障或用户关闭推测路线时，原始点、停留和断口仍可正常查看。
7. 只调用管理员当前显式选中的一个 Provider，不自动重试，不跨 Provider fallback。
8. 第三方测试不使用用户位置；坐标、时间戳、请求 URI/Body、响应 Body 和凭据不得进入日志。
9. 任何回滚都只能退化为“原始点 + 停留 + 断口”，不得恢复原始点直连。

### 1.3 非目标

- 不实现 Python、OSMnx、NetworkX、SQLite/R-Tree、Python sidecar、离线路网 baker 或 `.pimroads.sqlite.gz`。
- 不根据速度自动选择 `driving`、`walking` 或 `cycling`。
- 不做多 Provider 排序、自动切换、周期性第三方健康轮询或后台预计算全部历史数据。
- 不自动安装 OSRM、不自动下载 OSM 数据、不自动修改服务器防火墙。
- 本规格不承诺第三方能从两个稀疏点恢复真实路径；此类输出始终明确标识为推测路线。

## 2. 总体架构

```text
Android 定位上传
  -> 原始点存储（事实）
  -> MapFactsBuilder
       -> rawPoints / stays / breaks / eligible move fragments
  -> MapMatchingCoordinator（仅推测路线开启时）
       -> persistent cache + per-key single-flight
       -> 当前一个 IMapMatchingProvider
       -> provider-specific parser
       -> MapMatchResultValidator
  -> inferredRouteSegments / routeSummary（派生结果）

Web 历史定位
  -> 先加载 map facts，立即画原始点、停留和断口
  -> 推测路线开启时再 resolve inferred routes
  -> 只把 inferredRouteSegments 交给 Polyline
```

### 2.1 组件边界

`MapFactsBuilder` 负责读取当前用户的原始点，执行排序、质量归一化、hard break、停留识别和移动片段生成。原始历史接口与路线协调器必须复用这一个组件，避免页面断口与 Provider 输入采用不同规则。

`MapMatchingCoordinator` 负责读取当前配置、计算 cache key、分块、选择唯一 Provider、调用验收器、合并成功块和生成结构化状态。它不包含第三方 JSON/GPX 细节。

`IMapMatchingProvider` 的三个实现分别封装 Mapbox、GraphHopper 和 OSRM 的请求、响应解析及 Provider 能力限制。接口至少提供配置校验、连接测试和轨迹匹配；`none` 由协调器直接处理，不实现虚假的 Provider。

`MapMatchResultValidator` 负责 Provider 无关的 geometry、锚点、吸附距离和顺序验收，并调用少量 Provider 特有证据规则。Provider 返回 HTTP 200 不等于结果可展示。

持久化设置和缓存由 Mobile 模块拥有，使用现有 EF Core module entity/configuration 模式及 `PimDbContext.Set<TEntity>()`，避免给 Infrastructure 引入对 Mobile 模块的反向依赖。凭据保护复用 `ISecretProtector`。

## 3. 原始事实与分段

### 3.1 采样间隔元数据

定位点新增可空的 `ExpectedSamplingIntervalSeconds`，并贯穿 Android 上传请求、`MobileLocationPointEntity` 及相关读取 DTO。它表示采集该点时客户端预期的下一次采样间隔，而不是相邻点已经发生的实际间隔。

Android 按实际采集策略填写该字段：移动约 60 秒、常规约 180 秒、低频约 900 秒。服务端不从实际缺口反推该字段，因为这样会让一次漏报反过来放宽断点阈值。

服务端只接受正数且能安全参与秒数计算的声明值；非法声明按缺失处理。历史记录为 `NULL`，使用固定 30 分钟断点阈值。

### 3.2 排序与 hard break

原始点先按 `DeviceId` 分组，再按 `RecordedAtUtc`、`Id` 稳定排序。以下情况形成 hard break：

- 设备边界。
- 相邻记录时间相同或不递增。
- 纬度、经度不是有限值或超出合法范围。
- 点的持久化 `Quality` 为 `rejected`；当前上传服务会把水平精度大于等于 50 米的点标为 rejected。
- 明确存在但不是正数的水平精度，或水平精度大于 100 米。
- 相邻时间差大于自适应阈值。

数据库现有水平精度是非空字段。为兼容未来导入数据和统一匹配 DTO，缺失精度按 25 米处理，不单独形成 hard break；明确非法值与缺失值必须区分。无效坐标记录仍保留在数据库和审计数据中，但无法作为地图 Marker，API 以 break reason 表达其位置。

低质量或 rejected 但坐标合法的点仍返回给地图并使用弱化样式。它不参与匹配，并在其前后形成断口；实现不得简单过滤该点后再连接它两侧的点。100 米是独立于当前 50 米上传质量规则的绝对防线，不代表 50 至 100 米的 rejected 点重新获得匹配资格。

### 3.3 自适应时间阈值

对于前一点声明了有效采样间隔的相邻点：

```text
threshold = min(45 minutes, max(10 minutes, 3 * expected interval of previous point))
```

前一点没有声明时，`threshold = 30 minutes`。只有当实际间隔严格大于阈值才断开；等于阈值仍属于同一连续候选片段。例子：

| 前一点预期间隔 | 断点阈值 |
|---|---:|
| 1 分钟 | 10 分钟 |
| 3 分钟 | 10 分钟 |
| 15 分钟 | 45 分钟 |
| 缺失 | 30 分钟 |

当前固定 2 小时规则必须被统一替换；overview、tracks、segments、map facts 和 map matching 共用同一阈值实现。

### 3.4 停留、移动与原始距离

hard break 先于现有停留/移动识别执行。停留片段不发送 Provider，推测移动距离为 0。至少包含两个合格点的移动片段才成为匹配候选。

现有 `DistanceMeters` 是原始点间球面弦长之和，可继续用于内部 stay/move 分类和短期 API 兼容，但必须满足两点：

- 计算不能跨 hard break。
- Web 历史定位不得再把它标为“里程”或“总里程”。

兼容期在 C# DTO 属性和 TypeScript 接口上标记该字段为 deprecated，并在字段说明中写明“raw chord estimate, not route mileage”。后续 API 主版本将其重命名为 `RawChordDistanceMeters`；任何新组件不得依赖旧名称作为里程数据源。

正式展示的移动里程只来自 `routeSummary.computedDistanceMeters` 或各个已验收推测子段的距离。Provider 未配置或没有可验收路线时显示不可计算，而不是显示 0 公里或原始弦长。

### 3.5 分块

每个 Provider 声明自己的最大点数；协调器的单块硬上限为 100 点。超长连续片段按时间顺序切块，相邻块重叠两个原始锚点。

只有两块都验收成功且共享的锚点映射一致时才在共享锚点处去重合并。任一块失败时，该块覆盖的区间保持断开；禁止把失败块前后的成功 geometry 直接相连。

## 4. Provider 配置与管理

### 4.1 配置状态

系统级设置包含：

- `ActiveProvider`: `none`、`mapbox`、`graphhopper`、`osrm`，默认 `none`。
- 通用手工 profile: `driving`、`walking`、`cycling`。
- Mapbox 加密 Access Token 和配置版本。
- GraphHopper 加密 API Key 和配置版本。
- OSRM Base URL、实例 profile、部署 manifest 的合成 smoke trace 和配置版本。
- 每个 Provider 最近测试的配置版本、测试时间、脱敏状态码及脱敏错误类别。

管理员设置 API 返回 ActiveProvider、profile、脱敏测试状态及 OSRM Base URL/synthetic smoke trace 等非机密配置，并以 `isSecretConfigured` 表示 Mapbox/GraphHopper 凭据状态；它不返回密文或明文凭据。普通用户接口只返回推测路线是否可用，不暴露 OSRM 内网地址、配置版本或测试详情。Token 与 API Key 使用现有 `ISecretProtector` 加密后入库，并且不得写入普通应用配置文件。

保存、测试、启用是三个独立动作。修改 Provider 配置会增加配置版本、使旧测试结果失效并把 `ActiveProvider` 置为 `none`；管理员必须对新版本重新测试并再次显式启用。一次后续的瞬时测试失败只更新状态，不自动切换已经启用且配置未变化的 Provider；实际匹配失败按断口降级。

系统不执行周期性第三方连接测试。服务器可以长期保持 `none`，其他 PIM 功能不依赖任何路线服务。

### 4.2 手工 profile

管理员手工选择通用 profile，协调器不读取速度决定交通方式。适配器按当前官方契约转换：

| 通用值 | Mapbox | GraphHopper | OSRM |
|---|---|---|---|
| `driving` | `mapbox/driving` | 当前托管 API 对应 car/driving 参数 | 配置的 driving 实例 profile |
| `walking` | `mapbox/walking` | 当前托管 API 对应 foot/walking 参数 | 配置的 walking 实例 profile |
| `cycling` | `mapbox/cycling` | 当前托管 API 对应 bike/cycling 参数 | 配置的 cycling 实例 profile |

GraphHopper 托管 API 的 `vehicle`/`profile` 参数存在版本差异，适配器实现必须以实施时可访问的官方文档和真实手工连接测试为准，不能根据训练知识猜测。OSRM 数据在预处理时已经绑定路由 profile；切换 PIM 下拉框不能把 driving 数据变成 walking 数据。

### 4.3 连接测试

连接测试验证网络、认证、profile、响应解析及非空 geometry，但不自动启用 Provider：

- Mapbox 和 GraphHopper 使用代码中固定的三点公共道路合成轨迹和固定递增时间戳。
- OSRM 的覆盖范围取决于管理员部署的数据，使用部署 manifest 中的合成 smoke trace，或管理员明确填写的非历史测试坐标。
- 测试端点从数据库读取已保存配置，不接受前端提交任意轨迹，也不读取用户历史定位。
- 返回类别限于成功、配置无效、认证失败、无法访问、超时、限流、profile 不支持、无匹配和响应无效。

### 4.4 OSRM Base URL 安全边界

OSRM 需要访问 `127.0.0.1` 或内网地址，因此不能使用“禁止私网”的常规 SSRF 规则。该能力只对管理员开放，并采用以下限制：

- 仅允许 `http`/`https`，禁止 URL user-info、query、fragment 和非预期路径模板。
- PIM 自己追加固定的 `/match/v1/{profile}/...` 路径。
- 禁止自动重定向，限制连接/总超时和最大响应体。
- Base URL、DNS 解析结果及完整请求 URI不进入普通日志。

管理员显式配置内网目标本身属于受信任管理操作；UI 必须说明该设置会让 PIM 服务器向目标地址发起请求。

## 5. Provider 契约与标准化

### 5.1 Mapbox

使用官方 Map Matching v5：

```text
GET https://api.mapbox.com/matching/v5/{profile}/{lon,lat;...}.json
```

请求使用 `geometries=geojson`、`overview=full`、递增 `timestamps`、由有效精度推导的 `radiuses`、`tidy=false` 和 Access Token。显式关闭 `tidy` 是为了保持输入点顺序和逐点证据不被 Provider 重采样或聚类；PIM 宁可在稀疏数据上保守断开。坐标数为 2 至 100。解析 `tracepoints[]`、`matchings[]`、每个 matching 的 geometry、distance、duration 和 `confidence`。

### 5.2 OSRM

使用当前配置 Base URL 上的 Match service：

```text
GET /match/v1/{profile}/{lon,lat;...}
```

请求使用 `geometries=geojson`、`overview=full`、`timestamps`、`radiuses`、`tidy=false` 和 `gaps=ignore`。显式关闭 `tidy` 以保留 PIM 的原始点证据；PIM 已在请求前执行自己的自适应断点，使用 `gaps=ignore` 可避免 OSRM 默认按大于 60 秒再次拆开正常的低频采样。集成测试必须断言每个发送给 OSRM 的片段已经在 PIM hard break 处严格分割。解析 `tracepoints[]`、`matchings[]` 和每个 matching 的 `confidence`；`NoMatch`、`NoSegment`、`TooBig` 均为不可展示结果或分块能力反馈。

### 5.3 GraphHopper

使用托管 Map Matching API 的 `POST /match`，以 GPX 1.1 body 传递轨迹和时间戳，认证信息放在官方要求的查询参数中。标准成功响应以 `paths[].points` 提供路线 geometry，并包含 `map_matching` 汇总；标准合同没有可依赖的 `confidence`，也没有与 Mapbox/OSRM `tracepoints[]` 等价的可靠逐点映射。

GraphHopper 的 `confidence` 和逐点映射在统一模型中必须为 `null`，不得用常量、距离比例或内部 HMM 概率伪造。实现前复核当前托管文档、官方源码与真实测试响应，并保存脱敏 fixture 作为 parser contract test。

### 5.4 标准化结果

每个推测子段至少包含：

```text
routeSegmentId
sourceFragmentId
sourcePointIds[]
geometry: GeoJSON LineString coordinates [longitude, latitude]
computedDistanceMeters
provider
profile
confidence: number?          // 子段级，可空
pointMappings: mapping[]?    // 可空
unmatchedPointIds: string[]? // 可空
```

顶层 `routeSummary` 包含 `status`、`computedDistanceMeters`、`eligibleFragmentCount`、`matchedFragmentCount`、`provider` 和 `profile`。状态只有：

- `complete`: 所有可匹配片段均成功；没有可匹配片段时也可为 `complete`，计数为 `0/0`。
- `partial`: 至少一个片段成功且至少一个片段失败或只部分匹配。
- `unavailable`: 已选择 Provider，但本次没有可展示结果或服务不可用。
- `unconfigured`: `ActiveProvider=none` 或当前配置版本未完成启用流程。

响应同时包含脱敏的 `inferenceBreaks[]` 和 `unresolvedFragments[]`。前者用相邻原始点 ID 与 `unmatched`/`low-evidence` 表达 Provider 产生的内部断口；后者用 `sourceFragmentId` 与 `unmatched`/`low-evidence`/`provider-unavailable` 表达没有成功 geometry 的候选片段。它们不得携带第三方原始错误文本。

认证/授权失败仍使用正常 HTTP 401/403；以上状态只描述路线推测业务结果。

## 6. 结果验收与里程

### 6.1 通用验收

Provider geometry 只有同时满足以下条件才可展示：

1. 坐标均为有限合法值，LineString 至少包含两个不同坐标。
2. 每个展示子段至少由两个按时间递增的原始点锚定。
3. 原始锚点到 geometry 的距离不超过该点的允许吸附范围。
4. Provider 明确报告的未匹配点不位于被接受子段内部；以这些点为断口继续拆分。
5. 输入点、映射点和 geometry 的时间/路径顺序一致。

允许吸附范围为：

```text
effectiveAccuracy = stored accuracy, or 25m when genuinely absent
snapRadius = min(50m, max(10m, effectiveAccuracy))
```

明确非法或大于 100 米的精度已在分段阶段形成 hard break，不进入 Provider 请求。Provider 不支持所需半径时不得放宽 PIM 的 50 米上限。

### 6.2 Provider 特有证据

- Mapbox 和 OSRM：每个 matching 的 `confidence >= 0.5`，并依据 `tracepoints` 的 null、matching index 和 waypoint index 拆分。`0.5` 是 PIM 的保守验收策略，不是 Mapbox 或 OSRM 官方推荐阈值；阈值作用于子 matching，不把多个 matching 平均成一个分数。
- GraphHopper：由于没有可靠 confidence/逐点映射，所有输入点都必须在各自吸附范围内投影到返回 geometry，且沿 geometry 的投影位置随时间单调不减。存在无法唯一解释的回环投影、超距点或逆序点时拒绝整个请求块。

不使用速度、Provider duration、直线距离/路线距离比例或“看起来合理”的前端判断补充验收。保守拒绝会产生更多断口，这是预期行为。

### 6.3 可计算里程

Provider 返回的 distance 只用于诊断和合同测试。正式 `computedDistanceMeters` 由后端对验收后的 `overview=full` geometry 统一计算，并按成功子段求和。断口、停留、失败块和未匹配区间贡献 0，但页面在没有任何成功子段时显示“不可计算”，不把它解释为实际移动 0 公里。

## 7. 缓存与并发

验收成功的标准化子段按 fragment/chunk 持久缓存 30 天。顶层 `partial` 每次由已缓存成功子段和本次未成功子段重新汇总；超时、未匹配和其他失败状态不进入 30 天缓存，避免一次瞬时故障长期冻结断口。缓存只保存标准化 geometry、映射、计数和必要元数据，不保存第三方原始请求/响应或凭据。

Cache key 至少包含：

- 当前 `UserId`。
- 有序原始点 ID、时间、坐标、精度和预期采样间隔的稳定摘要。
- Provider、profile 和 Provider 配置版本。
- 分段算法版本、验收算法版本和 geometry 请求版本。

缓存 geometry 与原始定位一样属于敏感位置数据，必须使用同一用户授权边界。删除原始点、用户或覆盖时间范围时同步失效相关缓存；过期项读取时视为 miss 并清理。不得在 cache key、日志或指标中暴露可逆坐标。

同一 fragment/chunk cache key 使用进程内 single-flight，保证并发请求最多产生一次 Provider 调用。single-flight 只优化当前实例；数据库唯一键保证并发写入幂等。失败结果只做不超过 60 秒的内存抑制，防止页面连续刷新形成请求风暴。

## 8. API 与前端行为

### 8.1 两阶段接口

第一阶段新增只返回事实的历史地图合同：

```text
GET /api/v1/mobile/location/analytics/map-facts

rawPoints[]  // 当前用户、指定设备/范围内所有坐标合法的原始点
stays[]
breaks[]     // beforePointId, afterPointId, reason, gapSeconds?
range / device / generatedAt
```

每个 raw point 带有质量、精度和 `eligibleForMatching`，但不带连接 geometry。该接口不读取 Provider 设置，不触发第三方请求。

第二阶段是可能产生外部调用的显式操作：

```text
POST /api/v1/mobile/location/analytics/inferred-routes/resolve

request:  deviceId, rangeStartUtc, rangeEndUtc
response: inferredRouteSegments[], inferenceBreaks[], unresolvedFragments[], routeSummary
```

前端不提交坐标。后端按当前用户重新授权、重新读取原始点并复用 `MapFactsBuilder`。预期的 Provider 失败返回 HTTP 200 和四种业务状态；请求格式错误返回 400，认证/授权失败返回 401/403，PIM 自身不可恢复的基础设施故障仍按标准 5xx 处理。由于原始事实已先显示，第二阶段故障不会让地图白屏。

### 8.2 推测路线开关

- `ActiveProvider` 有效启用时，新浏览器默认开启推测路线；用户关闭偏好保存在浏览器本地。
- 关闭状态不调用 resolve 接口。切换为关闭时使用 `AbortController` 取消前端等待并忽略迟到结果。
- 请求到达 PIM 或第三方后无法保证撤回；关闭开关只能阻止尚未发出的请求并取消结果消费，UI 不得宣称能撤销已发送坐标。
- Provider 为 `none` 时开关关闭并显示“未配置”，地图事实层保持可用。

### 8.3 地图与状态

- 原始点始终位于推测路线之上；低质量点使用弱化视觉，但仍可查看时间和精度。
- 停留沿用现有停留标记。事实 `breaks` 与推测 `inferenceBreaks` 都保持真正留白，可在端点显示小型空心标记和脱敏原因。
- 只有 `inferredRouteSegments[].geometry` 能创建 Leaflet `Polyline`，使用明确的虚线样式。
- 状态区域显示“可计算里程”和“已匹配 X/Y 个连续片段”。`partial` 不能称为完整总里程；`unavailable`/`unconfigured` 显示不可计算。
- 普通用户只看到未配置、计算中、完整、部分可用或暂不可用；认证、限流和网络细节只在管理员设置页展示。

### 8.4 旧合同与消费方迁移

`MobileLocationSegmentDto.Path` 在兼容期继续表示原始点，不新增含义，也不能承载推测 geometry。新的 Web 地图改用 map-facts 和 inferred-routes 合同。

必须审计所有 `segment.path` 和 location `distanceMeters` 消费方，至少包括：

- `HistoricalLocationLeafletMap.tsx`：移除原始 `Polyline`，Marker 改读事实点，新增推测路线 prop。
- `LocationHistoryMap.tsx`：总里程改读 `routeSummary`，未匹配时显示不可计算。
- `LocationMetricStrip.tsx`：停止使用旧 overview 的 raw `DistanceMeters`；改为消费 resolve 请求的 `routeSummary.computedDistanceMeters`。路线尚未计算或状态为 `unconfigured`/`unavailable` 时显示“不可计算”，不显示 0。
- `LocationSegmentDetail.tsx` 和地图 Popup：按 `sourceFragmentId` 查找并汇总已匹配子段距离；没有对应子段时显示“不可计算”。`LocationStayMoveTimeline.tsx` 移除当前 raw 距离值，仅保留持续时间、点数和精度等事实信息。
- 新增明确命名的 `formatComputedDistanceMeters(number | null, RouteSummaryStatus)` 格式化入口，禁止把 `null` 传给当前会把空值格式化成 `0 m` 的 `formatDistanceMeters`。

同一审计必须覆盖 Android `MobileModels.kt` 及其历史定位消费方；若 Android 只解析而不展示该字段，保留兼容解析但不得新增 raw 里程展示。

旧 `/tracks` 合同仍可服务其他分析，但其 2 小时分段必须迁移到统一 hard break，避免内部距离跨越长缺口。兼容字段应在 API 文档中明确为 raw chord estimate，不能被新 UI 当作路线里程。

## 9. 错误处理、隐私与日志

### 9.1 降级

超时、DNS/连接失败、HTTP 429、401/403、Provider 5xx、`NoMatch`、`NoSegment`、`TooBig`、空 geometry、低置信度及解析失败均收敛为断口和结构化状态。协调器不自动重试，不调用另一个 Provider，不用原始点补线。

管理员设置保存失败、数据库读取失败等 PIM 自身错误不伪装成 Provider 未匹配；管理接口返回正常错误合同，推测路线接口与事实接口保持故障隔离。

### 9.2 第三方披露

启用 Mapbox 或 GraphHopper 前，管理员界面明确说明连续片段的坐标、时间戳和定位精度会发给所选第三方。OSRM 目标可能是本机、内网或管理员控制的远端，界面显示实际主机但不显示凭据。

Mapbox/GraphHopper 只有配置保存、同版本连接测试成功并由管理员显式启用后才能接收真实轨迹。OSRM 也遵守相同的配置版本、测试和显式启用门禁。连接测试始终使用合成数据。

### 9.3 日志

允许记录：Provider 名称、点数、耗时、HTTP 状态类别、标准化结果类别、匹配片段计数和随机关联 ID。

禁止记录：完整或部分 Request URI、query、请求/响应 Body、坐标、时间戳、bbox、原始点 ID 列表、Access Token、API Key、OSRM smoke trace 和可逆 cache key。

Mapbox 与 OSRM 的坐标位于 URI，Mapbox Token 位于 query；GraphHopper 坐标位于 GPX，API Key 位于 query。为这些 named `HttpClient` 禁用或替换默认 URI 日志，禁止记录原始 HTTP 异常 Message；日志只写经过枚举映射的错误类别。自动化测试必须捕获日志并断言 fixture 坐标和凭据均不存在。

## 10. OSRM Ubuntu 24.04 原生部署交付

目标服务器为 Ubuntu 24.04.2 LTS、Linux 6.8.0-63-generic、x86_64，服务器不运行 Docker；本地电脑可以使用 Docker。实现阶段交付原生部署文档和可复现辅助脚本，边界如下：

1. 固定 OSRM 版本和镜像/源码校验和，不使用浮动 `latest`。
2. 本地 Docker 使用官方 `osrm/osrm-backend` 固定版本完成小数据集 `/match` 合同测试和 OSM 数据预处理。
3. 原生二进制在 Ubuntu 24.04 builder 环境构建并 staged install；部署包包含运行所需二进制、运行库、profiles、许可证、版本 manifest 和 SHA-256 清单。打包脚本必须对每个 ELF 执行 `ldd` 审计，记录 glibc、Boost、STXXL、TBB、LuaJIT 等实际运行库版本和 unresolved dependency；不把“安装了 dev 包”当作运行时验证。
4. 部署包必须在干净的 Ubuntu 24.04 容器中以“无构建工具链”方式通过启动和 synthetic trace smoke test，证明它不是只在构建容器中可运行；该测试同时验证 manifest 中列出的运行库是否足够。
5. 管理员在本地准备自己的 `.osm.pbf`，执行 `osrm-extract`、`osrm-partition`、`osrm-customize`，生成与 profile 对应的数据和覆盖范围内的合成 smoke trace。
6. 管理员手工上传二进制包和预处理数据；服务器流程不下载 OSM、不安装 Docker、不自动安装系统包、不修改防火墙。
7. 服务器以专用低权限用户和 `systemd` 运行 `osrm-routed`，数据目录只读，默认绑定 `127.0.0.1:5000`，并提供启动、状态、日志、smoke test、版本切换和回滚命令。
8. 文档另列服务器直接源码构建的管理员参考路径，并明确 OSRM 官方推荐 Docker，Ubuntu Noble 原生构建属于 PIM 验证的 best-effort 路径，不是上游一键支持。
9. walking/cycling 使用相应 profile 重新预处理数据并运行独立实例；PIM 同一时刻仍只连接当前一个 OSRM 配置。

部署 manifest 至少记录 OSRM 版本、目标平台、profile、数据集标识和校验和、生成时间、服务参数及 synthetic smoke trace。smoke trace 是管理员选择的公开/合成点，不从 PIM 历史数据库生成。

## 11. 存储、迁移与回滚

数据库迁移新增可空采样间隔字段、全局 Provider 设置表和用户隔离的路线缓存表。Provider 设置默认 `ActiveProvider=none`，因此数据库迁移不会触发任何外部请求。

推荐部署顺序：数据库/API（兼容旧 Android 的空采样间隔）-> 新 Web 事实层 -> 新 Android 上报预期间隔 -> 管理员按需配置 Provider。旧 Android 不升级时仍按 30 分钟阈值工作。

从固定 2 小时切换到 30 分钟旧数据阈值后，未升级客户端可能看到数量更多、范围更短的 track/segment，但 JSON 兼容字段保持可解析。这是消除长缺口飞跃的预期行为，不为旧客户端保留 2 小时分段旁路。

关闭 Provider 是首选运行时回滚：设为 `none` 后 Web 只显示原始点、停留和断口。代码版本回滚也必须保留“不画原始连接线”的最小修复；不得把恢复旧 `Polyline` 作为回滚步骤。缓存表和新增可空列可留存，只有确认所有运行版本均不再读取后才执行 schema rollback。

## 12. 测试与验收

### 12.1 后端测试

- 断点公式：1/3/15 分钟采样、缺失声明、非法声明、阈值等于/大于边界、45 分钟上限。
- 质量：合法低质量点仍出现在 facts；非法坐标不可渲染；精度缺失用 25 米；精度非法或大于 100 米在前后断开。
- 停留与移动：先 hard break 后分类；停留不请求 Provider；速度字段改变不影响任何断点、profile 或验收结果。
- 分块：100 点上限、两个重叠锚点、失败块不跨接、共享锚点不一致不合并。
- Mapbox/OSRM：官方 fixture 的 tracepoint 拆分、confidence 边界 `0.5`、NoMatch/NoSegment/TooBig。
- GraphHopper：`confidence=null`、`pointMappings=null`、超距点、逆序投影和回环歧义拒绝整个块。
- 距离：只累计已验收 full geometry；停留、断口、失败块和未匹配区间不计入。
- 状态：`none`、全部成功、部分成功、全部失败准确映射为四种业务状态。
- 缓存：用户隔离、30 天 TTL、配置/点/算法版本失效、single-flight、删除原始数据后失效。
- 安全：设置版本门禁、密文不回显、OSRM URL 约束、HTTP/log fixture 中的坐标和凭据不进入日志。

Adapter CI 测试使用模拟 `HttpMessageHandler` 和脱敏官方响应 fixture，不调用真实付费 API。手工 Provider 连接测试是管理员运行的独立验收，不是 CI 依赖。

### 12.2 前端测试

- `segment.path` 或 `rawPoints` 从不成为 `Polyline.positions`；Polyline 只接收推测 geometry。
- facts 先显示，推测路线 loading/失败不清空地图。
- 开关关闭不调用 resolve；切换关闭会 abort 并忽略迟到结果。
- `unconfigured`、`unavailable`、`partial`、`complete` 的状态、里程和片段计数文案正确。
- 原始点、停留、断口和虚线路线在桌面/移动视口无重叠控件，点位 Popup 不再显示原始弦长里程。

### 12.3 OSRM 验证

- 本地 Docker 用固定小型 OSM 数据运行真实 `/match`，验证 OSRM adapter。
- 生成的 Ubuntu 24.04 原生部署包在干净同版本环境通过 manifest synthetic trace。
- 服务器部署后用同一 trace 做连接测试，且服务只监听配置的本地/内网地址。

### 12.4 最终验收场景

1. 长时间缺失在 Provider 调用前断开，地图无跨越线，缺口距离不计入可计算里程。
2. 所有 Provider 均缺失时，历史定位和 PIM 其他功能正常。
3. 低质量点可见但其两侧不被跨接；速度异常不会单独造成断线。
4. 任一 Provider 超时、限流、认证失败或返回低证据结果时，只影响推测图层。
5. 关闭推测路线后不再产生 resolve 请求；重新打开可使用缓存或当前一个 Provider。
6. 日志和 API 设置响应中不存在明文凭据、用户坐标或合成 fixture 坐标；源代码和部署 manifest 中允许存在专门用于测试的公开合成坐标，但不得包含用户历史位置。
7. 历史页面所有里程显示都来自已验收推测 geometry；未匹配时明确显示不可计算。

## 13. 官方契约依据与版本风险

- Mapbox Map Matching API v5: <https://docs.mapbox.com/api/navigation/map-matching/>
- OSRM Match service: <https://github.com/Project-OSRM/osrm-backend/blob/master/docs/http.md>
- GraphHopper 官方服务源码：`graphhopper/graphhopper` 中的 `MapMatchingResource.java`、`MatchResult.java`，以及官方 `directions-api-js-client` 的 `GraphHopperMapMatching`。

Mapbox 官方合同明确支持 2 至 100 点、`timestamps`、`radiuses`、`tracepoints`、`matchings` 和 `confidence`。官方 URL 模板包含 `.json` 后缀；规格中的 `.json` 不是从邻近 API 推断。OSRM 官方合同明确提供 `tracepoints`、`matchings`、`confidence` 及 `NoMatch`/`NoSegment`/`TooBig`。

截至本规格日期，`docs.graphhopper.com` 在当前调查网络中受 Cloudflare 阻挡。可复核的官方源码位置为 `map-matching/src/main/java/com/graphhopper/matching/MatchResult.java` 和 `web-bundle/src/main/java/com/graphhopper/resources/MapMatchingResource.java`，另有官方 README 的 map-matching 示例。它们足以确认 `POST /match`、GPX 输入以及标准响应不提供可依赖 `confidence`；托管参数和套餐限制仍可能随服务版本变化。因此 GraphHopper adapter 上线门槛包含当前官方合同复核、真实合成轨迹连接测试和脱敏 parser fixture，任何缺失字段按不可用或 `null` 处理，不能伪造。
