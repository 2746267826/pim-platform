# src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：移动数据 Room DAO：使用/元数据/定位/同步批次/日志/设备配置的插入与按 sync_status 查询更新。
- 主要依赖：Mobile*Entity
- 被谁使用：同步与定位仓储

## 函数级结构化伪代码

### insert*/upsert*
- 各类实体 REPLACE/Upsert

### get*BySyncStatus
- 按 PENDING 等状态限量拉取队列

### mark*Synced / count / delete 等
- 标记同步、计数 Flow、清理历史（文件后半部分）

## 近逐行中文伪代码

1. 统一移动侧本地持久化入口。
2. 上传管线按 sync_status 取批。
3. 定位点/丢弃诊断/策略切换写入。
4. 日志与设备 profile 维护。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "label": "MobileDataDao",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataDao.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/data/MobileEntities.kt",
      "type": "depends_on"
    }
  ]
}
`
