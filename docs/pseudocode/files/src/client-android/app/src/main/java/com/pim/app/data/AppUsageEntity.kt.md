# src/client-android/app/src/main/java/com/pim/app/data/AppUsageEntity.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：Room 实体 app_usage：包名、起止、时长、lastTimeUsed、synced。
- 主要依赖：Room
- 被谁使用：AppUsageDao/AppDatabase

## 函数级结构化伪代码

### AppUsageEntity
- 主键自增 id；列映射；synced 默认 false

## 近逐行中文伪代码

1. 表 app_usage。
2. 字段覆盖一次应用使用会话。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageEntity.kt",
      "label": "AppUsageEntity",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageEntity.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/AppUsageEntity.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
`
