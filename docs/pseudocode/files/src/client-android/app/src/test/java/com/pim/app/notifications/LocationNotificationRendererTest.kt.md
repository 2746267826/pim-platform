# src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android test / com.pim.app.notifications
- 职责：校验定位前台通知折叠/展开文案包含策略、下次采样、精度、队列、API 与丢弃原因。
- 主要依赖：`LocationNotificationRenderer`、`LocationNotificationState`、`LocationPolicyMode`
- 被谁使用：测试运行器

## 函数级结构化伪代码

### collapsedTextShowsStrategyNextAccuracyQueueAndApi
- 构造 ScheduleLowFrequency 状态；断言折叠文案含「日程低频」、下次时间、精度、待上传数、API 状态

### expandedTextShowsDroppedReason
- MovementRecovery + 丢弃原因；断言展开文案含「移动恢复」、API 错误、最近丢弃说明

## 近逐行中文伪代码

1. 折叠文案应浓缩策略/下次/精度/队列/API。
2. 展开文案应附加最近丢弃原因。
3. 使用中文 UI 文案断言，无业务副作用。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt",
      "label": "LocationNotificationRendererTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/notifications/LocationNotificationRendererTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/notifications/LocationNotificationRenderer.kt",
      "type": "tests"
    }
  ]
}
```
