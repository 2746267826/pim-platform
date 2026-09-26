# src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：Room 数据库声明：移动使用/定位/同步/日志等实体，version 3。
- 主要依赖：Room、各 Entity、AppUsageDao、MobileDataDao
- 被谁使用：DI 模块提供数据库

## 函数级结构化伪代码

### AppDatabase
- @Database entities 列表 10 个，exportSchema=true
- 抽象 appUsageDao()、mobileDataDao()

## 近逐行中文伪代码

1. 注册实体与 version=3。
2. 暴露两个 DAO 抽象方法。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "label": "AppDatabase",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageDao.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/main/java/com/pim/app/data/AppDatabase.kt", "to": "src/client-android/app/src/main/java/com/pim/app/data/AppUsageEntity.kt", "type": "depends_on" }
  ]
}
```
