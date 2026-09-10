# src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (app test)
- 职责：单测日程窗口选择与映射——当前/即将到来窗口需有地点；API `EventResponse` 映射过滤无地点事件。
- 主要依赖：`ScheduleWindowSelector`、`ScheduleWindowRepository`、`ScheduleWindow`、`EventResponse`
- 被谁使用：单元测试运行器

## 函数级结构化伪代码

### ScheduleWindowRepositoryTest
#### currentWindowRequiresTimeRangeAndLocationText()
- 输入：无
- 输出：无（断言）
- 副作用：无
- 步骤：
  1. 构造无地点与有地点两窗口（时间覆盖 now=10000）
  2. 断言 current 选中有地点 id=`2`
  3. now=12000 时 current 为 null
- 分支与异常：无
- 调用：`ScheduleWindowSelector.current`

#### upcomingReturnsOnlyFutureWindowsWithLocation()
- 步骤：past/blank/future 列表 → upcoming 仅 `future`
- 调用：`ScheduleWindowSelector.upcoming`

#### mapsApiEventsWithLocationsToScheduleWindows()
- 步骤：`mapEvents` 两个 EventResponse → 仅保留有 location 的一条
- 调用：`ScheduleWindowRepository.mapEvents`

## 近逐行中文伪代码

1. [L9] 测试类
2. [L10-20] current：必须时间命中且 locationText 非空
3. [L22-31] upcoming：仅未来且有地点
4. [L33-57] mapEvents：过滤空 location，保留 id/locationText

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt",
      "label": "ScheduleWindowRepositoryTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/schedule/ScheduleWindowRepository.kt", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/ScheduleWindow.kt", "type": "depends_on" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/schedule/ScheduleWindowRepositoryTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models/AuthModels.kt", "type": "depends_on" }
  ]
}
```
