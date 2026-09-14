# src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.core.models（client-android core）
- 职责：端点通知动作请求/响应 DTO，供 kotlinx.serialization 序列化。
- 主要依赖：`kotlinx.serialization.Serializable`
- 被谁使用：Android 端点通知动作 API 调用层

## 函数级结构化伪代码

### EndpointNotificationActionRequestDto
#### 数据类字段
- 输入：action、riskLevel、可选 confirmationId/relatedObjectType/relatedObjectId
- 输出：可序列化请求体
- 副作用：无
- 步骤：
  1. 声明必填 action、riskLevel
  2. 可选关联确认与对象标识
- 分支与异常：无
- 调用：无

### EndpointNotificationActionResponseDto
#### 数据类字段
- 输入：result、可选 detailUrl、message
- 输出：可序列化响应体
- 副作用：无
- 步骤：
  1. 声明 result
  2. 可选详情 URL 与消息
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. [L1] 包 `com.pim.core.models`
2. [L3] 导入 `@Serializable`
3. [L5-12] `EndpointNotificationActionRequestDto` 请求字段
4. [L14-19] `EndpointNotificationActionResponseDto` 响应字段

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt",
      "label": "EndpointModels",
      "path": "src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/main/java/com/pim/core/models/EndpointModels.kt", "to": "kotlinx.serialization.Serializable", "type": "depends_on" }
  ]
}
```
