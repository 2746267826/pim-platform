# src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android (core test)
- 职责：契约测试——源码静态断言 `ApiService` 含 mobile 查询路径与 query 参数；`MobileModels` 含 V2 查询 DTO 字段。
- 主要依赖：`ApiService.kt`、`MobileModels.kt` 源文件、`java.io.File`
- 被谁使用：单元测试运行器

## 函数级结构化伪代码

### MobileQueryApiContractTest
#### apiServiceContainsMobileQueryEndpoints()
- 步骤：读 ApiService.kt；断言 GET mobile/summary|timeline|quality|location/* 与 date/range/timezone Query

#### mobileModelsContainQueryDtosUsedByAndroidV2()
- 步骤：读 MobileModels.kt；断言一组 data class 名与 range/local 日期字段存在

#### repoFile(vararg parts): File
- 步骤：自当前目录向上定位文件，否则 error

## 近逐行中文伪代码

1. [L7-8] 测试类
2. [L9-23] 断言 ApiService 注解路径与 Query 签名字符串
3. [L25-47] 断言 MobileModels 中 analytics/history/timeline 等 DTO
4. [L49-57] repoFile 向上查找

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt",
      "label": "MobileQueryApiContractTest",
      "path": "src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/network/ApiService.kt", "type": "tests" },
    { "from": "src/client-android/core/src/test/java/com/pim/core/network/MobileQueryApiContractTest.kt", "to": "src/client-android/core/src/main/java/com/pim/core/models/MobileModels.kt", "type": "tests" }
  ]
}
```
