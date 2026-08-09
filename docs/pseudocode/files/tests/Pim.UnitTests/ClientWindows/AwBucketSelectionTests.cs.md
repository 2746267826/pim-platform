# tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs

## 元信息
- 语言：C#
- 程序集或包：Pim.UnitTests
- 职责：ActivityWatch 桶选择：支持 window/afk/web；排除 input；DescribeBucketKind 标签。
- 主要依赖：AwBucketSelection
- 被谁使用：dotnet test

## 函数级结构化伪代码

### IsSupportedUploadBucket_IncludesWindowAfkAndBrowserPages
### IsSupportedUploadBucket_ExcludesInputBuckets
### DescribeBucketKind_ReturnsStableLogLabels

## 近逐行中文伪代码

1. 三类支持桶 true
2. input 桶 false
3. kind 标签 window/afk/web/unknown

## 关系边
```json
{
  "nodes": [
    {
      "id": "tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs",
      "label": "AwBucketSelectionTests.cs",
      "path": "tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs",
      "doc": "docs/pseudocode/files/tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs.md",
      "layer": "tests",
      "kind": "test"
    }
  ],
  "edges": {"from":"tests/Pim.UnitTests/ClientWindows/AwBucketSelectionTests.cs","to":"src/client-windows/Pim.Client.Core/Services/AwBucketSelection.cs","type":"tests"}
}
```