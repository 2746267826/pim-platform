# src/client-android/core/src/main/java/com/pim/core/models/StatsModels.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android core / com.pim.core.models
- 职责：应用使用统计上传 DTO：单条 AppUsageEntry 与批次 UploadBatch（kotlinx.serialization）。
- 主要依赖：kotlinx.serialization Serializable/SerialName
- 被谁使用：旧/兼容 stats 上传或序列化测试路径

## 函数级结构化伪代码

### AppUsageEntry
- 字段：package_name、start_time、end_time、duration_ms、last_time_used（JSON 蛇形命名）

### UploadBatch
- 字段：device_id、entries: List<AppUsageEntry>

## 近逐行中文伪代码

1. @Serializable data class AppUsageEntry 五个时间/包名字段。
2. @Serializable data class UploadBatch 含 deviceId 与 entries 列表。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/main/java/com/pim/core/models/StatsModels.kt",
      "label": "StatsModels",
      "path": "src/client-android/core/src/main/java/com/pim/core/models/StatsModels.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/main/java/com/pim/core/models/StatsModels.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
