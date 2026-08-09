# src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：判定端点操作种类是否允许离线入队；采集类可缓存，事实变更等必须在线。
- 主要依赖：无（BCL `HashSet`）
- 被谁使用：`Startup` DI 注册为 Singleton；供上传/出站边界逻辑消费

## 函数级结构化伪代码

### EndpointOperationBoundaryResult (record)
#### 主构造 (AllowedOffline, Kind, Message)
- 输入：是否允许离线、结果种类码、中文说明
- 输出：不可变结果
- 副作用：无
- 步骤：位置参数记录
- 分支与异常：无
- 调用：无

### EndpointCollectionBoundaryService
#### CanQueueOffline(string operationKind)
- 输入：操作种类字符串
- 输出：bool
- 副作用：无
- 步骤：`operationKind.Trim()` 后查大小写不敏感集合 `OfflineQueueableOperations`
- 分支与异常：集合未命中为 false
- 调用：`HashSet.Contains`

#### Guard(string operationKind)
- 输入：操作种类
- 输出：`EndpointOperationBoundaryResult`
- 副作用：无
- 步骤：
  1. 若 `CanQueueOffline`：`AllowedOffline=true`，`Kind=QueuedOffline`，消息说明可离线缓存重试。
  2. 否则：`AllowedOffline=false`，`Kind=BlockedOnlineOnly`，消息说明须在线执行。
- 分支与异常：无
- 调用：`CanQueueOffline`

#### 静态集合 OfflineQueueableOperations
- 内容：`collection-upload`、`pc-activity`、`window-context`、`browser-context`、`input-activity`、`device-state`、`upload-retry`
- 比较：`OrdinalIgnoreCase`

## 近逐行中文伪代码

1. 记录结果：允许离线、种类、消息。
2. 服务内静态 HashSet 列出可离线采集/上传类操作。
3. `CanQueueOffline`：trim 后 Contains。
4. `Guard`：可离线则 QueuedOffline；否则 BlockedOnlineOnly 并返回中文说明。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs",
      "label": "EndpointCollectionBoundaryService",
      "path": "src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.App/Startup.cs", "to": "src/client-windows/Pim.Client.Core/Services/EndpointCollectionBoundaryService.cs", "type": "depends_on" }
  ]
}
```
