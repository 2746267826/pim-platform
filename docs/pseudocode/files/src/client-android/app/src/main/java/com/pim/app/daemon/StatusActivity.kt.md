# src/client-android/app/src/main/java/com/pim/app/daemon/StatusActivity.kt

## 元信息
- 语言：Kotlin
- 程序集或包：client-android
- 职责：简单状态 Activity 展示「数据采集运行中」占位文案。
- 主要依赖：AppCompatActivity、TextView
- 被谁使用：AndroidManifest / 通知入口（占位）

## 函数级结构化伪代码

### StatusActivity.onCreate
- 创建 TextView，固定中文状态模板，padding 48，setContentView

## 近逐行中文伪代码

1. 继承 AppCompatActivity。
2. onCreate 构建纯文本状态页，无真实队列数据绑定。

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-android/app/src/main/java/com/pim/app/daemon/StatusActivity.kt",
      "label": "StatusActivity",
      "path": "src/client-android/app/src/main/java/com/pim/app/daemon/StatusActivity.kt",
      "doc": "docs/pseudocode/files/src/client-android/app/src/main/java/com/pim/app/daemon/StatusActivity.kt.md",
      "layer": "client-android",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
