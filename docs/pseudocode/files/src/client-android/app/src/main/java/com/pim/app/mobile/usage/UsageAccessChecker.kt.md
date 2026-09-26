# src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.mobile.usage
- 职责：探测本机是否已授予「使用情况访问」权限（Usage Access），供同步/状态中心判断能否采集应用使用数据。
- 主要依赖：`UsageStatsManager`、`Context`（Hilt `@ApplicationContext`）
- 被谁使用：移动同步协调、权限状态相关 UI/仓储

## 函数级结构化伪代码

### UsageAccessChecker
#### hasUsageAccess(nowUtc, lookbackMs) -> Boolean
- 输入：可选当前 UTC 毫秒、回看窗口（默认 24h）
- 输出：是否能查到至少一条 usage stats
- 副作用：无（只读系统服务）
- 步骤：
  1. 取 `USAGE_STATS_SERVICE`，失败则 false
  2. 计算 beginUtc = max(0, nowUtc - lookbackMs)
  3. `queryUsageStats(INTERVAL_DAILY, beginUtc, nowUtc)`
  4. 列表非空则 true
- 分支与异常：`SecurityException` -> false；服务缺失 -> false
- 调用：`Context.getSystemService`、`UsageStatsManager.queryUsageStats`

### companion
- `DEFAULT_LOOKBACK_MS = 24h`

## 近逐行中文伪代码

1. `@Singleton` + `@Inject` 构造，注入应用级 `Context`。
2. `hasUsageAccess`：默认 `nowUtc=System.currentTimeMillis()`，`lookbackMs=24h`。
3. 强转 `UsageStatsManager`，为空直接 false。
4. begin 时间下限钳到 0。
5. try：按日间隔查询回看窗内 stats；`orEmpty().isNotEmpty()` 即视为已授权。
6. catch `SecurityException`：返回 false（系统拒绝查询）。
7. 常量默认回看一天。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt",
      "label": "UsageAccessChecker",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/usage/UsageAccessChecker.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
