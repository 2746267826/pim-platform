# src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.offline（client-android app）
- 职责：判断某类操作是否可在离线队列中排队，还是必须在线执行。
- 主要依赖：无外部项目依赖（纯集合判断）
- 被谁使用：离线/在线操作门禁相关 UI 或服务

## 函数级结构化伪代码

### OnlineOperationGuard
#### canQueueOffline(operationKind: String): Boolean
- 输入：`operationKind` 操作种类字符串
- 输出：是否属于可离线排队集合
- 副作用：无
- 步骤：
  1. 维护私有集合 `offlineQueueableOperations`：`collection-upload`、`android-location`、`android-usage`、`device-state`、`upload-retry`
  2. 对 `operationKind` 做 `trim()` 后判断是否 `in` 该集合
  3. 返回布尔结果
- 分支与异常：无
- 调用：`String.trim`、集合 `in`

#### requiresOnline(operationKind: String): Boolean
- 输入：`operationKind`
- 输出：需要在线（不可离线排队）则为 true
- 副作用：无
- 步骤：
  1. 返回 `!canQueueOffline(operationKind)`
- 分支与异常：无
- 调用：`canQueueOffline`

## 近逐行中文伪代码

1. [L1] 包声明 `com.pim.app.offline`
2. [L3] 类 `OnlineOperationGuard`
3. [L4-10] 私有集合 `offlineQueueableOperations` 含五类可排队操作
4. [L12-14] `canQueueOffline`：trim 后判断是否在集合中
5. [L16-18] `requiresOnline`：返回 `canQueueOffline` 的取反

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt",
      "label": "OnlineOperationGuard",
      "path": "src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
