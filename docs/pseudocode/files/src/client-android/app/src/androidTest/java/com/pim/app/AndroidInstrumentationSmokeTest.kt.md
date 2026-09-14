# src/client-android/app/src/androidTest/java/com/pim/app/AndroidInstrumentationSmokeTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：测试 `AndroidInstrumentationSmokeTest`：仪器/单元冒烟验证。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### AndroidInstrumentationSmokeTest
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L10 声明 `AndroidInstrumentationSmokeTest`
- 分支与异常：无
- 调用：无

### applicationIdMatchesProductionPackage
#### applicationIdMatchesProductionPackage(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `applicationIdMatchesProductionPackage` 参数：无
  2. 执行：val context = InstrumentationRegistry.getInstrumentation().targetContext
  3. 执行：assertEquals("com.pim.app", context.packageName)
- 分支与异常：无显著分支
- 调用：applicationIdMatchesProductionPackage、InstrumentationRegistry.getInstrumentation、assertEquals

## 近逐行中文伪代码

1. [L9] 注解 @RunWith
2. [L10] 定义类 `AndroidInstrumentationSmokeTest`
3. [L11] 注解 @Test
4. [L12] 函数 `applicationIdMatchesProductionPackage` 参数：无
5. [L13] 执行：val context = InstrumentationRegistry.getInstrumentation().targetContext
6. [L14] 执行：assertEquals("com.pim.app", context.packageName)

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/androidTest/java/com/pim/app/AndroidInstrumentationSmokeTest.kt",
      "label": "AndroidInstrumentationSmokeTest",
      "path": "src/client-android/app/src/androidTest/java/com/pim/app/AndroidInstrumentationSmokeTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/androidTest/java/com/pim/app/AndroidInstrumentationSmokeTest.kt.md",
      "layer": "client-android",
      "kind": "test"
    }
  ],
  "edges": []
}
```
