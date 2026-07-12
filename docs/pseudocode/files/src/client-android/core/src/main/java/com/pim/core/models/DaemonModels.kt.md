# src/client-android/core/src/main/java/com/pim/core/models/DaemonModels.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.core.models（client-android core）
- 职责：守护进程心跳请求/响应 DTO 与数据源状态常量。
- 主要依赖：kotlinx.serialization
- 被谁使用：心跳上报与 API 反序列化

## 函数级结构化伪代码

### DaemonHeartbeatRequest
#### 可序列化数据类
- 输入：设备与守护进程字段
- 输出：请求体
- 副作用：无
- 步骤：字段含 deviceId/daemonKind/version/serverUrl、上传时间与错误、队列数、AW/KeyStats 状态、collectionPaused、statusJson
- 分支与异常：可选字段默认 null/UNKNOWN/false/`{}`
- 调用：无

### DaemonHeartbeatDto
#### 可序列化数据类
- 输入：服务端回显字段
- 输出：DTO
- 副作用：无
- 步骤：与请求类似并增加必填 `receivedAt`
- 分支与异常：无
- 调用：无

### DaemonSourceStates
#### 常量
- 输入：无
- 输出：字符串常量
- 副作用：无
- 步骤：UNKNOWN / AVAILABLE / UNAVAILABLE
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L1-4] 包与 Serializable 导入
2. [L5-19] `DaemonHeartbeatRequest` 字段与默认
3. [L21-36] `DaemonHeartbeatDto` 含 receivedAt
4. [L38-42] `DaemonSourceStates` 三常量

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/models/DaemonModels.kt",
      "label": "DaemonModels",
      "path": "src/client-android/core/src/main/java/com/pim/core/models/DaemonModels.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/models/DaemonModels.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
