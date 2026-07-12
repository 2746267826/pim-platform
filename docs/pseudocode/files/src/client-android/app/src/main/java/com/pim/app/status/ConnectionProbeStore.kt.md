# src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.status
- 职责：连接探测结果的持久化与内存 StateFlow 证据库；按服务器身份与 5 分钟新鲜度查询。
- 主要依赖：`SharedPreferences`、`kotlinx.serialization.Json`、`ConnectionProbeResult`
- 被谁使用：连接探测服务与状态中心读取/写入探测证据

## 函数级结构化伪代码

### ConnectionProbeEvidenceStore
#### 接口声明
- 输入：无
- 输出：契约符号
- 副作用：无
- 步骤：
  1. 暴露 `result: StateFlow<ConnectionProbeResult?>`
  2. `save` / `freshResult` 契约
- 分支与异常：无
- 调用：无

### ConnectionProbeStore
#### 构造 ConnectionProbeStore(preferences, json)
- 输入：SharedPreferences、Json
- 输出：实现 `ConnectionProbeEvidenceStore` 的存储实例
- 副作用：构造时 `load()` 初始化 StateFlow
- 步骤：
  1. 创建锁与 `MutableStateFlow(load())`
  2. 对外只读 `asStateFlow()`
- 分支与异常：load 失败得 null
- 调用：`load`

#### save(result: ConnectionProbeResult): Boolean
- 输入：探测结果
- 输出：是否成功 commit 到 prefs
- 副作用：写 SharedPreferences；成功则更新 StateFlow
- 步骤：
  1. JSON 编码 result
  2. synchronized 内 putString + commit
  3. commit 成功则 `mutableResult.value = result`
- 分支与异常：commit 失败返回 false，不更新内存
- 调用：`json.encodeToString`

#### clear(): Boolean
- 输入：无
- 输出：是否成功清除
- 副作用：移除 prefs 键；成功则 StateFlow=null
- 步骤：synchronized remove KEY_RESULT + commit
- 分支与异常：commit 失败不改内存
- 调用：无

#### isFresh(serverIdentity, nowMillis): Boolean
- 输入：服务器身份、当前时间
- 输出：是否存在新鲜结果
- 副作用：无
- 步骤：`freshResult(...) != null`
- 分支与异常：无
- 调用：`freshResult`

#### freshResult(serverIdentity, nowMillis): ConnectionProbeResult?
- 输入：serverIdentity、nowMillis
- 输出：新鲜结果或 null
- 副作用：无
- 步骤：
  1. 当前 result 为空 → null
  2. serverIdentity 不匹配 → null
  3. age = now - checkedAt；age 在 [0, FRESHNESS_MILLIS) 才返回
- 分支与异常：时钟回拨 age&lt;0 视为不新鲜
- 调用：无

#### load(): ConnectionProbeResult?
- 输入：无（读 prefs）
- 输出：反序列化结果或 null
- 副作用：无
- 步骤：
  1. getString KEY_RESULT
  2. runCatching decode；失败 null
- 分支与异常：解码失败吞掉
- 调用：`json.decodeFromString`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.status`
2. [L11-L15] 接口 `ConnectionProbeEvidenceStore`：result / save / freshResult
3. [L17-L20] 类 `ConnectionProbeStore(preferences, json)` 实现接口
4. [L21-L24] 锁 + 从 load 初始化 MutableStateFlow，对外 asStateFlow
5. [L26-L35] save：编码 → 同步 commit → 成功更新流
6. [L37-L45] clear：同步 remove → 成功置 null
7. [L47-L49] isFresh：委托 freshResult
8. [L51-L60] freshResult：身份匹配且年龄 &lt; 5 分钟
9. [L62-L67] load：prefs 字符串解码，失败 null
10. [L69-L72] KEY_RESULT、`FRESHNESS_MILLIS = 5 分钟`

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt",
      "label": "ConnectionProbeStore",
      "path": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeStore.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/status/ConnectionProbeModels.kt",
      "type": "depends_on"
    }
  ]
}
```
