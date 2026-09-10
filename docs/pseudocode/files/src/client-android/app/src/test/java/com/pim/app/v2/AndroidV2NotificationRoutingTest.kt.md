# src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.v2（client-android app test）
- 职责：基于源码文本断言的架构守卫：通知打开状态页、遗留 endpoint 走 Shell 且不启动 collector、前台服务在关闭采集时 stopSelf 并重注册策略间隔。
- 主要依赖：`java.io.File` 向上查找仓库源文件、JUnit
- 被谁使用：JUnit 测试运行器

## 函数级结构化伪代码

### AndroidV2NotificationRoutingTest
#### locationStatusIntentTargetsStatusDestination()
- 输入：读取 ForegroundLocationController、MainActivity、PimRootScreen 源码
- 输出：断言包含 EXTRA_OPEN_DESTINATION / initialDestination / PimDestination.Status
- 副作用：读文件系统
- 步骤：
  1. `repoFile` 定位三个源文件并 `readText`
  2. assertTrue 关键字存在
- 分支与异常：找不到文件则 `error`
- 调用：`repoFile`

#### legacyEndpointNotificationDetailsRemainLegacyShellWithoutStartingCollector()
- 输入：NotificationActionReceiver、PimShellActivity 源码
- 输出：断言 intentFor 到 detailUrl 与 /endpoint-shell；Shell 不含 collector.start()
- 副作用：读文件
- 步骤：字符串 contains / !contains
- 分支与异常：文件缺失 error
- 调用：`repoFile`

#### foregroundServiceStopsWhenCollectionDisabledAndReregistersPolicyIntervals()
- 输入：ForegroundLocationService 源码
- 输出：断言 continuousCollectionEnabled 判断、stopSelf、requestLocationUpdates、motion 信号、ScheduleWindowSelector
- 副作用：读文件
- 步骤：多条 contains 断言
- 分支与异常：无
- 调用：`repoFile`

#### repoFile(vararg parts): File
- 输入：相对路径分段
- 输出：存在的 File
- 副作用：从 cwd 向上遍历父目录
- 步骤：
  1. current = canonicalFile 的 ""
  2. 循环：parts fold resolve；exists 则返回
  3. parent 为空仍未找到 → error
- 分支与异常：找不到抛 error
- 调用：`File.resolve`、`exists`

## 近逐行中文伪代码

1. [L1-6] 包、File、JUnit
2. [L7-18] 测试定位状态 Intent 目标 Status
3. [L20-28] 测试遗留 endpoint 通知走 Shell 且不 start collector
4. [L30-39] 测试前台服务停服与策略间隔重注册关键字
5. [L41-49] `repoFile` 向上查找仓库文件

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt",
      "label": "AndroidV2NotificationRoutingTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationController.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/ui/shell/PimShellActivity.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/v2/AndroidV2NotificationRoutingTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationService.kt", "type": "tests" }
  ]
}
```
