# src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncWindowSplitterTest.kt

## 元信息
- 语言：Kotlin / JUnit
- 程序集或包：client-android tests
- 职责：验证 `splitGapWindowForUpload` 将缺口窗口切成最多 2 小时上传窗口。
- 主要依赖：splitGapWindowForUpload、UploadWindow
- 被谁使用：测试运行器

## 函数级结构化伪代码

### MobileSyncWindowSplitterTest
#### splitGapWindowUsesTwoHourUploadWindows
- 5.5h 缺口 → 2h + 2h + 1.5h 三个 UploadWindow

#### splitGapWindowReturnsSingleWindowWhenAlreadySmall
- 45min 已小于 2h → 单窗口原样

#### splitGapWindowReturnsEmptyListForInvalidRange
- start==end 或 start>end → emptyList

## 近逐行中文伪代码

1. 长缺口按 2 小时切片，尾段保留剩余。
2. 短缺口不切分。
3. 非法区间返回空列表。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncWindowSplitterTest.kt",
      "label": "MobileSyncWindowSplitterTest",
      "path": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncWindowSplitterTest.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncWindowSplitterTest.kt.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    {
      "from": "src/client-android/app/src/test/java/com/pim/app/mobile/sync/MobileSyncWindowSplitterTest.kt",
      "to": "src/client-android/app/src/main/java/com/pim/app/mobile/sync",
      "type": "tests"
    }
  ]
}
```
