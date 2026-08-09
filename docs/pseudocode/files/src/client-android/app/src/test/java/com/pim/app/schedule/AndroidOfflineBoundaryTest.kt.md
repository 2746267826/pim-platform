# src/client-android/app/src/test/java/com/pim/app/schedule/AndroidOfflineBoundaryTest.kt

## 元信息
- 语言：Kotlin (JUnit)
- 程序集或包：client-android test / com.pim.app.schedule
- 职责：契约测试——仅采集类上传可离线入队，任务/确认/Outlook/恢复删除等写操作禁止离线排队。
- 主要依赖：`OnlineOperationGuard`
- 被谁使用：测试运行器

## 函数级结构化伪代码

### AndroidOfflineBoundaryTest
#### onlyCollectionUploadsCanQueueOffline()
- 输入：无
- 输出：断言通过
- 副作用：无
- 步骤：
  1. 新建 `OnlineOperationGuard`
  2. 允许：`collection-upload`、`android-location`、`device-state`
  3. 禁止：`task-fact-change`、`confirmation-decision`、`outlook-writeback`、`restore-delete-operation`
- 分支与异常：无
- 调用：`canQueueOffline`

## 近逐行中文伪代码

1. [L1] 包 `com.pim.app.schedule`
2. [L3] 导入 OnlineOperationGuard
3. [L8] 测试类 AndroidOfflineBoundaryTest
4. [L9-L11] 测试 onlyCollectionUploadsCanQueueOffline；new guard
5. [L13-L15] assertTrue 三类采集上传
6. [L16-L19] assertFalse 四类业务写操作
7. [L20-L21] 结束

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidOfflineBoundaryTest.kt",
      "label": "AndroidOfflineBoundaryTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidOfflineBoundaryTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/schedule/AndroidOfflineBoundaryTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/schedule/AndroidOfflineBoundaryTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/offline/OnlineOperationGuard.kt",
      "type": "tests"
    }
  ]
}
```
