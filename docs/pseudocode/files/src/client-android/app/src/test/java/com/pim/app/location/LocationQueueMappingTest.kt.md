# src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：com.pim.app.location（client-android app test）
- 职责：验证定位接受/丢弃/策略切换到 Room 实体的映射，以及 DB 迁移 2→3 已注册。
- 主要依赖：`MobileLocation*Entity`、`QualityAcceptedLocation`、`RawLocationFix`、`PolicyDecision`、`PimDatabaseMigrations`
- 被谁使用：JUnit 测试运行器

## 函数级结构化伪代码

### LocationQueueMappingTest
#### acceptedLocationStoresPolicyAndNullAltitudeFlag()
- 输入：构造带 null 高度与 qualityFlags 的 `QualityAcceptedLocation`
- 输出：断言通过
- 副作用：无
- 步骤：
  1. 构造 RawLocationFix（上海坐标、18m 精度、ScheduleLowFrequency、Still）
  2. `MobileLocationPointEntity.fromAccepted`
  3. 断言 policyMode、scheduleLowFrequency、altitude null、qualityFlags、accuracy、submittedAt、motionState
- 分支与异常：断言失败则测试失败
- 调用：`fromAccepted`

#### droppedDiagnosticStoresReasonAndPolicyMetadata()
- 输入：精度过低的 RawLocationFix
- 输出：断言通过
- 副作用：无
- 步骤：
  1. `fromDropped(reason=horizontal-accuracy-too-low)`
  2. 断言 recordedAt、provider、accuracy、policyMode、reason、createdAt
- 分支与异常：无
- 调用：`MobileLocationDroppedDiagnosticEntity.fromDropped`

#### policyTransitionStoresModeNamesAndReason()
- 输入：`PolicyDecision` MovementRecovery
- 输出：断言通过
- 副作用：无
- 步骤：
  1. `fromDecision(fromMode=ScheduleLowFrequency, decision, occurredAt)`
  2. 断言 from/to mode 名、reason、occurredAt
- 分支与异常：无
- 调用：`MobileLocationPolicyTransitionEntity.fromDecision`

#### databaseMigrationTwoToThreeIsRegistered()
- 输入：无
- 输出：断言 ALL 中存在 startVersion=2 endVersion=3 的迁移
- 副作用：无
- 步骤：
  1. `PimDatabaseMigrations.ALL.any { ... }`
- 分支与异常：无
- 调用：迁移注册表查询

## 近逐行中文伪代码

1. [L1-14] 包与导入实体/策略/质量模型/JUnit
2. [L16-45] 测试 accepted 映射：fromAccepted 后字段断言
3. [L47-73] 测试 dropped 诊断实体映射
4. [L75-95] 测试策略切换实体映射
5. [L97-100] 测试迁移 2→3 已注册

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt",
      "label": "LocationQueueMappingTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt", "to": "com.pim.app.data.MobileLocationPointEntity", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt", "to": "com.pim.app.data.MobileLocationDroppedDiagnosticEntity", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt", "to": "com.pim.app.data.MobileLocationPolicyTransitionEntity", "type": "tests" },
    { "from": "src/client-android/app/src/test/java/com/pim/app/location/LocationQueueMappingTest.kt", "to": "com.pim.app.data.PimDatabaseMigrations", "type": "tests" }
  ]
}
```
