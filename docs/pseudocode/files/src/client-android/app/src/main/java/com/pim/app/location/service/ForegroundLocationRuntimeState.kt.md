# src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：前台定位服务运行时状态快照 data class。
- 主要依赖：无
- 被谁使用：ForegroundLocationService / UI 观察

## 函数级结构化伪代码

### ForegroundLocationRuntimeState
- 字段默认：isRunning=false、policy Off、下次定位 null、文案「无」、pending=0、api「等待采集」、lastDropped null

## 近逐行中文伪代码

1. 纯数据类承载运行标志、策略、计数与展示文案。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt",
      "label": "ForegroundLocationRuntimeState",
      "path": "src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/location/service/ForegroundLocationRuntimeState.kt.md",
      "layer": "client-android",
      "kind": "dto"
    }
  ],
  "edges": []
}
```
