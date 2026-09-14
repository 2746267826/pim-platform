# src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.Client.Core
- 职责：ActivityWatch 桶上传过滤与类型标签：仅允许 currentwindow/afkstatus/web.tab.current，排除 input 相关。
- 主要依赖：无外部类型
- 被谁使用：`AwCollectorService` 等采集上传路径

## 函数级结构化伪代码

### AwBucketSelection
#### 静态字段 SupportedTypes
- 输入：无
- 输出：Ordinal 比较的 HashSet：`currentwindow`、`afkstatus`、`web.tab.current`
- 副作用：无
- 步骤：初始化集合
- 分支与异常：无
- 调用：无

#### static bool IsSupportedUploadBucket(bucketId, bucketType, client)
- 输入：桶 ID、类型、client 名
- 输出：是否允许上传
- 副作用：无
- 步骤：
  1. type == `os.hid.input` → false
  2. client == `aw-watcher-input` → false
  3. bucketId 以 `aw-watcher-input_` 开头 → false
  4. 否则 `SupportedTypes.Contains(bucketType)`
- 分支与异常：无
- 调用：`string.Equals`/`StartsWith`/`Contains`

#### static string DescribeBucketKind(bucketType)
- 输入：桶类型字符串
- 输出：`window` | `afk` | `web` | `unknown`
- 副作用：无
- 步骤：switch 映射三种已知类型，默认 unknown
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. 支持类型集合：窗口/AFK/网页当前标签
2. IsSupportedUploadBucket：排除 HID 输入类型、input client、input 前缀桶 ID，再查支持类型
3. DescribeBucketKind：currentwindow→window，afkstatus→afk，web.tab.current→web，否则 unknown

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs",
      "label": "AwBucketSelection",
      "path": "src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs",
      "doc": "docs/pseudocode/files/src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs.md",
      "layer": "client-windows",
      "kind": "service"
    }
  ],
  "edges": [
    { "from": "src/client-windows/Pim.Client.Core/Services/AwCollectorService.cs", "to": "src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs", "type": "calls" }
  ]
}
```
