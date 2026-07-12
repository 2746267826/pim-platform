# src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：从本机 ActivityWatch 拉取支持的 bucket 事件，经 `/pc/aw/upload-complete` 完整上传到 PIM API；支持周期采集、手动同步、时间范围回填与游标状态。
- 主要依赖：`HttpClient`（AW）、`ApiClient`、`AwBucketSelection`、`AwCollectorCursorState`、`System.Text.Json`
- 被谁使用：Windows 守护进程/主应用启动、`StatusWindow` 手动同步

## 函数级结构化伪代码

### AwCollectorService
#### AwCollectorService(ApiClient apiClient)
- 输入：API 客户端
- 输出：实例
- 副作用：创建指向 `AW_BASE_URL` 或 `http://127.0.0.1:5600` 的 HttpClient
- 步骤：赋值 `_api`；初始化 CTS、采集闸门、游标、缓存
- 调用：无

#### 属性 QueueCount / LastUploadTime / LastUploadError / Log
- 输入：无
- 输出：线程安全读字段；可选日志回调
- 副作用：锁 `_lock`
- 调用：无

#### Task SyncNowAsync()
- 输入：无
- 输出：无
- 副作用：一次 CollectAndUpload；异常记 Log
- 调用：`CollectAndUploadAsync`

#### Task BackfillAsync(startUtc, endUtc)
- 输入：UTC 起止
- 输出：无
- 副作用：按时间范围拉事件并批量上传；更新 lastUpload*
- 步骤：
  1. 获取 `_collectionGate`
  2. end≤start → 记错误返回
  3. 缓存 AW info；`FetchSupportedBucketsAsync`
  4. 每 bucket `BackfillBucketAsync`；汇总健康消息与 uploaded
  5. 有上传则更新 LastUploadTime/Error；Log 摘要
  6. finally Release 闸门
- 调用：`FetchAwInfoAsync`、`BackfillBucketAsync`、`BuildUploadHealthMessage`

#### void Start()
- 输入：无
- 输出：无
- 副作用：后台循环每 30s 采集，直到取消
- 步骤：Task.Run while；Delay 30s；CollectAndUpload；TaskCanceled 退出
- 调用：`CollectAndUploadAsync`

#### private CollectAndUploadAsync()
- 输入：无
- 输出：无
- 副作用：更新队列 pending、上传时间/错误
- 步骤：闸门 → info → buckets → 每桶 CollectBucketAndUpload → pending=Fetched-Uploaded → 有上传写 health
- 调用：`CollectBucketAndUploadAsync`

#### private CollectBucketAndUploadAsync(bucket)
- 输入：bucket 载荷
- 输出：`AwBucketUploadOutcome`
- 副作用：POST 完整事件；成功提交游标
- 步骤：
  1. `LastForBucket` + `FetchNewEvents`
  2. 空 → (0,0)
  3. 按 500 批构造 `CompleteAwUploadPayload` → `PostAsync /pc/aw/upload-complete`
  4. null 响应/非 0|200 code → 记错误返回部分 uploaded
  5. 全成功：`RecordFetched`+`CommitFetched`；QueueCount=0
  6. HttpRequestException/其他：记错误，Uploaded=0
- 调用：`_api.PostAsync`、`IsSuccessResponse`、`AwBucketSelection.DescribeBucketKind`

#### private BackfillBucketAsync(bucket, start, end)
- 输入：桶与时间窗
- 输出：outcome
- 步骤：GET events?start&end；空返回；Chunk 200 上传；收集错误 join；不推进周期游标
- 调用：`_aw.GetFromJsonAsync`、`_api.PostAsync`

#### IsSuccessResponse / FetchAwInfoAsync / FetchSupportedBucketsAsync / EnsureBucketId / FetchBucketAsync
- 输入：响应或 bucketId
- 输出：bool / info / 支持桶列表 / 规范化桶 / 单桶
- 步骤：Code 0 或 200；GET /api/0/info；GET buckets 过滤 `IsSupportedUploadBucket` 排序缓存；Id 空则用字典键；缓存未命中则 GET 单桶
- 调用：`AwBucketSelection`

#### FetchNewEvents / BuildEventsUrl / ChunkCompleteAwUploadEvents / BuildUploadHealthMessage
- 输入：bucketId、lastId、events
- 输出：未处理事件列表、pendingLastId、分批、pending 健康消息
- 步骤：limit=-1 拉全量；Id>lastId 排序；max Id 为 pending；pending>0 则 Partial 消息
- 注意：同步阻塞 `GetAwaiter().GetResult()` 读事件
- 调用：无

#### Dispose()
- 输入：无
- 副作用：Cancel CTS；Dispose 闸门与 AW HttpClient

### 内部 DTO
#### RawAwEvent / AwInfoPayload / AwBucketPayload / AwEventPayload / CompleteAwUploadPayload / AwBucketUploadOutcome
- 职责：AW 原始事件、info、bucket、上传事件、完整上传体、结果结构体

### AwCollectorCursorState
#### LastForBucket / RecordFetched / CommitFetched
- 输入：bucketId、lastId
- 输出：已提交游标；暂存 pending；合并 pending→committed 后清空 pending
- 副作用：内存字典
- 步骤：GetValueOrDefault；pending 取 Max；commit 循环 Max 后 Clear

## 近逐行中文伪代码

1. 常量：无界 limit=-1；完整上传批 500
2. 字段：AW/API 客户端、CTS、SemaphoreSlim(1)、游标、队列/时间/错误、bucket 缓存、静态锁
3. AW_BASE 环境变量或 127.0.0.1:5600
4. 属性用 lock 读队列与上传状态
5. SyncNow：try Collect 记日志
6. Backfill：闸门校验时间窗→桶列表→逐桶回填→健康汇总
7. Start：30 秒周期 Task.Run
8. CollectAndUpload：逐桶上传→pending 作 QueueCount
9. CollectBucket：游标后新事件→500 批 complete upload→成功才提交游标
10. BackfillBucket：时间查询 200 批；错误可部分成功
11. 发现桶：缓存 + 支持类型过滤排序
12. FetchNewEvents：同步 HTTP 过滤 Id
13. Dispose 取消并释放
14. AwCollectorCursorState：两阶段游标避免半失败推进

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs",
      "label": "AwCollectorService",
      "path": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Services/ApiClient.cs", "type": "depends_on" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs", "type": "calls" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "to": "http://127.0.0.1:5600", "type": "http" },
    { "from": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "to": "/pc/aw/upload-complete", "type": "http" }
  ]
}
```
