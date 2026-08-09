# src/client-android/app/src/main/java/com/pim/app/data/MobileDataModule.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：DI/模块 `MobileDataModule`：提供依赖绑定。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### MobileDataModule
#### 类型/结构声明
- 输入：无
- 输出：类型符号
- 副作用：无
- 步骤：1. 在 L11 声明 `MobileDataModule`
- 分支与异常：无
- 调用：无

### provideMobileDataDao
#### provideMobileDataDao(database: AppDatabase)
- 输入：database: AppDatabase
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 函数 `provideMobileDataDao` 参数：database: AppDatabase
  2. 返回 database.mobileDataDao()
- 分支与异常：无显著分支
- 调用：provideMobileDataDao、database.mobileDataDao

## 近逐行中文伪代码

1. [L9] 注解 @Module
2. [L10] 注解 @InstallIn
3. [L11] 单例 object `MobileDataModule`
4. [L12] 注解 @Provides
5. [L13] 注解 @Singleton
6. [L14] 函数 `provideMobileDataDao` 参数：database: AppDatabase
7. [L15] 返回 database.mobileDataDao()

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataModule.kt",
      "label": "MobileDataModule",
      "path": "src/client-android/app/src/main/java/com/pim/app/data/MobileDataModule.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/data/MobileDataModule.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
```
