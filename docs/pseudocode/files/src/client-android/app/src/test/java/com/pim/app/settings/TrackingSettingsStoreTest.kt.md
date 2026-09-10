# src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsStoreTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.settings（client-android app 测试）
- 职责：验证 `TrackingSettings` 默认值与 `TrackingSettingsStore` 持久化/局部更新；内嵌内存 SharedPreferences。
- 主要依赖：`TrackingSettings`、`TrackingSettingsStore`、Robolectric、JUnit
- 被谁使用：测试运行器

## 函数级结构化伪代码

### TrackingSettingsStoreTest
#### defaultProfileIsPowerSavingAndConfigurableValuesMatchSpec
- 输入：`TrackingSettings.defaults()`
- 输出：断言 profile 与间隔/阈值/精度等
- 副作用：无
- 步骤：profile=power-saving；continuous=false；各 interval 与阈值阈值符合规格
- 分支与异常：无
- 调用：`defaults`

#### storePersistsCollectionAndPolicyValues
- 输入：write 覆盖字段
- 输出：read 回读一致
- 副作用：内存 prefs
- 步骤：构造 store → write → read 全字段断言
- 分支与异常：无
- 调用：`write`/`read`

#### setContinuousCollectionPreservesPolicyValues
- 输入：先写 interval 再 setContinuous true
- 输出：开关 true 且 interval 保留
- 副作用：prefs
- 步骤：局部更新不覆盖策略字段
- 分支与异常：无
- 调用：`setContinuousCollectionEnabled`

#### syncOnUnmeteredOnlyDefaultIsFalse / PersistsTrue
- 输入：默认与 write true
- 输出：布尔断言
- 副作用：prefs
- 步骤：默认 false；可持久化为 true
- 分支与异常：无
- 调用：`read`/`write`

### InMemorySharedPreferences
#### get*/edit/apply 语义
- 输入：key/value
- 输出：读写
- 副作用：可变 Map
- 步骤：Editor 暂存 edits/removals/clear；apply 提交；commit 调 apply
- 分支与异常：类型强转默认 defValue
- 调用：无

## 近逐行中文伪代码

1. [L1-12] 测试类与 Robolectric SDK 34
2. [L14-26] 默认规格断言
3. [L28-52] 全字段持久化
4. [L54-63] 连续采集开关保留策略
5. [L65-78] unmetered-only 默认与持久化
6. [L81-121] 内存 SharedPreferences 与 Editor 实现

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsStoreTest.kt",
      "label": "TrackingSettingsStoreTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsStoreTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsStoreTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/settings/TrackingSettingsStoreTest.kt", "to": "src/client-android/app/src/main/java/com/pim/app/settings/TrackingSettingsStore.kt", "type": "tests" }
  ]
}
```
