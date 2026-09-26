# tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：完整捕获链路：模型/Schema、AW 上传幂等与校验、keystats 分钟快照、摘要/时间线分类、明细查询中网页合并与浏览器窗隐藏、原始 web 视图与多设备隔离。
- 主要依赖：`PcTrackerService`、`PcTrackerSchemaInitializer`、`ActivityClassificationSnapshotService`、`BrowserPageTimelineBuilder`、AW/Keystats 实体
- 被谁使用：xUnit

## 函数级结构化伪代码

### PcTrackerCompleteCaptureTests
#### Model / Schema
- 输入：无
- 输出：无
- 副作用：InMemory 模型探测；读 SchemaSql
- 步骤：
  1. 模型含 AwBucketEntity、KeystatsSampleEntity
  2. SchemaSql 先 CREATE pc_aw_events 再 ALTER
  3. string.Format 安全（保留 `'{}'::jsonb`）
- 分支与异常：无
- 调用：`PimDbContext`、`PcTrackerSchemaInitializer.SchemaSql`

#### UploadCompleteAwEventsAsync_*
- 输入：CompleteAwUploadRequest
- 输出：无
- 副作用：写/更新 AwEvent
- 步骤：
  1. 同 bucket+SourceEventId upsert（二次返回 0，Duration 更新）
  2. 同请求内重复 sourceId 去重取后
  3. 库内已有重复源行仍可更新其中之一
  4. 非法时间戳跳过，合法入库
  5. >500 事件 ArgumentException
  6. web.tab.current 存为 EventType=web
- 分支与异常：超批抛错
- 调用：`PcTrackerService.UploadCompleteAwEventsAsync`

#### UpsertKeystatsSampleAsync_StoresRawMinuteSnapshot()
- 输入：两分钟同分钟采样
- 输出：无
- 副作用：单行 KeystatsSample
- 步骤：分钟对齐 UTC；KeyPresses 取后；JSON 含 keyCounts/appStats/raw 字段
- 分支与异常：无
- 调用：`UpsertKeystatsSampleAsync`

#### GetSummaryAsync_*
- 输入：样本/日快照/窗口+web
- 输出：无
- 副作用：可能写分类
- 步骤：
  1. 无日快照用最新 sample 作 keystats 与 app ranking
  2. 时间线用浏览器页记录（域名作 AppName）
  3. 无 AppName 窗口过滤，保留网页；AppSwitchCount=0
  4. keyPressCounts 完整映射；topKeys 截断 10
  5. Heatmap 小时用本地业务时
- 分支与异常：无
- 调用：`GetSummaryAsync`

#### GetTimelineAsync_*
- 输入：窗口/网页/规则
- 输出：无
- 副作用：可能持久化分类快照
- 步骤：
  1. 网页优先展示
  2. 后端启发式分类（学习/色/项目标签）
  3. 规则命中写 ActivityClassificationEntity
  4. 过滤无 app 窗口
  5. 重叠窗口切分为非重叠区间
- 分支与异常：无
- 调用：`GetTimelineAsync`

#### QueryCompleteDetailAsync_*（核心明细）
- 输入：DetailQueryParams
- 输出：无
- 副作用：分类快照读写
- 步骤：
  1. 返回 window + input-minute（keystats 差分）
  2. 规则分类持久化；manual 快照受保护不覆盖
  3. reset 差分 KeyPresses=0 且 KeyCounts 空；gap 用真实经过秒
  4. 起止归一 UTC 排序
  5. 短网页并入下一有效页/尾部并入前页；长间隔不跨并
  6. 网页解释浏览器窗则隐藏窗并挂 browser 元数据；他应用占区间则丢网页
  7. 无网页时保留浏览器窗
  8. EventType=web 或 View=raw 返回原始 web，不夹 input-minute
  9. bucket 类型 web 但 EventType 非 web 仍可 raw 查
  10. raw web 查询不落无关 window 分类快照
  11. 合并/隐藏按 DeviceId 隔离
  12. 全短网页仍产出一页；尾部短页扩展后更新 browser 元数据
  13. 多窗重叠只隐藏选中浏览器；优先匹配 web bucket 的浏览器；主页浏览器优先于前导短页来源浏览器
- 分支与异常：无
- 调用：`QueryCompleteDetailAsync`

#### CompleteAwUploadRequest_BindsActivityWatchSnakeCaseFields()
- 输入：snake_case JSON
- 输出：无
- 副作用：无
- 步骤：反序列化 device_id/last_updated；再序列化仍 snake_case
- 分支与异常：无
- 调用：`JsonSerializer`

#### 辅助 WebEvent / WindowEvent / WindowEventWithoutApp / CodeWindowRule / MakeDetailQuery
- 输入：时间/时长/url/app 等
- 输出：实体或查询参数
- 副作用：无
- 步骤：标准化 bucket/DataJson/归一 app；默认查询 2026-05-20
- 分支与异常：无
- 调用：`AppNameNormalizer`、JsonSerializer

## 近逐行中文伪代码

1. [L15-L53] 模型与 Schema 安全
2. [L55-L298] AW 完整上传：upsert/去重/容错/非法时间/批限
3. [L300-L358] keystats 分钟快照
4. [L360-L674] 摘要：样本回落、网页时间线、热力/键位
5. [L485-L604] 时间线：网页分类、快照、非重叠
6. [L676-L913] 明细：window+input、分类/manual、DTO snake_case
7. [L915-L1041] 差分 reset/gap、UTC 排序
8. [L1043-L1085] web.tab.current 入库
9. [L1087-L1589] 网页合并、浏览器隐藏、raw 视图、多设备、主浏览器选择
10. [L1591-L1682] 事件与规则工厂

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs",
      "label": "PcTrackerCompleteCaptureTests",
      "path": "tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcTrackerSchemaInitializer.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/ActivityClassificationSnapshotService.cs", "type": "tests" },
    { "from": "tests/Pim.UnitTests/Services/PcTrackerCompleteCaptureTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/BrowserPageTimelineBuilder.cs", "type": "tests" }
  ]
}
```
