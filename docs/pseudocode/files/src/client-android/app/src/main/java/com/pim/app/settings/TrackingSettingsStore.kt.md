# src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android / com.pim.app.settings
- 职责：定位/采集策略本地设置的读写（SharedPreferences），并映射为 `TrackingPolicy`。
- 主要依赖：`SharedPreferences`、`TrackingPolicy`
- 被谁使用：定位策略引擎、设置页、前台定位服务

## 函数级结构化伪代码

### TrackingSettings（data class）
- 字段：profile、continuousCollectionEnabled、各类 interval、recovery 阈值、海拔等待超时、上传精度上限、仅非计量网络同步
- `defaults()`：省电配置默认值（3min 正常 / 15min 日程低频 / 1min 移动 / 100m 恢复 / 15s 海拔等待 / 50m 精度 / 不同步仅 unmetered）

### TrackingSettingsStore
#### read() -> TrackingSettings
- 输入：无
- 输出：偏好项叠加默认值的完整设置
- 副作用：读 SharedPreferences
- 步骤：`defaults()` 为底，逐 key `getString/getBoolean/getLong/getFloat` 覆盖

#### write(settings) -> TrackingSettings
- 输入：完整 `TrackingSettings`
- 输出：写后 `read()` 回读结果
- 副作用：`edit().put*().apply()` 异步落盘
- 步骤：写入全部 key 后 `read()` 返回

#### setContinuousCollectionEnabled(enabled) -> TrackingSettings
- 读-改 continuous 标志-写

### toTrackingPolicy()
- 扩展：将 settings 中策略相关字段投影到 `TrackingPolicy`（不含 profile/continuous/sync 开关）

## 近逐行中文伪代码

1. `TrackingSettings` 承载 profile 与间隔/阈值/精度等参数。
2. `defaults()`：profile=`power-saving`，连续采集关，各毫秒/米阈值见常量。
3. `TrackingSettingsStore` 包装 `SharedPreferences`。
4. `read`：对每个 key 用默认值回填，float 存 double 阈值。
5. `write`：批量 put 后 apply，再 read 保证一致。
6. `setContinuousCollectionEnabled`：局部更新 continuous 标志。
7. companion 定义 `tracking.*` 键名。
8. `toTrackingPolicy`：拷贝 interval/阈值/超时/精度到策略对象。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt",
      "label": "TrackingSettingsStore",
      "path": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/location/policy/LocationPolicyTypes.kt",
      "type": "depends_on"
    }
  ]
}
```
