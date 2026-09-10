# src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：Room DAO：未同步应用使用记录查询/插入/标记/清理。
- 主要依赖：AppUsageEntity、Room
- 被谁使用：AppDatabase、同步上传

## 函数级结构化伪代码

### getUnsynced(limit)
- synced=0 按 start_time 升序限量

### unsyncedCount
- Flow 计数未同步

### insertAll / markSynced / deleteSyncedOlderThan
- REPLACE 插入；按 id 标已同步；删旧已同步

## 近逐行中文伪代码

1. 接口 AppUsageDao。
2. 五个 Room 查询/写操作覆盖上传生命周期。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt",
      "label": "AppUsageDao",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageEntity.kt", "type": "depends_on" }
  ]
}
```
