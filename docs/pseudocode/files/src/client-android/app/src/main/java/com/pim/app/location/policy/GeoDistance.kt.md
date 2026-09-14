# src/client-android/app/src/main/java/com/pim/app/location/policy/GeoDistance.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：`GeoDistance`：见源文件职责（GeoDistance.kt）。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### GeoDistance
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L8 声明 `GeoDistance`
- 分支与异常：无
- 调用：无

### metersBetween
#### metersBetween(a: PolicyLocation, b: PolicyLocation)
- 输入：a: PolicyLocation, b: PolicyLocation
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `metersBetween` 参数：a: PolicyLocation, b: PolicyLocation
  2. 执行：val deltaLatitude = Math.toRadians(b.latitude - a.latitude)
  3. 执行：val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
  4. 执行：val startLatitude = Math.toRadians(a.latitude)
  5. 执行：val endLatitude = Math.toRadians(b.latitude)
  6. 执行：val haversine = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
  7. 执行：cos(startLatitude) * cos(endLatitude) *
  8. 执行：sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
  9. 执行：val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
  10. 返回 EARTH_RADIUS_METERS * centralAngle
- 分支与异常：无显著分支
- 调用：metersBetween、Math.toRadians、sin、cos、atan2、sqrt

## 近逐行中文伪代码

1. [L8] 单例 object `GeoDistance`
2. [L9] 执行：private const val EARTH_RADIUS_METERS = 6_371_000.0
3. [L11] 函数 `metersBetween` 参数：a: PolicyLocation, b: PolicyLocation
4. [L12] 执行：val deltaLatitude = Math.toRadians(b.latitude - a.latitude)
5. [L13] 执行：val deltaLongitude = Math.toRadians(b.longitude - a.longitude)
6. [L14] 执行：val startLatitude = Math.toRadians(a.latitude)
7. [L15] 执行：val endLatitude = Math.toRadians(b.latitude)
8. [L17] 执行：val haversine = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
9. [L18] 执行：cos(startLatitude) * cos(endLatitude) *
10. [L19] 执行：sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
11. [L20] 执行：val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
12. [L21] 返回 EARTH_RADIUS_METERS * centralAngle

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/policy/GeoDistance.kt",
      "label": "GeoDistance",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/policy/GeoDistance.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/policy/GeoDistance.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
