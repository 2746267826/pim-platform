# src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android test / com.pim.app.location.quality
- 职责：单元测试 `AltitudeWaitCoordinator`：缺海拔等待超时、精度丢弃、迟到海拔取消超时。
- 主要依赖：`AltitudeWaitCoordinator`、`LocationQualityGate`、`RawLocationFix`
- 被谁使用：测试运行器

## 函数级结构化伪代码

### missingAltitudeDelaysUntilDeadlineThenAcceptsNullAltitude
- 模拟 now/delay；无海拔 fix 触发 15s delay；最终接受 null 海拔并带 `altitude-missing-timeout` 标志

### droppedFixDoesNotDelayOrAccept
- 精度 50m 不满足 `<50` 规则；不 delay、不接受；dropped 原因为 `horizontal-accuracy-too-low`

### laterAltitudeFixBeforeDeadlineCancelsNullAltitudeTimeout
- delay 半程注入带海拔 fix；仅接受一条带 12.5m 海拔、无 qualityFlags

### fix(...) 辅助
- 构造上海坐标 `RawLocationFix` 测试夹具

## 近逐行中文伪代码

1. 测试类构造可控 `nowMillis`/`delayMillis` 的 coordinator。
2. 用例一：缺海拔等到 deadline 后接受，标志含超时。
3. 用例二：精度过差立即丢弃，不进入等待。
4. 用例三：等待中补到海拔则取消超时路径。
5. `fix` 工厂固定 lat/lon/provider/policyMode。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt",
      "label": "AltitudeWaitCoordinatorTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/quality/AltitudeWaitCoordinator.kt",
      "type": "tests"
    },
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/location/quality/AltitudeWaitCoordinatorTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/quality/LocationQualityGate.kt",
      "type": "depends_on"
    }
  ]
}
```
