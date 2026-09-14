# src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：结构化 JSONL 日志：debug/info/warn/error 写文件，recent 倒序读取。
- 主要依赖：Context filesDir、Timber、Mutex
- 被谁使用：移动同步/诊断

## 函数级结构化伪代码

### StructuredLogEntry
- level/tag/message/throwable/occurredAtUtc

### StructuredLogRepository
- 按日 mobile-yyyy-MM-dd.jsonl 追加
- mutex 串行写；recent 扫最新文件倒序解析 limit 条

## 近逐行中文伪代码

1. 级别 API 统一 write。
2. IO 线程写 JSON 行。
3. recent 从新到旧收集条目。
4. 损坏行跳过。

## 关系边
`json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt",
      "label": "StructuredLogRepository",
      "path": "src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/mobile/logs/StructuredLogRepository.kt.md",
      "layer": "client-android",
      "kind": "service"
    }
  ],
  "edges": []
}
`
