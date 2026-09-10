# src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：定位队列仓储：接受点入队、丢弃诊断、策略切换记录。
- 主要依赖：MobileDataDao、质量/策略类型实体工厂
- 被谁使用：LocationCaptureRepository 等

## 函数级结构化伪代码

### enqueueAccepted
- Entity.fromAccepted → insertLocationPoint

### recordDropped
- fromDropped → insertDroppedLocationDiagnostic

### recordPolicyTransition
- fromDecision → insertPolicyTransition

## 近逐行中文伪代码

1. 注入 MobileDataDao。
2. 三个 suspend 写库方法返回 row id。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt",
      "label": "LocationQueueRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/location/LocationQueueRepository.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/quality/QualityAcceptedLocation.kt", "type": "depends_on" }
  ]
}
```
