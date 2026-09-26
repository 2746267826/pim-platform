# tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：活动记录稳定键：优先 AW bucket+eventId；合并网页排序；fallback 低稳定性且忽略分类字段。
- 主要依赖：`PcActivityRecordKeyService`、`PcDetailRecord`
- 被谁使用：xUnit

## 函数级结构化伪代码

### PcActivityRecordKeyServiceTests
#### Build_PrefersAwBucketAndSourceEventId()
- 输入：SourceBucketIds + SourceWindowEventIds
- 输出：无
- 副作用：无
- 步骤：RecordKey=`pc-aw-v1:bucket:id`；KeyVersion/SourceType/Stability=stable；JSON 序列化源 id
- 分支与异常：无
- 调用：`PcActivityRecordKeyService.Build`

#### Build_UsesSortedSourceIdsForMergedWebPage()
- 输入：web-page 多 SourceWebEventIds
- 输出：无
- 副作用：无
- 步骤：ids 排序拼接 7-8-9；stable
- 分支与异常：无
- 调用：同上

#### Build_FallsBackWithExplicitLowerStability()
- 输入：无 bucket/event 源
- 输出：无
- 副作用：无
- 步骤：`pc-fallback-v1:` 前缀；SourceType=fallback；Stability=low
- 分支与异常：无
- 调用：同上

#### Build_FallbackIgnoresClassificationFields()
- 输入：仅 Category/Explanation 不同
- 输出：无
- 副作用：无
- 步骤：两键相等
- 分支与异常：无
- 调用：同上

#### Build_FallbackStillDistinguishesSourceEventIdsWhenBucketIsMissing()
- 输入：仅不同 window event id
- 输出：无
- 副作用：无
- 步骤：键不同；仍 fallback/low；EventIdsJson 有值 Bucket 空数组
- 分支与异常：无
- 调用：同上

#### Build_FallbackStillDistinguishesSourceBucketIdsWhenEventIdIsMissing()
- 输入：仅不同 bucket
- 输出：无
- 副作用：无
- 步骤：键不同；EventIds 空 Bucket 有值
- 分支与异常：无
- 调用：同上

#### Build_WebPageFallbackUsesInterpretedPageIdentityNotBrowserShell()
- 输入：同 domain/path/title，不同 BrowserApp/Title
- 输出：无
- 副作用：无
- 步骤：键相同（页面身份，非浏览器壳）
- 分支与异常：无
- 调用：同上

#### NewRecord()
- 输入：无
- 输出：默认 PcDetailRecord window
- 副作用：无
- 步骤：固定时间窗与 Code.exe
- 分支与异常：无
- 调用：无

## 近逐行中文伪代码

1. AW 稳定键优先
2. 合并网页 id 排序
3. 无源 fallback low
4. 分类字段不进键
5. 缺 bucket 或 event 仍可区分
6. 网页键用页面身份
7. NewRecord 基线

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs",
      "label": "PcActivityRecordKeyServiceTests",
      "path": "tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": [
    { "from": "tests/Pim.UnitTests/Services/PcActivityRecordKeyServiceTests.cs", "to": "src/modules/Pim.Module.PcTracker/Services/PcActivityRecordKeyService.cs", "type": "tests" }
  ]
}
```
